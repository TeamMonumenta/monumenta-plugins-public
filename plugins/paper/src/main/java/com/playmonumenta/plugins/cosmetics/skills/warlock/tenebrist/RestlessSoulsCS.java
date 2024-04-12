package com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
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

public class RestlessSoulsCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.RESTLESS_SOULS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.VEX_SPAWN_EGG;
	}

	public void vexTick(Player player, LivingEntity vex, int ticks) {
		Location loc = vex.getLocation();
		new PartialParticle(Particle.SOUL, loc.clone().add(0, 0.25, 0), 1, 0.2, 0.2, 0.2, 0.01).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.25, 0), 1, 0.2, 0.2, 0.2, 0.01).spawnAsPlayerActive(player);
	}

	public void vexTarget(Player player, LivingEntity vex, LivingEntity target) {
		World world = player.getWorld();
		Location loc = vex.getLocation();

		world.playSound(loc, Sound.ENTITY_VEX_AMBIENT, SoundCategory.PLAYERS, 1.5f, 1.0f);
		Vector dir = LocationUtils.getHalfHeightLocation(target).subtract(loc).toVector().normalize();
		new PPLine(Particle.SOUL_FIRE_FLAME, loc, LocationUtils.getEntityCenter(target))
			.countPerMeter(1.5)
			.directionalMode(true)
			.delta(dir.getX(), dir.getY(), dir.getZ())
			.extra(0.12)
			.includeEnd(false)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.SOUL, target.getLocation().add(0, target.getEyeHeight() + 1, 0), 8).extra(0.02).spawnAsPlayerActive(player);
	}

	public void vexAttack(Player player, LivingEntity vex, LivingEntity enemy, double radius) {
		World world = player.getWorld();
		world.playSound(vex.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 1.5f, 1.0f);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, LocationUtils.getEntityCenter(enemy), 8, 0, 0, 0, 0.14).spawnAsPlayerActive(player);
	}

	public void vexDespawn(Player player, LivingEntity vex) {
		World world = player.getWorld();
		Location loc = vex.getLocation();

		world.playSound(loc, Sound.ENTITY_VEX_DEATH, SoundCategory.PLAYERS, 1.5f, 1.0f);
		new PartialParticle(Particle.SOUL, loc, 20, 0.2, 0.2, 0.2).spawnAsPlayerActive(player);
	}
}
