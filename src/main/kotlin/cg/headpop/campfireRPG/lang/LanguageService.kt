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
        val file = File(plugin.dataFolder, "lang/$fileName")
        if (!file.exists()) {
            plugin.saveResource("lang/$fileName", false)
        }
        bundle = YamlConfiguration.loadConfiguration(file)
    }

    fun get(key: String, vararg replacements: Pair<String, String>): String {
        var value = bundle.getString(key) ?: key
        for ((from, to) in replacements) {
            value = value.replace("{$from}", to)
        }
        return value.replace('&', '§')
    }
}
