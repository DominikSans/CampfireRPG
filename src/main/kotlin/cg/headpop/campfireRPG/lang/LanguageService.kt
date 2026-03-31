package cg.headpop.campfireRPG.lang

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class LanguageService(
    private val plugin: CampfireRPG,
) {

    private lateinit var bundle: YamlConfiguration

    fun reload() {
        val locale = plugin.settingsLoader.settings.language.locale.lowercase()
        val fileName = if (locale == "es") "es.yml" else "en.yml"
        ensureLanguageFiles()
        val file = File(plugin.dataFolder, "lang/$fileName")
        bundle = YamlConfiguration.loadConfiguration(file)
    }

    fun ensureLanguageFiles() {
        val langFolder = File(plugin.dataFolder, "lang")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }
        listOf("en.yml", "es.yml").forEach { fileName ->
            val file = File(langFolder, fileName)
            if (!file.exists()) {
                plugin.saveResource("lang/$fileName", false)
            }
        }
    }

    fun get(key: String, vararg replacements: Pair<String, String>): String {
        var value = bundle.getString(key) ?: key
        for ((from, to) in replacements) {
            value = value.replace("{$from}", to)
        }
        return value.replace('&', '§')
    }
}
