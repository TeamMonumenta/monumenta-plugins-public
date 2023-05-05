package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AlchemicalArtilleryCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ALCHEMICAL_ARTILLERY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_CREAM;
	}

	public void periodicEffects(Player caster, MagmaCube grenade, Item physicsItem, int ticks) {
		Location particleLoc = LocationUtils.getHalfHeightLocation(grenade);
		new PartialParticle(Particle.SMOKE_LARGE, particleLoc, 2, 0, 0, 0, 0.05).spawnAsPlayerActive(caster);
		new PartialParticle(Particle.FLAME, particleLoc, 3, 0, 0, 0, 0.05).spawnAsPlayerActive(caster);
	}

	public void explosionEffect(Player caster, Location loc, double radius) {
		// radius indicator
		new PPCircle(Particle.REDSTONE, loc, radius)
			.count(45)
			.extra(0.0025)
			.data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f))
			.spawnAsPlayerActive(caster);

		// explosion effect
		new PartialParticle(Particle.FLAME, loc, 100, radius / 4, 0, radius / 4, 0.04 * radius).spawnAsPlayerActive(caster);
		new PartialParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(caster);

		int effectDuration = 5;
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				// flat exploding fire
				new PPCircle(Particle.FLAME, loc.clone().add(0, 0.25, 0), 0.5 * radius * (0.5 + 0.5 * mT / effectDuration))
					.ringMode(false)
					.count(10)
					.randomizeAngle(true)
					.rotateDelta(true)
					.directionalMode(true)
					.delta(0.1 * radius * (1 - 0.5 * mT / effectDuration), 0, 0)
					.extraRange(0.8, 1.1)
					.spawnAsPlayerActive(caster);

				// lava particles to simulate burning debris
				new PartialParticle(Particle.LAVA, loc, 10).spawnAsPlayerActive(caster);

				mT++;
				if (mT >= effectDuration) {
					cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		// mushroom cloud
		double smokeSpeed = 0.2;
		new PPParametric(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0),
			(param, builder) -> {
				double rand = FastUtils.randomDoubleInRange(0, 1.3);
				if (rand < 1) {
					// ring
					double r1 = FastUtils.randomDoubleInRange(0, 360);
					double r2 = FastUtils.randomDoubleInRange(0, 360);
					double distance = Math.sqrt(FastUtils.randomDoubleInRange(0.25, 1));
					Vector dir = VectorUtils.rotateYAxis(VectorUtils.rotateZAxis(new Vector(0, distance * 0.2, 0), r1).add(new Vector(0.4, 0, 0)), r2);
					builder.offset(dir.getX() * smokeSpeed, (dir.getY() + 1) * smokeSpeed, dir.getZ() * smokeSpeed);
				} else {
					// column
					rand = 0.1 + (rand - 1) / 0.3 * 0.8;
					double rot = FastUtils.randomDoubleInRange(0, 360);
					Vector dir = VectorUtils.rotateYAxis(new Vector(0.15 * rand, rand, 0), rot);
					builder.offset(dir.getX() * smokeSpeed, dir.getY() * smokeSpeed, dir.getZ() * smokeSpeed);
				}
			})
			.count(200)
			.directionalMode(true)
			.extra(1)
			.spawnAsPlayerActive(caster);

		// sounds
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 0f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 1.25f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 1.5f, 2f);
	}

}
