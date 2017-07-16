import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vectors
import com.redislabs.client.redisml.MLClient
import redis.clients.jedis.{Jedis, _}

// Load and parse the data
val data = sc.textFile("data/mllib/kmeans_data.txt")
val parsedData = data.map(s => Vectors.dense(s.split(' ').map(_.toDouble))).cache()

// Cluster the data into two classes using KMeans
val numClusters = 4
val numIterations = 20
val clusters = KMeans.train(parsedData, numClusters, numIterations)

val cmd = "my_km_model" +: numClusters.toString +: "3" +: clusters.clusterCenters.map(x => x.toArray).flatten.mkString(",").split(",")
val jedis = new Jedis("localhost")
jedis.getClient.sendCommand(MLClient.ModuleCommand.KMEANS_SET, cmd: _*)
jedis.getClient().getStatusCodeReply


var redisRes = 0L
var sparkRes = 0.0
var rtotal = 0.0
var stotal = 0.0
var diffs = 0.0

def benchmark(b: Int) {
  rtotal = 0.0
  stotal = 0.0
  diffs = 0.0
  val jedis = new Jedis("localhost")
  for (i <- 0 to b) {
    val rt0 = System.nanoTime()
    val cmd = Array("my_km_model", i.toString, i.toString, i.toString)
    jedis.getClient.sendCommand(MLClient.ModuleCommand.KMEANS_PREDICT, cmd: _*)
    redisRes = jedis.getClient().getIntegerReply
    val rt1 = System.nanoTime()
    println(cmd.mkString(", "))
    println("Redis time: " + (rt1 - rt0) / 1000000.0 + "ms, res=" + redisRes)
    val v = Vectors.dense(i, i, i)
    val p = Seq(v)
    val rdd = sc.parallelize(p,1)
    val st0 = System.nanoTime()
    val rawSparkRes = clusters.predict(rdd).collect
    val st1 = System.nanoTime()
    sparkRes = rawSparkRes(0)
    println("Spark time: " + (st1 - st0) / 1000000.0 + "ms, res=" + sparkRes)
    println("---------------------------------------");
    if (sparkRes - redisRes.toFloat != 0) {
      diffs += 1
    }
    rtotal += (rt1 - rt0) / 1000000.0
    stotal += (st1 - st0) / 1000000.0
  }
  println("Classification averages:")
  println(s"redis: ${rtotal / b.toFloat} ms")
  println(s"spark: ${stotal / b.toFloat} ms")
  println(s"ratio: ${stotal / rtotal}")
  println(s"diffs: $diffs")
}

benchmark(20)