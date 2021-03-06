package it.ancientrealms.manager

import com.palmergames.bukkit.towny.TownyUniverse
import com.palmergames.bukkit.towny.`object`.Government
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
import it.ancientrealms.models.CommandTarget
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.scheduler.BukkitTask
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

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

    fun removeAllFortresses(){
        fortresses.clear()
        fortressesByName.clear()
    }

    fun startSiege(fortress: FortressModel, player: Player) {
        val resident = TownyUniverse.getInstance().getResident(player.name)
        resident?.let {
            val town = Utils.getTown(resident)
            town?.let {
                if (fortress.owner == town) {
                    throw AlreadyOwnedFortress()
                } else {
                    val task = Bukkit.getScheduler().runTaskLater(
                        Fortress.INSTANCE,
                        SiegeTask(fortress, town, ongoingSieges),
                        Fortress.INSTANCE.pluginConfig.config.getLong("siege-duration")
                    )
                    val siege = Siege(fortress, town, player.uniqueId, HashSet<UUID>(), task)
                    val siegeDurationInTicks = Fortress.INSTANCE.pluginConfig.config.getLong("siege-duration")
                    val siegeDurationInMillis = siegeDurationInTicks / 20 * 1000
                    val besiegeStartTime = Instant.now().toEpochMilli()
                    val besiegeEndTime =
                        besiegeStartTime + siegeDurationInMillis
                    siege.timerTasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(Fortress.INSTANCE, Runnable {
                        siege.participants.forEach {
                            Bukkit.getServer().getPlayer(it)?.let { player ->
                                val date =
                                    Date.from(Instant.ofEpochMilli(besiegeEndTime - Instant.now().toEpochMilli()))
                                player.spigot().sendMessage(
                                    ChatMessageType.ACTION_BAR,
                                    *TextComponent.fromLegacyText(
                                        languageManager.getMessage(
                                            "siege-timer-notification",
                                            date.minutes.toString(),
                                            date.seconds.toString()
                                        )
                                    )
                                )
                            }
                        }
                    }, 0, 100))
                    val quarterSiegeTime = siegeDurationInTicks / 4
                    var remainingTime = siegeDurationInMillis
                    siege.timerTasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(Fortress.INSTANCE, Runnable {
                        remainingTime -= quarterSiegeTime / 20 * 1000
                        val hours = TimeUnit.MILLISECONDS.toHours(remainingTime)
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) -
                                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(remainingTime))
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remainingTime))
                        if (hours != 0L || minutes != 0L || seconds != 0L) {
                            if (hours > 0) {
                                Bukkit.getServer().onlinePlayers.forEach {
                                    it.sendMessage(
                                        languageManager.getMessage(
                                            "global-remaining-siege-message-with-hours",
                                            fortress.name,
                                            hours.toString(),
                                            minutes.toString(),
                                            seconds.toString()
                                        )
                                    )
                                }
                            } else {
                                Bukkit.getServer().onlinePlayers.forEach {
                                    it.sendMessage(
                                        languageManager.getMessage(
                                            "global-remaining-siege-message-with-minutes",
                                            fortress.name,
                                            minutes.toString(),
                                            seconds.toString()
                                        )
                                    )
                                }
                            }
                        }
                    }, quarterSiegeTime, quarterSiegeTime))
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
                    fortress.chunks.forEach { chunk ->
                        run {
                            chunk.entities.filterIsInstance<Player>().filter { it.uniqueId != player.uniqueId }
                                .forEach { playerInChunk ->
                                    if (canPlayerSiege(playerInChunk, fortress)) {
                                        addParticipant(playerInChunk.uniqueId, fortress)
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    private fun endSiege(fortress: FortressModel) {
        val siege = ongoingSieges.remove(fortress.name)
        siege?.let { it ->
            siege.endSiegeTask.cancel()
            siege.timerTasks.forEach(BukkitTask::cancel)
            Bukkit.getServer().onlinePlayers.forEach { player ->
                player.sendMessage(
                    languageManager.getMessage(
                        "unsuccessful-conquest-notification",
                        fortress.name
                    )
                )
            }
            Utils.getAllPossibleParticipants(it.attacker).forEach { player ->
                if (siege.fortress.owner == null) {
                    player?.sendTitle(
                        languageManager.getMessage("unconquered-siege-lost-attackers-title-notification"),
                        languageManager.getMessage(
                            "unconquered-siege-lost-attackers-subtitle-notification",
                            fortress.name
                        ),
                        30,
                        100,
                        30
                    )
                } else {
                    player?.sendTitle(
                        languageManager.getMessage("siege-lost-attackers-title-notification"),
                        languageManager.getMessage(
                            "siege-lost-attackers-subtitle-notification",
                            fortress.name,
                            fortress.owner?.name ?: ""
                        ),
                        30,
                        100,
                        30
                    )
                }
            }
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
            getSiege(fortress)?.participants?.let {
                it.remove(uuid)
                if (it.isEmpty()) {
                    endSiege(fortress)
                }
            }
        }
    }

    fun addDeadParticipant(uuid: UUID, fortress: FortressModel) {
        val siege = getSiege(fortress)
        siege?.let {
            getSiege(fortress)?.deadParticipants?.add(uuid)
        }
    }

    fun canPlayerSiege(player: Player, fortress: FortressModel): Boolean {
        val resident = TownyUniverse.getInstance().getResident(player.name)
        resident?.let {
            if (it.hasTown()) {
                val town = Utils.getTown(it)
                town?.let {
                    if (!isBesieged(fortress)) {
                        return town != fortress.owner && town.isPVP
                    } else {
                        val siege = getSiege(fortress)
                        siege?.let {
                            val attackers = siege.attacker
                            val attackersNation = Utils.getNation(attackers)
                            val townNation = Utils.getNation(town)
                            return town.isPVP && (town == attackers || (attackersNation != null && townNation != null && (attackersNation == townNation || attackersNation.allies.contains(
                                townNation
                            ) && !siege.participants.contains(
                                player.uniqueId
                            ))))
                        }
                    }
                }
            }
        }
        return false
    }

    fun saveFortresses() {
        fortressesByName.forEach { (name, fortress) ->
            Fortress.INSTANCE.fortressesConfig.config.set(name, fortress)
        }
        Fortress.INSTANCE.fortressesConfig.save()
    }

    private class SiegeTask(
        val fortress: FortressModel,
        val attacker: Town,
        val ongoingSieges: HashMap<String, Siege>
    ) : Runnable {
        override fun run() {
            val languageManager = Fortress.INSTANCE.languageManager
            val siege = ongoingSieges.remove(fortress.name)
            siege?.let {
                it.timerTasks.forEach(BukkitTask::cancel)
                Utils.getAllPossibleParticipants(it.attacker).forEach { player ->
                    player?.sendTitle(
                        languageManager.getMessage("siege-won-attackers-title-notification"),
                        languageManager.getMessage(
                            "siege-won-attackers-subtitle-notification",
                            fortress.name
                        ), 30, 100, 30
                    )
                }
                fortress.onConquestCommands.entries.forEach { (k, v) ->
                    when (k) {
                        CommandTarget.PARTICIPANTS -> v.forEach { command ->
                            it.participants.forEach {
                                Bukkit.getServer().run {
                                    dispatchCommand(
                                        consoleSender,
                                        command.replace("\${player}", getPlayer(it)?.name ?: "")
                                    )
                                }
                            }
                        }
                        CommandTarget.SIEGE_LEADER -> v.forEach { command ->
                            Bukkit.getServer().run {
                                dispatchCommand(
                                    consoleSender,
                                    command.replace("\${player}", getPlayer(it.siegeStarter)?.name ?: "")
                                )
                            }
                        }
                        CommandTarget.RANDOM_PARTICIPANT -> v.forEach { command ->
                            Bukkit.getServer().run {
                                dispatchCommand(
                                    consoleSender,
                                    command.replace("\${player}", getPlayer(it.participants.random())?.name ?: "")
                                )
                            }
                        }
                        CommandTarget.DEFENDERS -> v.forEach { command ->
                            Utils.getAllPossibleParticipants(fortress.owner as Government)
                                .forEach {
                                    Bukkit.getServer().run {
                                        dispatchCommand(consoleSender, command.replace("\${player}", it?.name ?: ""))
                                    }
                                }
                        }
                    }
                }
            }
            if (!fortress.notOwnable) {
                fortress.owner = attacker
            }
            fortress.lastTimeBesieged = DateTimeFormatter.ofPattern("yyyy-MM-dd-H:m").format(
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
                        attacker.name,
                        fortress.name
                    )
                )
            }
        }
    }

}