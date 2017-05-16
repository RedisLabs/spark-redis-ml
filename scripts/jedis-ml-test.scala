import org.apache.spark.ml.regression.LinearRegression
import com.redislabs.client.redisml.MLClient
import redis.clients.jedis.{Jedis, _}

// Load training data and train
val training = spark.read.format("libsvm").load("data/mllib/sample_linear_regression_data.txt")
val lr = new LinearRegression().setMaxIter(10).setRegParam(0.3).setElasticNetParam(0.8)
val lrModel = lr.fit(training)
println(s"Coefficients: ${lrModel.coefficients} Intercept: ${lrModel.intercept}")

// Connect to Redis
val jedis = new Jedis("localhost")

// Load model to Redis
val cmd = "my_lr_model" +: lrModel.intercept.toString +: lrModel.coefficients.toArray.mkString(",").split(",")
jedis.getClient.sendCommand(MLClient.ModuleCommand.LINREG_SET, cmd: _*)
jedis.getClient().getStatusCodeReply

// Perform prediction with Redis
val cmd = Array("my_lr_model", "1", "2", "5")
jedis.getClient.sendCommand(MLClient.ModuleCommand.LINREG_PREDICT, cmd: _*)
jedis.getClient().getStatusCodeReply

