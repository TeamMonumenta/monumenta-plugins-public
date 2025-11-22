package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.abilities.PhantomForceBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PhantomForce extends DepthsAbility {
	public static final String ABILITY_NAME = "Phantom Force";
	public static final int COOLDOWN = 5 * 20;
	public static final int SPAWN_COUNT = 2;
	public static final int TWISTED_SPAWN_COUNT = 3;
	public static final double MOVEMENT_SPEED = 5;
	public static final int DURATION = 15 * 20;
	public static final double[] DAMAGE = {6, 7.5, 9, 10.5, 12, 15};
	public static final int VULNERABILITY_DURATION = 4 * 20;
	public static final double[] VULNERABILITY_AMOUNT = {0.1, 0.13, 0.16, 0.19, 0.22, 0.28};
	public static final int RADIUS = 2;
	public static final String CHARM_COOLDOWN = "Phantom Force Cooldown";

	public static final DepthsAbilityInfo<PhantomForce> INFO =
		new DepthsAbilityInfo<>(PhantomForce.class, ABILITY_NAME, PhantomForce::new, DepthsTree.SHADOWDANCER, DepthsTrigger.WILDCARD)
			.linkedSpell(ClassAbility.PHANTOM_FORCE)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.CHARCOAL)
			.descriptions(PhantomForce::getDescription);

	private final int mSpawnCount;
	private final double mDamage;
	private final double mVulnerabilityAmount;
	private final int mVulnerabilityDuration;
	private final double mRadius;
	private final int mDuration;
	private final double mSpeed;

	public PhantomForce(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSpawnCount = (mRarity == 6 ? TWISTED_SPAWN_COUNT : SPAWN_COUNT) + (int) CharmManager.getLevel(mPlayer, CharmEffects.PHANTOM_FORCE_SPAWN_COUNT.mEffectName);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.PHANTOM_FORCE_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRadius = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.PHANTOM_FORCE_RADIUS.mEffectName, RADIUS);
		mVulnerabilityAmount = VULNERABILITY_AMOUNT[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.PHANTOM_FORCE_VULNERABILITY_AMPLIFIER.mEffectName);
		mVulnerabilityDuration = CharmManager.getDuration(mPlayer, CharmEffects.PHANTOM_FORCE_VULNERABILITY_DURATION.mEffectName, VULNERABILITY_DURATION);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.PHANTOM_FORCE_VEX_DURATION.mEffectName, DURATION);
		mSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.PHANTOM_FORCE_MOVEMENT_SPEED.mEffectName, MOVEMENT_SPEED);
	}

	public void summonVexes(Location loc) {
		loc.getWorld().playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1f, 0.5f);
		loc.getWorld().playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 0.7f, 0.8f);
		new PartialParticle(Particle.SPELL_MOB, loc, 20).delta(0.5).spawnAsPlayerActive(mPlayer);

		for (int i = 0; i < mSpawnCount; i++) {
			Location spawnLoc = loc.clone().add(VectorUtils.rotateYAxis(new Vector(1, 0, 0), 360D * i / mSpawnCount));

			Vex vex = (Vex) LibraryOfSoulsIntegration.summon(spawnLoc, "PhantomForce");
			if (vex == null) {
				MMLog.warning("Failed to summon PhantomForce");
				return;
			}
			PhantomForceBoss phantomForceBoss = BossUtils.getBossOfClass(vex, PhantomForceBoss.class);
			if (phantomForceBoss == null) {
				MMLog.warning("Failed to get PhantomForceBoss");
				return;
			}
			ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
			phantomForceBoss.spawn(mPlayer, mDamage, mRadius, mVulnerabilityAmount, mVulnerabilityDuration, playerItemStats);

			new PartialParticle(Particle.SQUID_INK, vex.getEyeLocation(), 1).spawnAsPlayerActive(mPlayer);

			cancelOnDeath(new BukkitRunnable() {
				final Vex mBoss = Objects.requireNonNull(vex);
				@Nullable
				LivingEntity mTarget;
				int mTicks = 0;
				double mRadian = 0;

				@Override
				public void run() {
					Location pLoc = LocationUtils.getEntityCenter(mBoss).subtract(mBoss.getLocation().getDirection().multiply(0.5));
					new PartialParticle(Particle.SMOKE_NORMAL, pLoc, 4).delta(0.1).extra(0.05).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.FALLING_DUST, pLoc, 1).data(Material.BLACK_CONCRETE.createBlockData()).spawnAsPlayerActive(mPlayer);

					// aggro
					mTarget = mBoss.getTarget();
					if (mTarget == null || mTarget.isDead() || mTarget.getHealth() <= 0 || !mTarget.isValid()) {
						List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), 30, mBoss);
						nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
						nearbyMobs.removeIf(mob -> DamageUtils.isImmuneToDamage(mob, DamageType.MELEE_SKILL));
						if (!nearbyMobs.isEmpty()) {
							LivingEntity nearbyMob = EntityUtils.getNearestMob(mBoss.getLocation(), nearbyMobs);
							if (nearbyMob != null) {
								mBoss.setTarget(nearbyMob);
								mTarget = nearbyMob;

								loc.getWorld().playSound(loc, Sound.ENTITY_VEX_AMBIENT, SoundCategory.PLAYERS, 0.75f, 0.75f);
								loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 0.75f, 0.75f);
								Vector dir = LocationUtils.getHalfHeightLocation(mTarget).subtract(mBoss.getLocation()).toVector().normalize();
								new PPLine(Particle.SMOKE_NORMAL, loc, LocationUtils.getEntityCenter(mTarget))
									.countPerMeter(2)
									.directionalMode(true)
									.delta(dir.getX(), dir.getY(), dir.getZ())
									.extra(0.12)
									.includeEnd(false)
									.spawnAsPlayerActive(mPlayer);
							}
						}
					}

					// movement
					Location vexLoc = mBoss.getLocation();
					if (mTarget != null && !mTarget.isDead() && vexLoc.distanceSquared(mTarget.getEyeLocation()) > 1) {
						mBoss.setCharging(true);
						Vector direction = LocationUtils.getDirectionTo(mTarget.getEyeLocation(), vexLoc);
						double yDiff = (mTarget.getLocation().getY() - mBoss.getLocation().getY()) * 0.1;
						if (yDiff > direction.getY()) {
							direction.setY(yDiff);
						}
						vexLoc.setDirection(direction);
						// set speed
						vexLoc.add(direction.multiply(mSpeed * 1 / 20));
					} else {
						mBoss.setCharging(false);
					}
					mBoss.teleport(vexLoc.clone().add(0, FastMath.sin(mRadian) * 0.05, 0));
					mRadian += Math.PI / 20D;

					if (mTicks > mDuration || !mBoss.isValid()) {
						new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 40).delta(0.5).extra(0.2).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation(), 10).delta(0.5).data(Material.BLACK_CONCRETE.createBlockData()).spawnAsPlayerActive(mPlayer);

						mBoss.remove();
						this.cancel();
						return;
					}
					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1));
		}
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (isOnCooldown()) {
			return;
		}
		LivingEntity entity = event.getEntity();
		if (EntityUtils.isElite(entity)) {
			summonVexes(LocationUtils.getEntityCenter(entity));
			putOnCooldown();
		}
	}

	private static Description<PhantomForce> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Killing an elite spawns ")
			.add(a -> a.mSpawnCount, rarity == 6 ? TWISTED_SPAWN_COUNT : SPAWN_COUNT, false, null, rarity == 6)
			.add(" vexes that hover near your enemies. Hitting them will detonate them, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" melee damage in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius and applies ")
			.addPercent(a -> a.mVulnerabilityAmount, VULNERABILITY_AMOUNT[rarity - 1], false, true)
			.add(" vulnerability for ")
			.addDuration(a -> a.mVulnerabilityDuration, VULNERABILITY_DURATION)
			.add(" seconds. Vexes last for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds and have a movement speed of ")
			.add(a -> a.mSpeed, MOVEMENT_SPEED)
			.add(" blocks per second.")
			.addCooldown(COOLDOWN);
	}
}
