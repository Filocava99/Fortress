package it.ancientrealms.models

import org.bukkit.configuration.serialization.ConfigurationSerializable

enum class CommandTarget : ConfigurationSerializable {
    SIEGE_LEADER {
        override fun serialize(): MutableMap<String, Any> = mutableMapOf<String, Any>(Pair("command-target", SIEGE_LEADER.name))
    },
    PARTICIPANTS {
        override fun serialize(): MutableMap<String, Any> = mutableMapOf<String, Any>(Pair("command-target", PARTICIPANTS.name))
    },
    RANDOM_PARTICIPANT {
        override fun serialize(): MutableMap<String, Any> = mutableMapOf<String, Any>(Pair("command-target", RANDOM_PARTICIPANT.name))
    },
    DEFENDERS {
        override fun serialize(): MutableMap<String, Any> = mutableMapOf<String, Any>(Pair("command-target", DEFENDERS.name))
    };

    fun valueOf(map: Map<String, Any>): CommandTarget {
        return CommandTarget.valueOf(map["command-target"] as String)
    }
}