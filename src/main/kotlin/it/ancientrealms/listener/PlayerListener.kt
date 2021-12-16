package it.ancientrealms.listener

import it.ancientrealms.Fortress
import it.ancientrealms.utils.Utils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitTask
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.temporal.TemporalAccessor
import java.util.*
import kotlin.collections.HashMap
import kotlin.contracts.Returns

class PlayerListener : Listener {

    private val plugin = Fortress.INSTANCE
    private val tasks = HashMap<UUID, Pair<UUID, BukkitTask>>()
    private val townsWithRunningTask = HashSet<UUID>()
    private val languageManager = Fortress.INSTANCE.languageManager
    private val kickTasks = HashMap<UUID, BukkitTask>()

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val playerNewLocation = event.to!!
        val playerOldLocation = event.from
        if (playerOldLocation.chunk != playerNewLocation.chunk && Utils.getFortressFromChunk(playerOldLocation.chunk) != Utils.getFortressFromChunk(
                playerNewLocation.chunk
            )
        ) {
            val fortressesManager = plugin.fortressesManager
            Utils.getFortressFromChunk(playerOldLocation.chunk)?.let {
                if (fortressesManager.isBesieged(it)) {
                    Utils.getResident(player)?.let { it1 -> Utils.getTown(it1) }
                    val task = kickTasks.remove(player.uniqueId)
                    task?.cancel()
                    kickTasks[player.uniqueId] =
                        Bukkit.getScheduler().runTaskLaterAsynchronously(Fortress.INSTANCE, Runnable {
                            if (!it.chunks.contains(player.location.chunk)) {
                                fortressesManager.removeParticipant(player.uniqueId, it)
                            }
                        }, plugin.pluginConfig.config.getLong("delay-before-siege-kick"))
                } else {
                    tasks.remove(player.uniqueId)?.let { pair ->
                        townsWithRunningTask.remove(pair.first)
                        pair.second.cancel()
                    }
                }
            }
            Utils.getFortressFromChunk(playerNewLocation.chunk)?.let {
                if (fortressesManager.isBesieged(it)) {
                    if (fortressesManager.canPlayerSiege(player, it)) {
                        fortressesManager.addParticipant(player.uniqueId, it)
                    } else {
                        val siege = fortressesManager.getSiege(it)
                        player.sendTitle(
                            languageManager.getMessage("fortress-under-siege-title", it.name),
                            languageManager.getMessage(
                                "fortress-under-siege-subtitle",
                                siege?.attacker?.name ?: "",
                                it.owner?.name ?: ""
                            ),
                            20,
                            40,
                            20
                        )
                    }
                } else if (it.canBeBesieged()) {
                    if(it.owner != null){
                        player.sendTitle(
                            languageManager.getMessage("fortress-besiegable-title", it.name),
                            languageManager.getMessage(
                                "fortress-besiegable-subtitle"
                            , it.owner?.name ?: ""), 20, 40, 20
                        )
                    }else{
                        player.sendTitle(
                            languageManager.getMessage("unconquered-fortress-besiegable-title", it.name),
                            languageManager.getMessage(
                                "unconquered-fortress-besiegable-subtitle"
                            ), 20, 40, 20
                        )
                    }
                    if (fortressesManager.canPlayerSiege(player, it)) {
                        Utils.getResident(player)?.let { it1 ->
                            Utils.getTown(it1)?.let { town ->
                                if (!townsWithRunningTask.contains(town.getUUID())) {
                                    tasks[player.uniqueId] = Pair(
                                        town.getUUID(),
                                        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
                                            if (playerNewLocation.chunk == player.location.chunk) {
                                                plugin.fortressesManager.startSiege(it, player)
                                            }
                                        }, plugin.pluginConfig.config.getLong("delay-before-siege-start"))
                                    )
                                    townsWithRunningTask.add(town.getUUID())
                                }
                            }
                        }
                    }
                } else {
                    if (it.owner == null) {
                        player.sendTitle(
                            languageManager.getMessage("unconquered-fortress-not-besiegable-title", it.name),
                            languageManager.getMessage(
                                "unconquered-fortress-not-besiegable-subtitle", it.owner?.name ?: ""
                            ), 20, 40, 20
                        )
                    } else {
                        player.sendTitle(
                            languageManager.getMessage("fortress-not-besiegable-title", it.name),
                            languageManager.getMessage(
                                "fortress-not-besiegable-subtitle", it.owner?.name ?: ""
                            ), 20, 40, 20
                        )
                    }
                    val lastTimeBesiegedInstant = SimpleDateFormat("yyyy-MM-dd-H:m").parse(it.lastTimeBesieged).toInstant()
                    val date = Date.from(Instant.ofEpochMilli(Instant.now().toEpochMilli() - (lastTimeBesiegedInstant.toEpochMilli()-it.besiegeInterval*1000)))
                    if(it.ignoreBesiegeHour){
                        player.sendMessage(languageManager.getMessage("besiegable-in-interval", date.hours.toString(), date.minutes.toString()))
                    }else{
                        val weekdays = DateFormatSymbols.getInstance(Locale(plugin.pluginConfig.config.getString("language"))).weekdays
                        var days = ""
                        it.besiegeDays.forEachIndexed{ index, day ->
                            days += weekdays[index]
                            if(it.besiegeDays.size > 1 && index+1 < it.besiegeDays.size){
                                days += ", "
                            }
                        }
                        player.sendMessage(languageManager.getMessage("besiegable-during", days, "${it.besiegeHour}:00", "${it.besiegeHour+it.besiegePeriod}:00"))
                    }
                }
            }
        }
    }

    @EventHandler
    fun onBlockPlaced(event: BlockPlaceEvent) {
        cancelInteractionEvent(event, event.player)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        cancelInteractionEvent(event, event.player)
    }

    private fun cancelInteractionEvent(event: Cancellable, player: Player) {
        if (player.isOp) return
        if (player.hasPermission("fortress.interact")) return
        if (Utils.getFortressFromChunk(player.location.chunk) != null) {
            event.isCancelled = true
            if (Fortress.INSTANCE.pluginConfig.config.getBoolean("notify-blocked-interaction")) {
                player.sendMessage(languageManager.getMessage("cant-interact-inside-fortress"))
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val location = player.location
        Utils.getFortressFromChunk(location.chunk)?.let { fortress ->
            val fortressesManager = Fortress.INSTANCE.fortressesManager
            if (fortressesManager.isBesieged(fortress)) {
                fortressesManager.removeParticipant(player.uniqueId, fortress)
                fortressesManager.addDeadParticipant(player.uniqueId, fortress)
            }
        }
    }

}