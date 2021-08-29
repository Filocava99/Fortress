package it.ancientrealms.utils

import com.palmergames.bukkit.towny.`object`.Government
import com.palmergames.bukkit.towny.`object`.Nation
import com.palmergames.bukkit.towny.`object`.Resident
import com.palmergames.bukkit.towny.`object`.Town
import it.ancientrealms.ARFortress
import it.ancientrealms.NamespacedKeys
import org.bukkit.Chunk
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.*

class Utils {

    companion object{
        fun getFortressFromChunk(chunk: Chunk) = chunk.persistentDataContainer[NamespacedKeys.FORTRESS.namespacedKey, PersistentDataType.STRING]?.let {
            ARFortress.INSTANCE.fortressesManager.getFortress(
                it
            )
        }

        fun getGovernment(resident: Resident): Government? {
            return if (resident.town?.hasNation() == true) resident.town.nation else resident.town
        }

        fun getAllPossibleParticipants(government: Government): List<Player?> {
            return if(government is Town){
                (government as Town).residents.map { resident -> resident.player }
            }else{
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
    }

}