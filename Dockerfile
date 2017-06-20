FROM ubuntu

RUN apt-get -y update && apt-get install -y build-essential git wget unzip python vim
RUN git clone https://github.com/xetorthio/jedis.git
RUN git clone https://github.com/RedisLabs/jedis-ml.git
RUN git clone https://github.com/RedisLabs/spark-redis-ml.git
RUN git clone https://github.com/shaynativ/spark.git

RUN apt-get install -y maven default-jdk 

RUN cd jedis && mvn package -Dmaven.test.skip=true

RUN cd jedis-ml && mkdir lib &&  cp ../jedis/target/jedis-3.0.0-SNAPSHOT.jar lib/ && mvn install 

RUN echo "deb http://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
RUN apt-get -y update
RUN apt-get install -y sbt

RUN cd spark && mvn clean package -DskipTests=true

WORKDIR /spark-redis-ml

RUN mkdir lib &&\
cp /spark/mllib/target/spark-mllib_2.11-2.2.0-SNAPSHOT.jar lib/ &&\
cp ../jedis/target/jedis-3.0.0-SNAPSHOT.jar lib/ &&\
cp ../jedis-ml/target/jedis-ml-1.0-SNAPSHOT.jar lib/

RUN sbt assembly

WORKDIR /spark-redis-ml/forest-example
RUN mkdir lib && cp ../lib/* lib/
RUN  cp ../target/scala-2.11/spark-redis-ml-assembly-0.1.0.jar lib/
RUN git pull
RUN sbt package

WORKDIR /

RUN wget http://files.grouplens.org/datasets/movielens/ml-100k.zip &&\
unzip ml-100k.zip &&\
cp spark-redis-ml/scripts/gen_data.py ml-100k/ &&\
mkdir ml-100k/out &&\
cd ml-100k && ./gen_data.py &&\
/bin/sh -c 'for i in `seq 1 20`; do cp /ml-100k/out/$i /spark/data/mllib/; done' &&\
rm  /ml-100k.zip && rm -rf /ml-100k

WORKDIR /spark-redis-ml/forest-example
CMD ["/spark/bin/spark-submit", "--master", "local[*]", "--jars", "lib/jedis-ml-1.0-SNAPSHOT.jar,lib/jedis-3.0.0-SNAPSHOT.jar,lib/spark-redis-ml-assembly-0.1.0.jar,lib/spark-mllib_2.11-2.2.0-SNAPSHOT.jar", "./target/scala-2.11/forestexample_2.11-0.1.0.jar", "/spark/data/mllib/10", "20"]


