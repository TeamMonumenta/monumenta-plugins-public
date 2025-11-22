package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
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

	private static final Particle.DustOptions APOTHECARY_LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.5f);
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(162, 50, 192), 1.5f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.PANACEA;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TRIDENT;
	}

	public void castEffects(Player player, double radius) {
		Location loc = player.getLocation();
		World world = player.getWorld();
		world.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, SoundCategory.PLAYERS, 1.6f, 0.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.4f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 0.2f, 0.7f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.5f, 1.2f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.7f);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 25, 0.2, 0, 0.2, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_WITCH, loc, 25, 0.2, 0, 0.2, 1).spawnAsPlayerActive(player);
	}

	public void projectileEffects(Player player, Location loc, double radius, int totalTicks, double moveSpeed, Vector increment) {
		double degrees = totalTicks * 12;
		Vector vec;
		double ratio = radius / Panacea.PANACEA_RADIUS;
		for (int i = 0; i < 2; i++) {
			double radian1 = Math.toRadians(degrees + (i * 180));
			vec = new Vector(FastUtils.cos(radian1), 0, FastUtils.sin(radian1)).multiply(radius / 2);
			vec = VectorUtils.rotateXAxis(vec, loc.getPitch() - 90);
			vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

			Location l = loc.clone().add(vec);
			new PartialParticle(Particle.REDSTONE, l, (int) (2 * ratio * ratio), 0.1 * ratio, 0.1, 0.1 * ratio, APOTHECARY_LIGHT_COLOR).spawnAsPlayerActive(player);
			new PartialParticle(Particle.REDSTONE, l, (int) (2 * ratio * ratio), 0.1 * ratio, 0.1, 0.1 * ratio, APOTHECARY_DARK_COLOR).spawnAsPlayerActive(player);

			new PartialParticle(Particle.SPELL_WITCH, l.clone().add(increment), 1, 0.1 * ratio, 0.1, 0.1 * ratio, 1).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SPELL_WITCH, l.clone().add(increment), 1, 0.1 * ratio, 0.1, 0.1 * ratio, 1).spawnAsPlayerActive(player);
		}
		if (totalTicks % 10 == 0) {
			ParticleUtils.drawParticleCircleExplosion(player, loc, 0, 0.5, 0, 90, (int) (10 * ratio * ratio), (float) (radius * 0.1), true, 0, Particle.END_ROD);
		}
	}

	public void projectileReverseEffects(Player player, Location loc, double radius) {
	}

	public void projectileEndEffects(Player player, Location loc, double radius) {
		double ratio = radius / Panacea.PANACEA_RADIUS;
		player.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.2f, 2.4f);
		new PartialParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), (int) (8 * ratio * ratio), 0.25 * ratio, 0.5, 0.25 * ratio, 0.5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL, player.getLocation().add(0, 1, 0), (int) (8 * ratio * ratio), 0.35 * ratio, 0.5, 0.35 * ratio).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), (int) (25 * ratio * ratio), 0.35 * ratio, 0.5, 0.35 * ratio, APOTHECARY_LIGHT_COLOR).spawnAsPlayerActive(player);
	}

	public void projectileHitEffects(Player player, LivingEntity hitEntity, double radius) {
	}

	public void damageOverTimeEffects(LivingEntity target) {
		new PartialParticle(Particle.SQUID_INK, target.getEyeLocation(), 8, 0.4, 0.4, 0.4, 0.1).spawnAsEnemy();
	}

}
