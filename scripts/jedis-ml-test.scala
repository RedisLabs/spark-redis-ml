import com.redislabs.client.redisml.MLClient
import redis.clients.jedis.{Jedis, _}
val jedis = new Jedis("localhost")
val cmdArr = Array("fff5","0",".","numeric","1","777")
jedis.getClient.sendCommand(MLClient.ModuleCommand.FOREST_ADD, cmdArr: _*)
jedis.getClient().getStatusCodeReply

