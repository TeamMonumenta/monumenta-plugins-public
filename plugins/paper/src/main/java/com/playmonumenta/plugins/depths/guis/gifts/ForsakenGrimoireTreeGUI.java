package com.playmonumenta.plugins.depths.guis.gifts;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.gifts.ForsakenGrimoire;
import org.bukkit.entity.Player;

public class ForsakenGrimoireTreeGUI extends AbstractDepthsSelectionGUI<DepthsTree> {
	public ForsakenGrimoireTreeGUI(Player player) {
		super(player, "Grimoire (Select Tree)", ForsakenGrimoire.ABILITY_NAME, DepthsManager.getInstance().getEligibleTrees(player), DepthsTree::createItem, false);
	}

	@Override
	protected void selected(DepthsTree selection) {
		new ForsakenGrimoireAbilityGUI(mPlayer, selection).open();
	}
}
