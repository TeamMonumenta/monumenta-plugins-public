package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DepthsGenerosityGUI extends AbstractDepthsRewardGUI {

	public DepthsGenerosityGUI(Player player, boolean fromSummaryGUI) {
		super(player, fromSummaryGUI, "Accept or Reject the Gift");
	}

	@Override
	protected @Nullable List<@Nullable DepthsAbilityItem> getOptions(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null || dp.mGenerosityGifts.isEmpty()) {
			return null;
		}
		// null is translated to the reject button
		return Arrays.asList(dp.mGenerosityGifts.get(0), null);
	}

	@Override
	protected void playerClickedItem(Player player, int slot) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mEarnedRewards.poll();
		if (dp.mGenerosityGifts.isEmpty()) {
			return;
		}
		if (slot == 0) {
			DepthsAbilityItem item = dp.mGenerosityGifts.get(0);
			if (DepthsManager.getInstance().getPlayerLevelInAbility(item.mAbility, player) < item.mRarity &&
				    DepthsManager.getInstance().getPlayerAbilities(player).stream()
					    .filter(abilityInfo -> !abilityInfo.getDepthsTrigger().equals(DepthsTrigger.PASSIVE))
					    .noneMatch(abilityInfo -> abilityInfo.getDepthsTrigger().equals(item.mTrigger))) {
				DepthsManager.getInstance().setPlayerLevelInAbility(item.mAbility, player, item.mRarity);
			}
		}
		dp.mGenerosityGifts.remove(0);
	}
}
