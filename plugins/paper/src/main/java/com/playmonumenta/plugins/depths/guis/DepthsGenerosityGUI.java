package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DepthsGenerosityGUI extends AbstractDepthsRewardGUI {

	public DepthsGenerosityGUI(Player player, boolean fromSummaryGUI) {
		super(player, fromSummaryGUI, "Accept or Reject the Gift", false);
	}

	@Override
	protected @Nullable List<@Nullable DepthsAbilityItem> getOptions() {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(mPlayer);
		if (dp == null || dp.mGenerosityGifts.isEmpty()) {
			return null;
		}
		// null is translated to the reject button
		return Arrays.asList(dp.mGenerosityGifts.get(0), null);
	}

	@Override
	protected void playerClickedItem(int slot, boolean sendMessage) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(mPlayer);
		if (dp == null) {
			return;
		}
		dp.mEarnedRewards.poll();
		if (dp.mGenerosityGifts.isEmpty()) {
			return;
		}
		if (slot == 0) {
			DepthsAbilityItem item = dp.mGenerosityGifts.get(0);
			if (dp.getLevelInAbility(item.mAbility) < item.mRarity &&
				    DepthsManager.getInstance().getPlayerAbilities(mPlayer).stream()
					    .filter(abilityInfo -> !Objects.equals(abilityInfo.getDisplayName(), item.mAbility))
					    .filter(abilityInfo -> !abilityInfo.getDepthsTrigger().equals(DepthsTrigger.PASSIVE))
					    .noneMatch(abilityInfo -> abilityInfo.getDepthsTrigger().equals(item.mTrigger))) {
				DepthsManager.getInstance().setPlayerLevelInAbility(item.mAbility, mPlayer, item.mRarity);
			}
		}
		dp.mGenerosityGifts.remove(0);
	}
}
