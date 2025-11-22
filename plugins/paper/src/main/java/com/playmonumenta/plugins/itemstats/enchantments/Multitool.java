package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.CheckReturnValue;

/*
 * Multitool - Level one allows you to swap the tool
 * between an axe and shovel, level 2 adds a pickaxe
 * to the rotation. Swapping tools keeps the same
 * Name/Lore/Stats.
 */
public class Multitool implements Enchantment {

	public static final String MULTITOOL_DISABLE_AUTO_SWAP_TAG = "DisableMultitoolAutoSwap";

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
		// Ignore offhand
		if (!EquipmentSlot.HAND.equals(event.getHand())) {
			return;
		}

		// Swap to best tool when left-clicking a block
		if (Action.LEFT_CLICK_BLOCK.equals(event.getAction())) {
			onLeftClickBlock(player, event);
			return;
		}

		// Ignore right click event if that is not the multitool trigger
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

	public static void onSwapInInventory(InventoryClickEvent event, Player player, ItemStack item) {
		Material mat = item.getType();
		int level = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.MULTITOOL);
		if (getMaterials(mat, level).contains(Material.COMPASS)) {
			return;
		}

		if (ScoreboardUtils.toggleTag(player, MULTITOOL_DISABLE_AUTO_SWAP_TAG)) {
			player.sendMessage(Component.text("Multitool Auto-Swap disabled.", NamedTextColor.GOLD));
		} else {
			player.sendMessage(Component.text("Multitool Auto-Swap enabled.", NamedTextColor.GOLD));
		}

		player.playSound(player, Sound.BLOCK_CHEST_CLOSE, SoundCategory.PLAYERS, 1.0f, 2.0F);
		event.setCancelled(true);
	}

	private void onLeftClickBlock(Player player, PlayerInteractEvent event) {
		if (player.getScoreboardTags().contains(MULTITOOL_DISABLE_AUTO_SWAP_TAG)) {
			return;
		}

		Block block = event.getClickedBlock();
		if (block == null) {
			return;
		}
		Material blockMat = block.getType();
		ItemStack tool = player.getInventory().getItemInMainHand();
		Material toolMat = tool.getType();
		int multitoolLevel = ItemStatUtils.getEnchantmentLevel(tool, EnchantmentType.MULTITOOL);
		Material bestToolMat = getBestMaterial(blockMat, toolMat, multitoolLevel);
		if (toolMat.equals(bestToolMat)) {
			return;
		}

		tool = tool.withType(bestToolMat);
		player.getInventory().setItemInMainHand(tool);
		playSwapSound(player, bestToolMat);
		player.updateInventory();
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
			playSwapSound(player, mat);
			player.updateInventory();
		}

		return item;
	}

	public static void playSwapSound(Player player, Material newMaterial) {
		player.playSound(player, Sound.BLOCK_CHEST_CLOSE, SoundCategory.PLAYERS, 1.0f, 2.0F);
		if (Tag.ITEMS_AXES.isTagged(newMaterial)) {
			player.playSound(player, Sound.BLOCK_WOOD_BREAK, SoundCategory.PLAYERS, 1.0f, 0.707f);
		} else if (Tag.ITEMS_SHOVELS.isTagged(newMaterial)) {
			player.playSound(player, Sound.ITEM_SHOVEL_FLATTEN, SoundCategory.PLAYERS, 0.5f, 1.414f);
		} else if (Tag.ITEMS_PICKAXES.isTagged(newMaterial)) {
			player.playSound(player, Sound.BLOCK_STONE_HIT, SoundCategory.PLAYERS, 1.0f, 0.707f);
		} else if (Material.SHEARS.equals(newMaterial)) {
			player.playSound(player, Sound.BLOCK_GRASS_HIT, SoundCategory.PLAYERS, 1.0f, 0.707f);
		} else {
			player.playSound(player, Sound.UI_LOOM_SELECT_PATTERN, SoundCategory.PLAYERS, 1.0f, 1.414f);
		}
	}

	public static boolean isValidMultitoolMaterial(Material mat) {
		return Tag.ITEMS_PICKAXES.isTagged(mat)
			|| Tag.ITEMS_AXES.isTagged(mat)
			|| Tag.ITEMS_SHOVELS.isTagged(mat)
			|| mat == Material.COMPASS
			|| mat == Material.SHEARS;
	}

	public static List<Material> getMaterials(Material mat, int level) {
		String[] str = mat.toString().split("_", 2);
		if (level == 1) {
			if (Material.COMPASS.equals(mat) || Material.SHEARS.equals(mat)) {
				return List.of(Material.COMPASS, Material.SHEARS);
			}
			if (
				Tag.ITEMS_AXES.isTagged(mat)
					|| Tag.ITEMS_SHOVELS.isTagged(mat)
			) {
				return List.of(
					Material.valueOf(str[0] + "_AXE"),
					Material.valueOf(str[0] + "_SHOVEL")
				);
			}
		}
		if (level == 2) {
			if (
				Tag.ITEMS_AXES.isTagged(mat)
					|| Tag.ITEMS_SHOVELS.isTagged(mat)
					|| Tag.ITEMS_PICKAXES.isTagged(mat)
			) {
				return List.of(
					Material.valueOf(str[0] + "_AXE"),
					Material.valueOf(str[0] + "_SHOVEL"),
					Material.valueOf(str[0] + "_PICKAXE")
				);
			}
		}
		return List.of(mat);
	}

	public static Material getNextMaterial(Material mat, int level) {
		List<Material> materials = getMaterials(mat, level);
		int numMats = materials.size();
		int oldIndex = materials.indexOf(mat);
		int index = (oldIndex + 1) % numMats;
		return materials.get(index);
	}

	public static Material getBestMaterial(Material blockMat, Material toolMat, int level) {
		if (Material.COMPASS.equals(toolMat)) {
			// Never auto-swap a compass
			return toolMat;
		}

		// Skip any block that can always be instantly mined (or can't be mined, which is conveniently negative)
		if (blockMat.getHardness() <= 0.0f) {
			return toolMat;
		}

		// Get a list of acceptable tools; there may be more than one
		List<Material> bestMats = new ArrayList<>();
		for (Material testMat : getMaterials(toolMat, level)) {
			if (Tag.ITEMS_AXES.isTagged(testMat) && Tag.MINEABLE_AXE.isTagged(blockMat)) {
				bestMats.add(testMat);
			}
			if (Tag.ITEMS_SHOVELS.isTagged(testMat) && Tag.MINEABLE_SHOVEL.isTagged(blockMat)) {
				bestMats.add(testMat);
			}
			if (Tag.ITEMS_PICKAXES.isTagged(testMat) && Tag.MINEABLE_PICKAXE.isTagged(blockMat)) {
				bestMats.add(testMat);
			}
		}

		// If the list of acceptable tools is empty or includes the current tool, don't switch...
		Material bestMat = toolMat;
		if (!bestMats.isEmpty() && !bestMats.contains(toolMat)) {
			// ...otherwise, pick any of the valid tools
			bestMat = bestMats.get(0);
		}

		return bestMat;
	}

	public static Material getBaseMaterial(Material mat) {
		if (mat == Material.COMPASS || mat == Material.SHEARS) {
			return Material.COMPASS;
		}
		if (Tag.ITEMS_AXES.isTagged(mat)) {
			return mat;
		}
		if (Tag.ITEMS_SHOVELS.isTagged(mat) || Tag.ITEMS_PICKAXES.isTagged(mat)) {
			String[] str = mat.toString().split("_", 2);
			return Material.valueOf(str[0] + "_AXE");
		}
		return mat;
	}

}
