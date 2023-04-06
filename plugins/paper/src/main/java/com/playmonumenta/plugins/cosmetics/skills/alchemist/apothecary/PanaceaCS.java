package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PanaceaCS implements CosmeticSkill {

	private static final Particle.DustOptions APOTHECARY_LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.0f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.PANACEA;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TRIDENT;
	}

	public void castEffects(Player mPlayer, double radius) {
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1, 1.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1, 1.25f);
		new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1).spawnAsPlayerActive(mPlayer);
	}

	public void projectileEffects(Player mPlayer, Location loc, double radius, int totalTicks, double moveSpeed, Vector increment) {
		double degrees = totalTicks * 12;
		Vector vec;
		double ratio = radius / Panacea.PANACEA_RADIUS;
		for (int i = 0; i < 2; i++) {
			double radian1 = Math.toRadians(degrees + (i * 180));
			vec = new Vector(FastUtils.cos(radian1) * 0.325, 0, FastUtils.sin(radian1) * 0.325);
			vec = VectorUtils.rotateXAxis(vec, loc.getPitch() - 90);
			vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

			Location l = loc.clone().add(vec);
			new PartialParticle(Particle.REDSTONE, l, (int) (5 * ratio * ratio), 0.1 * ratio, 0.1, 0.1 * ratio, APOTHECARY_LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, l, (int) (5 * ratio * ratio), 0.1 * ratio, 0.1, 0.1 * ratio, APOTHECARY_DARK_COLOR).spawnAsPlayerActive(mPlayer);
		}
		new PartialParticle(Particle.SPELL_INSTANT, loc, (int) (5 * ratio * ratio), 0.35 * ratio, 0.35, 0.35 * ratio, 1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_WITCH, loc, (int) (5 * ratio * ratio), 0.35 * ratio, 0.35, 0.35 * ratio, 1).spawnAsPlayerActive(mPlayer);
	}

	public void projectileReverseEffects(Player mPlayer, Location loc, double radius) {
	}

	public void projectileEndEffects(Player mPlayer, Location loc, double radius) {
		double ratio = radius / Panacea.PANACEA_RADIUS;
		mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.2f, 2.4f);
		new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1, 0), (int) (8 * ratio * ratio), 0.25 * ratio, 0.5, 0.25 * ratio, 0.5).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL, mPlayer.getLocation().add(0, 1, 0), (int) (8 * ratio * ratio), 0.35 * ratio, 0.5, 0.35 * ratio).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), (int) (25 * ratio * ratio), 0.35 * ratio, 0.5, 0.35 * ratio, APOTHECARY_LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
	}

	public void projectileHitEffects(Player mPlayer, LivingEntity hitEntity, double radius) {
	}

	public void damageOverTimeEffects(LivingEntity target) {
		new PartialParticle(Particle.SQUID_INK, target.getEyeLocation(), 8, 0.4, 0.4, 0.4, 0.1).spawnAsEnemy();
	}

}
