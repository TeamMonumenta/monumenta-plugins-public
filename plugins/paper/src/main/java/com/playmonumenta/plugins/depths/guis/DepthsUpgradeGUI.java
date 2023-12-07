package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class DepthsUpgradeGUI extends CustomInventory {
	private static final Material FILLER = GUIUtils.FILLER_MATERIAL;
	private final boolean mReturnToSummary;
	private final List<Integer> mSlotsUsed = new ArrayList<>();


	public DepthsUpgradeGUI(Player player, boolean fromSummaryGUI) {
		super(player, 27, "Select an Upgrade");
		mReturnToSummary = fromSummaryGUI;

		List<DepthsAbilityItem> items = DepthsManager.getInstance().getAbilityUpgradeOptions(player);
		if (items == null || items.isEmpty()) {
			return;
		} else if (items.size() == 1) {
			mSlotsUsed.add(13);
		} else if (items.size() == 2) {
			mSlotsUsed.addAll(List.of(11, 15));
		} else {
			mSlotsUsed.addAll(List.of(10, 13, 16));
		}

		GUIUtils.fillWithFiller(mInventory, true);


		mInventory.setItem(mSlotsUsed.get(0), items.get(0).mItem);

		if (items.size() > 1) {
			mInventory.setItem(mSlotsUsed.get(1), items.get(1).mItem);
		}

		if (items.size() > 2) {
			mInventory.setItem(mSlotsUsed.get(2), items.get(2).mItem);
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		if (event.getClickedInventory() != mInventory
			    || event.getCurrentItem() == null
			    || event.getCurrentItem().getType() == FILLER
			    || event.isShiftClick()) {
			return;
		}
		int slot;
		if (event.getSlot() == mSlotsUsed.get(0)) {
			slot = 0;
		} else if (event.getSlot() == mSlotsUsed.get(1)) {
			slot = 1;
		} else if (event.getSlot() == mSlotsUsed.get(2)) {
			slot = 2;
		} else {
			return;
		}

		DepthsManager.getInstance().playerUpgradedItem((Player) event.getWhoClicked(), slot);
		DepthsPlayer playerInstance = DepthsManager.getInstance().mPlayers.get(event.getWhoClicked().getUniqueId());
		event.getWhoClicked().closeInventory();
		if (playerInstance != null && playerInstance.mEarnedRewards.size() > 0) {
			DepthsManager.getInstance().getRoomReward((Player) event.getWhoClicked(), null, mReturnToSummary);
		} else if (mReturnToSummary) {
			DepthsGUICommands.summary(Plugin.getInstance(), (Player) event.getWhoClicked());
		}
	}
}
