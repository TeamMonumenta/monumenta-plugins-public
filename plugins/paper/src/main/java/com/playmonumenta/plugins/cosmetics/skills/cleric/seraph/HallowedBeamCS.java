package com.playmonumenta.plugins.cosmetics.skills.cleric.seraph;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class HallowedBeamCS implements CosmeticSkill {

	private static final int mDelay = 7;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.HALLOWED_BEAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BOW;
	}

	public void beamCast(Player player, Location startLocation, double range, Location targetLocation) {
		final double mShiftStart = 1.5;
		World world = player.getWorld();
		world.playSound(startLocation, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.2f, 2.0f);
		world.playSound(startLocation, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.5f, 1.0f);
		world.playSound(startLocation, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.5f, 2.0f);
		new PPLine(Particle.VILLAGER_HAPPY, startLocation, targetLocation)
			.includeStart(false)
			.shiftStart(mShiftStart)
			.shift(-0.75)
			.offset(FastUtils.randomDoubleInRange(0, 0.75))
			.countPerMeter(2.5)
			.delay(mDelay)
			.spawnAsPlayerActive(player);
		// Composter, Villager_happy
		new PPLine(Particle.SCRAPE, startLocation, targetLocation)
			.includeStart(false)
			.shiftStart(mShiftStart)
			.shift(-0.75)
			.delay(mDelay)
			.offset(FastUtils.randomDoubleInRange(0, 0.75))
			.countPerMeter(2)
			.spawnAsPlayerActive(player);
		new PPLine(Particle.GLOW, startLocation, targetLocation)
			.includeStart(false)
			.shiftStart(mShiftStart)
			.shift(-0.75)
			.delay(mDelay)
			.offset(FastUtils.randomDoubleInRange(0, 0.75))
			.countPerMeter(4)
			.deltaVariance(true)
			.delta(0.3)
			.spawnAsPlayerActive(player);
	}

	public void beamSplash(Player player, Location targetLocation, double radius) {
		new PartialParticle(Particle.EXPLOSION_LARGE, targetLocation.clone().add(0, 0.5, 0)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, targetLocation.clone().add(0, 0.6, 0), 20, 0.7, 0.5, 0.7, 0.15).spawnAsPlayerActive(player);
		new PPCircle(Particle.CLOUD, targetLocation.clone().add(0, 0.1, 0), 0.25 * radius).delta(0.5, 0, 0).extra(0.35).directionalMode(true).rotateDelta(true).countPerMeter(2).spawnAsPlayerActive(player);
		new PPCircle(Particle.SCRAPE, targetLocation.clone().add(0, 0.1, 0), radius).delta(0.5, 0, 0).extraRange(0.8, 1).directionalMode(true).rotateDelta(true).countPerMeter(1).spawnAsPlayerActive(player);
		new PPCircle(Particle.VILLAGER_HAPPY, targetLocation.clone().add(0, 0.1, 0), 0.15 * radius).delta(0.02).countPerMeter(2).spawnAsPlayerActive(player);
		World world = player.getWorld();
		world.playSound(targetLocation, Sound.ITEM_TOTEM_USE, 0.6f, 2f);
		world.playSound(targetLocation, Sound.ITEM_TRIDENT_RETURN, 1.2f, 1.2f);
		world.playSound(targetLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.1f, 1.5f);
		world.playSound(targetLocation, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.5f, 0.4f);
	}
}
