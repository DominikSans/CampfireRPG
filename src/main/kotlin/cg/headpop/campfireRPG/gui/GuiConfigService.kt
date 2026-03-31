package cg.headpop.campfireRPG.gui

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.SkullMeta
import java.io.File

data class GuiItemSpec(
    val material: Material,
    val amount: Int,
    val name: String,
    val lore: List<String>,
    val customModelData: Int?,
    val glow: Boolean,
    val itemFlags: Set<ItemFlag>,
    val enchants: Map<Enchantment, Int>,
    val skullOwner: String?,
)

class GuiConfigService(
    private val plugin: CampfireRPG,
) {

    private lateinit var config: YamlConfiguration

    fun reload() {
        val file = File(plugin.dataFolder, "gui.yml")
        if (!file.exists()) {
            plugin.saveResource("gui.yml", false)
        }
        config = YamlConfiguration.loadConfiguration(file)
    }

    fun title(): String = resolveText(config.getString("gui.title") ?: "CampfireRPG Control")

    fun item(key: String, fallbackMaterial: Material, fallbackName: String, fallbackLore: List<String>): GuiItemSpec {
        val section = config.getConfigurationSection("items.$key")
        val material = Material.matchMaterial(section?.getString("material") ?: fallbackMaterial.name) ?: fallbackMaterial
        return GuiItemSpec(
            material = material,
            amount = (section?.getInt("amount") ?: 1).coerceAtLeast(1),
            name = resolveText(section?.getString("name") ?: fallbackName),
            lore = (section?.getStringList("lore") ?: fallbackLore).map(::resolveText),
            customModelData = if (section?.contains("custom-model-data") == true) section.getInt("custom-model-data") else null,
            glow = section?.getBoolean("glow", false) == true,
            itemFlags = section?.getStringList("item-flags")
                ?.mapNotNull { runCatching { ItemFlag.valueOf(it.uppercase()) }.getOrNull() }
                ?.toSet()
                ?: emptySet(),
            enchants = section?.getConfigurationSection("enchantments")
                ?.getKeys(false)
                ?.mapNotNull { key ->
                    val enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.lowercase())) ?: return@mapNotNull null
                    enchantment to section.getConfigurationSection("enchantments")!!.getInt(key).coerceAtLeast(1)
                }
                ?.toMap()
                ?: emptyMap(),
            skullOwner = section?.getString("skull-owner"),
        )
    }

    fun applyExtraMeta(meta: org.bukkit.inventory.meta.ItemMeta, spec: GuiItemSpec) {
        spec.enchants.forEach { (enchantment, level) -> meta.addEnchant(enchantment, level, true) }
        if (spec.glow && spec.enchants.isEmpty()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        meta.addItemFlags(*spec.itemFlags.toTypedArray())
        if (meta is SkullMeta && !spec.skullOwner.isNullOrBlank()) {
            meta.owningPlayer = plugin.server.getOfflinePlayer(spec.skullOwner)
        }
    }

    private fun resolveText(raw: String): String {
        return if (raw.startsWith("lang:")) {
            plugin.languageService.get(raw.removePrefix("lang:"))
        } else {
            raw.replace('&', '§')
        }
    }
}
