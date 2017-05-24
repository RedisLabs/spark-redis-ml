#!/usr/bin/env bash

~/dev/spark/bin/spark-submit --master local[*] --jars lib/jedis-ml-1.0-SNAPSHOT.jar,lib/jedis-3.0.0-SNAPSHOT.jar,lib/spark-redis-ml-assembly-0.1.0.jar,lib/spark-mllib_2.11-2.2.0-SNAPSHOT.jar ./target/scala-2.11/forestexample_2.11-0.1.0.jar data/$1 $2

#for f in {1..10}; do ./run_spark_demo.sh $f 30; done