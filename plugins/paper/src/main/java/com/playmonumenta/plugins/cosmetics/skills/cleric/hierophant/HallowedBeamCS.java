package com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class HallowedBeamCS implements CosmeticSkill {

	private static final int mDelay = 13;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.HALLOWED_BEAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BOW;
	}

	public void beamHealEffect(Player player, LivingEntity entity, Vector dir, double range, Location targetLocation) {
		final double mShiftStart = 1.5;
		World world = player.getWorld();
		Location eyeLoc = player.getEyeLocation();
		world.playSound(eyeLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.2f, 2.0f);
		world.playSound(eyeLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.5f, 1.0f);
		world.playSound(eyeLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.5f, 2.0f);
		new PPLine(Particle.VILLAGER_HAPPY, eyeLoc, targetLocation)
			.includeStart(false)
			.shiftStart(mShiftStart)
			.shift(-0.75)
			.offset(FastUtils.randomDoubleInRange(0, 0.75))
			.countPerMeter(2.5)
			.delay(mDelay)
			.spawnAsPlayerActive(player);
		// Composter, Villager_happy
		new PPLine(Particle.SCRAPE, eyeLoc, targetLocation)
			.includeStart(false)
			.shiftStart(mShiftStart)
			.shift(-0.75)
			.delay(mDelay)
			.offset(FastUtils.randomDoubleInRange(0, 0.75))
			.countPerMeter(2)
			.spawnAsPlayerActive(player);
		new PPLine(Particle.GLOW, eyeLoc, targetLocation)
			.includeStart(false)
			.shiftStart(mShiftStart)
			.shift(-0.75)
			.delay(mDelay)
			.offset(FastUtils.randomDoubleInRange(0, 0.75))
			.countPerMeter(4)
			.deltaVariance(true)
			.delta(0.3)
			.spawnAsPlayerActive(player);

		// Effect on target
		world.playSound(targetLocation, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.3f, 1.2f);
		world.playSound(targetLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.1f, 1.5f);
		world.playSound(targetLocation, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.5f, 0.4f);
	}

	public void beamHarm(Player player, LivingEntity entity, Vector dir, double radius, Location targetLocation) {
		final double mShiftStart = 1;
		World world = player.getWorld();
		Location eyeLoc = player.getEyeLocation();
		world.playSound(eyeLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.4f, 1.3f);
		world.playSound(eyeLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.5f, 0.6f);
		world.playSound(eyeLoc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 0.3f, 1.5f);
		world.playSound(eyeLoc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 0.1f, 2.0f);
		new PPLine(Particle.END_ROD, eyeLoc, targetLocation)
			.includeStart(false)
			.shiftStart(mShiftStart)
			.delay(mDelay)
			.offset(FastUtils.RANDOM.nextDouble())
			.countPerMeter(2.5)
			.delta(0.01)
			.spawnAsPlayerActive(player);
		new PPLine(Particle.CRIT, eyeLoc, targetLocation)
			.includeStart(false)
			.shiftStart(mShiftStart)
			.delay(mDelay)
			.offset(FastUtils.RANDOM.nextDouble())
			.countPerMeter(2)
			.deltaVariance(true)
			.delta(0.3)
			.spawnAsPlayerActive(player);
		new PPLine(Particle.SMALL_FLAME, eyeLoc, targetLocation)
			.includeStart(false)
			.shiftStart(mShiftStart)
			.delay(mDelay)
			.offset(FastUtils.RANDOM.nextDouble())
			.countPerMeter(6)
			.deltaVariance(true)
			.delta(0.3)
			.spawnAsPlayerActive(player);

		world.playSound(targetLocation, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.3f, 1.2f);
		world.playSound(targetLocation, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.7f, 1.2f);
		world.playSound(targetLocation, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.5f, 0.4f);
		world.playSound(targetLocation, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.5f, 0.1f);
		world.playSound(targetLocation, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.1f, 1.1f);
		world.playSound(targetLocation, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 1.4f, 2.0f);
	}

	public void beamHealTarget(Player player, Player target, Location targetLocation) {
		Location headLoc = target.getEyeLocation().add(0, 0.1, 0);
		Location ringLoc = LocationUtils.getHeightLocation(target, 0.2);
		new PPCircle(Particle.HEART, headLoc, 0.75)
			.ringMode(true)
			.countPerMeter(0.5)
			.delta(0, 0.2, 0)
			.deltaVariance(true, true, true, false, true, true)
			.spawnAsPlayerActive(player);
		// Are you here because someone complained about the totem particles getting in the way? Just spawn them for everyone but the target player! :3
		new PPParametric(Particle.TOTEM, ringLoc, (parameter, builder) -> {
				double theta = parameter * Math.PI * 2;
				builder.offset(FastUtils.cos(theta), builder.offsetY(), FastUtils.sin(theta));
			})
			.delta(0, 1.4, 0)
			.directionalMode(true)
			.extra(0.4)
			.count(30)
			.spawnAsPlayerActive(player);
	}

	public void beamHarmCrusade(Player player, LivingEntity target, Location targetLocation) {
		beamHarmEffect(player, target, targetLocation);
	}

	public void beamHarmOther(Player player, LivingEntity target, Location targetLocation) {
		beamHarmEffect(player, target, targetLocation);
	}

	private void beamHarmEffect(Player player, LivingEntity target, Location targetLocation) {
		Location headLoc = target.getEyeLocation().add(0, 0.1, 0);
		new PPCircle(Particle.FLAME, headLoc, 0.75)
			.ringMode(true)
			.countPerMeter(0.75)
			.delta(0, 0.2, 0)
			.deltaVariance(true, true, true, false, true, true)
			.spawnAsPlayerActive(player);
		new PPParametric(Particle.SMALL_FLAME, targetLocation, (parameter, builder) -> {
				double theta = parameter * Math.PI * 2;
				builder.offset(FastUtils.cos(theta), builder.offsetY(), FastUtils.sin(theta));
			})
			.directionalMode(true)
			.extra(0.1)
			.count(45)
			.spawnAsPlayerActive(player);
	}
}
