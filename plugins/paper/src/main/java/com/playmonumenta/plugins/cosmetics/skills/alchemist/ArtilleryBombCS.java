package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ArtilleryBombCS extends AlchemicalArtilleryCS {

	public static final String NAME = "Artillery Bomb";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"While many alchemists regard gunpowder",
			"with contempt, it is certainly a very",
			"effective tool in the hands of a",
			"less... principled alchemist.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.TNT;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void periodicEffects(Player caster, MagmaCube grenade) {
		Location particleLoc = grenade.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.SMOKE_LARGE, particleLoc, 2, 0, 0, 0, 0.05).spawnAsPlayerActive(caster);
		new PartialParticle(Particle.FLAME, particleLoc, 3, 0, 0, 0, 0.05).spawnAsPlayerActive(caster);
	}

	@Override
	public void explosionEffect(Player caster, Location loc, double radius) {

		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.75f, 0f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.75f, 1.25f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.75f, 2f);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				// lava particles to simulate burning debris
				new PartialParticle(Particle.LAVA, loc, 10).spawnAsPlayerActive(caster);

				// radius indicator
				new PPCircle(Particle.FLAME, loc.clone().add(0, 0.25, 0), radius)
					.ringMode(true)
					.countPerMeter(0.25)
					.randomizeAngle(true)
					.spawnAsPlayerActive(caster);

				// flat exploding fire
				new PPCircle(Particle.FLAME, loc.clone().add(0, 0.25, 0), 0.01)
					.count(10)
					.randomizeAngle(true)
					.extraRange(0.8, 1.1)
					.rotateDelta(true)
					.delta(0.1 * radius, 0, 0)
					.directionalMode(true)
					.spawnAsPlayerActive(caster);

				mT++;
				if (mT >= 5) {
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

	}

}
