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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDepthsRewardGUI extends CustomInventory {

	public static final List<List<Integer>> SLOT_MAP = Arrays.asList(
		Arrays.asList(13),
		Arrays.asList(11, 15),
		Arrays.asList(10, 13, 16),
		Arrays.asList(10, 12, 14, 16),
		Arrays.asList(9, 11, 13, 15, 17)
	);

	protected static final Material FILLER = GUIUtils.FILLER_MATERIAL;
	protected final boolean mReturnToSummary;
	protected final List<Integer> mSlotsUsed;

	public AbstractDepthsRewardGUI(Player player, boolean fromSummaryGUI, String title) {
		super(player, 27, title);

		mReturnToSummary = fromSummaryGUI;

		List<DepthsAbilityItem> items = getOptions(player);
		if (items == null || items.isEmpty()) {
			mSlotsUsed = new ArrayList<>();
			return;
		}

		mSlotsUsed = SLOT_MAP.get(items.size() - 1);

		GUIUtils.fillWithFiller(mInventory, true);

		for (int i = 0; i < mSlotsUsed.size(); i++) {
			DepthsAbilityItem dai = items.get(i);
			ItemStack item;
			if (dai == null) {
				item = GUIUtils.createBasicItem(Material.BARRIER, "Do not accept the gift", NamedTextColor.RED, true);
			} else {
				item = dai.mItem;
			}
			mInventory.setItem(mSlotsUsed.get(i), item);
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

		Player player = (Player) event.getWhoClicked();
		playerClickedItem(player, slot);
		DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
		player.closeInventory();
		if (depthsPlayer != null && depthsPlayer.mEarnedRewards.size() > 0) {
			DepthsManager.getInstance().getRoomReward(player, null, mReturnToSummary);
		} else if (mReturnToSummary) {
			DepthsGUICommands.summary(Plugin.getInstance(), player);
		}
	}

	protected abstract @Nullable List<@Nullable DepthsAbilityItem> getOptions(Player player);

	protected abstract void playerClickedItem(Player player, int slot);
}
