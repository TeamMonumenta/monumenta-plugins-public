package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DiscoBall extends DepthsAbility {

	public static final String ABILITY_NAME = "Disco Ball";
	public static final double[] DAMAGE = {3.5, 4.5, 5.5, 6.5, 7.5, 9.5};
	public static final int COOLDOWN = 18 * 20;
	public static final double MAX_HEIGHT = 10;
	public static final int DURATION = 5 * 20;
	public static final int SHOOT_INTERVAL = 10;
	public static final int SPOTLIGHT_PROJECTILES = 3;
	public static final int PARTICLE_REFRESH_INTERVAL = 10;
	public static final double BLAST_RADIUS = 2.5;
	public static final String BALL_HEAD_BASE64 = "ewogICJ0aW1lc3RhbXAiIDogMTY5Njc1Nzc0ODc0MSwKICAicHJvZmlsZUlkIiA6ICI0Y2FlYmM0N2YxNzI0ZTMyYWY2YTAxYmQwMGI2MWI1ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJiaWtlc3VwZXIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzQ4MDQyMTJiMjczYTVjYjRlOGMwM2M5ZTI5NTYxYmViY2M5NWQyNDQ5ZDhlNzYwZTY2YzgyMTRjMzc2YTRjYiIKICAgIH0KICB9Cn0=";
	public static final int[] TREE_COLORS = {DepthsUtils.DAWNBRINGER, DepthsUtils.EARTHBOUND, DepthsUtils.FLAMECALLER, DepthsUtils.FROSTBORN, DepthsUtils.PRISMATIC, DepthsUtils.SHADOWDANCER, DepthsUtils.STEELSAGE, DepthsUtils.WINDWALKER};

	public static final DepthsAbilityInfo<DiscoBall> INFO =
		new DepthsAbilityInfo<>(DiscoBall.class, ABILITY_NAME, DiscoBall::new, DepthsTree.PRISMATIC, DepthsTrigger.SHIFT_BOW)
			.linkedSpell(ClassAbility.DISCO_BALL)
			.cooldown(COOLDOWN)
			.displayItem(Material.PEARLESCENT_FROGLIGHT)
			.descriptions(DiscoBall::getDescription)
			.priorityAmount(949); // Needs to trigger before Rapid Fire;

	private final WeakHashMap<Projectile, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;
	private double mDamage;

	public DiscoBall(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPlayerItemStatsMap = new WeakHashMap<>();
		mDamage = DAMAGE[mRarity - 1];
		DepthsParty party = DepthsManager.getInstance().getDepthsParty(mPlayer);
		if (party != null) {
			mDamage *= party.getPrismaticDamageMultiplier();
		}
	}

	private void spawnDiscoBall(Projectile proj, Location loc) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(proj);
		// Find either the ceiling, or hit max height
		Location spawnLoc = LocationUtils.rayTraceToBlock(loc, new Vector(0, 1, 0), MAX_HEIGHT + 1, null);
		Location groundLoc = LocationUtils.rayTraceToBlock(spawnLoc, new Vector(0, -1, 0), 100, null);
		initDiscoBall(spawnLoc, spawnLoc.distance(groundLoc), playerItemStats);
		new PPLine(Particle.SPELL_INSTANT, loc, spawnLoc).countPerMeter(2).spawnAsPlayerActive(mPlayer);
	}

	private void initDiscoBall(Location loc, double distanceFromGround, ItemStatManager.PlayerItemStats playerItemStats) {
		loc.subtract(0, 1, 0);
		loc.setDirection(new Vector(0, -1, 0));
		@Nullable ItemDisplay ballHead = DisplayEntityUtils.spawnItemDisplayWithBase64Head(loc, BALL_HEAD_BASE64);
		if (ballHead != null) {
			new DisplayEntityUtils.DisplayAnimation(ballHead)
				.addDelay(1)
				.addKeyframe(new Transformation(new Vector3f(), new Quaternionf(0, 0.995f, 0, 0.1f), new Vector3f(1), new Quaternionf()), DURATION)
				.removeDisplaysAfterwards()
				.play();
		}

		loc.getWorld().playSound(loc, "block.amethyst_block.resonate", SoundCategory.PLAYERS, 5, 1);
		loc.getWorld().playSound(loc, "block.amethyst_block.resonate", SoundCategory.PLAYERS, 5, 1);
		cancelOnDeath(new BukkitRunnable() {
			final Location mBallLoc = loc;
			final Location mCeilingAboveLoc = LocationUtils.rayTraceToBlock(mBallLoc, new Vector(0, 1, 0), 100, null);
			final boolean mFinalBlast = checkFinalBlast();
			final double mDistanceFromGround = distanceFromGround;
			final int mMaxShots = DURATION / SHOOT_INTERVAL;

			int mTicks = 1;
			int mTimesShot = 0;
			double mAngle = 5;
			final ArrayList<LivingEntity> mAlreadyHitMobs = new ArrayList<>();

			@Override
			public void run() {
				if (mTicks % PARTICLE_REFRESH_INTERVAL == 0) {
					drawDiscoBall();
				}

				if (mTicks % SHOOT_INTERVAL == 0) {
					mAlreadyHitMobs.clear();
					mTimesShot++;
					if (mTimesShot == mMaxShots && mFinalBlast) {
						doFinalBlast();
					} else {
						shoot(SPOTLIGHT_PROJECTILES);
					}
					mAngle += 5;
				}

				mTicks++;
				if (mTicks > DURATION) {
					mBallLoc.getWorld().playSound(mBallLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 5, 0.5f);
					cancel();
				}
			}

			public void drawDiscoBall() {
				new PartialParticle(Particle.END_ROD, mBallLoc).count(2).delta(0.2).spawnAsPlayerActive(mPlayer);
				// Line that connects the disco ball to the ceiling
				new PPLine(Particle.ENCHANTMENT_TABLE, mBallLoc, mCeilingAboveLoc).countPerMeter(1).spawnAsPlayerActive(mPlayer);
			}

			public void shoot(int amount) {
				for (int shot = 0; shot < amount; shot++) {
					// Try to target mobs under the cone of the disco ball first.
					List<LivingEntity> hitMobs = Hitbox.approximateCone(mBallLoc, mDistanceFromGround, Math.toRadians(mAngle)).getHitMobs();
					hitMobs.removeIf(mob -> !LocationUtils.hasLineOfSight(mBallLoc, LocationUtils.getHalfHeightLocation(mob)));
					hitMobs.removeIf(mAlreadyHitMobs::contains);
					if (!hitMobs.isEmpty()) {
						Collections.shuffle(hitMobs);
						impactLocation(hitMobs.get(0).getLocation());
						mAlreadyHitMobs.add(hitMobs.get(0));
					} else {
						shootRandomly();
					}
				}
			}

			public void shootRandomly() {
				Vector dir = new Vector(0, -1, 0);
				dir = VectorUtils.rotateXAxis(dir, FastUtils.randomDoubleInRange(0, mAngle));
				dir = VectorUtils.rotateYAxis(dir, FastUtils.randomDoubleInRange(0, 360));
				LocationUtils.rayTraceToBlock(mBallLoc, dir, 50, this::impactLocation);
			}

			public void impactLocation(Location loc) {
				new PPLine(Particle.SPELL_INSTANT, mBallLoc, loc).countPerMeter(1).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.END_ROD, loc).count(15).delta(0.2).extra(0.05).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc).count(25).extra(0.1).spawnAsPlayerActive(mPlayer);
				loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.5f);
				damageLocation(loc, false);
			}

			public void impactLocationSpecial(Location loc, int colorIndex) {
				Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(TREE_COLORS[colorIndex]), 2);
				new PPLine(Particle.REDSTONE, mBallLoc, loc).countPerMeter(2).data(dustOptions).spawnAsPlayerActive(mPlayer);
				new PPCircle(Particle.REDSTONE, loc, BLAST_RADIUS).countPerMeter(2).data(dustOptions).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc).count(25).extra(0.1).spawnAsPlayerActive(mPlayer);
				loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.5f);
				damageLocation(loc, true);
			}

			public void damageLocation(Location loc, boolean quadruple) {
				double damage = mDamage * (quadruple ? 4 : 1);
				Hitbox hitbox = new Hitbox.SphereHitbox(loc, BLAST_RADIUS);
				for (LivingEntity mob : hitbox.getHitMobs()) {
					if (!mAlreadyHitMobs.contains(mob)) {
						DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, false, false);
					}
				}
			}

			public boolean checkFinalBlast() {
				DepthsParty depthsParty = DepthsManager.getInstance().getDepthsParty(mPlayer);
				if (depthsParty == null) {
					return false;
				}

				Set<DepthsTree> globalDepthsTrees = new HashSet<>();
				depthsParty.mPlayersInParty.forEach(dPlayer -> {
					List<DepthsAbilityInfo<?>> abilities = DepthsManager.getInstance().getPlayerAbilities(dPlayer.getPlayer());
					Set<DepthsTree> activeTrees = abilities.stream()
						.filter(depthsAbilityInfo -> !depthsAbilityInfo.getDepthsTrigger().equals(DepthsTrigger.PASSIVE))
						.map(DepthsAbilityInfo::getDepthsTree)
						.filter(Objects::nonNull) // Weapon Aspects have null tree
						.filter(tree -> tree != DepthsTree.CURSE)
						.collect(Collectors.toSet());
					globalDepthsTrees.addAll(activeTrees);
				});
				// If there are 8 different trees in the Set, it means at least one active ability in each of them
				// is owned across the entire party.
				return globalDepthsTrees.size() >= 8;
			}

			public void doFinalBlast() {
				cancelOnDeath(new BukkitRunnable() {
					final double mThetaStep = Math.PI / 4;
					double mTheta = 0;
					int mRuns = 0;
					@Override
					public void run() {
						// Try to target mobs under the cone of the disco ball first.
						List<LivingEntity> hitMobs = Hitbox.approximateCone(mBallLoc, mDistanceFromGround, Math.PI).getHitMobs();
						hitMobs.removeIf(e -> e.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
						if (!hitMobs.isEmpty()) {
							Collections.shuffle(hitMobs);
							impactLocationSpecial(hitMobs.get(0).getLocation(), mRuns);
						} else {
							Vector dir = new Vector(FastUtils.cos(mTheta) * 0.5, -1, FastUtils.sin(mTheta) * 0.5).normalize();
							LocationUtils.rayTraceToBlock(mBallLoc, dir, 50, l -> impactLocationSpecial(l, mRuns));
						}

						if (mRuns >= 7) {
							cancel();
							return;
						}
						mRuns++;
						mTheta += mThetaStep;
					}
				}.runTaskTimer(Plugin.getInstance(), 0, 2));
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1));
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity damager = event.getDamager();
		if (event.getType() == DamageEvent.DamageType.PROJECTILE && damager instanceof AbstractArrow arrow && mPlayerItemStatsMap.containsKey(damager)) {
			spawnDiscoBall(arrow, enemy.getLocation());
		}
		return false;
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Snowball && mPlayerItemStatsMap.containsKey(proj)) {
			spawnDiscoBall(proj, proj.getLocation());
		}
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (isOnCooldown()
			|| !mPlayer.isSneaking()
			|| !EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			return true;
		}
		putOnCooldown((int) (getModifiedCooldown() * BowAspect.getCooldownReduction(mPlayer)));

		if (projectile instanceof AbstractArrow arrow) {
			arrow.setPierceLevel(0);
			arrow.setCritical(true);
			arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
		}

		mPlayerItemStatsMap.put(projectile, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));

		mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.SPELL_INSTANT);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {

				if (mT > COOLDOWN || !mPlayerItemStatsMap.containsKey(projectile)) {
					projectile.remove();
					this.cancel();
					return;
				}
				if (projectile.getVelocity().length() < .05 || projectile.isOnGround()) {
					spawnDiscoBall(projectile, projectile.getLocation());

					this.cancel();
				}
				mT++;
			}

		}.runTaskTimer(mPlugin, 0, 1);
		return true;
	}

	private static Description<DiscoBall> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DiscoBall>(color)
			.add("Fire a projectile weapon while sneaking to summon a spinning Disco Ball above where your projectile lands. The ball lasts ")
			.addDuration(DURATION)
			.add(" seconds, and rains ")
			.addDepthsDamage(a -> DAMAGE[rarity - 1], DAMAGE[rarity - 1], true)
			.add(" magic damage onto 3 targets every 0.5 seconds. The disco ball starts out with a narrow targeting angle and expands over time. If your party has an active trigger ability " +
				"from all eight trees, the final pulse of the ball will heat up the dance floor with a color show that " +
				"deals quadruple damage.")
			.addCooldown(COOLDOWN)
			.add((a, p) -> {
				if (p == null) {
					return Component.empty();
				}
				DepthsParty party = DepthsManager.getInstance().getDepthsParty(p);
				if (party == null) {
					return Component.empty();
				}
				return Component.text("\n\nAscension Damage Bonus: " + StringUtils.multiplierToPercentageWithSign(party.getPrismaticDamageMultiplier() - 1));
			});
	}
}
