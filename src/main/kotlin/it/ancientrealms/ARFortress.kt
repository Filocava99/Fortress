package it.ancientrealms

import it.ancientrealms.command.*
import it.ancientrealms.listener.PlayerListener
import it.ancientrealms.listener.TownyListener
import it.ancientrealms.manager.FortressesManager
import it.ancientrealms.manager.LanguageManager
import it.ancientrealms.models.Fortress
import it.tigierrei.configapi.Config
import org.bukkit.command.CommandExecutor
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.plugin.java.JavaPlugin

class ARFortress : JavaPlugin() {

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

    private fun initManagers(){
        languageManager = LanguageManager()
        fortressesManager = FortressesManager()
    }

    private fun registerSerializableClasses() {
        ConfigurationSerialization.registerClass(Fortress::class.java)
    }

    fun loadConfig() {
        pluginConfig = Config("config.yml", this)
    }

    fun loadData(){
        fortressesConfig = Config("fortresses.yml", this)
        fortressesConfig.config.getKeys(false)
            .forEach { key -> fortressesManager.addFortress(key, fortressesConfig.config.get(key) as Fortress) }
        fortressesManager.getFortresses().forEach { println(it.name + " " + it.lastTimeBesieged) }
    }

    private fun registerCommands() {
        val mainCommand = MainCommand()
        MainCommand().addSubCommand(listOf("create", "new"), CreateCommand())
            .addSubCommand(listOf("reload"), ReloadCommand())
            .addSubCommand(listOf("setHour", "sh"), SetHourCommand())
            .addSubCommand(listOf("help","?"), HelpCommand())
            .register(this, "fortress", "arfortress")
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(PlayerListener(), this)
        server.pluginManager.registerEvents(TownyListener(), this)
    }

    companion object {
        private var plugin: ARFortress? = null
        val INSTANCE: ARFortress
            get() {
                if (plugin == null) {
                    plugin = getPlugin(ARFortress::class.java)
                }
                return plugin!!
            }
    }

    private fun CommandExecutor.register(javaPlugin: JavaPlugin, command: String, vararg aliases: String) {
        javaPlugin.getCommand(command)?.setExecutor(this)
        if (aliases.isNotEmpty()) ARFortress.INSTANCE.getCommand(command)?.aliases = aliases.toMutableList()
    }
}