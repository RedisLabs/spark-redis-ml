#!/usr/bin/env python
from collections import defaultdict

OCCUPATION_FILE = "./u.occupation"
GENRE_FILE = "./u.genre"
ITEM_FILE = "./u.item"
USER_FILE = "./u.user"
RATINGS_FILE = "./u.data"

occupations_fo = open(OCCUPATION_FILE, "r")
genres_fo = open(GENRE_FILE, "r")
items_fo = open(ITEM_FILE, "r")
users_fo = open(USER_FILE, "r")
ratings_fo = open(RATINGS_FILE, "r")

gender_map = {'M':'0', 'F':'1'}
user_ratings_map = defaultdict(lambda: {})
item_raters = defaultdict(lambda: [])
user_genres_avg = defaultdict(lambda: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0])
user_rating_count = defaultdict(lambda: 0)

def main():
    occupations_map = dict((x[:-1],str(i+1)) for i,x in enumerate(occupations_fo.readlines()))
    occupations_fo.close()

    item_genres_map = dict((x.split('|')[0], x[:-1].split('|')[5:]) for x in items_fo.readlines())
    items_fo.close()

    # add user info to user_ratings_map. format: uid|age|gender|occupation|zip(excluded)
    for x in users_fo.readlines():
        x = x[:-1].split('|')
        uid = int(x[0])
        # 1700 = age, 1701 = gender, 1702 = occupation
        user_ratings_map[uid][1700] = x[1]
        user_ratings_map[uid][1701] = gender_map[x[2]]
        user_ratings_map[uid][1702] = occupations_map[x[3]]

    # add item ratings, count user events, and accumulate genres info. format: user,item,rating
    # item range is 1 to 1650
    for x in ratings_fo.readlines():
        x = x[:-1].split('\t')
        uid = int(x[0])
        item = int(x[1])
        rating = int(x[2])
        item_raters[int(x[1])].append(uid)
        user_rating_count[uid] = user_rating_count[uid] + 1
        user_ratings_map[uid][item] = rating
        user_genres_avg[uid] = [(int(x) * rating + user_genres_avg[uid][i]) for i,x in enumerate(item_genres_map[x[1]])]

    # calculate genre avg per user. 1800 = count, 1801 - 1819 = genres
    for uid, count in user_rating_count.iteritems():
        user_ratings_map[uid][1800] = count
        factor = count if count > 1 else 1
        for i,x in enumerate(user_genres_avg[uid]):
            user_ratings_map[uid][1801+i] = "{:.2f}".format(1.0*x/factor) 
        # print(user_ratings_map[uid])

    # generate rating file for each item
    for item, users in item_raters.iteritems():
        f = open("./out/{}".format(item),'w')
        line = ''
        for uid in users:
            map = user_ratings_map[uid]
            zero_val = map.pop(item) #not using NULL, all keys must be present.
            line = line + str(zero_val)
            for k,v in sorted(map.iteritems()): #libsvm requires sorting
                line = line + " {}:{}".format(k,v)
            line = line + "\n"
            f.write(line) 
        f.close()

if __name__ == "__main__":
    main()