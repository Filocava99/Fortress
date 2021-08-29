package it.ancientrealms.manager

import com.palmergames.bukkit.towny.TownyUniverse
import com.palmergames.bukkit.towny.`object`.Nation
import com.palmergames.bukkit.towny.`object`.Town
import it.ancientrealms.ARFortress
import it.ancientrealms.exception.AlreadyOwnedFortress
import it.ancientrealms.models.Fortress
import it.ancientrealms.models.Siege
import it.ancientrealms.utils.Utils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class FortressesManager {

    private val fortresses = HashSet<Fortress>()
    private val fortressesByName = HashMap<String, Fortress>()
    private val ongoingSieges = HashMap<String, Siege>()
    private val languageManager = ARFortress.INSTANCE.languageManager

    fun getFortresses() = Collections.unmodifiableSet(fortresses)

    fun removeFortress(name: String) = fortresses.remove(fortressesByName.remove(name))

    fun getFortress(name: String) = fortressesByName[name]

    fun addFortress(name: String, fortress: Fortress) {
        fortresses.add(fortress)
        fortressesByName[name] = fortress
    }

    fun startSiege(fortress: Fortress, player: Player) {
        val resident = TownyUniverse.getInstance().getResident(player.name)
        resident?.let {
            val government = Utils.getGovernment(resident)
            government?.let {
                if (fortress.owner == government) {
                    throw AlreadyOwnedFortress()
                } else {
                    val task = Bukkit.getScheduler().runTaskLaterAsynchronously(ARFortress.INSTANCE, Runnable {
                        fortress.owner = government
                        fortress.lastTimeBesieged = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(
                            ZonedDateTime.now(
                                TimeZone.getTimeZone(
                                    ARFortress.INSTANCE.pluginConfig.config.getString("time-zone")
                                ).toZoneId()
                            )
                        );
                        ARFortress.INSTANCE.fortressesConfig.config.set(fortress.name, fortress)
                        ARFortress.INSTANCE.fortressesConfig.save()
                        Bukkit.getServer().onlinePlayers.forEach { player ->
                            player.sendMessage(
                                languageManager.getMessage(
                                    "successful-conquest-notification",
                                    government.name,
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
                    }, ARFortress.INSTANCE.pluginConfig.config.getLong("siege-duration"))
                    val siege = Siege(fortress, government, HashSet<UUID>(), task)
                    ongoingSieges[fortress.name] = siege
                    Bukkit.getServer().onlinePlayers.forEach { player ->
                        player.sendMessage(
                            languageManager.getMessage(
                                "siege-start-notification",
                                government.name,
                                fortress.name
                            )
                        )
                    }
                    addParticipant(player.uniqueId, fortress)
                }
            }
        }
    }

    fun endSiege(fortress: Fortress) {
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

    fun getSiege(fortress: Fortress) = ongoingSieges[fortress.name]

    fun isBesieged(fortress: Fortress) = ongoingSieges.contains(fortress.name)

    fun addParticipant(uuid: UUID, fortress: Fortress) {
        getSiege(fortress)?.participants?.add(uuid)
        Bukkit.getPlayer(uuid)?.sendMessage(languageManager.getMessage("attacker-join-notification", fortress.name))
    }

    fun removeParticipant(uuid: UUID, fortress: Fortress) {
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

    fun canPlayerSiege(player: Player, fortress: Fortress): Boolean {
        val resident = TownyUniverse.getInstance().getResident(player.name)
        val government = resident?.let { Utils.getGovernment(it) }
        government?.let {
            if (!isBesieged(fortress)) {
                return government != fortress.owner
            } else {
                val siege = getSiege(fortress)
                val attackers = siege?.attacker
                return if (government is Town) {
                    government == attackers
                } else {
                    if (attackers is Nation) {
                        val nation = government as Nation
                        nation == attackers || (attackers as Nation).allies.contains(nation)
                    } else {
                        false
                    }
                }
            }
        }
        return false
    }

    fun saveFortresses(){
        fortressesByName.forEach { (name, fortress) ->
            ARFortress.INSTANCE.fortressesConfig.config.set(name, fortress)
        }
        ARFortress.INSTANCE.fortressesConfig.save()
    }

}