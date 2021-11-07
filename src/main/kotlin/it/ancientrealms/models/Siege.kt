package it.ancientrealms.models

import com.palmergames.bukkit.towny.`object`.Town
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.collections.HashSet

data class Siege(
    val fortress: Fortress,
    val attacker: Town,
    val siegeStarter: UUID,
    val participants: MutableSet<UUID>,
    var endSiegeTask: BukkitTask,
    var timerTask: BukkitTask?,
    val deadParticipants: MutableSet<UUID> = HashSet()
)
