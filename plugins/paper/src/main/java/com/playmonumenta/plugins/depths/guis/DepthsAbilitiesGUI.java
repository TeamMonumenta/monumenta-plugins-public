package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import java.util.List;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DepthsAbilitiesGUI extends AbstractDepthsRewardGUI {

	public DepthsAbilitiesGUI(Player player, boolean fromSummaryGUI) {
		super(player, fromSummaryGUI, "Select an Ability");
	}

	@Override
	protected @Nullable List<@Nullable DepthsAbilityItem> getOptions(Player player) {
		return DepthsManager.getInstance().getAbilityUnlocks(player);
	}

	@Override
	protected void playerClickedItem(Player player, int slot) {
		DepthsManager.getInstance().playerChoseItem(player, slot);
	}
}
