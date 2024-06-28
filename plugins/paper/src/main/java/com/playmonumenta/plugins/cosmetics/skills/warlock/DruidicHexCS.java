package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class DruidicHexCS extends AmplifyingHexCS {

	private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(120, 170, 0), 1.1f);
	private static final Particle.DustOptions LIME = new Particle.DustOptions(Color.fromRGB(155, 210, 0), 1.1f);
	private static final Particle.DustOptions NEON = new Particle.DustOptions(Color.fromRGB(235, 255, 0), 1.1f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The jungle is no place for the unlucky..."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.FERN;
	}

	@Override
	public @Nullable String getName() {
		return "Druidic Hex";
	}

	@Override
	public void onCast(Player player, double radius, double angle) {
		World world = player.getWorld();

		Location loc = player.getLocation();
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 1f, 0.9f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 1f, 0.52f);
		world.playSound(loc, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 1f, 0.58f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 0.6f, 0.63f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_DEATH, SoundCategory.PLAYERS, 1f, 1f);

		new BukkitRunnable() {
			final Location mLoc = player.getLocation();
			double mRadiusIncrement = 2;
			@Override
			public void run() {
				if (mRadiusIncrement == 2) {
					mLoc.setDirection(player.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadiusIncrement += radius * 0.15;
				double degree = 90 - angle;
				int degreeSteps = ((int) (2 * angle)) / 12;
				double degreeStep = 2 * angle / degreeSteps;
				for (int step = 0; step < degreeSteps; step++, degree += degreeStep) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadiusIncrement,
						0.4,
						FastUtils.sin(radian1) * mRadiusIncrement);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(vec);

					new PartialParticle(Particle.TOTEM, l, 3, 0.5, 0.5, 0.5, 0.65).spawnAsPlayerActive(player);
				}

				if (mRadiusIncrement >= radius) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		// the pattern is all symmetrical, so we really only have to code half the particles and then mirror them
		for (int i = -1; i <= 1; i += 2) {
			int mirror = i;
			new BukkitRunnable() {
				int mTicks = 0;
				final int mMaxTicks = 5;
				final Location mLoc = loc.setDirection(loc.getDirection().setY(0).normalize());

				@Override
				public void run() {
					// draw circular arcs
					if (mTicks == 0) {
						new PPParametric(Particle.REDSTONE, mLoc,
							(param, builder) -> {
								double x = -FastUtils.cosDeg(angle * param + 90) * mirror;
								double y = 0.05;
								double z = FastUtils.sinDeg(angle * param + 90);
								Vector vec = new Vector(x, y, z);
								vec = VectorUtils.rotateXAxis(vec, loc.getPitch());
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

								builder.location(loc.clone().add(vec));
							}).data(GREEN).includeEnd(true).count((int) (25 * angle / 70)).spawnAsPlayerActive(player);
					}

					// draw the central fern
					drawCentralFern();

					// and draw additional things if the cone is wide enough
					double angleStep = 17.5;
					double angleCounter = angle;
					int i = 1;

					while (angleCounter > 10) {
						if (i % 2 == 1) {
							drawRadialSpike(90 - angleStep * i);
						} else {
							drawAuxLeaf(90 - angleStep * i);
						}

						i++;
						angleCounter -= angleStep;
					}

					mTicks++;
					if (mTicks > mMaxTicks) {
						this.cancel();
					}
				}

				private void drawRadialSpike(double angle) {
					Location origin = loc.clone().add(0, 0.05, 0);
					Vector direction = origin.getDirection();
					direction = VectorUtils.rotateYAxis(direction, (angle - 90) * mirror);

					if (mTicks == 0) {
						new PPLine(Particle.REDSTONE,
							origin.clone().add(direction.clone().multiply(2.2)),
							VectorUtils.rotateYAxis(direction, 165), 1.5)
							.data(GREEN).countPerMeter(8).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE,
							origin.clone().add(direction.clone().multiply(2.2)),
							VectorUtils.rotateYAxis(direction, -165), 1.5)
							.data(GREEN).countPerMeter(8).spawnAsPlayerActive(player);
					} else if (mTicks == 3 && radius >= 8) {
						// and draw a leaf too if we have enough room
						double leafHalfLength = Math.min(3.3, 0.33 * radius - 1);
						double leafAngle = Math.max(15, -0.5 * radius + 25);

						new PPLine(Particle.REDSTONE,
							origin.clone().add(direction.clone().multiply(radius)),
							VectorUtils.rotateYAxis(direction, 180 - leafAngle), leafHalfLength)
							.data(NEON).countPerMeter(12).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE,
							origin.clone().add(direction.clone().multiply(radius)),
							VectorUtils.rotateYAxis(direction, leafAngle - 180), leafHalfLength)
							.data(NEON).countPerMeter(12).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE,
							origin.clone().add(direction.clone().multiply(radius - 2 * leafHalfLength * FastUtils.cosDeg(15))),
							VectorUtils.rotateYAxis(direction, leafAngle), leafHalfLength)
							.data(NEON).countPerMeter(12).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE,
							origin.clone().add(direction.clone().multiply(radius - 2 * leafHalfLength * FastUtils.cosDeg(15))),
							VectorUtils.rotateYAxis(direction, -leafAngle), leafHalfLength)
							.data(NEON).countPerMeter(12).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE,
							origin.clone().add(direction.clone().multiply(radius - 0.8 * leafHalfLength * FastUtils.cosDeg(15))),
							VectorUtils.rotateYAxis(direction, 180 - leafAngle), 1.2 * radius / 10)
							.data(NEON).countPerMeter(12).spawnAsPlayerActive(player);
						new PPLine(Particle.REDSTONE,
							origin.clone().add(direction.clone().multiply(radius - 0.8 * leafHalfLength * FastUtils.cosDeg(15))),
							VectorUtils.rotateYAxis(direction, leafAngle - 180), 1.2 * radius / 10)
							.data(NEON).countPerMeter(12).spawnAsPlayerActive(player);
					}
				}

				private void drawCentralFern() {
					List<Integer> frames;
					List<Double> offsets;
					if (radius <= 4) {
						frames = List.of(0, 3, -1, -1, -1, -1);
						offsets = List.of(0.0, -3.5, 0.0, 0.0, 0.0, 0.0);
					} else if (radius <= 5) {
						frames = List.of(0, 3, -1, -1, -1, -1);
						offsets = List.of(0.0, -3.0, 0.0, 0.0, 0.0, 0.0);
					} else if (radius <= 8) {
						frames = List.of(0, 2, 3, -1, -1, -1);
						offsets = List.of(0.0, -2.0, -2.0, 0.0, 0.0, 0.0);
					} else if (radius <= 10) {
						frames = List.of(0, 1, 2, 3, -1, -1);
						offsets = List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
					} else {
						frames = List.of(1, 1, 2, 2, 3, -1);
						offsets = List.of(0.0, 2.0, 2.5, 4.5, 4.5, 0.0);
					}

					double offset = offsets.get(mTicks);
					switch (frames.get(mTicks)) {
						case 0 -> drawFernLine(0, 0, 1, 1.5 + offset, 5);
						case 1 -> {
							drawFernLine(0, 0, 1.5, 2.5 + offset, 5);
							drawFernParametric(-0.6, 0.6, 2.8, 1.2, 1.5 + offset, 20);
							drawFernParametric(-0.6, 0.6, 1.8, 0.9, 2.6 + offset, 15);
						}
						case 2 -> {
							drawFernLine(0, 0, 1.5, 4 + offset, 5);
							drawFernParametric(-0.5, 0.5, 2.2, 1.25, 3.4 + offset, 17);
							drawFernParametric(-0.5, 0.5, 1.2, 1, 4.4 + offset, 13);
						}
						case 3 -> {
							drawFernLine(0, 0, 3.7, 5.5 + offset, 14);
							drawFernParametric(-0.4, 0.4, 2, 1.3, 5.1 + offset, 15);
							drawFernParametric(-0.4, 0.4, 1.3, 0.8, 5.8 + offset, 11);
							drawFernLine(0.6, 0, 0.7, 7 + offset, 6);
							drawFernLine(0.6, 0, -1.5, 9.2 + offset, 10);
						}
						default -> { }
					}
				}

				private void drawFernLine(double xSlope, double xConstant, double zSlope, double zConstant, int count) {
					new PPParametric(Particle.REDSTONE, loc, (param, builder) -> {
						double x = (xConstant + param * xSlope) * mirror;
						double y = 0.05;
						double z = zConstant + param * zSlope;
						Vector vec = new Vector(x, y, z);
						vec = VectorUtils.rotateXAxis(vec, loc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

						builder.location(loc.clone().add(vec));
					}).data(LIME).count((int) (count * 1.5)).spawnAsPlayerActive(player);
				}

				private void drawFernParametric(double xSlope, double xConstant, double zSlope, double zExponent, double zConstant, int count) {
					new PPParametric(Particle.REDSTONE, loc, (param, builder) -> {
						double x = (xConstant + xSlope * FastUtils.cos(3.1416 * param)) * mirror;
						double y = 0.05;
						double z = zConstant + zSlope * Math.pow(param, zExponent);
						Vector vec = new Vector(x, y, z);
						vec = VectorUtils.rotateXAxis(vec, loc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

						builder.location(loc.clone().add(vec));
					}).data(LIME).count((int) (count * 1.5)).spawnAsPlayerActive(player);
				}

				private void drawAuxLeaf(double angle) {
					List<Integer> frames;
					List<Double> offsets;
					if (radius <= 4) {
						frames = List.of(0, 2, -1, -1, -1, -1);
						offsets = List.of(0.0, -3.0, 0.0, 0.0, 0.0, 0.0);
					} else if (radius <= 5) {
						frames = List.of(0, 2, -1, -1, -1, -1);
						offsets = List.of(0.0, -2.5, 0.0, 0.0, 0.0, 0.0);
					} else if (radius <= 8) {
						frames = List.of(0, 1, 2, -1, -1, -1);
						offsets = List.of(0.0, -0.5, -1.0, 0.0, 0.0, 0.0);
					} else if (radius <= 10) {
						frames = List.of(0, 1, 2, -1, -1, -1);
						offsets = List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
					} else {
						frames = List.of(0, 2, 1, 2, -1, -1);
						offsets = List.of(0.0, -2.5, 2.75, 3.5, 0.0, 0.0);
					}

					drawAuxLeafSection(frames.get(mTicks), offsets.get(mTicks), angle);
				}

				private void drawAuxLeafSection(int part, double offset, double angle) {
					Location o = loc.clone();
					Vector dir = o.getDirection();
					dir = VectorUtils.rotateYAxis(dir, (angle - 90) * mirror);

					switch (part) {
						case 0 -> drawAuxLeafLine(o, dir, 1.5 + offset, 0, 1);
						case 1 -> {
							drawAuxLeafLine(o, dir, 2.5 + offset, 0, 2);
							drawAuxLeafLine(o, dir, 2.75 + offset, 20, 2.5);
							drawAuxLeafLine(o, dir, 2.75 + offset, -20, 2.5);
							drawAuxLeafLine(o, dir, 3.75 + offset, 35, 1.5);
							drawAuxLeafLine(o, dir, 3.75 + offset, -35, 1.5);
						}
						case 2 -> {
							drawAuxLeafLine(o, dir, 4.5 + offset, 0, 2);
							drawAuxLeafLine(o, dir, 5.5 + offset, 35, 1.5);
							drawAuxLeafLine(o, dir, 5.5 + offset, -35, 1.5);
							drawAuxLeafLine(o, dir, 6.5 + offset, 35, 1.25);
							drawAuxLeafLine(o, dir, 6.5 + offset, -35, 1.25);
						}
						default -> { }
					}
				}

				public void drawAuxLeafLine(Location origin, Vector direction, double distance, double rotation, double length) {
					new PPLine(Particle.REDSTONE,
						origin.clone().add(direction.clone().multiply(distance)),
						VectorUtils.rotateYAxis(direction, rotation), length)
						.data(LIME).countPerMeter(10).spawnAsPlayerActive(player);
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	@Override
	public void onHit(Player player, LivingEntity mob) {
		Location loc = LocationUtils.getHalfHeightLocation(mob);

		double width = mob.getWidth();
		double angle = FastUtils.randomDoubleInRange(0, 360);

		Location triangle1 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(width, 0, 0), angle));
		Location triangle2 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(width, 0, 0), angle + 120));
		Location triangle3 = loc.clone().add(VectorUtils.rotateYAxis(new Vector(width, 0, 0), angle + 240));

		new PPLine(Particle.REDSTONE, triangle1, triangle2).data(NEON).countPerMeter(10).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, triangle2, triangle3).data(NEON).countPerMeter(10).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, triangle3, triangle1).data(NEON).countPerMeter(10).spawnAsPlayerActive(player);
	}
}
