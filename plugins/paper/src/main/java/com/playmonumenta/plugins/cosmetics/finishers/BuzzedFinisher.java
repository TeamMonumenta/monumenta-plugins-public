package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class BuzzedFinisher implements EliteFinisher {

	public static final String NAME = "Buzzed";

	private static final double ASCEND_SPEED = 0.05;
	private static final double MAX_HEIGHT = 4.0;
	private static final float HIVE_SCALE = 1.65f;
	private static final int MAX_DURATION_TICKS = 20 * 15;

	private static final float INITIAL_SPIN_SPEED = 1.0f;
	private static final float SPIN_ACCELERATION = 0.7f;
	private static final float PULSE_AMPLITUDE = 0.4f;
	private static final int PULSE_FREQUENCY = 20;

	private static final int INITIAL_FLAP_INTERVAL = 15;
	private static final int MINIMUM_FLAP_INTERVAL = 3;
	private static final int FLAP_ACCELERATION_DURATION = 80;

	@Override
	public Material getDisplayItem() {
		return Material.BEE_NEST;
	}

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		final Location groundLoc = loc.clone();
		final int totalBees = FastUtils.randomIntInRange(3, 5);
		final ItemDisplay beeNestDisplay = groundLoc.getWorld().spawn(groundLoc, ItemDisplay.class);
		beeNestDisplay.setItemStack(new ItemStack(Material.BEE_NEST));
		beeNestDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
		beeNestDisplay.setBrightness(new Display.Brightness(15, 15));
		GlowingManager.startGlowing(beeNestDisplay, NamedTextColor.YELLOW, 200, GlowingManager.PLAYER_ABILITY_PRIORITY);
		EntityUtils.setRemoveEntityOnUnload(beeNestDisplay);
		groundLoc.getWorld().playSound(groundLoc, Sound.BLOCK_BEEHIVE_WORK, SoundCategory.PLAYERS, 1.0f, 1.0f);
		groundLoc.getWorld().playSound(groundLoc, Sound.ENTITY_BEE_POLLINATE, SoundCategory.PLAYERS, 1.0f, 1.0f);

		new BukkitRunnable() {
			private int ticks = 0;
			private double currentHeight = 0;
			private float currentYaw = 0;
			private int beesSpawned = 0;
			private boolean cleanupStarted = false;

			private float currentSpinSpeed = INITIAL_SPIN_SPEED;
			private int pulseTicks = 0;
			private int ticksUntilNextFlap = INITIAL_FLAP_INTERVAL;

			@Override
			public void run() {
				if (ticks > MAX_DURATION_TICKS || beeNestDisplay.isDead()) {
					cleanup();
					this.cancel();
					return;
				}

				pulseTicks++;
				currentSpinSpeed += SPIN_ACCELERATION;
				currentYaw = (currentYaw + currentSpinSpeed) % 360;
				float currentScale = HIVE_SCALE + (float) (Math.sin(Math.toRadians(pulseTicks * PULSE_FREQUENCY)) * PULSE_AMPLITUDE);

				if (currentHeight < MAX_HEIGHT) {
					currentHeight = Math.min(MAX_HEIGHT, currentHeight + ASCEND_SPEED);
					Location currentHiveLoc = groundLoc.clone().add(0, currentHeight, 0);
					beeNestDisplay.teleport(currentHiveLoc);

					new PartialParticle(Particle.REDSTONE, currentHiveLoc, 5)
						.data(new Particle.DustOptions(Color.YELLOW, 1.2f))
						.delta(0.75, 0.75, 0.75).spawnAsPlayerActive(p);
					if (ticks % 4 == 0) {
						double hiveRadius = 0.5 * HIVE_SCALE;
						double randomAngle = FastUtils.randomDoubleInRange(0, Math.PI * 2);
						double randomDistFromCenter = FastUtils.randomDoubleInRange(0, hiveRadius);
						double offsetX = Math.cos(randomAngle) * randomDistFromCenter;
						double offsetZ = Math.sin(randomAngle) * randomDistFromCenter;
						Location dripLoc = currentHiveLoc.clone().add(offsetX, -0.6 * currentScale, offsetZ);
						new PartialParticle(Particle.FALLING_HONEY, dripLoc, 4).spawnAsPlayerActive(p);
					}
				}

				ticksUntilNextFlap--;
				if (ticksUntilNextFlap <= 0) {
					beeNestDisplay.getWorld().playSound(beeNestDisplay.getLocation(), Sound.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 1.0f, 1.25f);
					beeNestDisplay.getWorld().playSound(beeNestDisplay.getLocation(), Sound.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 1.0f, 1.25f);
					double progress = Math.min(1.0, (double) ticks / FLAP_ACCELERATION_DURATION);
					double nextInterval = INITIAL_FLAP_INTERVAL + (MINIMUM_FLAP_INTERVAL - INITIAL_FLAP_INTERVAL) * progress;
					ticksUntilNextFlap = (int) Math.round(nextInterval);
				}

				Transformation transform = new Transformation(
					new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0),
					new Vector3f(currentScale, currentScale, currentScale),
					new AxisAngle4f((float) Math.toRadians(currentYaw), 0, 1, 0)
				);
				beeNestDisplay.setTransformation(transform);
				beeNestDisplay.setInterpolationDuration(1);

				if (beesSpawned < totalBees) {
					double spawnThreshold = 2.0 + (beesSpawned * (2.0 / (totalBees - 1)));
					if (currentHeight >= spawnThreshold) {
						spawnBee(p, beeNestDisplay.getLocation());
						beesSpawned++;
					}
				}

				if (beesSpawned >= totalBees && !cleanupStarted) {
					cleanupStarted = true;
					Plugin.getInstance().getServer().getScheduler().runTaskLater(Plugin.getInstance(), this::cleanup, 10L);
				}
				ticks++;
			}

			private void spawnBee(Player p, final Location startLoc) {
				startLoc.getWorld().playSound(startLoc, Sound.BLOCK_BEEHIVE_ENTER, SoundCategory.PLAYERS, 1.0f, 1.2f);
				new PartialParticle(Particle.EXPLOSION_LARGE, startLoc, 1).spawnAsPlayerActive(p);

				final double distance = FastUtils.randomDoubleInRange(2.0, 6.0);
				final Vector direction = new Vector(FastUtils.randomDoubleInRange(-0.5, 0.5), 0, FastUtils.randomDoubleInRange(-0.5, 0.5)).normalize();

				final Bee bee = startLoc.getWorld().spawn(startLoc, Bee.class);
				bee.setAI(false);
				bee.setSilent(true);
				bee.setInvulnerable(true);

				final int travelTicks = 45;

				new BukkitRunnable() {
					int ticksTraveled = 0;

					@Override
					public void run() {
						if (ticksTraveled >= travelTicks || bee.isDead()) {
							if (!bee.isDead()) {
								Location beeLoc = bee.getLocation();
								bee.getWorld().playSound(beeLoc, Sound.ENTITY_BEE_DEATH, SoundCategory.PLAYERS, 1.0f, 1.15f);
								bee.getWorld().playSound(beeLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.15f, 1.5f);
								new PartialParticle(Particle.EXPLOSION_LARGE, beeLoc, 1).spawnAsPlayerActive(p);
								ParticleUtils.drawSphere(beeLoc, 5, 0.09,
									(l, t) -> {
										Vector vel = beeLoc.clone().subtract(l).toVector().normalize().multiply(1.7);
										new PartialParticle(Particle.EXPLOSION_NORMAL, l, 1).directionalMode(true)
											.delta(vel.getX(), vel.getY(), vel.getZ()).extra(0.15).spawnAsPlayerActive(p);
									}
								);
								bee.remove();
							}
							this.cancel();
							return;
						}

						double progress = (double) ticksTraveled / travelTicks;
						double easedProgress = 1 - Math.pow(1 - progress, 5);
						Location currentPos = startLoc.clone().add(direction.clone().multiply(distance * easedProgress));

						currentPos.setDirection(direction);
						bee.teleport(currentPos);

						if (ticksTraveled % 10 == 0) {
							new PartialParticle(Particle.FALLING_HONEY, bee.getLocation(), 1).extra(0).spawnAsPlayerActive(p);
						}
						if (ticksTraveled % 5 == 0) {
							new PartialParticle(Particle.END_ROD, bee.getLocation(), 1).extra(0).spawnAsPlayerActive(p);
						} else {
							new PartialParticle(Particle.REDSTONE, bee.getLocation(), 1).data(new Particle.DustOptions(Color.YELLOW, 1.55f)).extra(0).spawnAsPlayerActive(p);
						}
						ticksTraveled++;
					}
				}.runTaskTimer(Plugin.getInstance(), 0, 1);
			}

			private void cleanup() {
				if (!beeNestDisplay.isDead()) {
					float finalScale = HIVE_SCALE + PULSE_AMPLITUDE;
					Transformation finalTransform = new Transformation(
						new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0),
						new Vector3f(finalScale, finalScale, finalScale),
						new AxisAngle4f((float) Math.toRadians(currentYaw), 0, 1, 0)
					);
					beeNestDisplay.setTransformation(finalTransform);

					Location nestLoc = beeNestDisplay.getLocation();
					nestLoc.getWorld().playSound(nestLoc, Sound.ENTITY_BEE_HURT, SoundCategory.PLAYERS, 1.5f, 1.2f);
					nestLoc.getWorld().playSound(nestLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.0f);
					nestLoc.getWorld().playSound(nestLoc, Sound.BLOCK_BEEHIVE_WORK, SoundCategory.PLAYERS, 1.0f, 1.0f);
					new PartialParticle(Particle.EXPLOSION_LARGE, nestLoc, 1).spawnAsPlayerActive(p);
					ParticleUtils.drawSphere(nestLoc, 9, 0.15,
						(l, t) -> {
							Vector vel = nestLoc.clone().subtract(l).toVector().normalize().multiply(1.7);
							new PartialParticle(Particle.EXPLOSION_NORMAL, l, 1).directionalMode(true)
								.delta(vel.getX(), vel.getY(), vel.getZ()).extra(0.15).spawnAsPlayerActive(p);
						}
					);
					Plugin.getInstance().getServer().getScheduler().runTaskLater(Plugin.getInstance(), () -> {
						if (!beeNestDisplay.isDead()) {
							beeNestDisplay.remove();
						}
					}, 1L);
				}

				if (!isCancelled()) {
					cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
