package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/*
 * Multitool - Level one allows you to swap the tool
 * between an axe and shovel, level 2 adds a pickaxe
 * to the rotation. Swapping tools keeps the same
 * Name/Lore/Stats.
 */
public class Multitool implements Enchantment {
	@Override
	public String getName() {
		return "Multitool";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.MULTITOOL;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	// NB: if this trigger is ever changed, adapt AbilityTrigger.KeyOptions.NO_USABLE_ITEMS accordingly
	// (that code prevents using abilities with right clicks when holding a multitool)
	@Override
	public void onPlayerInteract(Plugin plugin, Player player, double level, PlayerInteractEvent event) {
		//Material of the block clicked
		Material eventMat = event.getClickedBlock() == null ? null : event.getClickedBlock().getType();
		//Does not swap when clicking an interactable
		// The clicked block may be air if something deleted the block in an event handler, but didn't cancel the event, e.g. opening strike chests.
		if (event.getAction() == Action.RIGHT_CLICK_AIR
			    || (event.getAction() == Action.RIGHT_CLICK_BLOCK && (!ItemUtils.interactableBlocks.contains(eventMat) || eventMat == Material.PUMPKIN) && eventMat != Material.AIR)) {
			ItemStack item = player.getInventory().getItemInMainHand();
			//Does not swap when player is sneaking
			if (player.isSneaking()) {
				return;
			}
			// You can swap your itemslot in the same tick, the event will begin when you right click the multitool item
			// and then perform actions on the swapped to item. Re-get the level for the item being changed to safeguard this.
			level = plugin.mItemStatManager.getEnchantmentLevel(player, getEnchantmentType());
			if (level > 0) {
				if (MetadataUtils.checkOnceThisTick(plugin, player, "MultitoolMutex")) {
					String[] str = item.getType().toString().split("_");
					if (ItemUtils.isAxe(item)) {
						Material mat = Material.valueOf(str[0] + "_" + "SHOVEL");
						item.setType(mat);
					} else if (ItemUtils.isShovel(item)) {
						if (level > 1) {
							Material mat = Material.valueOf(str[0] + "_" + "PICKAXE");
							item.setType(mat);
						} else {
							Material mat = Material.valueOf(str[0] + "_" + "AXE");
							item.setType(mat);
						}
					} else if (ItemUtils.isPickaxe(item)) {
						Material mat = Material.valueOf(str[0] + "_" + "AXE");
						item.setType(mat);
					}
					player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 2F);
					player.updateInventory();
				}

				if (eventMat == Material.GRASS_BLOCK || ItemUtils.isStrippable(eventMat)) {
					event.setCancelled(true);
				}
			}
		}
	}
}
