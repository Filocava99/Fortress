package it.ancientrealms.models

import com.palmergames.bukkit.towny.TownyAPI
import com.palmergames.bukkit.towny.`object`.Government
import com.palmergames.bukkit.towny.`object`.Town
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.configuration.serialization.ConfigurationSerializable
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

data class Fortress(
    val name: String,
    var owner: Government?,
    val chunk: Chunk,
    var besiegeHour: Int,
    var lastTimeBesieged: String
) : ConfigurationSerializable{

    fun canBeBesieged() : Boolean {
        val currentHour = ZonedDateTime.now(TimeZone.getTimeZone("Europe/Rome").toZoneId()).hour
        val lastTimeBesiegedInstant = SimpleDateFormat("yyyy-MM-dd").parse(lastTimeBesieged).toInstant()
        return besiegeHour >= currentHour && besiegeHour < currentHour+1 && Instant.now().minusMillis(lastTimeBesiegedInstant.toEpochMilli()).toEpochMilli() / 1000 >= 86400
    }

    override fun serialize(): MutableMap<String, Any> {
        val data: MutableMap<String, Any> = HashMap()
        data["name"] = name
        data["owner-type"] = if( owner == null) "" else if(owner is Town) "town" else "nation"
        data["owner-uuid"] = if(owner != null) owner!!.uuid.toString() else ""
        data["besiege-hour"] = besiegeHour
        data["last-siege"] = lastTimeBesieged
        data["chunk"] = mapOf(Pair("x",chunk.x),Pair("z", chunk.z), Pair("world",chunk.world.uid.toString()))
        return data
    }

    companion object{
        @JvmStatic fun deserialize(args: Map<String, Any>) : Fortress {
            val owner: Government? = if((args["owner-type"] as String) == "nation"){
                TownyAPI.getInstance().dataSource.getNation(UUID.fromString(args["owner-uuid"] as String))
            }else if((args["owner-type"] as String) == "town"){
                TownyAPI.getInstance().dataSource.getTown(UUID.fromString(args["owner-uuid"] as String))
            }else{
                null
            }
            val name:String = args["name"] as String
            val besiegeHour: Int = args["besiege-hour"] as Int
            val lastTimeBesieged: String = args["last-siege"] as String
            val world = Bukkit.getServer().getWorld(UUID.fromString((args["chunk"]as Map<*, *>)["world"] as String))!!
            val chunk = world.getChunkAt((args["chunk"]as Map<*, *>)["x"] as Int, (args["chunk"]as Map<*, *>)["z"] as Int)
            return Fortress(name, owner, chunk, besiegeHour, lastTimeBesieged)
        }
    }
}
