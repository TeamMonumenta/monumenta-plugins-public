package com.playmonumenta.plugins.depths.guis.gifts;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.gifts.PoetsQuill;
import org.bukkit.entity.Player;

public class PoetsQuillRemoveGUI extends AbstractDepthsSelectionGUI<DepthsTree> {
	public PoetsQuillRemoveGUI(Player player) {
		super(player, "Poet's Quill (Remove Tree)", PoetsQuill.ABILITY_NAME, DepthsManager.getInstance().getEligibleTrees(player), DepthsTree::createItem, false);
	}

	@Override
	protected void selected(DepthsTree selection) {
		new PoetsQuillReplaceGUI(mPlayer, selection).open();
	}
}
