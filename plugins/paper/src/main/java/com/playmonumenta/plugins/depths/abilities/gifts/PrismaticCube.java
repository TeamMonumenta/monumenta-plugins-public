package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PrismaticCube extends DepthsAbility {
	public static final String ABILITY_NAME = "Prismatic Cube";

	public static final DepthsAbilityInfo<PrismaticCube> INFO =
		new DepthsAbilityInfo<>(PrismaticCube.class, ABILITY_NAME, PrismaticCube::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.DIAMOND_BLOCK)
			.offerable(PrismaticCube::hasActive)
			.gain(PrismaticCube::gain)
			.descriptions(PrismaticCube::getDescription);

	public PrismaticCube(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.addReward(DepthsRoomType.DepthsRewardType.CUBE);
	}

	private static boolean hasActive(Player player) {
		DepthsManager dm = DepthsManager.getInstance();
		return dm.getPlayerAbilities(player).stream()
			.anyMatch(a -> a.getDepthsTrigger().isActive() && dm.getRandomReplaceablePrismatic(a.getDepthsTrigger()) != null);
	}

	private static Description<PrismaticCube> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("Replace an active ability with a ")
			.add(DepthsTree.PRISMATIC.getNameComponent())
			.add(" ability.");
	}
}
