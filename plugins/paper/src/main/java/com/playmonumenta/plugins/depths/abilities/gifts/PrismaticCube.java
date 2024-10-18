package com.playmonumenta.plugins.depths.abilities.gifts;

import com.comphenix.protocol.wrappers.Pair;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PrismaticCube extends DepthsAbility {
	public static final String ABILITY_NAME = "Prismatic Cube";

	public static final DepthsAbilityInfo<PrismaticCube> INFO =
		new DepthsAbilityInfo<>(PrismaticCube.class, ABILITY_NAME, PrismaticCube::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.DIAMOND_BLOCK)
			.offerable(player -> DepthsManager.getInstance().getRandomReplaceablePrismatic(player) != null)
			.gain(PrismaticCube::gain)
			.descriptions(PrismaticCube::getDescription);

	public PrismaticCube(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsManager dm = DepthsManager.getInstance();
		Pair<String, String> prismaticReplacement = dm.getRandomReplaceablePrismatic(player);
		if (prismaticReplacement != null) {
			int oldRarity = dm.getPlayerLevelInAbility(prismaticReplacement.getFirst(), player);
			dm.setPlayerLevelInAbility(prismaticReplacement.getFirst(), player, 0, true, true);
			dm.setPlayerLevelInAbility(prismaticReplacement.getSecond(), player, oldRarity, true, true);
		}
		dm.setPlayerLevelInAbility(ABILITY_NAME, player, 0, false);
	}

	private static Description<PrismaticCube> getDescription() {
		return new DescriptionBuilder<PrismaticCube>().add("Replace one random active ability with a ")
			.add(DepthsTree.PRISMATIC.getNameComponent())
			.add(" ability.");
	}
}
