package it.ancientrealms.listener

import it.ancientrealms.Fortress
import it.ancientrealms.utils.Utils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.collections.HashMap
import kotlin.contracts.Returns

class PlayerListener : Listener {

    private val plugin = Fortress.INSTANCE
    private val tasks = HashMap<UUID, BukkitTask>()
    private val languageManager = Fortress.INSTANCE.languageManager

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
                    val task = tasks.remove(player.uniqueId)
                    task?.cancel()
                    tasks[player.uniqueId] =
                        Bukkit.getScheduler().runTaskLaterAsynchronously(Fortress.INSTANCE, Runnable {
                            if (!it.chunks.contains(player.location.chunk)) {
                                fortressesManager.removeParticipant(player.uniqueId, it)
                            }
                        }, plugin.pluginConfig.config.getLong("delay-before-siege-kick"))
                }
            }
            Utils.getFortressFromChunk(playerNewLocation.chunk)?.let {
                if (fortressesManager.isBesieged(it)) {
                    if (fortressesManager.canPlayerSiege(player, it)) {
                        fortressesManager.addParticipant(player.uniqueId, it)
                    }
                } else if (it.canBeBesieged()) {
                    if (fortressesManager.canPlayerSiege(player, it)) {
                        player.sendTitle(
                            languageManager.getMessage("fortress-besiegable-title", it.name),
                            languageManager.getMessage(
                                "fortress-besiegable-subtitle"
                            ), 20, 40, 20
                        )
                        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
                            if (playerNewLocation.chunk == player.location.chunk) {
                                plugin.fortressesManager.startSiege(it, player)
                            }
                        }, plugin.pluginConfig.config.getLong("delay-before-siege-start"))
                    }
                } else {
                    if(it.owner == null){
                        player.sendTitle(
                            languageManager.getMessage("unconquered-fortress-not-besiegable-title", it.name),
                            languageManager.getMessage(
                                "unconquered-fortress-not-besiegable-subtitle", it.owner?.name ?: ""
                            ), 20, 40, 20
                        )
                    }else{
                        player.sendTitle(
                            languageManager.getMessage("fortress-not-besiegable-title", it.name),
                            languageManager.getMessage(
                                "fortress-not-besiegable-subtitle", it.owner?.name ?: ""
                            ), 20, 40, 20
                        )
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent){
        if(event.clickedBlock == null || event.clickedBlock?.blockData?.material == Material.AIR) return
        if(event.player.isOp) return
        if(event.player.hasPermission("fortress.interact")) return
        if(Utils.getFortressFromChunk(event.player.location.chunk) != null){
            event.isCancelled = true
            if(Fortress.INSTANCE.pluginConfig.config.getBoolean("notify-blocked-interaction")){
                event.player.sendMessage(languageManager.getMessage("cant-interact-inside-fortress"))
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent){
        val player = event.entity
        val location = player.location
        Utils.getFortressFromChunk(location.chunk)?.let { fortress ->
            val fortressesManager = Fortress.INSTANCE.fortressesManager
            if(fortressesManager.isBesieged(fortress)){
                fortressesManager.removeParticipant(player.uniqueId, fortress)
                fortressesManager.addDeadParticipant(player.uniqueId, fortress)
            }
        }
    }

}