#!/usr/bin/python

import operator
import redis
config = {"host":"localhost", "port":6379}
r = redis.StrictRedis(**config)

user_profile = r.get("user-1-profile")

results = {}

for i in range(1, 11):
    results[i] = r.execute_command("ML.FOREST.RUN", "movie-{}".format(i), user_profile)

print "Movies sorted by scores:"
sorted_results = sorted(results.items(), key=operator.itemgetter(1), reverse=True)
for k,v in sorted_results:
    print "movie-{}:{}".format(k,v)

print ""
print "Recommended movie: movie-{}".format(sorted_results[0][0])

