package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.CheckReturnValue;

/*
 * Multitool - Level one allows you to swap the tool
 * between an axe and shovel, level 2 adds a pickaxe
 * to the rotation. Swapping tools keeps the same
 * Name/Lore/Stats.
 */
public class Multitool implements Enchantment {

	public static final String MULTITOOL_TRIGGER_OPTION_SCORE = "MultitoolTrigger";
	public static final int TRIGGER_OPTION_RIGHT_CLICK = 0;
	public static final int TRIGGER_OPTION_SWAP = 1;
	public static final int TRIGGER_OPTION_DROP = 2;
	public static final int TRIGGER_OPTION_DISABLED = 3;

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
	public void onPlayerInteract(Plugin plugin, Player player, double levelUnused, PlayerInteractEvent event) {
		if (ScoreboardUtils.getScoreboardValue(player, MULTITOOL_TRIGGER_OPTION_SCORE).orElse(0) != TRIGGER_OPTION_RIGHT_CLICK) {
			return;
		}

		// Material of the block clicked
		Material eventMat = event.getClickedBlock() == null ? null : event.getClickedBlock().getType();
		Action eventAction = event.getAction();
		if (eventAction.equals(Action.RIGHT_CLICK_BLOCK)) {
			// The clicked block may be air if something deleted the block in an event handler, but didn't cancel the event, e.g. opening strike chests.
			if (Material.AIR.equals(eventMat)) {
				return;
			}
			// Does not swap when clicking an interactable
			if (ItemUtils.interactableBlocks.contains(eventMat) && !Material.PUMPKIN.equals(eventMat)) {
				return;
			}
		} else if (!eventAction.equals(Action.RIGHT_CLICK_AIR)) {
			return;
		}

		// Does not swap when player is sneaking
		if (player.isSneaking()) {
			return;
		}

		ItemStack item = player.getInventory().getItemInMainHand();

		// Do not swap compass on right click, as it has a right click action
		if (item.getType() == Material.COMPASS) {
			return;
		}

		player.getInventory().setItemInMainHand(swapChecked(plugin, player, item));

		if (eventMat == Material.GRASS_BLOCK || ItemUtils.isStrippable(eventMat)) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onPlayerSwapHands(Plugin plugin, Player player, double value, PlayerSwapHandItemsEvent event) {
		if (ScoreboardUtils.getScoreboardValue(player, MULTITOOL_TRIGGER_OPTION_SCORE).orElse(0) != TRIGGER_OPTION_SWAP) {
			return;
		}

		player.getInventory().setItemInMainHand(swapChecked(plugin, player, player.getInventory().getItemInMainHand()));
	}

	@Override
	public void onPlayerDropItem(Plugin plugin, Player player, double value) {
		if (ScoreboardUtils.getScoreboardValue(player, MULTITOOL_TRIGGER_OPTION_SCORE).orElse(0) != TRIGGER_OPTION_DROP) {
			return;
		}

		player.getInventory().setItemInMainHand(swapChecked(plugin, player, player.getInventory().getItemInMainHand()));
	}

	@CheckReturnValue
	public static ItemStack swapChecked(Plugin plugin, Player player, ItemStack item) {
		// You can swap your item slot in the same tick, the event will begin when you right-click the multitool item
		// and then perform actions on the swapped to item. Re-get the level for the item being changed to safeguard this.
		int level = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.MULTITOOL);
		if (level <= 0) {
			return item;
		}

		return swap(plugin, player, item);
	}

	@CheckReturnValue
	public static ItemStack swap(Plugin plugin, Player player, ItemStack item) {
		// Only allow swapping once every 2 ticks at most to prevent accidental double-swaps
		if (MetadataUtils.checkOnceInRecentTicks(plugin, player, "MultitoolMutex", 1)) {
			Material mat = getNextMaterial(item.getType(), ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.MULTITOOL));
			item = item.withType(mat);
			player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.PLAYERS, 1, 2F);
			player.updateInventory();
		}

		return item;
	}

	public static boolean isValidMultitoolMaterial(Material mat) {
		return Constants.Materials.PICKAXES.contains(mat)
			       || Constants.Materials.AXES.contains(mat)
			       || Constants.Materials.SHOVELS.contains(mat)
			       || mat == Material.COMPASS
			       || mat == Material.SHEARS;
	}

	public static Material getNextMaterial(Material mat, int level) {
		if (mat == Material.COMPASS) {
			return Material.SHEARS;
		} else if (mat == Material.SHEARS) {
			return Material.COMPASS;
		}
		String[] str = mat.toString().split("_", 2);
		if (Constants.Materials.AXES.contains(mat)) {
			return Material.valueOf(str[0] + "_SHOVEL");
		} else if (Constants.Materials.SHOVELS.contains(mat)) {
			if (level > 1) {
				return Material.valueOf(str[0] + "_PICKAXE");
			} else {
				return Material.valueOf(str[0] + "_AXE");
			}
		} else if (Constants.Materials.PICKAXES.contains(mat)) {
			return Material.valueOf(str[0] + "_AXE");
		}
		return mat;
	}

	public static Material getBaseMaterial(Material mat) {
		if (mat == Material.COMPASS || mat == Material.SHEARS) {
			return Material.COMPASS;
		}
		if (Constants.Materials.AXES.contains(mat)) {
			return mat;
		}
		if (Constants.Materials.SHOVELS.contains(mat) || Constants.Materials.PICKAXES.contains(mat)) {
			String[] str = mat.toString().split("_", 2);
			return Material.valueOf(str[0] + "_AXE");
		}
		return mat;
	}

}
