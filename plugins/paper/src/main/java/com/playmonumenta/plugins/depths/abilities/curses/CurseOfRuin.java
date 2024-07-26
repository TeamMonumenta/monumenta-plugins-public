package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CurseOfRuin extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Ruin";

	public static final DepthsAbilityInfo<CurseOfRuin> INFO =
		new DepthsAbilityInfo<>(CurseOfRuin.class, ABILITY_NAME, CurseOfRuin::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.CRACKED_NETHER_BRICKS)
			.canBeOfferedPastFloor1(false)
			.descriptions(CurseOfRuin::getDescription);

	public CurseOfRuin(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void downgrade(DepthsPlayer dp) {
		List<String> abilities = new ArrayList<>(dp.mAbilities.keySet());
		Collections.shuffle(abilities);
		while (!abilities.isEmpty()) {
			String ability = abilities.remove(0);
			int level = dp.mAbilities.getOrDefault(ability, 0);
			if (level <= 1 || level >= 6) {
				continue;
			}
			DepthsAbilityInfo<?> info = DepthsManager.getInstance().getAbility(ability);
			if (info == null || !info.getHasLevels()) {
				continue;
			}
			int newRarity = level - 1;
			DepthsManager.getInstance().setPlayerLevelInAbility(ability, mPlayer, newRarity, true, true);
			return;
		}
	}

	private static Description<CurseOfRuin> getDescription() {
		return new DescriptionBuilder<CurseOfRuin>()
			.add("When you select an ability or upgrade, downgrade a random ability by 1 level.");
	}
}
