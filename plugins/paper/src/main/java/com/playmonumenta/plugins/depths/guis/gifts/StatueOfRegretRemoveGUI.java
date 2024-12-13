package com.playmonumenta.plugins.depths.guis.gifts;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.gifts.StatueOfRegret;
import java.util.List;
import org.bukkit.entity.Player;

public class StatueOfRegretRemoveGUI extends AbstractDepthsSelectionGUI<DepthsAbilityInfo<?>> {
	public StatueOfRegretRemoveGUI(Player player) {
		super(player, "Regret (Remove Curse)", StatueOfRegret.ABILITY_NAME,
			getEligibleAbilities(player), dai -> dai.createAbilityItem(1, 0, 0, player, true), false);
	}

	@Override
	protected void selected(DepthsAbilityInfo<?> selection) {
		new StatueOfRegretReplaceGUI(mPlayer, selection.getDisplayName()).open();
	}

	private static List<DepthsAbilityInfo<?>> getEligibleAbilities(Player player) {
		return DepthsManager.getInstance().getPlayerAbilities(player).stream()
			.filter(dai -> dai.getDepthsTree() == DepthsTree.CURSE)
			.limit(8)
			.toList();
	}
}
