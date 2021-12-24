package it.ancientrealms

import it.ancientrealms.command.*
import it.ancientrealms.listener.PlayerListener
import it.ancientrealms.listener.TownyListener
import it.ancientrealms.manager.FortressesManager
import it.ancientrealms.manager.LanguageManager
import it.tigierrei.configapi.Config
import org.bukkit.command.CommandExecutor
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.plugin.java.JavaPlugin

typealias FortressModel = it.ancientrealms.models.Fortress

class Fortress : JavaPlugin() {

    lateinit var fortressesManager: FortressesManager
    lateinit var pluginConfig: Config
    lateinit var fortressesConfig: Config
    lateinit var languageManager: LanguageManager

    override fun onEnable() {
        loadConfig()
        initManagers()
        registerSerializableClasses()
        registerCommands()
        registerListeners()
        loadData()
    }

    private fun initManagers() {
        languageManager = LanguageManager()
        fortressesManager = FortressesManager()
    }

    private fun registerSerializableClasses() {
        ConfigurationSerialization.registerClass(FortressModel::class.java)
    }

    fun loadConfig() {
        pluginConfig = Config("config.yml", this)
    }

    fun loadData() {
        fortressesConfig = Config("fortresses.yml", this)
        fortressesConfig.config.getKeys(false)
            .forEach { key -> fortressesManager.addFortress(key, fortressesConfig.config.get(key) as FortressModel) }
        fortressesManager.getFortresses().forEach { println(it.name + " " + it.lastTimeBesieged) }
    }

    private fun registerCommands() {
        MainCommand().apply {
            addSubCommand(listOf("create", "new"), CreateCommand())
            addSubCommand(listOf("reload"), ReloadCommand())
            addSubCommand(listOf("setHour", "sh"), SetHourCommand())
            addSubCommand(listOf("help", "?"), HelpCommand())
            addSubCommand(listOf("info"), InfoCommand())
        }.register(this, "fortress", "arfortress")
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(PlayerListener(), this)
        server.pluginManager.registerEvents(TownyListener(), this)
    }

    companion object {
        private var plugin: Fortress? = null
        val INSTANCE: Fortress
            get() {
                if (plugin == null) {
                    plugin = getPlugin(Fortress::class.java)
                }
                return plugin!!
            }
    }

    private fun CommandExecutor.register(javaPlugin: JavaPlugin, command: String, vararg aliases: String) {
        javaPlugin.getCommand(command)?.setExecutor(this)
        if (aliases.isNotEmpty()) javaPlugin.getCommand(command)?.aliases = aliases.toMutableList()
    }
}