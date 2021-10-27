package it.ancientrealms.manager

import com.palmergames.bukkit.towny.TownyUniverse
import com.palmergames.bukkit.towny.`object`.Nation
import com.palmergames.bukkit.towny.`object`.Town
import it.ancientrealms.Fortress
import it.ancientrealms.exception.AlreadyOwnedFortress
import it.ancientrealms.models.Siege
import it.ancientrealms.utils.Utils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import it.ancientrealms.FortressModel

class FortressesManager {

    private val fortresses = HashSet<FortressModel>()
    private val fortressesByName = HashMap<String, FortressModel>()
    private val ongoingSieges = HashMap<String, Siege>()
    private val languageManager = Fortress.INSTANCE.languageManager

    fun getFortresses() = Collections.unmodifiableSet(fortresses)

    fun removeFortress(name: String) = fortresses.remove(fortressesByName.remove(name))

    fun getFortress(name: String) = fortressesByName[name]

    fun addFortress(name: String, fortress: FortressModel) {
        fortresses.add(fortress)
        fortressesByName[name] = fortress
    }

    fun startSiege(fortress: FortressModel, player: Player) {
        val resident = TownyUniverse.getInstance().getResident(player.name)
        resident?.let {
            val town = Utils.getTown(resident)
            town?.let {
                if (fortress.owner == town) {
                    throw AlreadyOwnedFortress()
                } else {
                    val task = Bukkit.getScheduler().runTaskLaterAsynchronously(Fortress.INSTANCE, Runnable {
                        fortress.owner = town
                        fortress.lastTimeBesieged = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(
                            ZonedDateTime.now(
                                TimeZone.getTimeZone(
                                    Fortress.INSTANCE.pluginConfig.config.getString("time-zone")
                                ).toZoneId()
                            )
                        );
                        Fortress.INSTANCE.fortressesConfig.config.set(fortress.name, fortress)
                        Fortress.INSTANCE.fortressesConfig.save()
                        Bukkit.getServer().onlinePlayers.forEach { player ->
                            player.sendMessage(
                                languageManager.getMessage(
                                    "successful-conquest-notification",
                                    town.name,
                                    fortress.name
                                )
                            )
                        }
                        val siege = ongoingSieges.remove(fortress.name)
                        siege?.let {
                            Utils.getAllPossibleParticipants(it.attacker).forEach { player ->
                                player?.sendTitle(
                                    languageManager.getMessage("siege-won-attackers-title-notification"),
                                    languageManager.getMessage(
                                        "siege-won-attackers-subtitle-notification",
                                        fortress.name
                                    ), 30, 100, 30
                                )
                            }
                        }
                    }, Fortress.INSTANCE.pluginConfig.config.getLong("siege-duration"))
                    val siege = Siege(fortress, town, HashSet<UUID>(), task)
                    ongoingSieges[fortress.name] = siege
                    Bukkit.getServer().onlinePlayers.forEach { player ->
                        player.sendMessage(
                            languageManager.getMessage(
                                "siege-start-notification",
                                town.name,
                                fortress.name
                            )
                        )
                    }
                    addParticipant(player.uniqueId, fortress)
                }
            }
        }
    }

    fun endSiege(fortress: FortressModel) {
        val siege = ongoingSieges.remove(fortress.name)
        siege?.let {
            siege.timerTask.cancel()
            Bukkit.getServer().onlinePlayers.forEach { player -> player.sendMessage(languageManager.getMessage("unsuccessful-conquest-notification", fortress.name)) }
            Utils.getAllPossibleParticipants(it.attacker).forEach { player ->
                player?.sendTitle(
                    languageManager.getMessage("siege-lost-attackers-title-notification"),
                    languageManager.getMessage("siege-lost-attackers-subtitle-notification", fortress.name, fortress.owner?.name ?: ""),
                    30,
                    100,
                    30
                )
            }
            ongoingSieges.remove(fortress.name)
        }
    }

    fun getSiege(fortress: FortressModel) = ongoingSieges[fortress.name]

    fun isBesieged(fortress: FortressModel) = ongoingSieges.contains(fortress.name)

    fun addParticipant(uuid: UUID, fortress: FortressModel) {
        getSiege(fortress)?.participants?.add(uuid)
        Bukkit.getPlayer(uuid)?.sendMessage(languageManager.getMessage("attacker-join-notification", fortress.name))
    }

    fun removeParticipant(uuid: UUID, fortress: FortressModel) {
        val siege = getSiege(fortress)
        siege?.let {
            val participants = getSiege(fortress)?.participants
            participants?.let {
                it.remove(uuid)
                if (it.isEmpty()) {
                    endSiege(fortress)
                }
            }
        }
    }

    fun canPlayerSiege(player: Player, fortress: FortressModel): Boolean {
        val resident = TownyUniverse.getInstance().getResident(player.name)
        val town = resident?.town
        town?.let {
            if (!isBesieged(fortress)) {
                return town != fortress.owner && town.isPVP
            } else {
                val siege = getSiege(fortress)
                siege?.let {
                    val attackers = siege.attacker
                    return town.isPVP && (town == attackers || (attackers.nation != null && town.nation != null && (attackers.nation == town.nation || attackers.nation.allies.contains(
                        town.nation
                    ))))
                }
            }
        }
        return false
    }

    fun saveFortresses(){
        fortressesByName.forEach { (name, fortress) ->
            Fortress.INSTANCE.fortressesConfig.config.set(name, fortress)
        }
        Fortress.INSTANCE.fortressesConfig.save()
    }

}