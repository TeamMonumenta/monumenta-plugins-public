package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class StellarDanceCS extends WindWalkCS {
	private boolean mSolar = false;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Dance with the wind, dance with",
			"the sky, dance with the stars."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGENTA_GLAZED_TERRACOTTA;
	}

	@Override
	public @Nullable String getName() {
		return "Stellar Dance";
	}

	@Override
	public void initialEffects(Player player, Location loc, World world) {
		mSolar = !mSolar;
		if (mSolar) {
			new PartialParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(player);
			drawSolar(player, loc, 10);
			world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1, 2f);
		} else {
			new PartialParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(player);
			drawCosmic(player, loc, 10);
			world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1, 1.6f);
		}
	}

	@Override
	public void trailEffect(Player player, int mTicks) {
		Location playerLocation = player.getLocation().add(0, 1, 0);
		if (mSolar) {
			drawSolar(player, player.getLocation().add(0, 1, 0), 3);
		} else {
			drawCosmic(player, player.getLocation().add(0, 1, 0), 3);
		}

		if (mTicks % 15 == 0) {
			Vector playerVelocity = player.getVelocity().normalize();
			Vector perpendicularPlaneVector = playerVelocity.clone().crossProduct(new Vector(0, 1, 0)).normalize();
			if (!Double.isFinite(perpendicularPlaneVector.getX())) {
				perpendicularPlaneVector = playerVelocity.clone().crossProduct(new Vector(1, 0, 0)).normalize();
			}
			Vector perpendicularPlaneVectorUp = perpendicularPlaneVector.clone().crossProduct(playerVelocity);

			for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 25) {
				Vector offsetX = perpendicularPlaneVector.clone().multiply(3 * FastUtils.cos(angle));
				Vector offsetZ = perpendicularPlaneVectorUp.clone().multiply(3 * FastUtils.sin(angle));
				if (mSolar) {
					drawSolar(player, playerLocation.clone().add(offsetX).add(offsetZ), 2);
				} else {
					drawCosmic(player, playerLocation.clone().add(offsetX).add(offsetZ), 2);
				}
			}
		}
	}

	private void drawCosmic(Player player, Location loc, int countMultiplier) {
		switch (FastUtils.randomIntInRange(0, 5)) {
			case 0 -> new PartialParticle(Particle.CRIT_MAGIC, loc, 5 * countMultiplier, 0.1, 0.1, 0.1, 0).minimumCount(0).spawnAsPlayerActive(player);
			case 1 -> new PartialParticle(Particle.SPELL_WITCH, loc, 5 * countMultiplier, 0.1, 0.1, 0.1, 0).minimumCount(0).spawnAsPlayerActive(player);
			case 2 -> new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 5 * countMultiplier, 0.1, 0.1, 0.1, 0).minimumCount(0).spawnAsPlayerActive(player);
			default -> new PartialParticle(Particle.REDSTONE, loc, 3 * countMultiplier, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(rollCosmicColor(), 1.5f)).minimumCount(0).spawnAsPlayerActive(player);
		}
	}

	private void drawSolar(Player player, Location loc, int countMultiplier) {
		switch (FastUtils.randomIntInRange(0, 5)) {
			case 0 -> new PartialParticle(Particle.CRIT, loc, 5 * countMultiplier, 0.1, 0.1, 0.1, 0).minimumCount(0).spawnAsPlayerActive(player);
			case 1 -> new PartialParticle(Particle.ELECTRIC_SPARK, loc, 5 * countMultiplier, 0.1, 0.1, 0.1, 0).minimumCount(0).spawnAsPlayerActive(player);
			case 2 -> new PartialParticle(Particle.SMALL_FLAME, loc, 5 * countMultiplier, 0.1, 0.1, 0.1, 0).minimumCount(0).spawnAsPlayerActive(player);
			default -> new PartialParticle(Particle.REDSTONE, loc, 3 * countMultiplier, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(rollSolarColor(), 1.5f)).minimumCount(0).spawnAsPlayerActive(player);
		}
	}

	private Color rollCosmicColor() {
		return Color.fromRGB(80 + FastUtils.randomIntInRange(0, 160), 80, 200);
	}

	private Color rollSolarColor() {
		int randColorGen = FastUtils.randomIntInRange(0, 160);
		return Color.fromRGB(240, randColorGen <= 120 ? 80 + randColorGen : 80, randColorGen > 120 ? randColorGen - 40 : 80);
	}
}
