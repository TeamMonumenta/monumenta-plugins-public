package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.abilities.RestlessSoulsBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist.RestlessSoulsCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class RestlessSouls extends Ability {
	private static final int DAMAGE_1 = 10;
	private static final int DAMAGE_2 = 15;
	private static final int SILENCE_DURATION_1 = 2 * 20;
	private static final int SILENCE_DURATION_2 = 3 * 20;
	private static final int VEX_DURATION = 15 * 20;
	private static final int VEX_CAP_1 = 3;
	private static final int VEX_CAP_2 = 5;
	private static final int DEBUFF_DURATION = 4 * 20;
	public static final String VEX_NAME = "RestlessSoul";
	private static final int TICK_INTERVAL = 1;
	private static final int DETECTION_RANGE = 24;
	private static final int RANGE = 8;
	private static final double DEBUFF_RANGE = 0.25;
	private static final double MOVESPEED = 5; // block(s) per second

	public static final String CHARM_DAMAGE = "Restless Souls Damage";
	public static final String CHARM_RADIUS = "Restless Souls Radius";
	public static final String CHARM_DURATION = "Restless Souls Duration";
	public static final String CHARM_CAP = "Restless Souls Vex Cap";
	public static final String CHARM_SILENCE_DURATION = "Restless Souls Silence Duration";
	public static final String CHARM_DEBUFF_RANGE = "Restless Souls Debuff Radius";
	public static final String CHARM_DEBUFF_DURATION = "Restless Souls Debuff Duration";
	public static final String CHARM_SPEED = "Restless Souls Movement Speed";

	public static final AbilityInfo<RestlessSouls> INFO =
		new AbilityInfo<>(RestlessSouls.class, "Restless Souls", RestlessSouls::new)
			.linkedSpell(ClassAbility.RESTLESS_SOULS)
			.scoreboardId("RestlessSouls")
			.shorthandName("RS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Mobs that die near you spawn a vex that targets enemies, dealing damage and silencing them.")
			.displayItem(Material.ENDER_EYE);

	private final boolean mLevel;
	private final double mDamage;
	private final int mSilenceTime;
	private final int mVexCap;
	private final double mDebuffRange;
	private final int mDebuffDuration;
	private final double mMoveSpeed;
	private final double mRadius;
	private final int mDuration;
	private final List<Vex> mVexList = new ArrayList<>();
	private final RestlessSoulsCS mCosmetic;

	public RestlessSouls(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		boolean isLevelOne = isLevelOne();
		mLevel = isLevelOne;
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne ? DAMAGE_1 : DAMAGE_2);
		mSilenceTime = CharmManager.getDuration(player, CHARM_SILENCE_DURATION, isLevelOne ? SILENCE_DURATION_1 : SILENCE_DURATION_2);
		mVexCap = (int) CharmManager.getLevel(player, CHARM_CAP) + (isLevelOne ? VEX_CAP_1 : VEX_CAP_2);
		mDebuffRange = CharmManager.calculateFlatAndPercentValue(player, CHARM_DEBUFF_RANGE, DEBUFF_RANGE);
		mDebuffDuration = CharmManager.getDuration(player, CHARM_DEBUFF_DURATION, DEBUFF_DURATION);
		mMoveSpeed = CharmManager.calculateFlatAndPercentValue(player, CHARM_SPEED, MOVESPEED);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RANGE);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, VEX_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new RestlessSoulsCS());
	}

	@Override
	public double entityDeathRadius() {
		return mRadius;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		Location summonLoc = event.getEntity().getLocation();

		if (!summonLoc.isChunkLoaded()) {
			// mob is standing somewhere that's not loaded, abort
			return;
		}

		mVexList.removeIf(e -> !e.isValid() || e.isDead());

		Set<String> tags = event.getEntity().getScoreboardTags();
		if (tags.contains("TeneGhost") || tags.contains(AbilityUtils.IGNORE_TAG)) {
			return;
		}

		if (mVexList.size() < mVexCap) {
			Vex vex = (Vex) LibraryOfSoulsIntegration.summon(summonLoc.clone(), VEX_NAME);
			if (vex == null) {
				MMLog.warning("Failed to summon RestlessSoul");
				return;
			}
			mVexList.add(vex);

			RestlessSoulsBoss restlessSoulsBoss = BossUtils.getBossOfClass(vex, RestlessSoulsBoss.class);
			if (restlessSoulsBoss == null) {
				MMLog.warning("Failed to get RestlessSoulsBoss");
				return;
			}
			ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
			restlessSoulsBoss.spawn(mPlayer, mDamage, mDebuffRange, mSilenceTime, mDebuffDuration, mLevel, playerItemStats, mCosmetic);

			new BukkitRunnable() {
				int mTicksElapsed = 0;
				@Nullable LivingEntity mTarget;
				final Vex mBoss = Objects.requireNonNull(vex);
				double mRadian = FastUtils.randomDoubleInRange(0, Math.PI);
				final int mNumber = mVexList.size() - 1;

				@Override
				public void run() {
					mCosmetic.vexTick(mPlayer, mBoss, mTicksElapsed);

					boolean isOutOfTime = mTicksElapsed >= mDuration;
					if (isOutOfTime || !mBoss.isValid()) {
						if (mBoss.isValid()) {
							mCosmetic.vexDespawn(mPlayer, mBoss);
						}
						mVexList.remove(mBoss);
						mBoss.remove();

						this.cancel();
						return;
					}

					// re-aggro
					mTarget = mBoss.getTarget();
					if (mTarget == null || mTarget.isDead() || mTarget.getHealth() <= 0) {
						Location pLoc = mPlayer.getLocation();
						List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(pLoc, DETECTION_RANGE, mBoss);
						if (!nearbyMobs.isEmpty()) {
							nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
							nearbyMobs.removeIf(mob -> DamageUtils.isImmuneToDamage(mob, DamageEvent.DamageType.MAGIC));
							// check mob count again after removal of vexes
							if (!nearbyMobs.isEmpty()) {
								Collections.shuffle(nearbyMobs);
								LivingEntity randomMob = nearbyMobs.get(0);
								if (randomMob != null) {
									mBoss.setTarget(randomMob);
									mTarget = randomMob;

									mCosmetic.vexTarget(mPlayer, mBoss, randomMob);
								}
							}
						}
					}

					// haunted move boss method
					// movement
					Location vexLoc = mBoss.getLocation();
					mBoss.setCharging(true);
					if (mTarget != null && !mTarget.isDead()) {
						Vector direction = LocationUtils.getDirectionTo(LocationUtils.getEntityCenter(mTarget), vexLoc);
						//0.2x distance for vertical movement for flying mobs
						double yDiff = (mTarget.getLocation().getY() - mBoss.getLocation().getY()) * 0.2;
						if (yDiff > direction.getY()) {
							direction.setY(yDiff);
						}
						vexLoc.setDirection(direction);
						// set speed
						vexLoc.add(direction.multiply(mMoveSpeed * TICK_INTERVAL / 20));
						// attack
						if (mBoss.getBoundingBox().overlaps(mTarget.getBoundingBox())) {
							mVexList.remove(mBoss);
							mBoss.attack(mTarget);
						}
					} else {
						// Follow player if there's no valid targets around
						Location playerLoc = mPlayer.getLocation();
						playerLoc.setPitch(0);
						Vector front = playerLoc.getDirection();
						Vector up = new Vector(0, 1, 0);
						Vector right = front.getCrossProduct(up);
						Vector behind = front.clone().multiply(-1);

						// will place vexes in a circle behind the player
						Vector circle = up.clone().multiply(FastUtils.sinDeg(90 + mNumber * 360.0 / mVexCap)).add(right.clone().multiply(FastUtils.cosDeg(90 + mNumber * 360.0 / mVexCap)));
						Location finalPlayerLoc = mPlayer.getEyeLocation().add(behind).add(circle);
						Vector direction = LocationUtils.getDirectionTo(finalPlayerLoc, vexLoc);
						vexLoc.add(direction.multiply(mMoveSpeed * TICK_INTERVAL / 20 * Math.max(Math.min(vexLoc.distance(finalPlayerLoc) / 3, 1), 0)));
						vexLoc.setDirection(direction.setY(0));
					}
					// bobbing
					mBoss.teleport(vexLoc.clone().add(0, FastMath.sin(mRadian) * 0.05, 0));
					mRadian += Math.PI / 20D; // Finishes a sin bob in (20 * 2) ticks
					mTicksElapsed += TICK_INTERVAL;
				}
			}.runTaskTimer(mPlugin, 0, TICK_INTERVAL);
		}
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (mVexList != null) {
			mVexList.removeIf(e -> !e.isValid() || e.isDead());
			if (!mVexList.isEmpty()) {
				for (Vex v : mVexList) {
					v.remove();
				}
				mVexList.clear();
			}
		}
	}

	private static Description<RestlessSouls> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Whenever an enemy dies within ")
			.add(a -> a.mRadius, RANGE)
			.add(" blocks of you, an invulnerable vex spawns. The vex targets mobs and possesses them, dealing ")
			.add(a -> a.mDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage to the target and silences all mobs within ")
			.add(a -> a.mDebuffRange, DEBUFF_RANGE)
			.add(" blocks for ")
			.addDuration(a -> a.mSilenceTime, SILENCE_DURATION_1, false, Ability::isLevelOne)
			.add(" seconds. Vex count is capped at ")
			.add(a -> a.mVexCap, VEX_CAP_1, false, Ability::isLevelOne)
			.add(" and each lasts for ")
			.addDuration(a -> a.mDuration, VEX_DURATION)
			.add(" seconds. Each vex can only possess 1 enemy. Enemies killed by the vex will not spawn additional vexes.");
	}

	private static Description<RestlessSouls> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" and silence duration increased to ")
			.addDuration(a -> a.mSilenceTime, SILENCE_DURATION_2, false, Ability::isLevelTwo)
			.add(" seconds. Maximum vex count increased to ")
			.add(a -> a.mVexCap, VEX_CAP_2, false, Ability::isLevelTwo)
			.add(". Additionally, the possessed mob is inflicted with a level 1 debuff of the corresponding active skill that is on cooldown for ")
			.addDuration(a -> a.mDebuffDuration, DEBUFF_DURATION)
			.add(" seconds. Grasping Claws > 10% Slowness. Level 1 Choleric Flames > Set mobs on Fire. Level 2 Choleric Flames > -100% Healing. Melancholic Lament > 10% Weaken. Withering Gaze > Decay 1. Haunting Shades > 10% Vulnerability.");
	}
}
