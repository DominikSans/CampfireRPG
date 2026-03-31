package cg.headpop.campfireRPG.config

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class RuntimeConfigService(
    private val plugin: CampfireRPG,
) {

    private lateinit var file: File
    private lateinit var runtimeConfig: YamlConfiguration
    private lateinit var mergedConfig: YamlConfiguration

    fun reload() {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        file = File(plugin.dataFolder, "runtime-overrides.yml")
        if (!file.exists()) {
            file.createNewFile()
        }

        runtimeConfig = YamlConfiguration.loadConfiguration(file)
        mergedConfig = YamlConfiguration()
        plugin.config.getValues(true).forEach { (path, value) ->
            mergedConfig.set(path, value)
        }
        runtimeConfig.getValues(true).forEach { (path, value) ->
            mergedConfig.set(path, value)
        }
    }

    fun merged(): YamlConfiguration = mergedConfig

    fun contains(path: String): Boolean = runtimeConfig.contains(path)

    fun get(path: String): Any? = mergedConfig.get(path)

    fun getBoolean(path: String, default: Boolean): Boolean = mergedConfig.getBoolean(path, default)

    fun set(path: String, value: Any) {
        runtimeConfig.set(path, value)
        mergedConfig.set(path, value)
    }

    fun save() {
        runtimeConfig.save(file)
    }
}
