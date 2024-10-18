package com.playmonumenta.plugins.depths.guis.gifts;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.gifts.PoetsQuill;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;

public class PoetsQuillReplaceGUI extends AbstractDepthsSelectionGUI<DepthsTree> {
	private final DepthsTree mOldTree;

	public PoetsQuillReplaceGUI(Player player, DepthsTree oldTree) {
		super(player, "Poet's Quill (Replace Tree)", PoetsQuill.ABILITY_NAME, availableTrees(player, oldTree), DepthsTree::createItem, true);
		mOldTree = oldTree;
	}

	@Override
	protected void selected(DepthsTree selection) {
		if (mDepthsPlayer == null) {
			return;
		}
		DepthsManager.getInstance().setPlayerLevelInAbility(PoetsQuill.ABILITY_NAME, mPlayer, 0, false);
		// remove old tree
		mDepthsPlayer.mEligibleTrees.remove(mOldTree);
		DepthsManager dm = DepthsManager.getInstance();
		for (DepthsAbilityInfo<?> dai : DepthsManager.getAbilities()) {
			String name = dai.getDisplayName();
			if (mOldTree == dai.getDepthsTree() && mDepthsPlayer.getLevelInAbility(name) > 0) {
				dm.setPlayerLevelInAbility(name, mPlayer, 0, true, true);
			}
		}
		// add new tree
		mDepthsPlayer.mEligibleTrees.add(selection);
	}

	private static List<DepthsTree> availableTrees(Player player, DepthsTree oldTree) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return Collections.emptyList();
		}
		return Arrays.stream(DepthsTree.OWNABLE_TREES).filter(tree -> !dp.mEligibleTrees.contains(tree) || tree == oldTree).toList();
	}
}
