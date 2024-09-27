package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.gui.PlayerItemStatsGUI;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;

public class PlayerInventoryCustomInventory extends CustomInventory {
	private static final int BACK_LOC = 0;
	private final boolean mFromPDGUI;
	private final Player mRequestingPlayer;
	private final Player mTargetPlayer;

	public PlayerInventoryCustomInventory(Player requestingPlayer, Player clickedPlayer, boolean fromPDGUI) {
		super(requestingPlayer, 27, clickedPlayer.getName() + "'s Inventory");
		mFromPDGUI = fromPDGUI;
		mRequestingPlayer = requestingPlayer;
		mTargetPlayer = clickedPlayer;
		setLayout(clickedPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		if (event.getSlot() == BACK_LOC && mFromPDGUI) {
			mInventory.close();
			new PlayerDisplayCustomInventory(mRequestingPlayer, mTargetPlayer).openInventory(mRequestingPlayer, Plugin.getInstance());
		}
	}

	public void setLayout(Player clickedPlayer) {
		mInventory.clear();
		//Added for tracking to prevent them from clicking on stuff

		PlayerInventory playInv = clickedPlayer.getInventory();
		boolean showVanity = Plugin.getInstance().mVanityManager.getData(clickedPlayer).mGuiVanityEnabled;

		//Set the fake inventory's top row to be the armor and offhand of the player
		mInventory.setItem(9, PlayerItemStatsGUI.getPlayerItemWithVanity(clickedPlayer, EquipmentSlot.HEAD, showVanity));
		mInventory.setItem(10, PlayerItemStatsGUI.getPlayerItemWithVanity(clickedPlayer, EquipmentSlot.CHEST, showVanity));
		mInventory.setItem(11, PlayerItemStatsGUI.getPlayerItemWithVanity(clickedPlayer, EquipmentSlot.LEGS, showVanity));
		mInventory.setItem(12, PlayerItemStatsGUI.getPlayerItemWithVanity(clickedPlayer, EquipmentSlot.FEET, showVanity));
		mInventory.setItem(13, PlayerItemStatsGUI.getPlayerItemWithVanity(clickedPlayer, EquipmentSlot.OFF_HAND, showVanity));

		//Set the fake inventory's bottom row to be the players hotbar
		mInventory.setItem(18, playInv.getItem(0));
		mInventory.setItem(19, playInv.getItem(1));
		mInventory.setItem(20, playInv.getItem(2));
		mInventory.setItem(21, playInv.getItem(3));
		mInventory.setItem(22, playInv.getItem(4));
		mInventory.setItem(23, playInv.getItem(5));
		mInventory.setItem(24, playInv.getItem(6));
		mInventory.setItem(25, playInv.getItem(7));
		mInventory.setItem(26, playInv.getItem(8));

		if (mFromPDGUI) {
			mInventory.setItem(BACK_LOC, GUIUtils.createBasicItem(Material.ARROW, "Back to Player Details GUI",
				NamedTextColor.GRAY, true, "", NamedTextColor.WHITE));
		}
	}
}
