package com.playmonumenta.plugins.depths.guis.gifts;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.gifts.CallicarpasPointedHat;
import org.bukkit.entity.Player;

public class CallicarpasPointedHatGUI extends AbstractDepthsSelectionGUI<DepthsTree> {
	public CallicarpasPointedHatGUI(Player player) {
		super(player, "Pointed Hat (Select Tree)", CallicarpasPointedHat.ABILITY_NAME, DepthsManager.getInstance().getEligibleTrees(player), DepthsTree::createItem, true);
	}

	@Override
	protected void selected(DepthsTree selection) {
		if (mDepthsPlayer == null) {
			return;
		}
		mDepthsPlayer.mPointedHatTree = selection;
		mDepthsPlayer.mPointedHatStacks = 3;
	}
}
