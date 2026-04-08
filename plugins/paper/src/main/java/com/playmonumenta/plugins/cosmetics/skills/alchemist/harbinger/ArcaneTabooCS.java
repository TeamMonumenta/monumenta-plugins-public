package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcanePotionsCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
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
	public void periodicEffects(Player player, boolean twoHertz, boolean oneSecond, int ticks, double currentSelfDamage, double absorptionLossThreshold) {
		Location loc = player.getLocation();
		if (oneSecond) {
			AbilityUtils.playPassiveAbilitySound(player, player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 1);

			new PPCircle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, -0.25, 0), 0.5)
				.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
				.directionalMode(true).delta(0, 1, 0).extra(1)
				.spawnAsPlayerPassive(player);
			new PPCircle(Particle.FALLING_OBSIDIAN_TEAR, loc.clone().add(0, 0.75, 0), 0.5)
				.count(3)
				.spawnAsPlayerPassive(player);
		}

		if (currentSelfDamage >= absorptionLossThreshold) {
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_GRAVEL_BREAK, 0.2f, 0.5f);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CHORUS_FLOWER_GROW, 2f, 1f);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CHORUS_FLOWER_DEATH, 2f, 1f);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CHORUS_FLOWER_DEATH, 2f, 1f);
		} else if (currentSelfDamage >= absorptionLossThreshold / 2 && twoHertz) {
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_GRAVEL_BREAK, 0.25f, 0.5f);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CHORUS_FLOWER_GROW, 1.5f, 1f);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CHORUS_FLOWER_GROW, 1.5f, 1f);
		} else if (oneSecond) {
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_GRAVEL_BREAK, 0.2f, 0.5f);
		}
	}

	@Override
	public void toggle(Player player, boolean active) {
		super.toggle(player, active);
		World world = player.getWorld();
		Location loc = player.getLocation();
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1, 1);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1, 1);

		if (active) {
			ArcanePotionsCS.drawAlchemyCircle(player, player.getLocation(), 3, 5, false, ArcanePotionsCS.BISMUTH, false, false, false);
		} else {
			new PartialParticle(Particle.ENCHANTMENT_TABLE, LocationUtils.getHalfHeightLocation(player), 20)
				.delta(0.2, 0.6, 0.6)
				.spawnAsPlayerActive(player);
		}
	}

}
