package it.ancientrealms.models

import com.palmergames.bukkit.towny.TownyAPI
import com.palmergames.bukkit.towny.`object`.Town
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.inventory.ItemStack
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import kotlin.collections.HashMap

data class Fortress(
    val name: String,
    var owner: Town?,
    val chunks: Set<Chunk>,
    var ignoreBesiegeHour: Boolean = false,
    var besiegeHour: Int,
    var besiegeDays: Set<Int>,
    var besiegePeriod: Int,
    var besiegeInterval: Int = 86400,
    var lastTimeBesieged: String,
    val townUpkeepDiscount: Float = 0f,
    val nationUpkeepDiscount: Float = 0f,
    val onlyCapitalGetsBonus: Boolean = false,
    val dailyMoneyBonus: Int = 0,
    val radius: Int = 0,
    var onConquestCommands: MutableMap<CommandTarget, MutableList<String>> = mutableMapOf(Pair(CommandTarget.PARTICIPANTS, mutableListOf("give \${player} DIRT 1"))),
    var notOwnable: Boolean = false
) : ConfigurationSerializable {

    fun canBeBesieged(): Boolean {
        val timeZone = ZonedDateTime.now(TimeZone.getTimeZone("Europe/Rome").toZoneId())
        val currentHour = timeZone.hour
        val lastTimeBesiegedInstant = SimpleDateFormat("yyyy-MM-dd-H:m").parse(lastTimeBesieged).toInstant()
        val currentDay = timeZone.dayOfWeek.value
        val besiegeHourCheck = if(ignoreBesiegeHour){
            true
        }else{
            besiegeHour >= currentHour && besiegeHour < currentHour + besiegePeriod
        }
        /*
        println(besiegeDays.contains(currentDay))
        println(besiegeHourCheck)
        println(besiegeHour < currentHour + besiegePeriod)
        println(Instant.now().minusMillis(lastTimeBesiegedInstant.toEpochMilli()).toEpochMilli() / 1000 >= besiegeInterval)
        */
        return besiegeDays.contains(currentDay) && besiegeHourCheck && Instant.now()
            .minusMillis(lastTimeBesiegedInstant.toEpochMilli()).toEpochMilli() / 1000 >= besiegeInterval
    }

    override fun serialize(): MutableMap<String, Any> {
        val data: MutableMap<String, Any> = HashMap()
        data["name"] = name
        data["owner-uuid"] = if (owner != null) owner!!.getUUID().toString() else ""
        data["ignore-besiege-hour"] = ignoreBesiegeHour
        data["besiege-hour"] = besiegeHour
        data["besiege-days"] = besiegeDays.toList()
        data["besiege-period"] = besiegePeriod
        data["besiege-interval"] = besiegeInterval
        data["last-siege"] = lastTimeBesieged
        data["chunks"] = chunks.map { chunk ->
            mapOf(
                Pair("x", chunk.x),
                Pair("z", chunk.z),
                Pair("world", chunk.world.uid.toString())
            )
        }
        data["town-upkeep-discount"] = townUpkeepDiscount
        data["nation-upkeep-discount"] = nationUpkeepDiscount
        data["only-capital-gets-bonus"] = onlyCapitalGetsBonus
        data["daily-money-bonus"] = dailyMoneyBonus
        data["radius"] = radius
        val commands = HashMap<String, Any>()
        data["on-conquest-commands"] = commands
        onConquestCommands.keys.forEach { target ->
            commands[target.name] = onConquestCommands[target] ?: emptyList<String>()
        }
        data["not-ownable"] = notOwnable
        return data
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fortress

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object {
        @JvmStatic
        fun deserialize(args: Map<String, Any>): Fortress {
            val owner =
                if (args["owner-uuid"] == "") null else TownyAPI.getInstance().dataSource.getTown(UUID.fromString(args["owner-uuid"] as String))
            val name: String = args["name"] as String
            val ignoreBesiegeHour = args["ignore-besiege-hour"] as Boolean
            val besiegeHour: Int = args["besiege-hour"] as Int
            val besiegeDays: Set<Int> = (args["besiege-days"] as List<*>).map { day -> day as Int }.toSet()
            val besiegePeriod: Int = args["besiege-period"] as Int
            val besiegeInterval: Int = args["besiege-interval"] as Int
            val lastTimeBesieged: String = args["last-siege"] as String
            val chunks = (args["chunks"] as List<*>).filterIsInstance<Map<*, *>>().map { map ->
                Bukkit.getServer().getWorld(UUID.fromString(map["world"] as String))!!
                    .getChunkAt(map["x"] as Int, map["z"] as Int)
            }.toMutableSet()
            val townUpkeep = (args["town-upkeep-discount"] as Double).toFloat()
            val nationUpkeep = (args["nation-upkeep-discount"] as Double).toFloat()
            val onlyCapitalBonus = args["only-capital-gets-bonus"] as Boolean
            val dailyMoneyBonus = args["daily-money-bonus"] as Int
            val radius = args["radius"] as Int
            val onConquestCommands = HashMap<CommandTarget, MutableList<String>>()
            @Suppress("UNCHECKED_CAST")
            (args["on-conquest-commands"] as Map<String, List<String>>).entries.forEach { (target, commands) -> onConquestCommands[CommandTarget.valueOf(target)] = commands.toMutableList()}
            val notOwnable = args["not-ownable"] as Boolean
            return Fortress(
                name,
                owner,
                chunks,
                ignoreBesiegeHour,
                besiegeHour,
                besiegeDays,
                besiegePeriod,
                besiegeInterval,
                lastTimeBesieged,
                townUpkeep,
                nationUpkeep,
                onlyCapitalBonus,
                dailyMoneyBonus,
                radius,
                onConquestCommands,
                notOwnable
            )
        }
    }

}
