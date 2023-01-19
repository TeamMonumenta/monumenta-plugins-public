package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class DepthsDebugGUI extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	public static class DebugGUIItem {
		int mSlot;
		Material mMaterial;
		String mName;
		String mLore;
		String mCommand;

		public DebugGUIItem(int s, Material m, String n, String l, String c) {
			mSlot = s;
			mMaterial = m;
			mName = n;
			mLore = l;
			mCommand = c;
		}
	}

	List<DebugGUIItem> GUI_ITEMS = new ArrayList<>();
	Player mPlayerToDebug;
	Plugin mPlugin;



	public DepthsDebugGUI(Player requestingPlayer, Player targetPlayer, Plugin plugin) {
		super(requestingPlayer, 27, "Depths Debug GUI");
		mPlayerToDebug = targetPlayer;
		mPlugin = plugin;

		GUI_ITEMS.add(new DebugGUIItem(10, Material.PAPER, "List Party Info", "Prints the party info for the requested player.", "partyinfo"));
		GUI_ITEMS.add(new DebugGUIItem(13, Material.PINK_DYE, "List Party Abilities", "Opens the depths summary GUI as if you are the player.", "abilityinfo"));
		GUI_ITEMS.add(new DebugGUIItem(16, Material.MAGMA_BLOCK, "List Party Delve Info", "Opens the delve GUI as if you are the player.", "delveinfo"));

		for (int i = 0; i < 27; i++) {
			mInventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1));
		}

		for (DebugGUIItem item : GUI_ITEMS) {
			createCustomItem(item);
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() != mInventory ||
			    event.getCurrentItem() == null ||
			    event.getCurrentItem().getType() == FILLER ||
			    event.isShiftClick()) {
			return;
		}
		Player player = (Player) event.getWhoClicked();
		for (DebugGUIItem item : GUI_ITEMS) {
			if (event.getSlot() == item.mSlot) {
				switch (item.mCommand) {
					case "partyinfo" -> {
						event.getWhoClicked().sendMessage(Component.text("Output from " + mPlayerToDebug.getName() + "'s Point of View:", NamedTextColor.LIGHT_PURPLE)
							.decoration(TextDecoration.ITALIC, false));
						event.getWhoClicked().sendMessage(DepthsManager.getInstance().getPartySummary(mPlayerToDebug));
						event.getWhoClicked().closeInventory();
					}
					case "abilityinfo" -> new DepthsSummaryGUI(player, mPlayerToDebug).openInventory(player, mPlugin);
					case "delveinfo" -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "openmoderatordmsgui " + player.getName() + " " + mPlayerToDebug.getName() + " depths");
					default -> {
						return;
					}
				}
			}
		}
	}

	public void createCustomItem(DebugGUIItem targetItem) {
		ItemStack item = GUIUtils.createBasicItem(targetItem.mMaterial, targetItem.mName, NamedTextColor.GOLD, targetItem.mLore);
		mInventory.setItem(targetItem.mSlot, item);
	}
}
