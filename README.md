# Spark-Redis-ML

### A spark package for loading Spark ML models to  [Redis-ML](https://github.com/RedisLabsModules/redis-ml "Redis-ML")   

## Requirments: 

Apache [Spark](https://github.com/apache/spark) 2.0 or later

[Redis](https://github.com/antirez/redis) build from unstable branch

[Jedis](https://github.com/xetorthio/jedis)

[Jedis-ml](https://github.com/RedisLabsModules/jedis-ml)

## Installation:

```sh
#get and build redis-ml
git clone https://github.com/RedisLabsModules/redis-ml.git
cd redis-ml/src
make 

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

#get and build spark-jedis-ml
cd.. git clone https://github.com/RedisLabs/spark-redis-ml.git
cd spark-redis-ml
cp ../jedis/target/jedis-3.0.0-SNAPSHOT.jar lib/
cp ../jedis-ml/target/jedis-ml-1.0-SNAPSHOT.jar lib/
sbt assembly
```



### Usage:

Run Redis server with redis-ml module:

```sh
/path/to/redis-server --loadmodule ./redis-ml.so
```



From Spark root directory, Run Spark shell with the required jars:

```sh
 ./bin/spark-shell --jars ../spark-redis-ml/target/scala-2.11/spark-redis-ml_2.11-0.3.2.jar,../jedis/target/jedis-3.0.0-SNAPSHOT.jar,../jedis-ml/target/jedis-ml-1.0-SNAPSHOT.jar 
```



On Spark shell:

```sh
scala> :load "../spark-redis-ml/scripts/forest-example.scala"
```



### 
