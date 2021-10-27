package it.ancientrealms.models

import com.palmergames.bukkit.towny.`object`.Town
import org.bukkit.scheduler.BukkitTask
import java.util.*

data class Siege(
    val fortress: Fortress,
    val attacker: Town,
    val participants: MutableSet<UUID>,
    var timerTask: BukkitTask
)
