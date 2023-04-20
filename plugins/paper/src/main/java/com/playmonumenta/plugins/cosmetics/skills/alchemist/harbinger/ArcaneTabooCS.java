package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcanePotionsCS;
import com.playmonumenta.plugins.particle.PPCircle;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ArcaneTabooCS extends TabooCS {

	public static final String NAME = "Arcane Taboo";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Using your own life force to fuel potions",
			"is forbidden by most alchemist circles.",
			"The power gained is vast, but fleeting.",
			"And why consider it, when there are so many",
			"lesser life forms one can use as sacrifices instead?"
		);
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public void periodicEffects(Player player, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			new PPCircle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, -0.25, 0), 0.5)
				.ringMode(true).countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
				.directionalMode(true).delta(0, 1, 0).extra(1)
				.spawnAsPlayerActive(player);
			new PPCircle(Particle.FALLING_OBSIDIAN_TEAR, player.getLocation().add(0, 0.75, 0), 0.5)
				.ringMode(true).count(3)
				.spawnAsPlayerActive(player);
			// no sound for players that like to have this enabled at all times
		}
	}

}
