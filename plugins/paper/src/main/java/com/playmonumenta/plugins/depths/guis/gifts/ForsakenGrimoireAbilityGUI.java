package com.playmonumenta.plugins.depths.guis.gifts;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.gifts.ForsakenGrimoire;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class ForsakenGrimoireAbilityGUI extends AbstractDepthsSelectionGUI<DepthsAbilityInfo<?>> {
	public ForsakenGrimoireAbilityGUI(Player player, DepthsTree tree) {
		super(player, "Grimoire (Select Ability)", ForsakenGrimoire.ABILITY_NAME,
			getEligibleAbilities(player, tree), dai -> dai.createAbilityItem(3, 0, 0, player, false), true);
	}

	@Override
	protected void selected(DepthsAbilityInfo<?> selection) {
		if (mDepthsPlayer == null) {
			return;
		}
		DepthsManager.getInstance().setPlayerLevelInAbility(ForsakenGrimoire.ABILITY_NAME, mPlayer, mDepthsPlayer, 0, false, false);
		DepthsManager.getInstance().setPlayerLevelInAbility(selection.getDisplayName(), mPlayer, mDepthsPlayer, 3, true, false);
		mPlayer.playSound(mPlayer, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	private static List<DepthsAbilityInfo<?>> getEligibleAbilities(Player player, DepthsTree tree) {
		DepthsManager dm = DepthsManager.getInstance();
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return Collections.emptyList();
		}
		List<DepthsTrigger> usedTriggers = dp.mAbilities.keySet().stream()
			.map(dm::getAbility).filter(Objects::nonNull)
			.map(DepthsAbilityInfo::getDepthsTrigger).filter(Objects::nonNull)
			.toList();
		return DepthsManager.getFilteredAbilities(List.of(tree)).stream()
			.filter(ability -> ability.getDepthsTrigger().isActive())
			.filter(ability -> !usedTriggers.contains(ability.getDepthsTrigger()))
			.toList();
	}
}
