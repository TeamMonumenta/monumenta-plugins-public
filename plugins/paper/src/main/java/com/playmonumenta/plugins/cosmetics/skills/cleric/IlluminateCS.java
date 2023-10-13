package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static org.bukkit.Material.AIR;
import static org.bukkit.Material.LIGHT;

public class IlluminateCS implements CosmeticSkill {

	// private static final Particle.DustOptions YELLOW_SMALL = new Particle.DustOptions(Color.fromRGB(255, 255, 60), 0.9f);
	private static final Particle.DustOptions YELLOW_LARGE = new Particle.DustOptions(Color.fromRGB(255, 255, 60), 1.1f);
	private static final Particle.DustOptions ORANGE_SMALL = new Particle.DustOptions(Color.fromRGB(255, 160, 0), 0.9f);
	private static final Particle.DustOptions ORANGE_LARGE = new Particle.DustOptions(Color.fromRGB(255, 160, 0), 1.1f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ILLUMINATE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.YELLOW_CANDLE;
	}

	public void castEffects(Player player) {
		Location loc = player.getLocation();
		World world = player.getWorld();
		world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1f, 1.1f);
		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.25f, 2.0f);
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.5f, 0.85f);
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.PLAYERS, 0.7f, 0.7f);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 15, 0.5f, 0.1f, 0.5f, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 30, 0.75f, 0.75f, 0.75f, 0, YELLOW_LARGE).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 30, 0.75f, 0.75f, 0.75f, 0, ORANGE_LARGE).spawnAsPlayerActive(player);
	}


	public void projectileEffects(Player player, Location loc) {
		new PartialParticle(Particle.REDSTONE, loc, 6, 0.15, 0.15, 0.15, 0, YELLOW_LARGE).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 6, 0.15, 0.15, 0.15, 0, ORANGE_LARGE).spawnAsPlayerActive(player);
	}


	public void trailEffects(Player player, Location location, double radius, Vector increment, int ticks, int maxTicks, @Nullable Location enhanceZone, double enhanceRadius) {
		Location loc = location.clone();

		// don't draw trail if already close enough to the enhance zone
		if (enhanceZone != null && location.distance(enhanceZone) < enhanceRadius + 0.5) {
			return;
		}

		Vector vec = new Vector(radius, 0, 0);
		vec = VectorUtils.rotateXAxis(vec, loc.getPitch() - 90);
		vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

		PPLine line1 = new PPLine(Particle.REDSTONE, loc.clone().add(vec), increment, radius).data(ORANGE_SMALL);
		PPLine line2 = new PPLine(Particle.REDSTONE, loc.clone().subtract(vec), increment, radius).data(ORANGE_SMALL);
		line1.delta(0.05).shift(-radius * 0.5).offset(Math.random()).count(1).spawnAsPlayerActive(player);
		line2.delta(0.05).shift(-radius * 0.5).offset(Math.random()).count(1).spawnAsPlayerActive(player);


		if ((ticks != 0 && ticks % 40 == 0) || ticks == maxTicks - 1) {
			Location spark1 = loc.clone().add(vec).add(0, 0.5, 0);
			new PartialParticle(Particle.REDSTONE, spark1, 2, 0.25, 0).data(YELLOW_LARGE).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SPELL_INSTANT, spark1, 1, 0.25, 0).spawnAsPlayerActive(player);

			Location spark2 = loc.clone().subtract(vec).add(0, 0.5, 0);
			new PartialParticle(Particle.REDSTONE, spark2, 2, 0.25, 0).data(YELLOW_LARGE).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SPELL_INSTANT, spark2, 1, 0.25, 0).spawnAsPlayerActive(player);

			PPCircle sparkfloor = new PPCircle(Particle.REDSTONE, loc, radius * 0.9).data(ORANGE_SMALL).ringMode(false);
			sparkfloor.count((int) (radius * radius * 3)).location(loc.clone().subtract(0, 0.35, 0)).spawnAsPlayerActive(player);
		}

		// create light blocks? lol? only replace air though
		if (ticks == 0 && location.getBlock().getType().equals(AIR)) {
			TemporaryBlockChangeManager.INSTANCE.changeBlock(location.getBlock(), LIGHT, maxTicks);
		} else if (ticks == maxTicks - 1) {
			TemporaryBlockChangeManager.INSTANCE.revertChangedBlock(location.getBlock(), LIGHT);
		}
	}

	public void projectileExplosionEffects(Player player, Location location) {
		World world = player.getWorld();
		Location loc = location.clone();

		if (loc.getBlock().isSolid()) {
			loc.add(0, 0.5, 0);
		}

		new PartialParticle(Particle.END_ROD, loc, 40, 0.05, 0.05, 0.05, 0.2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 40, 1.75, 1.75, 1.75, 0, YELLOW_LARGE).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 40, 1.75, 1.75, 1.75, 0, ORANGE_LARGE).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.5f, 1.2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.5f, 0.75f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 0.95f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.75f, 0.8f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.75f, 0.5f);
	}

	public void explosionHitEffects(Player player, LivingEntity damagee) {
		World world = player.getWorld();
		Location loc = damagee.getLocation().add(0, damagee.getEyeHeight() / 2, 0);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.05f, 0.05f, 0.05f, 0.1).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.25f, 0.8f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.25f, 0.5f);
	}

	public void sanctifiedZoneEffects(Player player, Location location, double radius, int ticks) {
		Location loc = location.clone();

		// attempt to step up to avoid placing the ring inside the floor
		int mTries = 0;
		while (loc.getBlock().isSolid() && mTries < 10) {
			loc.add(0, 0.1, 0);
			mTries++;
		}
		if (mTries > 0) {
			loc.add(0, 0.2, 0);
		}

		if (ticks <= 10) {
			PPCircle border = new PPCircle(Particle.REDSTONE, loc, radius / 10 * ticks).data(ORANGE_LARGE);
			border.countPerMeter(1.5).spawnAsPlayerActive(player);
		} else {
			new PPCircle(Particle.REDSTONE, loc, radius).data(ORANGE_LARGE).countPerMeter(0.33).offset(Math.random()).spawnAsPlayerActive(player);
			new PPCircle(Particle.SPELL_INSTANT, loc.add(0, 0.2, 0), radius).count(2).offset(Math.random()).spawnAsPlayerActive(player);
			// drawCross(player, loc, radius, ticks, width, increment);
		}
	}


	public void enhanceTickDamageEffect(Player player, LivingEntity mob) {
		new PartialParticle(Particle.SPELL_INSTANT, mob.getLocation()).count(2).extra(1).spawnAsPlayerActive(player);
	}
}
