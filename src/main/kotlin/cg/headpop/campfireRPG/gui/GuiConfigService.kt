package cg.headpop.campfireRPG.gui

import cg.headpop.campfireRPG.CampfireRPG
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import java.io.File

data class GuiItemSpec(
    val key: String,
    val material: Material,
    val amount: Int,
    val name: String,
    val lore: List<String>,
    val glow: Boolean,
    val itemFlags: Set<ItemFlag>,
    val enchants: Map<Enchantment, Int>,
    val skullOwner: String?,
    val itemModel: NamespacedKey?,
    val slots: List<Int>,
)

class GuiConfigService(
    private val plugin: CampfireRPG,
) {

    private lateinit var config: YamlConfiguration
    private var itemSpecs: Map<String, GuiItemSpec> = emptyMap()
    private var decorateSpecs: Map<String, GuiItemSpec> = emptyMap()

    fun reload() {
        val file = File(plugin.dataFolder, "gui.yml")
        if (!file.exists()) {
            plugin.saveResource("gui.yml", false)
        }
        config = YamlConfiguration.loadConfiguration(file)
        itemSpecs = loadSpecs("items")
        decorateSpecs = loadSpecs("items-decorate")
        validate()
    }

    fun title(): String = resolveText(config.getString("gui.title") ?: "CampfireRPG Control")

    fun rows(): Int = config.getInt("gui.rows", 5).coerceIn(1, 6)

    fun item(key: String, fallback: GuiItemSpec): GuiItemSpec = itemSpecs[key] ?: fallback

    fun decorationItems(): Collection<GuiItemSpec> = decorateSpecs.values

    fun slotsFor(key: String, fallback: List<Int>): List<Int> = itemSpecs[key]?.slots ?: fallback

    fun firstSlot(key: String, fallback: Int): Int = slotsFor(key, listOf(fallback)).firstOrNull() ?: fallback

    fun applyExtraMeta(meta: ItemMeta, spec: GuiItemSpec) {
        spec.enchants.forEach { (enchantment, level) -> meta.addEnchant(enchantment, level, true) }
        if (spec.glow && spec.enchants.isEmpty()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        meta.addItemFlags(*spec.itemFlags.toTypedArray())
        applyItemModel(meta, spec.itemModel)
        if (meta is SkullMeta && !spec.skullOwner.isNullOrBlank()) {
            meta.owningPlayer = plugin.server.getOfflinePlayer(spec.skullOwner)
        }
    }

    private fun loadSpecs(root: String): Map<String, GuiItemSpec> {
        val section = config.getConfigurationSection(root) ?: return emptyMap()
        return section.getKeys(false).associateWith { key ->
            loadSpec(key, requireNotNull(section.getConfigurationSection(key)))
        }
    }

    private fun loadSpec(key: String, section: ConfigurationSection): GuiItemSpec {
        val material = Material.matchMaterial(section.getString("material") ?: "STONE") ?: Material.STONE
        return GuiItemSpec(
            key = key,
            material = material,
            amount = section.getInt("amount", 1).coerceIn(1, material.maxStackSize.coerceAtLeast(1)),
            name = resolveText(section.getString("name") ?: material.name),
            lore = section.getStringList("lore").map(::resolveText),
            glow = section.getBoolean("glow", false),
            itemFlags = section.getStringList("item-flags")
                .mapNotNull { runCatching { ItemFlag.valueOf(it.uppercase()) }.getOrNull() }
                .toSet(),
            enchants = parseEnchantments(section.getConfigurationSection("enchantments")),
            skullOwner = section.getString("skull-owner"),
            itemModel = section.getString("item_model")?.let { NamespacedKey.fromString(it, plugin) },
            slots = parseSlots(section),
        )
    }

    private fun parseEnchantments(section: ConfigurationSection?): Map<Enchantment, Int> {
        if (section == null) {
            return emptyMap()
        }
        return section.getKeys(false).mapNotNull { key ->
            val enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft(key.lowercase()))
                ?: return@mapNotNull null
            enchantment to section.getInt(key).coerceAtLeast(1)
        }.toMap()
    }

    private fun parseSlots(section: ConfigurationSection): List<Int> {
        val rawEntries = mutableListOf<Any>()
        if (section.contains("slot")) {
            rawEntries += section.get("slot")!!
        }
        rawEntries += section.getList("slots").orEmpty()

        val parsed = linkedSetOf<Int>()
        rawEntries.forEach { entry ->
            when (entry) {
                is Int -> parsed += entry
                is String -> parsed += expandSlotExpression(entry)
            }
        }
        return parsed.toList()
    }

    private fun expandSlotExpression(expression: String): List<Int> {
        val trimmed = expression.trim()
        if (trimmed.isBlank()) {
            return emptyList()
        }
        if ('-' !in trimmed) {
            return listOfNotNull(trimmed.toIntOrNull())
        }
        val parts = trimmed.split('-', limit = 2)
        val start = parts[0].trim().toIntOrNull() ?: return emptyList()
        val end = parts[1].trim().toIntOrNull() ?: return emptyList()
        return if (start <= end) {
            (start..end).toList()
        } else {
            (start downTo end).toList()
        }
    }

    private fun validate() {
        val inventorySize = rows() * 9
        val globals = mutableMapOf<Int, String>()

        decorationItems().forEach { spec ->
            validateSlots(spec, inventorySize)
            spec.slots.forEach { slot ->
                globals.putIfAbsent(slot, "items-decorate.${spec.key}")?.let { existing ->
                    plugin.logger.warning("Duplicate GUI slot $slot between $existing and items-decorate.${spec.key}")
                }
            }
        }

        listOf("overview", "toggles", "numeric", "classes", "profile").forEach { page ->
            val occupied = globals.toMutableMap()
            itemSpecs.values
                .filter { belongsToPage(it.key, page) || isGlobalKey(it.key) }
                .forEach { spec ->
                    validateSlots(spec, inventorySize)
                    spec.slots.forEach { slot ->
                        occupied.putIfAbsent(slot, "items.${spec.key}")?.let { existing ->
                            plugin.logger.warning("Duplicate GUI slot $slot on page '$page' between $existing and items.${spec.key}")
                        }
                    }
                }
        }
    }

    private fun validateSlots(spec: GuiItemSpec, inventorySize: Int) {
        if (spec.slots.isEmpty()) {
            plugin.logger.warning("GUI item '${spec.key}' has no slot or slots configured.")
        }
        spec.slots.filter { it !in 0 until inventorySize }.forEach { slot ->
            plugin.logger.warning("GUI item '${spec.key}' uses out-of-range slot $slot for $inventorySize-slot inventory.")
        }
    }

    private fun isGlobalKey(key: String): Boolean {
        return key.startsWith("nav-") || key == "previous" || key == "next"
    }

    private fun belongsToPage(key: String, page: String): Boolean {
        return key.startsWith("$page-")
    }

    private fun applyItemModel(meta: ItemMeta, itemModel: NamespacedKey?) {
        if (itemModel == null) {
            return
        }
        runCatching {
            val method = meta.javaClass.methods.firstOrNull {
                it.name == "setItemModel" &&
                    it.parameterTypes.size == 1 &&
                    NamespacedKey::class.java.isAssignableFrom(it.parameterTypes[0])
            } ?: return
            method.invoke(meta, itemModel)
        }.onFailure {
            plugin.logger.warning("Failed to apply item_model '$itemModel' to GUI item meta: ${it.message}")
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
