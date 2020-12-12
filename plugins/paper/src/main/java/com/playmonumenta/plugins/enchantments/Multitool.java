package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

/*
 * Multitool - Level one allows you to swap the tool
 * between an axe and shovel, level 2 adds a pickaxe
 * to the rotation. Swapping tools keeps the same
 * Name/Lore/Stats.
 */
public class Multitool implements BaseEnchantment {
	public static String PROPERTY_NAME = ChatColor.GRAY + "Multitool";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event, int level) {
		//Does not swap when clicking an interactable
		if (event.getAction() == Action.RIGHT_CLICK_AIR
				|| (event.getAction() == Action.RIGHT_CLICK_BLOCK && !ItemUtils.interactableBlocks.contains(event.getClickedBlock().getBlockData().getMaterial()))) {
			ItemStack item = player.getInventory().getItemInMainHand();
			//Does not swap when player is sneaking
			if (player.isSneaking()) {
				return;
			}
			// You can swap your itemslot in the same tick, the event will begin when you right click the multitool item
			// and then perform actions on the swapped to item. Re-get the level for the item being changed to safeguard this.
			int confirmLevel = this.getLevelFromItem(item);
			if (confirmLevel > 0 && MetadataUtils.checkOnceThisTick(plugin, player, "MultitoolMutex")) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					String[] str = item.getType().toString().split("_");
					if (InventoryUtils.isAxeItem(item)) {
						Material mat = Material.valueOf(str[0] + "_" + "SHOVEL");
						item.setType(mat);
					} else if (InventoryUtils.isShovelItem(item)) {
						if (confirmLevel > 1) {
							Material mat = Material.valueOf(str[0] + "_" + "PICKAXE");
							item.setType(mat);
						} else {
							Material mat = Material.valueOf(str[0] + "_" + "AXE");
							item.setType(mat);
						}
					} else if (InventoryUtils.isPickaxeItem(item)) {
						Material mat = Material.valueOf(str[0] + "_" + "AXE");
						item.setType(mat);
					}
					player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 2F);
					player.updateInventory();
				});
			}
		}
	}

}
