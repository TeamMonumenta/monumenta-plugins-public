package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import java.util.List;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DepthsAbilitiesGUI extends AbstractDepthsRewardGUI {

	public DepthsAbilitiesGUI(Player player, boolean fromSummaryGUI) {
		super(player, fromSummaryGUI, "Select an Ability", true);
	}

	@Override
	protected @Nullable List<@Nullable DepthsAbilityItem> getOptions() {
		return DepthsManager.getInstance().getAbilityUnlocks(mPlayer);
	}

	@Override
	protected void playerClickedItem(int slot, boolean sendMessage) {
		DepthsManager.getInstance().playerChoseItem(mPlayer, slot, sendMessage);
	}
}
