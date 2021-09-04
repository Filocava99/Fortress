package it.ancientrealms.listener

import it.ancientrealms.Fortress
import it.ancientrealms.utils.Utils
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.collections.HashMap

class PlayerListener() : Listener {

    private val plugin = Fortress.INSTANCE
    private val tasks = HashMap<UUID,BukkitTask>()

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent){
        val player = event.player
        val playerNewLocation = event.to!!
        val playerOldLocation = event.from
        if(playerOldLocation.chunk != playerNewLocation.chunk){
            val fortressesManager = plugin.fortressesManager
            Utils.getFortressFromChunk(playerOldLocation.chunk)?.let {
                if(fortressesManager.isBesieged(it)){
                    val task = tasks.remove(player.uniqueId)
                    task?.cancel()
                    tasks[player.uniqueId] = Bukkit.getScheduler().runTaskLaterAsynchronously(Fortress.INSTANCE, Runnable {
                        if(player.location.chunk != it.chunk){
                            fortressesManager.removeParticipant(player.uniqueId, it)
                        }
                    }, plugin.pluginConfig.config.getLong("delay-before-siege-kick"))
                }
            }
            Utils.getFortressFromChunk(playerNewLocation.chunk)?.let {
                if(fortressesManager.isBesieged(it)){
                    if(fortressesManager.canPlayerSiege(player, it)) {
                        fortressesManager.addParticipant(player.uniqueId, it)
                    }
                }else if(it.canBeBesieged()){
                    if(fortressesManager.canPlayerSiege(player, it)){
                        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
                            if(playerNewLocation.chunk == player.location.chunk){
                                plugin.fortressesManager.startSiege(it, player)
                            }
                        }, plugin.pluginConfig.config.getLong("delay-before-siege-start"))
                    }
                }
            }
        }
    }

}