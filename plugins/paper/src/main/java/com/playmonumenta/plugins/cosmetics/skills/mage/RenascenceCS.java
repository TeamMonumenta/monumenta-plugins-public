package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class RenascenceCS extends PrismaticShieldCS {
	public static final String NAME = "Renascence";
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(84, 7, 28), 1f);
	private static final Particle.DustOptions ROSE = new Particle.DustOptions(Color.fromRGB(184, 24, 67), 1f);
	private static final BlockData BROWN_TERRACOTTA = Material.BROWN_TERRACOTTA.createBlockData();
	private static final BlockData GREEN_TERRACOTTA = Material.GREEN_TERRACOTTA.createBlockData();
	private static final BlockData SOUL_SAND = Material.SOUL_SAND.createBlockData();
	private static final Color DRAIN_COLOR = Color.fromRGB(224, 139, 158);
	private static final Color DRAIN_COLOR_LIGHT = Color.fromRGB(235, 209, 215);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Every Fall, a miasma of hunger and atrophy,",
			"and every Spring, a wave of pure, unfettered",
			"life. And so we are bound, to this endless",
			"cycle of hunger and rebirth."
		);
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.PRISMATIC_SHIELD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ROSE_BUSH;
	}

	@Override
	public void prismaEffect(World world, Player player, Location loc, double radius) {
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.75f, 1.3f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_DEATH, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.8f, 1.4f);
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 0.75f, 1.65f);
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.PLAYERS, 0.75f, 0.65f);
		world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.2f, 0.5f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 1.0f, 0.8f);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT > 2) {
					this.cancel();
				}
				switch (mT) {
					case 0 -> new PPParametric(Particle.REDSTONE, loc, (param, builder) -> {
						double x = (radius - 1) * FastUtils.cos(14 * Math.PI * param) * FastUtils.cos(4 * Math.PI * param);
						double z = (radius - 1) * FastUtils.cos(14 * Math.PI * param) * FastUtils.sin(4 * Math.PI * param);
						Vector vec = new Vector(x, 0.25, z);
						vec.setY(vec.lengthSquared() / 8);
						builder.location(loc.clone().add(vec));

						Color color = ParticleUtils.getTransition(RED.getColor(), ROSE.getColor(), vec.length() / radius);
						builder.data(new Particle.DustOptions(color, 1.25f));
					}).count(200).spawnAsPlayerActive(player);

					case 1 -> new PPParametric(Particle.REDSTONE, loc, (param, builder) -> {
						double x = (radius - 0.5) * FastUtils.cos(14 * Math.PI * param) * FastUtils.cos(4 * Math.PI * param);
						double z = (radius - 0.5) * FastUtils.cos(14 * Math.PI * param) * FastUtils.sin(4 * Math.PI * param);
						Vector vec = new Vector(x, 0.15, z);
						vec.setY(vec.lengthSquared() / 8);
						builder.location(loc.clone().add(vec));

						Color color = ParticleUtils.getTransition(RED.getColor(), ROSE.getColor(), vec.length() / radius);
						builder.data(new Particle.DustOptions(color, 1.25f));
					}).count(200).spawnAsPlayerActive(player);

					default -> new PPParametric(Particle.REDSTONE, loc, (param, builder) -> {
						double x = radius * FastUtils.cos(14 * Math.PI * param) * FastUtils.cos(4 * Math.PI * param);
						double z = radius * FastUtils.cos(14 * Math.PI * param) * FastUtils.sin(4 * Math.PI * param);
						Vector vec = new Vector(x, 0.05, z);
						vec.setY(vec.lengthSquared() / 8);
						builder.location(loc.clone().add(vec));

						Color color = ParticleUtils.getTransition(RED.getColor(), ROSE.getColor(), vec.length() / radius);
						builder.data(new Particle.DustOptions(color, 1.25f));
					}).count(500).spawnAsPlayerActive(player);
				}

				mT++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		new BukkitRunnable() {
			int mTicks = 0;
			int mIter = 0;
			double mDegree = 0;

			@Override
			public void run() {
				for (int i = 0; i < 9; i++) {
					for (int spiral = 0; spiral < 6; spiral++) {
						double degree = mDegree + spiral * 60;
						Location l = loc.clone();
						if ((mIter * 0.01) < 0.6647) {
							l.add(FastUtils.cosDeg(degree) * (2 - Math.pow(2 * (mIter * 0.01), 2)), Math.pow(mIter, 2) / 800, FastUtils.sinDeg(degree) * (2 - Math.pow(2 * (mIter * 0.01), 2)));
						} else {
							l.add(FastUtils.cosDeg(degree) * (0.2 * Math.pow((mIter * 0.01) - 3.74, 2)), Math.pow(mIter, 2) / 800, FastUtils.sinDeg(degree) * (0.2 * Math.pow((mIter * 0.01) - 3.74, 2)));
						}
						new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, new Particle.DustOptions(
							ParticleUtils.getTransition(RED.getColor(), ROSE.getColor(), (float) mIter / 42), 1.2f))
							.spawnAsPlayerActive(player);
					}

					mDegree += 5;
					mIter++;
				}

				mTicks++;
				if (mTicks > 6) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
		new PartialParticle(Particle.TOTEM, LocationUtils.getHalfHeightLocation(player), 75, 0.2, 0.2, 0.2, 0.5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIMSON_SPORE, LocationUtils.getHalfHeightLocation(player), 75, 0.2, 0.2, 0.2, 0.5).spawnAsPlayerActive(player);

	}

	@Override
	public void prismaOnStun(LivingEntity mob, int stunTime, Player player) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks++ >= stunTime) {
					this.cancel();
				}
				new PartialParticle(Particle.BLOCK_CRACK, mob.getLocation(), 5, 0.5, 0.1, 0.5, 0.1, SOUL_SAND).spawnAsPlayerActive(player);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void prismaOnHeal(Player player, LivingEntity enemy) {
		createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(1, 1.5),
			FastUtils.randomDoubleInRange(-0.75, 0.75)), player.getLocation().clone().add(0, 1, 0), player, enemy.getLocation().clone().add(0, 1, 0), null);
	}

	@Override
	public void prismaBuff(Player player, int duration) {
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT++ > duration) {
					this.cancel();
				}

				if (mT % 5 == 0 || mT % 5 == 1) {
					new PPCircle(Particle.SMOKE_NORMAL, player.getLocation().clone().add(0, FastUtils.randomDoubleInRange(2, 2.5), 0), 2.7).extraRange(0.4, 0.7).countPerMeter(0.35).innerRadiusFactor(0.1)
						.directionalMode(true).delta(-0.35, 0.4, 1).rotateDelta(true).spawnAsPlayerActive(player);
					new PPCircle(Particle.SMOKE_NORMAL, player.getLocation().clone().add(0, FastUtils.randomDoubleInRange(-0.25, 0.25), 0), 3).extraRange(0.25, 0.55).countPerMeter(0.35).innerRadiusFactor(0.1)
						.directionalMode(true).delta(-0.25, 1, 1).rotateDelta(true).spawnAsPlayerActive(player);
				}
				new PartialParticle(Particle.FALLING_DUST, player.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.1, BROWN_TERRACOTTA).spawnAsPlayerActive(player);
				new PartialParticle(Particle.FALLING_DUST, player.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.1, GREEN_TERRACOTTA).spawnAsPlayerActive(player);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void createOrb(Vector dir, Location loc, Player player, Location targetLoc, @Nullable Location optLoc) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			final Location mL = targetLoc;
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = optLoc != null ? optLoc : LocationUtils.getHalfHeightLocation(player);

				for (int j = 0; j < 2; j++) {
					Color c = FastUtils.RANDOM.nextBoolean() ? RED.getColor() : ROSE.getColor();
					double red = c.getRed() / 255D;
					double green = c.getGreen() / 255D;
					double blue = c.getBlue() / 255D;
					new PartialParticle(Particle.SPELL_MOB,
						mL.clone().add(FastUtils.randomDoubleInRange(-0.05, 0.05),
							FastUtils.randomDoubleInRange(-0.05, 0.05),
							FastUtils.randomDoubleInRange(-0.05, 0.05)),
						1, red, green, blue, 1)
						.directionalMode(true).spawnAsPlayerActive(player);
				}

				for (int i = 0; i < 4; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.065;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.2) {
						mD.normalize().multiply(0.2);
					}

					mL.add(mD);

					Color c = FastUtils.RANDOM.nextBoolean() ? DRAIN_COLOR : DRAIN_COLOR_LIGHT;
					new PartialParticle(Particle.REDSTONE, mL, 1, 0, 0, 0, 0,
						new Particle.DustOptions(c, 1.4f))
						.spawnAsPlayerActive(player);

					if (mT > 5 && mL.distance(to) < 0.35) {
						world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.2f, 1.6f);
						world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 1f, 2f);
						new PartialParticle(Particle.FIREWORKS_SPARK, mL, 10, 0f, 0f, 0f, 0.2F)
							.spawnAsPlayerActive(player);
						new PartialParticle(Particle.HEART, player.getLocation().clone().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.2F)
							.spawnAsPlayerActive(player);
						new BukkitRunnable() {
							double mRotation = 0;
							double mY = 0.15;
							final double mRadius = 1.15;

							@Override
							public void run() {
								Location loc = player.getLocation();
								mRotation += 15;
								mY += 0.175;
								for (int i = 0; i < 3; i++) {
									double degree = Math.toRadians(mRotation + (i * 120));
									loc.add(FastUtils.cos(degree) * mRadius, mY, FastUtils.sin(degree) * mRadius);
									new PartialParticle(Particle.COMPOSTER, loc, 1, 0.05, 0.05, 0.05, 0.05).spawnAsPlayerActive(player);
									new PartialParticle(Particle.SPELL_INSTANT, loc, 1, 0.05, 0.05, 0.05, 0).spawnAsPlayerActive(player);
									loc.subtract(FastUtils.cos(degree) * mRadius, mY, FastUtils.sin(degree) * mRadius);
								}

								if (mY >= 1.8) {
									this.cancel();
								}
							}

						}.runTaskTimer(Plugin.getInstance(), 0, 1);
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
