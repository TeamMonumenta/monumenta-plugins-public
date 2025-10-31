package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ScorchingSigilCS extends CholericFlamesCS {
	private static final Particle.DustOptions RED_1 = new Particle.DustOptions(Color.fromRGB(180, 70, 40), 1.1f);
	private static final Particle.DustOptions RED_2 = new Particle.DustOptions(Color.fromRGB(220, 100, 10), 1.1f);
	private static final Particle.DustOptions RED_3 = new Particle.DustOptions(Color.fromRGB(255, 140, 0), 1.1f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Raze the earth, so that new",
			"growth may sprout from the ashes."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_POWDER;
	}

	@Override
	public @Nullable String getName() {
		return "Scorching Sigil";
	}

	@Override
	public void flameEffects(Player player, World world, Location loc, double range) {
		world.playSound(loc, Sound.ENTITY_CAT_HISS, SoundCategory.PLAYERS, 0.85f, 1.2f);
		world.playSound(loc, Sound.ENTITY_SPIDER_DEATH, SoundCategory.PLAYERS, 0.85f, 0.85f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 0.85f, 0.75f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 0.85f, 0.775f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.4f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.5f);
		world.playSound(loc, Sound.ENTITY_CREEPER_DEATH, SoundCategory.PLAYERS, 1.0f, 0.65f);
		world.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.35f, 2f);

		new PPCircle(Particle.FLAME, loc.clone().subtract(0, 0.5, 0), range)
			.directionalMode(true).delta(0, 1, 0)
			.extraRange(0.05, 0.15)
			.ringMode(false).count((int) (range * 10))
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, 30).delta(0.5).extraRange(0.35, 0.6).spawnAsPlayerActive(player);
		new PartialParticle(Particle.LAVA, loc, 30).delta(range / 2).spawnAsPlayerActive(player);

		new BukkitRunnable() {
			int mTicks = 0;
			int mIter = 0;
			double mDegree = 0;

			@Override
			public void run() {
				for (int i = 0; i < 10; i++) {
					for (int spiral = 0; spiral < 5; spiral++) {
						double degree = mDegree + spiral * 360.0 / 5;
						Location l = loc.clone().add(FastUtils.cosDeg(degree) * range * 0.75, Math.pow(mIter, 2) / 350, FastUtils.sinDeg(degree) * range * 0.75);
						Vector v = new Vector(-FastUtils.sinDeg(degree), 0.5, FastUtils.cosDeg(degree));
						new PartialParticle(Particle.FLAME, l, 1, v.getX(), v.getY(), v.getZ(), 0.08, null, true, 0.02)
							.spawnAsPlayerActive(player);
						new PartialParticle(Particle.SMALL_FLAME, l, 1, v.getX(), v.getY() + 0.2, v.getZ(), 0.14, null, true, 0.04)
							.spawnAsPlayerActive(player);
					}

					mDegree += 5;
					mIter++;
				}

				mTicks++;
				if (mTicks > 5) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 1);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			for (int i = 0; i < 6; i++) {
				double angle = 360.0 / 6 * i + player.getLocation().getYaw() + 90;
				Location runeLoc = loc.clone().add(VectorUtils.rotateYAxis(new Vector(range * 0.7, 0, 0), angle));

				new PPLine(Particle.REDSTONE, runeLoc, new Vector(0, 1, 0), 2).data(RED_1)
					.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, runeLoc.clone().add(0, 2, 0), new Vector(0, 1, 0), 2).data(RED_2)
					.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, runeLoc.clone().add(0, 4, 0), new Vector(0, 1, 0), 2).data(RED_3)
					.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);

				new PPLine(Particle.REDSTONE, runeLoc, VectorUtils.rotateYAxis(new Vector(0, 2, 1), angle).normalize(), 2.5).data(RED_1)
					.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, runeLoc, VectorUtils.rotateYAxis(new Vector(0, 2, -1), angle).normalize(), 2.5).data(RED_1)
					.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);

				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					runeLoc.add(0, 1, 0);
					new PPLine(Particle.REDSTONE, runeLoc, VectorUtils.rotateYAxis(new Vector(0, 2, 1), angle).normalize(), 1.6).data(RED_2)
						.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, runeLoc, VectorUtils.rotateYAxis(new Vector(0, 2, -1), angle).normalize(), 1.6).data(RED_2)
						.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);

					runeLoc.add(0, 1, 0);
					new PPLine(Particle.REDSTONE, runeLoc, VectorUtils.rotateYAxis(new Vector(0, 2.5, 1), angle).normalize(), 2.0).data(RED_2)
						.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, runeLoc, VectorUtils.rotateYAxis(new Vector(0, 2.5, -1), angle).normalize(), 2.0).data(RED_2)
						.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);

					runeLoc.add(0, 1, 0);
					new PPLine(Particle.REDSTONE, runeLoc, VectorUtils.rotateYAxis(new Vector(0, 3, 1), angle).normalize(), 1.3).data(RED_3)
						.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
					new PPLine(Particle.REDSTONE, runeLoc, VectorUtils.rotateYAxis(new Vector(0, 3, -1), angle).normalize(), 1.3).data(RED_3)
						.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
				}, 1);

			}
		}, 2);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			new PPCircle(Particle.FLAME, loc, range)
				.count(100).randomizeAngle(true)
				.delta(0, 1, 0).directionalMode(true).extraRange(0.1, 0.4)
				.spawnAsPlayerActive(player);
			new PPCircle(Particle.FLAME, loc, range)
				.count(200).randomizeAngle(true)
				.delta(0, 1, 0).directionalMode(true).extraRange(0.3, 0.65)
				.spawnAsPlayerActive(player);
			new PPCircle(Particle.SMALL_FLAME, loc, range)
				.count(120).randomizeAngle(true)
				.delta(0, 1, 0).directionalMode(true).extraRange(0.3, 0.9)
				.spawnAsPlayerActive(player);
		}, 3);
	}
}
