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

data class Fortress(
    val name: String,
    var owner: Town?,
    val chunks: Set<Chunk>,
    var besiegeHour: Int,
    var besiegeDays: Set<Int>,
    var besiegePeriod: Int,
    var lastTimeBesieged: String,
    val townUpkeepDiscount: Float = 0f,
    val nationUpkeepDiscount: Float = 0f,
    val onlyCapitalGetsBonus: Boolean = false,
    val dailyMoneyBonus: Int = 0,
    val radius: Int = 0,
    var itemsReward: MutableList<ItemStack> = mutableListOf(
        ItemStack(
            Material.BIRCH_PLANKS,
            32
        ).apply {
            itemMeta?.lore = listOf(
                "${ChatColor.RED}Example 1",
                "${ChatColor.GREEN}Example 2"
            ); itemMeta?.setDisplayName("${ChatColor.DARK_PURPLE}Example item")
        }),
    val everybodyGetReward: Boolean = false,
    val onlySiegeStarterGetsReward: Boolean = false
) : ConfigurationSerializable {

    fun canBeBesieged(): Boolean {
        val timeZone = ZonedDateTime.now(TimeZone.getTimeZone("Europe/Rome").toZoneId())
        val currentHour = timeZone.hour
        val lastTimeBesiegedInstant = SimpleDateFormat("yyyy-MM-dd").parse(lastTimeBesieged).toInstant()
        val currentDay = timeZone.dayOfWeek.value
        println(besiegeDays.contains(currentDay))
        println(besiegeHour >= currentHour)
        println(besiegeHour < currentHour + besiegePeriod)
        println(Instant.now().minusMillis(lastTimeBesiegedInstant.toEpochMilli()).toEpochMilli() / 1000 >= 86400)
        return besiegeDays.contains(currentDay) && besiegeHour >= currentHour && besiegeHour < currentHour + besiegePeriod && Instant.now()
            .minusMillis(lastTimeBesiegedInstant.toEpochMilli()).toEpochMilli() / 1000 >= 86400
    }

    override fun serialize(): MutableMap<String, Any> {
        val data: MutableMap<String, Any> = HashMap()
        data["name"] = name
        data["owner-uuid"] = if (owner != null) owner!!.getUUID().toString() else ""
        data["besiege-hour"] = besiegeHour
        data["besiege-days"] = besiegeDays.toList()
        data["besiege-period"] = besiegePeriod
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
        data["items-reward"] = itemsReward
        data["everybody-get-reward"] = everybodyGetReward
        data["only-siege-starter-gets-reward"] = onlySiegeStarterGetsReward
        return data
    }

    companion object {
        @JvmStatic
        fun deserialize(args: Map<String, Any>): Fortress {
            val owner =
                if (args["owner-uuid"] == "") null else TownyAPI.getInstance().dataSource.getTown(UUID.fromString(args["owner-uuid"] as String))
            val name: String = args["name"] as String
            val besiegeHour: Int = args["besiege-hour"] as Int
            val besiegeDays: Set<Int> = (args["besiege-days"] as List<*>).map { day -> day as Int }.toSet()
            val besiegePeriod: Int = args["besiege-period"] as Int
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
            val tempList = args["items-reward"] as List<*>
            val itemsReward =
                tempList.filterIsInstance<ItemStack>().takeIf { it.size == tempList.size }?.toMutableList()
                    ?: LinkedList<ItemStack>()
            val everybodyGetReward = args["everybody-get-reward"] as Boolean
            val onlySiegeStarterGetsReward = args["only-siege-starter-gets-reward"] as Boolean
            return Fortress(
                name,
                owner,
                chunks,
                besiegeHour,
                besiegeDays,
                besiegePeriod,
                lastTimeBesieged,
                townUpkeep,
                nationUpkeep,
                onlyCapitalBonus,
                dailyMoneyBonus,
                radius,
                itemsReward,
                everybodyGetReward,
                onlySiegeStarterGetsReward
            )
        }
    }
}
