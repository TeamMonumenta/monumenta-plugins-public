package com.playmonumenta.plugins.bosses.bosses;

import com.google.common.collect.Lists;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class WingedBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_winged";
	// In-game, Minecraft shows Pose values as degrees, but internally it's radians
	public static final double headMovement = 0.8 * Math.PI / 180;

	public static class Parameters extends BossParameters {
		@BossParam(help = "Only move towards players and idly oscillate if there is a player within this radius")
		public int DETECTION = 100;

		@BossParam(help = "Whether the boss can move vertically outside of its oscillation")
		public boolean VERTICAL = true;

		@BossParam(help = "Distance from target at which the boss no longer moves forwards")
		public double SAFE_DISTANCE = 0;

		@BossParam(help = "Banner pattern")
		public int BANNER_PATTERN = 1;

		@BossParam(help = "Manual Y offset for wing position relative to the entity")
		public double WING_Y_OFFSET = 0;

		@BossParam(help = "Ignore blocks and be able to move through walls")
		public boolean IGNORE_BLOCKS = false;
	}

	public WingedBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		final double[] distanceThresholds = {0, p.SAFE_DISTANCE + 0.01, p.SAFE_DISTANCE + 0.25, p.SAFE_DISTANCE + 5.25, p.SAFE_DISTANCE + 10.25, p.SAFE_DISTANCE + 20.25, 9999};
		final double[] speedValues = {0, 0, 0.125, 0.15, 0.2125, 0.5, 0.5};

		// Offset the boss' spawn location unless it would be inside a block
		Location spawnLoc = boss.getLocation();
		if (!LocationUtils.collidesWithBlocks(boss.getBoundingBox().shift(0, 2, 0), boss.getWorld())) {
			boss.teleport(spawnLoc.add(0, 2, 0));
		}
		boss.setGravity(false);

		// Spawn wings
		Collection<Entity> wings = createWings(spawnLoc);
		List<ItemStack[]> armorList = banner(p.BANNER_PATTERN);
		wings.forEach(e -> {
			if (e instanceof ArmorStand stand) {
				if (stand.getScoreboardTags().contains("left")) {
					stand.getEquipment().setArmorContents(armorList.get(0));
				} else {
					stand.getEquipment().setArmorContents(armorList.get(1));
				}
				stand.removeScoreboardTag("winged_wing");
			}
		});

		new BukkitRunnable() {
			final LinearInterpolator mInterp = new LinearInterpolator();
			final PolynomialSplineFunction mFunction = mInterp.interpolate(distanceThresholds, speedValues);

			boolean mReverseWingOscillation = false;
			int mWingOscillation = 14;
			int mVerticalOscillation = 15;

			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					destroyWings(wings);

					this.cancel();
					return;
				}

				// Return early if no players within detection range
				if (mBoss.getLocation().getNearbyPlayers(p.DETECTION).isEmpty()) {
					return;
				}

				// Temporarily stop flying if stunned
				if (EntityUtils.isStunned(mBoss)) {
					wings.forEach(e -> {
						if (e instanceof ArmorStand stand) {
							stand.teleport(mBoss.getLocation().add(0, p.WING_Y_OFFSET, 0));
						}
					});
					return;
				}

				// Make the target detection more accurate by ignoring oscillation
				Location loc = mBoss.getLocation();
				Location middleLoc = loc.clone().add(0, FastUtils.cosDeg(mVerticalOscillation * 6), 0);

				oscillateY(loc, FastUtils.sinDeg(mVerticalOscillation * 6) * 0.1, p.VERTICAL, p.IGNORE_BLOCKS);

				LivingEntity target = ((Mob) mBoss).getTarget();
				double mobSpeed = EntityUtils.getAttributeOrDefault(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, 0.2);
				if (target != null && mobSpeed != 0) {
					moveEntity(loc, target, middleLoc, p.VERTICAL, mFunction, mobSpeed, p.IGNORE_BLOCKS);
				}

				flapWings(wings, mReverseWingOscillation, p.WING_Y_OFFSET);

				mWingOscillation++;
				if (mWingOscillation == 27) {
					mReverseWingOscillation = !mReverseWingOscillation;
					mWingOscillation = 0;
				}

				mVerticalOscillation++;
				if (mVerticalOscillation == 60) {
					mVerticalOscillation = 0;
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), p.DETECTION, null);
	}

	private void flapWings(Collection<Entity> wings, boolean reverse, double yOffset) {
		wings.forEach(e -> {
			if (e instanceof ArmorStand stand) {
				stand.teleport(mBoss.getLocation().add(0, yOffset, 0));
				if (!reverse) {
					stand.setHeadPose(stand.getHeadPose().add(headMovement, 0, 0));
				} else {
					stand.setHeadPose(stand.getHeadPose().add(headMovement * -1, 0, 0));
				}
			}
		});
	}

	private void oscillateY(Location loc, double movementY, boolean vertical, boolean ignoreBlocks) {
		// Return if the boss would move into a block
		if (!ignoreBlocks && LocationUtils.collidesWithBlocks(mBoss.getBoundingBox().shift(0, movementY, 0), loc.getWorld())) {
			return;
		}

		// Teleport entity while keeping velocity, remove vertical knockback in non-vertical mode so that it can't offset the Y position
		Vector v = mBoss.getVelocity();
		if (!vertical) {
			v.setY(0);
		}
		EntityUtils.teleportStack(mBoss, loc.add(0, movementY, 0));
		mBoss.setVelocity(v);
	}

	private void moveEntity(Location loc, LivingEntity target, Location middleLoc, boolean verticalMovement, PolynomialSplineFunction function, double mobSpeed, boolean ignoreBlocks) {
		// Make the boss face the target
		Vector targetDir = target.getLocation().toVector().subtract(loc.toVector());
		double[] targetYawPitch = VectorUtils.vectorToRotation(targetDir);
		if (!Double.isFinite(targetYawPitch[0]) || !Double.isFinite(targetYawPitch[1])) {
			targetYawPitch = new double[] {0, 0};
		}
		mBoss.setRotation((float) targetYawPitch[0], (float) targetYawPitch[1]);

		// Move the boss towards the target, use middleLoc (location in the middle of the Y oscillation) for a more accurate distanceToTarget
		double distanceToTarget = middleLoc.distance(target.getEyeLocation());
		double speed = function.value(distanceToTarget);
		Location eyeLocation = mBoss.getEyeLocation();
		eyeLocation.setY(middleLoc.getY() + 0.5);
		Vector forwards = eyeLocation.getDirection().multiply(speed);

		double yDifference = middleLoc.getY() - target.getEyeLocation().getY();
		// If entity isn't level enough with the player, increase vertical movement by assuming the boss is further than it is in reality
		if (verticalMovement && (yDifference <= -2.5 || yDifference >= 1.5)) {
			forwards.setY(eyeLocation.getDirection().getY() * function.value(distanceToTarget + Math.abs(yDifference)));
		}

		if (!verticalMovement) {
			forwards.setY(0);
		}

		forwards.multiply(mobSpeed * 5);
		if (!ignoreBlocks) {
			NmsUtils.getVersionAdapter().moveEntity(mBoss, forwards);
		} else {
			// Teleport entity while keeping velocity
			Vector v = mBoss.getVelocity();
			Location endLoc = new Location(loc.getWorld(), loc.getX() + forwards.getX(), loc.getY() + forwards.getY(), loc.getZ() + forwards.getZ(), (float) targetYawPitch[0], (float) targetYawPitch[1]);
			EntityUtils.teleportStack(mBoss, endLoc);
			mBoss.setVelocity(v);
		}
	}

	private ArmorStand createWing(Location loc, EulerAngle headPose, String... tags) {
		return loc.getWorld().spawn(loc, ArmorStand.class, e -> {
			e.setInvisible(true);
			e.setMarker(true);
			e.setBasePlate(false);
			e.setGravity(false);
			e.setHeadPose(headPose);
			e.setSilent(true);
			e.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
			for (String tag : tags) {
				e.addScoreboardTag(tag);
			}
		});
	}

	private Collection<Entity> createWings(Location loc) {
		return List.of(
			createWing(loc, new EulerAngle(20f, 15f, -60f), "left"),
			createWing(loc, new EulerAngle(20f, 15f, -60f), "left", "effect_player"),
			createWing(loc, new EulerAngle(20f, 15f, -60f), "left"),
			createWing(loc, new EulerAngle(20f, 15f, -60f), "right"),
			createWing(loc, new EulerAngle(20f, 15f, -60f), "right"),
			createWing(loc, new EulerAngle(20f, 15f, -60f), "right")
		);
	}

	public static List<ItemStack[]> banner(int pattern) {
		@Nullable ItemStack banner = new ItemStack(Material.WHITE_BANNER);
		@Nullable ItemStack banner1 = new ItemStack(Material.WHITE_BANNER);
		ItemMeta im = banner.getItemMeta();
		ItemMeta im1 = banner1.getItemMeta();
		boolean differentHalves = false;
		if (im instanceof BannerMeta bannerMeta) {
			switch (pattern) {
				case 1:
				default:
					bannerMeta.addPattern(new Pattern(DyeColor.RED, PatternType.GRADIENT));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.BORDER));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_TOP));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.SKULL));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_BOTTOM));
					break;
				case 2:
					banner = new ItemStack(Material.BLACK_BANNER);
					bannerMeta.addPattern(new Pattern(DyeColor.RED, PatternType.GRADIENT));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.BORDER));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_TOP));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.SKULL));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_BOTTOM));
					break;
				case 3:
					bannerMeta.addPattern(new Pattern(DyeColor.PINK, PatternType.GRADIENT));
					bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
					bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.TRIANGLE_TOP));
					bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.SKULL));
					bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
					bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.TRIANGLE_BOTTOM));
					break;
				case 4:
					banner = new ItemStack(Material.BLACK_BANNER);
					bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_SMALL));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.CURLY_BORDER));
					bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.FLOWER));
					bannerMeta.addPattern(new Pattern(DyeColor.GRAY, PatternType.GRADIENT_UP));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.CIRCLE_MIDDLE));
					break;
				case 5:
					differentHalves = true;
					// Left half
					banner = new ItemStack(Material.RED_BANNER);
					bannerMeta.addPattern(new Pattern(DyeColor.MAGENTA, PatternType.GRADIENT_UP));
					bannerMeta.addPattern(new Pattern(DyeColor.ORANGE, PatternType.CROSS));
					bannerMeta.addPattern(new Pattern(DyeColor.YELLOW, PatternType.FLOWER));
					bannerMeta.addPattern(new Pattern(DyeColor.RED, PatternType.GRADIENT));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.DIAGONAL_LEFT_MIRROR));
					bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER));
					// Right half
					banner1 = new ItemStack(Material.RED_BANNER);
					if (im1 instanceof BannerMeta bannerMeta1) {
						bannerMeta1.addPattern(new Pattern(DyeColor.MAGENTA, PatternType.GRADIENT_UP));
						bannerMeta1.addPattern(new Pattern(DyeColor.ORANGE, PatternType.CROSS));
						bannerMeta1.addPattern(new Pattern(DyeColor.YELLOW, PatternType.FLOWER));
						bannerMeta1.addPattern(new Pattern(DyeColor.RED, PatternType.GRADIENT));
						bannerMeta1.addPattern(new Pattern(DyeColor.BLACK, PatternType.DIAGONAL_RIGHT));
						bannerMeta1.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER));
					}
			}
		}
		banner.setItemMeta(im);
		banner.setAmount(1);
		if (!differentHalves) {
			banner1 = banner;
		} else {
			banner1.setItemMeta(im1);
			banner1.setAmount(1);
		}

		List<ItemStack[]> armorList = Lists.newArrayList();
		armorList.add(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), banner});
		armorList.add(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), banner1});
		return armorList;

	}

	@Override
	public void onHurt(DamageEvent event) {
		// cancel all fall damage
		if (event.getType() == DamageEvent.DamageType.FALL) {
			event.setCancelled(true);
		}
	}

	public void destroyWings(Collection<Entity> wings) {
		wings.forEach(e -> {
			if (e instanceof ArmorStand stand) {
				// Only play runnable on one of the armor stands for optimization and so that particles/sounds don't duplicate
				stand.setGravity(true);
				stand.setMarker(false);

				if (stand.getScoreboardTags().contains("effect_player")) {
					stand.getWorld().playSound(stand.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 1.5f, 0.5f);
					new BukkitRunnable() {
						int mT = 0;
						boolean mHasLanded = false;

						@Override
						public void run() {
							Location loc = stand.getLocation();

							if (stand.isDead() || !stand.isValid()) {
								this.cancel();
								return;
							}

							// Particles while falling
							new PartialParticle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, 1.5, 0), 2).delta(0.5, 0.5, 0.5).spawnAsEntityActive(mBoss);

							// Delay destroying the wings for a better effect
							if (mT == 5) {
								stand.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 1.0f, 0.5f);
								stand.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1.0f, 0.5f);
								stand.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 1.0f, 0.5f);
								new PartialParticle(Particle.EXPLOSION_LARGE, loc.clone().add(0, 1, 0), 3).delta(0.5, 0.25, 0.5).spawnAsEntityActive(mBoss);
								ParticleUtils.drawSphere(loc.clone().add(0, 1.5, 0), 15, 1,
									(l, t) -> {
										new PartialParticle(Particle.BLOCK_CRACK, l, 1).directionalMode(true)
											.delta(FastUtils.randomDoubleInRange(-1, 1), 1, FastUtils.randomDoubleInRange(-1, 1)).extra(10).data(Material.GLASS.createBlockData()).spawnAsEntityActive(mBoss);
									});
								Bukkit.getScheduler().runTaskLater(mPlugin, () -> wings.forEach(Entity::remove), 1);
							}


							// Detect if the wings have landed or fallen into a cobweb
							List<Location> blocks = LocationUtils.getLocationsTouching(stand.getBoundingBox(), stand.getWorld());
							Predicate<Location> isCobweb = location -> location.getBlock().getType().equals(Material.COBWEB);
							if (stand.isOnGround() || blocks.stream().anyMatch(isCobweb)) {
								mHasLanded = true;
								wings.forEach(e -> {
									if (e instanceof ArmorStand stand) {
										stand.setMarker(true);
										stand.setGravity(false);
									}
								});
							}

							// Since the wings are offset from the base of the armor stand, the stand has to be manually moved at the end
							if (mHasLanded) {
								mT++;
								wings.forEach(e -> e.teleport(loc.subtract(0, 0.1, 0)));
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);
				}
			}
		});
	}
}
