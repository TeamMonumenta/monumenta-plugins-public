package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsRarity;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class OrbOfDarkness extends DepthsAbility {
	public static final String ABILITY_NAME = "Orb of Darkness";

	public static final DepthsAbilityInfo<OrbOfDarkness> INFO =
		new DepthsAbilityInfo<>(OrbOfDarkness.class, ABILITY_NAME, OrbOfDarkness::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.ENDER_PEARL)
			.offerable(p -> DepthsManager.getInstance().hasAbilityOfTree(p, DepthsTree.PRISMATIC))
			.gain(OrbOfDarkness::gain)
			.descriptions(OrbOfDarkness::getDescription);

	public OrbOfDarkness(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsManager dm = DepthsManager.getInstance();
		DepthsPlayer dp = dm.getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dm.setPlayerLevelInAbility(ABILITY_NAME, player, dp, 0, false, false);
		for (DepthsAbilityInfo<?> dai : DepthsManager.getPrismaticAbilities()) {
			String name = dai.getDisplayName();
			if (dp.hasAbility(name)) {
				dm.setPlayerLevelInAbility(name, player, dp, 0, true, true);
				dp.addReward(DepthsRoomType.DepthsRewardType.TWISTED);
				dp.addReward(DepthsRoomType.DepthsRewardType.TWISTED);
			}
		}
		player.playSound(player, Sound.BLOCK_PORTAL_TRIGGER, 0.7f, 1.7f);
	}

	private static Description<OrbOfDarkness> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("Lose all of your ")
			.add(DepthsTree.PRISMATIC.getNameComponent())
			.add(" abilities. For every ability lost, gain two ")
			.add(DepthsRarity.TWISTED.getDisplay())
			.add(" upgrades.");
	}
}
