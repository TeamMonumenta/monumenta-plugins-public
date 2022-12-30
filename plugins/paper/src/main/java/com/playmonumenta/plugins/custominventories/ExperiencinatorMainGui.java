package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorConfig;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import static org.bukkit.ChatColor.BOLD;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.GRAY;

/*
 * Main menu for Experiencinators.
 * Layout:
 *
 * ================================================================
 * |   -  |   -  |   -  |   -  |   -  |   -  |   -  |   -  |   -  |
 * |   -  |   -  |  opt |   -  |  ca  |   -  |  sc  |   -  |   -  |
 * |   -  |   -  |   -  |   -  |   -  |   -  |   -  |   -  |   -  |
 * ================================================================
 *
 * -: filler
 * opt: options
 * ca: convert all
 * sc: selective convert
 *
 */
public final class ExperiencinatorMainGui extends CustomInventory {

	private final Player mPlayer;
	private final ExperiencinatorConfig.Experiencinator mExperiencinator;
	private final ItemStack mExperiencinatorItem;

	private ExperiencinatorMainGui(Player owner, ExperiencinatorConfig.Experiencinator experiencinator, ItemStack experiencinatorItem) {
		super(owner, 3 * 9, experiencinator.getName() + " Menu");

		mPlayer = owner;
		mExperiencinator = experiencinator;
		mExperiencinatorItem = experiencinatorItem;

		setupInventory();

	}

	private void setupInventory() {
		{
			ItemStack options = new ItemStack(Material.CRAFTING_TABLE);
			ItemMeta meta = options.getItemMeta();
			meta.setDisplayName(GOLD + "" + BOLD + "Settings");
			meta.setLore(List.of(GRAY + "Configure your " + ItemUtils.getPlainName(mExperiencinatorItem)));
			options.setItemMeta(meta);
			mInventory.setItem(11, options);
		}
		{
			ItemStack convertAll = new ItemStack(Material.GOLD_INGOT);
			ItemMeta meta = convertAll.getItemMeta();
			meta.setDisplayName(GOLD + "" + BOLD + "Convert Now");
			meta.setLore(List.of(GRAY + "Convert all items in your inventory"));
			convertAll.setItemMeta(meta);
			mInventory.setItem(13, convertAll);
		}
		{
			ItemStack selectiveConvert = new ItemStack(Material.GOLD_NUGGET);
			ItemMeta meta = selectiveConvert.getItemMeta();
			meta.setDisplayName(GOLD + "" + BOLD + "Selective Conversion");
			meta.setLore(List.of(GRAY + "Convert specific items only"));
			selectiveConvert.setItemMeta(meta);
			mInventory.setItem(15, selectiveConvert);
		}

		// fill empty slots with filler
		GUIUtils.fillWithFiller(mInventory, Material.GRAY_STAINED_GLASS_PANE);
	}

	public static void show(Player player, Plugin plugin, ExperiencinatorConfig.Experiencinator experiencinator, ItemStack experiencinatorItem) {
		ExperiencinatorConfig config = ExperiencinatorUtils.getConfig(player.getLocation());
		if (config == null) {
			return;
		}
		if (!ExperiencinatorUtils.checkExperiencinator(experiencinator, experiencinatorItem, player)) {
			return;
		}
		new ExperiencinatorMainGui(player, experiencinator, experiencinatorItem).openInventory(player, plugin);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() != mInventory) {
			return;
		}
		if (event.getClick() != ClickType.RIGHT
			    && event.getClick() != ClickType.LEFT) {
			return;
		}
		if (event.getSlot() == 11) {
			close();
			ExperiencinatorSettingsGui.showConfig(mPlayer, com.playmonumenta.scriptedquests.Plugin.getInstance(), mExperiencinator, mExperiencinatorItem);
		} else if (event.getSlot() == 13) {
			close();
			ExperiencinatorUtils.useExperiencinator(mExperiencinator, mExperiencinatorItem, mPlayer);
		} else if (event.getSlot() == 15) {
			close();
			ExperiencinatorSelectiveConvertGui.show(mPlayer, com.playmonumenta.scriptedquests.Plugin.getInstance(), mExperiencinator, mExperiencinatorItem);
		}

	}
}
