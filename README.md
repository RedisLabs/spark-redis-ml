# jedis-ml

### Add  [Redis-ML](https://github.com/RedisLabsModules/redis-ml "Redis-ML") commands to  [Jedis](https://github.com/xetorthio/jedis "Jedis")  


## Installation:

```sh
#get and build jedis
git clone https://github.com/xetorthio/jedis.git
cd jedis
mvn package -Dmaven.test.skip=true

#get and build jedis-ml
cd..
git clone https://github.com/RedisLabs/jedis-ml.git
cd jedis-ml
cp ../jedis/target/jedis-3.0.0-SNAPSHOT.jar lib/
mvn install 
```



### Usage:

add jedis-ml/target/jedis-ml-1.0-SNAPSHOT.jar to the classpath.

Scala example (create a forest):

```scala
import com.redislabs.client.redisml.MLClient
import redis.clients.jedis.{Jedis, _}
val jedis = new Jedis("localhost")
val cmdArr = Array("my_forest","0",".","numeric","1","0.7")
jedis.getClient.sendCommand(MLClient.ModuleCommand.FOREST_ADD, cmdArr: _*)
jedis.getClient().getStatusCodeReply
```



### Supported Commands:

 FOREST_ADD 
 FOREST_RUN 
 FOREST_TEST 
 LINREG_SET 
 LINREG_PREDICT 
 LOGREG_SET 
 LOGREG_PREDICT
 MATRIX_SET 
 MATRIX_MULTIPLY 
 MATRIX_ADD 
 MATRIX_SCALE
 MATRIX_PRINT 
 MATRIX_TEST 