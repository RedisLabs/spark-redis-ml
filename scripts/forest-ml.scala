import scala.collection.mutable
import scala.language.reflectiveCalls
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.feature.{StringIndexer, VectorIndexer}
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.ml.regression.{RandomForestRegressionModel, RandomForestRegressor}
import org.apache.spark.ml.tree.{CategoricalSplit, ContinuousSplit, Split}
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql.{SparkSession, _}
import redis.clients.jedis.Protocol.Command
import redis.clients.jedis.{Jedis, _}
import com.redislabs.client.redisml.MLClient
import com.redislabs.provider.redis.ml.Forest

/** Load a dataset from the given path, using the given format */
def loadData(
              spark: SparkSession,
              path: String,
              format: String,
              expectedNumFeatures: Option[Int] = None): DataFrame = {
  import spark.implicits._

  format match {
    case "dense" => MLUtils.loadLabeledPoints(spark.sparkContext, path).toDF()
    case "libsvm" => expectedNumFeatures match {
      case Some(numFeatures) => spark.read.option("numFeatures", numFeatures.toString)
        .format("libsvm").load(path)
      case None => spark.read.format("libsvm").load(path)
    }
    case _ => throw new IllegalArgumentException(s"Bad data format: $format")
  }
}

def loadDatasets(
                  input: String,
                  dataFormat: String,
                  testInput: String,
                  algo: String,
                  fracTest: Double): (DataFrame, DataFrame) = {
  val spark = SparkSession
    .builder
    .getOrCreate()

  // Load training data
  val origExamples: DataFrame = loadData(spark, input, dataFormat)

  // Load or create test set
  val dataframes: Array[DataFrame] = if (testInput != "") {
    // Load testInput.
    val numFeatures = origExamples.first().getAs[Vector](1).size
    val origTestExamples: DataFrame =
      loadData(spark, testInput, dataFormat, Some(numFeatures))
    Array(origExamples, origTestExamples)
  } else {
    // Split input into training, test.
    origExamples.randomSplit(Array(1.0 - fracTest, fracTest), seed = 12345)
  }

  val training = dataframes(0).cache()
  val test = dataframes(1).cache()

  val numTraining = training.count()
  val numTest = test.count()
  val numFeatures = training.select("features").first().getAs[Vector](0).size
  println("Loaded data:")
  println(s"  numTraining = $numTraining, numTest = $numTest")
  println(s"  numFeatures = $numFeatures")

  (training, test)
}

case class Params(
                   input: String = "data/mllib/sample_libsvm_data.txt",
                   testInput: String = "",
                   dataFormat: String = "libsvm",
                   algo: String = "classification",
                   maxDepth: Int = 5,
                   maxBins: Int = 32,
                   minInstancesPerNode: Int = 1,
                   minInfoGain: Double = 0.0,
                   numTrees: Int = 10,
                   featureSubsetStrategy: String = "auto",
                   fracTest: Double = 0.2,
                   cacheNodeIds: Boolean = false,
                   checkpointDir: Option[String] = None,
                   checkpointInterval: Int = 10
                 )

val params = Params()

sc.setLogLevel("WARN")
params.checkpointDir.foreach(sc.setCheckpointDir)
val algo = params.algo.toLowerCase

println(s"RandomForestExample with parameters:\n$params")

// Load training and test data and cache it.
val (training: DataFrame, test: DataFrame) = loadDatasets(params.input,
  params.dataFormat, params.testInput, algo, params.fracTest)

// Set up Pipeline.
val stages = new mutable.ArrayBuffer[PipelineStage]()
// (1) For classification, re-index classes.
val labelColName = if (algo == "classification") "indexedLabel" else "label"
if (algo == "classification") {
  val labelIndexer = new StringIndexer().setInputCol("label").setOutputCol(labelColName)
  stages += labelIndexer
}
// (2) Identify categorical features using VectorIndexer.
//     Features with more than maxCategories values will be treated as continuous.
val featuresIndexer = new VectorIndexer().setInputCol("features").setOutputCol("indexedFeatures").setMaxCategories(10)
stages += featuresIndexer
// (3) Learn Random Forest.
val dt = new RandomForestClassifier().
  setFeaturesCol("indexedFeatures").
  setLabelCol(labelColName).
  setMaxDepth(params.maxDepth).
  setMaxBins(params.maxBins).
  setMinInstancesPerNode(params.minInstancesPerNode).
  setMinInfoGain(params.minInfoGain).
  setCacheNodeIds(params.cacheNodeIds).
  setCheckpointInterval(params.checkpointInterval).
  setFeatureSubsetStrategy(params.featureSubsetStrategy).
  setNumTrees(params.numTrees)

stages += dt
val pipeline = new org.apache.spark.ml.Pipeline().setStages(stages.toArray)

// Fit the Pipeline.
val startTime = System.nanoTime()
val pipelineModel = pipeline.fit(training)
val elapsedTime = (System.nanoTime() - startTime) / 1e9
println(s"Training time: $elapsedTime seconds")

val rfModel = pipelineModel.stages.last.asInstanceOf[RandomForestClassificationModel]
if (rfModel.totalNumNodes < 30) {
  println(rfModel.toDebugString) // Print full model.
} else {
  println(rfModel) // Print model summary.
}

val f = new Forest(rfModel.trees)
f.loadToRedis("forest-test", "localhost")

val localData = test.collect

def makeInputString(i: Int): String = {
  val sparseRecord = localData(i)(1).asInstanceOf[org.apache.spark.ml.linalg.SparseVector]
  val indices = sparseRecord.indices
  val values = sparseRecord.values
  var sep = ""
  var inputStr = ""
  for (i <- 0 to ((indices.length - 1))) {
    inputStr = inputStr + sep + indices(i).toString + ":" + values(i).toString
    sep = ","
  }
  inputStr
}

var redisRes = ""
var sparkRes = 0.0
var rtotal = 0.0
var stotal = 0.0

def benchmark(b: Int) {
  val jedis = new Jedis("localhost")
  for (i <- 0 to b) {
    val rt0 = System.nanoTime()
    jedis.getClient.sendCommand(MLClient.ModuleCommand.FOREST_RUN, "forest-test", makeInputString(i))
//    print("forest-test", makeInputString(i))
    redisRes = jedis.getClient().getStatusCodeReply
    val rt1 = System.nanoTime()
    println("Redis time: " + (rt1 - rt0) / 1000000.0 + "ms, res=" + redisRes)

//    val st0 = System.nanoTime()
//    sparkRes = rfModel.predict(localData(i).features)
//    val st1 = System.nanoTime()
//    println("Spark time: " + (st1 - st0) / 1000000.0 + "ms, res=" + sparkRes.toInt)
    println("---------------------------------------");
    rtotal += (rt1 - rt0) / 1000000.0
//    stotal += (st1 - st0) / 1000000.0
  }
  println("Classification averages:")
  println("redis:" + rtotal/b.toFloat + "ms")
//  println("spark:" + stotal/b.toFloat+ "ms")
}

println("*******************************************")

//println("Load model to Redis:")
//time(loadToRedis())
//println("*******************************************")
//println("Benchmark classification:")
//benchmark(20)
//// Save and load model
//println("*******************************************")
//println("Spark save model:")
//time {
//  model.save(sc, "target/tmp/myRandomForestClassificationModel")
//}
//
//println("*******************************************")
//println("Spark load model:")
//time {
//  val sameModel = RandomForestModel.load(sc, "target/tmp/myRandomForestClassificationModel")
//}
//
