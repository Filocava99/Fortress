package it.ancientrealms.utils

import com.palmergames.bukkit.towny.Towny
import com.palmergames.bukkit.towny.TownyAPI
import com.palmergames.bukkit.towny.TownyUniverse
import com.palmergames.bukkit.towny.`object`.Government
import com.palmergames.bukkit.towny.`object`.Nation
import com.palmergames.bukkit.towny.`object`.Resident
import com.palmergames.bukkit.towny.`object`.Town
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException
import it.ancientrealms.Fortress
import it.ancientrealms.NamespacedKeys
import org.bukkit.Chunk
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.*

class Utils {

    companion object {
        fun getFortressFromChunk(chunk: Chunk) =
            chunk.persistentDataContainer[NamespacedKeys.FORTRESS.namespacedKey, PersistentDataType.STRING]?.let {
                Fortress.INSTANCE.fortressesManager.getFortress(
                    it
                )
            }

        fun getGovernment(resident: Resident): Government? =
            if (resident.town?.hasNation() == true) resident.town.nation else resident.town

        fun getTown(resident: Resident) = try {
            resident.town
        } catch (e: NotRegisteredException) {
            null
        }

        fun getNation(resident: Resident) = try {
            resident.town.nation
        } catch (e: NotRegisteredException) {
            null
        }

        fun getNation(town: Town) = try {
            town.nation
        } catch (e: NotRegisteredException) {
            null
        }

        fun getAllPossibleParticipants(government: Government): List<Player?> {
            return if (government is Town) {
                government.residents.map { resident -> resident.player }
            } else {
                val list = LinkedList<Player?>()
                val nation = government as Nation
                listOf<Nation>(nation, *nation.allies.toTypedArray()).forEach { it ->
                    it.towns.forEach { town ->
                        list.addAll(town.residents.map { resident -> resident.player })
                    }
                }
                list
            }
        }

        fun getResident(player: Player) = TownyUniverse.getInstance().getResident(player.name)
    }

}