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
import java.util.stream.Collectors

data class Fortress(
    val name: String,
    var owner: Government?,
    val chunk: Chunk,
    var besiegeHour: Int,
    var besiegeDays: Set<Int>,
    var besiegePeriod: Int,
    var lastTimeBesieged: String,
    val townUpkeepDiscount: Float = 0f,
    val nationUpkeepDiscount: Float = 0f,
    val onlyCapitalGetsBonus: Boolean = false
) : ConfigurationSerializable{

    fun canBeBesieged() : Boolean {
        val timeZone = ZonedDateTime.now(TimeZone.getTimeZone("Europe/Rome").toZoneId())
        val currentHour = timeZone.hour
        val lastTimeBesiegedInstant = SimpleDateFormat("yyyy-MM-dd").parse(lastTimeBesieged).toInstant()
        val currentDay = timeZone.dayOfWeek.value
        return besiegeDays.contains(currentDay) && besiegeHour >= currentHour && besiegeHour < currentHour+besiegePeriod && Instant.now().minusMillis(lastTimeBesiegedInstant.toEpochMilli()).toEpochMilli() / 1000 >= 86400
    }

    override fun serialize(): MutableMap<String, Any> {
        val data: MutableMap<String, Any> = HashMap()
        data["name"] = name
        data["owner-type"] = if( owner == null) "" else if(owner is Town) "town" else "nation"
        data["owner-uuid"] = if(owner != null) owner!!.uuid.toString() else ""
        data["besiege-hour"] = besiegeHour
        data["besiege-days"] = besiegeDays.toList()
        data["besiege-period"] = besiegePeriod
        data["last-siege"] = lastTimeBesieged
        data["chunk"] = mapOf(Pair("x",chunk.x),Pair("z", chunk.z), Pair("world",chunk.world.uid.toString()))
        data["town-upkeep-discount"] = townUpkeepDiscount
        data["nation-upkeep-discount"] = nationUpkeepDiscount
        data["only-capital-gets-bonus"] = onlyCapitalGetsBonus
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
            val besiegeDays: Set<Int> = (args["besiege-days"] as List<*>).map { day -> Integer.parseInt(day as String) }.toSet()
            val besiegePeriod: Int = args["besiege-period"] as Int
            val lastTimeBesieged: String = args["last-siege"] as String
            val world = Bukkit.getServer().getWorld(UUID.fromString((args["chunk"]as Map<*, *>)["world"] as String))!!
            val chunk = world.getChunkAt((args["chunk"]as Map<*, *>)["x"] as Int, (args["chunk"]as Map<*, *>)["z"] as Int)
            val townUpkeep = args["town-upkeep-discount"] as Float
            val nationUpkeep = args["nation-upkeep-discount"] as Float
            val onlyCapitalBonus = args["only-capital-gets-bonus"] as Boolean
            return Fortress(name, owner, chunk, besiegeHour, besiegeDays, besiegePeriod, lastTimeBesieged, townUpkeep, nationUpkeep, onlyCapitalBonus)
        }
    }
}
