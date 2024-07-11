package com.playmonumenta.plugins.bosses.bosses;

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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
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
	}

	public WingedBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		final double[] distanceThresholds = {0, p.SAFE_DISTANCE + 0.01, p.SAFE_DISTANCE + 0.25, p.SAFE_DISTANCE + 5.25, p.SAFE_DISTANCE + 10.25, p.SAFE_DISTANCE + 20.25, 9999};
		final double[] speedValues = {0, 0, 0.125, 0.15, 0.2125, 0.5, 0.5};

		// Offset the boss' spawn location and disable gravity
		Location spawnLoc = boss.getLocation();
		boss.teleport(spawnLoc.add(0, 1.5, 0));
		mBoss.setGravity(false);

		// Spawn wings
		ItemStack[] armor = banner(p.BANNER_PATTERN);
		Collection<Entity> wings = createWings(spawnLoc);
		wings.forEach(e -> {
			if (e instanceof ArmorStand stand) {
				stand.getEquipment().setArmorContents(armor);
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

				// Temporarily stop flying if silenced/stunned
				if (EntityUtils.isSilenced(mBoss) || EntityUtils.isStunned(mBoss)) {
					wings.forEach(e -> {
						if (e instanceof ArmorStand stand) {
							stand.teleport(mBoss);
						}
					});
					return;
				}

				// Make the target detection more accurate by ignoring oscillation
				Location loc = mBoss.getLocation();
				Location middleLoc = mBoss.getLocation().clone().add(0, FastUtils.cosDeg(mVerticalOscillation * 6), 0);

				oscillateY(loc, FastUtils.sinDeg(mVerticalOscillation * 6) * 0.1, p.VERTICAL);

				LivingEntity target = ((Mob) mBoss).getTarget();
				if (target != null) {
					moveEntity(loc, target, middleLoc, p.VERTICAL, mFunction);
				}

				flapWings(wings, mReverseWingOscillation);

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

	private void flapWings(Collection<Entity> wings, boolean reverse) {
		wings.forEach(e -> {
			if (e instanceof ArmorStand stand) {
				stand.teleport(mBoss);
				if (!reverse) {
					stand.setHeadPose(stand.getHeadPose().add(WingedBoss.headMovement, 0, 0));
				} else {
					stand.setHeadPose(stand.getHeadPose().add(WingedBoss.headMovement * -1, 0, 0));
				}
			}
		});
	}

	private void oscillateY(Location loc, double movementY, boolean vertical) {
		// Oscillate the boss' Y position
		if (!LocationUtils.collidesWithSolid(loc.clone().add(0, movementY, 0))) {
			loc.add(0, movementY, 0);
		}

		// Teleport entity while keeping velocity, remove vertical knockback in non-vertical mode so that it can't offset the Y position
		Vector v = mBoss.getVelocity();
		if (!vertical) {
			v.setY(0);
		}
		mBoss.teleport(loc);
		mBoss.setVelocity(v);
	}

	private void moveEntity(Location loc, LivingEntity target, Location middleLoc, boolean verticalMovement, PolynomialSplineFunction function) {
		// Make the boss face the target
		Vector targetDir = target.getLocation().toVector().subtract(loc.toVector());
		double[] targetYawPitch = VectorUtils.vectorToRotation(targetDir);
		mBoss.setRotation((float) targetYawPitch[0], (float) targetYawPitch[1]);

		// Move the boss towards the target, use middleLoc (location in the middle of the Y oscillation) for a more accurate distanceToTarget
		double distanceToTarget = middleLoc.distance(target.getLocation());
		double speed = function.value(distanceToTarget);
		Location eyeLocation = mBoss.getEyeLocation();
		eyeLocation.setY(middleLoc.getY() + 0.5);
		Vector forwards = eyeLocation.getDirection().multiply(speed);
		if (!verticalMovement) {
			forwards.setY(0);
		}

		double mobSpeed = EntityUtils.getAttributeOrDefault(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, 0.2);
		forwards.multiply(mobSpeed * 5);
		NmsUtils.getVersionAdapter().moveEntity(mBoss, forwards);
	}

	private Collection<Entity> createWings(Location loc) {
		EntityUtils.summonEntityAt(loc, EntityType.ARMOR_STAND, "{Invisible:1b,Marker:1b,NoBasePlate:1b,NoGravity:1b,Pose:{Head:[20.0f,15.0f,-60.0f]},Silent:1b,Tags:[\"winged_wing\",\"REMOVE_ON_UNLOAD\"]}");
		EntityUtils.summonEntityAt(loc, EntityType.ARMOR_STAND, "{Invisible:1b,Marker:1b,NoBasePlate:1b,NoGravity:1b,Pose:{Head:[15.0f,0.0f,-90.0f]},Silent:1b,Tags:[\"winged_wing\",\"REMOVE_ON_UNLOAD\",\"effect_player\"]}");
		EntityUtils.summonEntityAt(loc, EntityType.ARMOR_STAND, "{Invisible:1b,Marker:1b,NoBasePlate:1b,NoGravity:1b,Pose:{Head:[20.0f,-15.0f,-120.0f]},Silent:1b,Tags:[\"winged_wing\",\"REMOVE_ON_UNLOAD\"]}");
		EntityUtils.summonEntityAt(loc, EntityType.ARMOR_STAND, "{Invisible:1b,Marker:1b,NoBasePlate:1b,NoGravity:1b,Pose:{Head:[20.0f,-15.0f,60.0f]},Silent:1b,Tags:[\"winged_wing\",\"REMOVE_ON_UNLOAD\"]}");
		EntityUtils.summonEntityAt(loc, EntityType.ARMOR_STAND, "{Invisible:1b,Marker:1b,NoBasePlate:1b,NoGravity:1b,Pose:{Head:[15.0f,0.0f,90.0f]},Silent:1b,Tags:[\"winged_wing\",\"REMOVE_ON_UNLOAD\"]}");
		EntityUtils.summonEntityAt(loc, EntityType.ARMOR_STAND, "{Invisible:1b,Marker:1b,NoBasePlate:1b,NoGravity:1b,Pose:{Head:[20.0f,15.0f,120.0f]},Silent:1b,Tags:[\"winged_wing\",\"REMOVE_ON_UNLOAD\"]}");

		Collection<Entity> wings = loc.getNearbyEntities(0.01, 0.01, 0.01);
		wings.removeIf((Entity entity) ->
			!entity.getScoreboardTags().contains("winged_wing"));
		return wings;
	}

	public static ItemStack[] banner(int pattern) {
		@Nullable ItemStack banner = new ItemStack(Material.WHITE_BANNER);
		ItemMeta im = banner.getItemMeta();
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
			}
		}
		banner.setItemMeta(im);
		banner.setAmount(1);

		return new ItemStack[]{new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), banner};
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
