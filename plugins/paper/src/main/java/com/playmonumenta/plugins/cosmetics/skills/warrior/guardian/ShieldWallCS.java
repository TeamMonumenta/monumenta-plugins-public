package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ShieldWallCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SHIELD_WALL;
	}

	@Override
	public Material getDisplayItem() {
		return Material.COBBLESTONE_WALL;
	}

	public void shieldStartEffect(World world, Player player, Location loc, double radius) {
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.7f, 0.1f);
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.3f, 0.1f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.3f, 2.0f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.4f, 0.1f);
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.8f, 0.1f);
		world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.5f, 0.1f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.5f, 0.1f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 70, 0, 0, 0, 0.3f).spawnAsPlayerActive(player);
	}

	public void wallParticles(Player player, Location center, double radius, double angle, double height) {
		for (int y = -1; y <= height; y++) {
			int y2 = y;
			new PPParametric(baseParticle(), center, (parameter, packagedValues) -> {
				double theta = Math.toRadians(angle * (parameter - 0.5));
				Vector vec = new Vector(-FastUtils.sin(theta) * radius, -1, FastUtils.cos(theta) * radius);
				vec = VectorUtils.rotateYAxis(vec, center.getYaw());
				Location l = center.clone().add(vec).add(0, y2 + 0.5, 0);
				packagedValues.location(l);
				packagedValues.particle(replaceParticle(parameter, y2 / height));
			})
				.count((int) (20 * angle / 180))
				.delta(0.03, 0.05, 0.03)
				.extra(1000000)
				.includeEnd(angle < 360)
				.spawnAsPlayerActive(player);
		}
	}

	public Particle baseParticle() {
		return Particle.ELECTRIC_SPARK;
	}

	public Particle replaceParticle(double angleRatio, double heightRatio) {
		return baseParticle();
	}

	public void shieldOnBlock(World world, Location eLoc, Player player) {
		world.playSound(eLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.75f, 1.5f);
		new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 5, 0, 0, 0, 0.25f).spawnAsPlayerActive(player);
	}

	public void shieldOnHit(World world, Location eLoc, Player player, float multiplier) {
		world.playSound(eLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f * multiplier, 1f);
		new PartialParticle(Particle.EXPLOSION_NORMAL, eLoc, (int) (50 * multiplier), 0, 0, 0, 0.35f).spawnAsPlayerActive(player);
	}
}
