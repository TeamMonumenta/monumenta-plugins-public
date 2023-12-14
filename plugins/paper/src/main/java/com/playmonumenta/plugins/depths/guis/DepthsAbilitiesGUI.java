package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class DepthsAbilitiesGUI extends CustomInventory {

	// Also used in DepthsUpgradeGUI
	public static final List<List<Integer>> SLOT_MAP = Arrays.asList(
		Arrays.asList(13),
		Arrays.asList(11, 15),
		Arrays.asList(10, 13, 16),
		Arrays.asList(10, 12, 14, 16),
		Arrays.asList(9, 11, 13, 15, 17)
	);

	private static final Material FILLER = GUIUtils.FILLER_MATERIAL;
	private final boolean mReturnToSummary;
	private final List<Integer> mSlotsUsed;

	public DepthsAbilitiesGUI(Player player, boolean fromSummaryGUI) {
		super(player, 27, "Select an Ability");
		mReturnToSummary = fromSummaryGUI;

		List<DepthsAbilityItem> items = DepthsManager.getInstance().getAbilityUnlocks(player);
		if (items == null || items.isEmpty()) {
			mSlotsUsed = new ArrayList<>();
			return;
		}

		mSlotsUsed = DepthsAbilitiesGUI.SLOT_MAP.get(items.size() - 1);

		GUIUtils.fillWithFiller(mInventory, true);

		for (int i = 0; i < mSlotsUsed.size(); i++) {
			mInventory.setItem(mSlotsUsed.get(i), items.get(i).mItem);
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		if (event.getClickedInventory() != mInventory ||
			    event.getCurrentItem() == null ||
			    event.getCurrentItem().getType() == FILLER ||
			    event.isShiftClick()) {
			return;
		}

		int slot = -1;
		for (int i = 0; i < mSlotsUsed.size(); i++) {
			if (event.getSlot() == mSlotsUsed.get(i)) {
				slot = i;
				break;
			}
		}
		if (slot < 0) {
			return;
		}

		DepthsManager.getInstance().playerChoseItem((Player) event.getWhoClicked(), slot);
		DepthsPlayer playerInstance = DepthsManager.getInstance().mPlayers.get(event.getWhoClicked().getUniqueId());
		event.getWhoClicked().closeInventory();
		if (playerInstance != null && playerInstance.mEarnedRewards.size() > 0) {
			DepthsManager.getInstance().getRoomReward((Player) event.getWhoClicked(), null, mReturnToSummary);
		} else if (mReturnToSummary) {
			DepthsGUICommands.summary(Plugin.getInstance(), (Player) event.getWhoClicked());
		}
	}
}
