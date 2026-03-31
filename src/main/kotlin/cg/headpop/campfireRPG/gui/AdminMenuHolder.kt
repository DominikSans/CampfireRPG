package cg.headpop.campfireRPG.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class AdminMenuHolder(
) : InventoryHolder {
    lateinit var backingInventory: Inventory
    var page: Int = 0
    var selectedProfileId: String = "normal"

    override fun getInventory(): Inventory = backingInventory
}
