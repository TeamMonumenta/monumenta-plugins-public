package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import java.util.List;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DepthsUpgradeGUI extends AbstractDepthsRewardGUI {

	public DepthsUpgradeGUI(Player player, boolean fromSummaryGUI) {
		super(player, fromSummaryGUI, "Select an Upgrade");
	}

	@Override
	protected @Nullable List<@Nullable DepthsAbilityItem> getOptions(Player player) {
		return DepthsManager.getInstance().getAbilityUpgradeOptions(player);
	}

	@Override
	protected void playerClickedItem(Player player, int slot) {
		DepthsManager.getInstance().playerUpgradedItem(player, slot);
	}
}
