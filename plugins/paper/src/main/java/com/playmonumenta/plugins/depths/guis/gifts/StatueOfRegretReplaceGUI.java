package com.playmonumenta.plugins.depths.guis.gifts;

import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.gifts.StatueOfRegret;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class StatueOfRegretReplaceGUI extends AbstractDepthsSelectionGUI<DepthsAbilityInfo<?>> {
	private final @Nullable String mRemovingCurse;

	public StatueOfRegretReplaceGUI(Player player, @Nullable String removingCurse) {
		super(player, "Regret (Replace Curse)", StatueOfRegret.ABILITY_NAME,
			getEligibleAbilities(player, removingCurse), dai -> dai.getAbilityDisplayItem(1, player), true);
		mRemovingCurse = removingCurse;
	}

	@Override
	protected void selected(DepthsAbilityInfo<?> selection) {
		if (mDepthsPlayer == null) {
			return;
		}
		DepthsManager.getInstance().setPlayerLevelInAbility(StatueOfRegret.ABILITY_NAME, mPlayer, mDepthsPlayer, 0, false, false);
		DepthsManager.getInstance().setPlayerLevelInAbility(mRemovingCurse, mPlayer, mDepthsPlayer, 0, true, false);
		DepthsManager.getInstance().setPlayerLevelInAbility(selection.getDisplayName(), mPlayer, mDepthsPlayer, 1, true, false);
		mPlayer.playSound(mPlayer, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		mDepthsPlayer.mRegretSelections.clear();
	}

	private static List<DepthsAbilityInfo<?>> getEligibleAbilities(Player player, @Nullable String removingCurse) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp != null) {
			List<DepthsAbilityInfo<?>> curses = DepthsManager.getFilteredAbilities(List.of(DepthsTree.CURSE)).stream()
				.filter(dai -> dai.getDisplayName() != null && !dai.getDisplayName().equals(removingCurse) && !dp.hasAbility(dai.getDisplayName()))
				.collect(Collectors.toList());
			if (dp.mRegretSelections.isEmpty()) {
				Collections.shuffle(curses);
				curses = curses.subList(0, Math.min(5, curses.size()));
				for (DepthsAbilityInfo<?> dai : curses) {
					dp.mRegretSelections.add(dai.getDisplayName());
				}
				return curses;
			} else {
				List<String> curseNames = curses.stream().map(AbilityInfo::getDisplayName).filter(Objects::nonNull).toList();
				dp.mRegretSelections.removeIf(c -> !curseNames.contains(c));
				return dp.mRegretSelections.stream()
					.map(name -> DepthsManager.getInstance().getAbility(name))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			}
		}
		return new ArrayList<>();
	}
}
