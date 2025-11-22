package com.playmonumenta.plugins.abilities.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.AbilityWithHealthBar;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.bosses.bosses.abilities.KeeperVirtueBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.seraph.KeeperVirtueCS;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Allay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class KeeperVirtue extends Ability implements AbilityWithHealthBar, AbilityWithDuration {
	private static final int VIRTUE_COOLDOWN = 20 * 20;
	private static final int VIRTUE_DAMAGE_R2 = 7;
	private static final int VIRTUE_DAMAGE_R3 = 10;
	private static final int VIRTUE_ATTACK_DELAY = 25;
	private static final int VIRTUE_ATTACK_DRAIN = 2;
	private static final int VIRTUE_ACTION_RANGE = 8;
	private static final int VIRTUE_DETECTION_RANGE = 16;
	private static final double VIRTUE_HEALING = 0.05;
	private static final int VIRTUE_HEAL_DELAY = 10;
	private static final int VIRTUE_HEAL_DRAIN = 2;
	private static final double VIRTUE_HEAL_LOWER_THRESHOLD = 0.6;
	private static final double VIRTUE_HEAL_UPPER_THRESHOLD = 0.7;
	private static final int VIRTUE_REGENERATION = 1; // special implementation as the allay has antiheal - this is a multiplier, not hp/s
	private static final int VIRTUE_MOVESPEED = 5; // blocks per second
	private static final double VIRTUE_SPEED = 0.15;
	private static final int VIRTUE_SPEED_RADIUS = 9;
	private static final double VIRTUE_VULN = 0.15;
	private static final int VIRTUE_VULN_DURATION = 4 * 20;
	private static final int VIRTUE_HEALTH_1 = 30;
	private static final int VIRTUE_HEALTH_2 = 35;
	private static final int VIRTUE_MINIMUM_HEALS = 5;

	public static final String CHARM_DAMAGE = "Keeper Virtue Damage";
	public static final String CHARM_ATTACK_DELAY = "Keeper Virtue Attack Delay";
	public static final String CHARM_ATTACK_DRAIN = "Keeper Virtue Attack Health Drain";
	public static final String CHARM_ACTION_RANGE = "Keeper Virtue Action Range";
	public static final String CHARM_DETECTION_RANGE = "Keeper Virtue Detection Range";
	public static final String CHARM_HEALING = "Keeper Virtue Healing";
	public static final String CHARM_HEAL_DELAY = "Keeper Virtue Heal Delay";
	public static final String CHARM_HEAL_DRAIN = "Keeper Virtue Heal Health Drain";
	public static final String CHARM_HEAL_LOWER_THRESHOLD = "Keeper Virtue Heal Lower Threshold";
	public static final String CHARM_HEAL_UPPER_THRESHOLD = "Keeper Virtue Heal Upper Threshold";
	public static final String CHARM_COOLDOWN = "Keeper Virtue Cooldown";
	public static final String CHARM_REGENERATION = "Keeper Virtue Regeneration";
	public static final String CHARM_MOVEMENT_SPEED = "Keeper Virtue Movement Speed";
	public static final String CHARM_SPEED = "Keeper Virtue Speed Amplifier";
	public static final String CHARM_SPEED_RADIUS = "Keeper Virtue Speed Radius";
	public static final String CHARM_VULN = "Keeper Virtue Vulnerability Amplifier";
	public static final String CHARM_VULN_DURATION = "Keeper Virtue Vulnerability Duration";
	public static final String CHARM_HEALTH = "Keeper Virtue Health";

	private @Nullable Allay mBoss = null;
	private @Nullable LivingEntity mTarget;
	private VirtueMode mMode = VirtueMode.INACTIVE;
	private boolean mModeIsLocked = false;
	private int mLastToggleTick = 0;
	private static final Map<Allay, Player> playerVirtueMap = new HashMap<>(); // A map to make sure only one Virtue can heal a player at a time

	public static final AbilityInfo<KeeperVirtue> INFO =
		new AbilityInfo<>(KeeperVirtue.class, "Keeper Virtue", KeeperVirtue::new)
			.linkedSpell(ClassAbility.KEEPER_VIRTUE)
			.scoreboardId("KeeperVirtue")
			.shorthandName("KV")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("An angelic spirit follows you, supporting nearby players and attacking Heretics but losing health in the process.")
			.cooldown(VIRTUE_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("toggleHeal", "toggle (only heal)", a -> a.changeModeCast(VirtueMode.ACTIVE_SUPPORT), new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false).lookDirections(AbilityTrigger.LookDirection.UP).enabled(false)))
			.addTrigger(new AbilityTriggerInfo<>("toggleDamage", "toggle (only damage)", a -> a.changeModeCast(VirtueMode.ACTIVE_COMBAT), new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false).lookDirections(AbilityTrigger.LookDirection.DOWN).enabled(false)))
			.addTrigger(new AbilityTriggerInfo<>("toggle", "toggle", a -> a.changeModeCast(VirtueMode.ACTIVE_GENERIC), new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)))
			.displayItem(Material.MUSIC_DISC_RELIC);

	private final double mDamage;
	private final int mAttackDelay;
	private final double mAttackDrain;
	private final double mActionRange;
	private final double mDetectionRange;
	private final double mHealing;
	private final int mHealDelay;
	private final double mHealDrain;
	private final double mHealLowerThreshold;
	private final double mHealUpperThreshold;
	private final double mRegeneration;
	private final double mMoveSpeed;
	private final double mSpeedAmplifier;
	private final double mSpeedRadius;
	private final double mVulnAmplifier;
	private final int mVulnDuration;
	private final double mHealth;
	private final int mMinimumHeals;
	private final int mCooldown;
	private final KeeperVirtueCS mCosmetic;

	public KeeperVirtue(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, ServerProperties.getAbilityEnhancementsEnabled(player) ? VIRTUE_DAMAGE_R3 : VIRTUE_DAMAGE_R2);
		mAttackDelay = CharmManager.getDuration(player, CHARM_ATTACK_DELAY, VIRTUE_ATTACK_DELAY);
		mAttackDrain = CharmManager.calculateFlatAndPercentValue(player, CHARM_ATTACK_DRAIN, VIRTUE_ATTACK_DRAIN);
		mActionRange = CharmManager.getRadius(player, CHARM_ACTION_RANGE, VIRTUE_ACTION_RANGE);
		mDetectionRange = CharmManager.getRadius(player, CHARM_DETECTION_RANGE, VIRTUE_DETECTION_RANGE);
		mHealing = CharmManager.getExtraPercent(player, CHARM_HEALING, VIRTUE_HEALING);
		mHealDelay = CharmManager.getDuration(player, CHARM_HEAL_DELAY, VIRTUE_HEAL_DELAY);
		mHealDrain = CharmManager.calculateFlatAndPercentValue(player, CHARM_HEAL_DRAIN, VIRTUE_HEAL_DRAIN);
		mHealLowerThreshold = VIRTUE_HEAL_LOWER_THRESHOLD + CharmManager.getLevelPercentDecimal(player, CHARM_HEAL_LOWER_THRESHOLD);
		mHealUpperThreshold = VIRTUE_HEAL_UPPER_THRESHOLD + CharmManager.getLevelPercentDecimal(player, CHARM_HEAL_UPPER_THRESHOLD);
		mRegeneration = CharmManager.getExtraPercent(player, CHARM_REGENERATION, VIRTUE_REGENERATION);
		mMoveSpeed = CharmManager.calculateFlatAndPercentValue(player, CHARM_MOVEMENT_SPEED, VIRTUE_MOVESPEED);
		mSpeedAmplifier = VIRTUE_SPEED + CharmManager.getLevelPercentDecimal(player, CHARM_SPEED);
		mSpeedRadius = CharmManager.getRadius(player, CHARM_SPEED_RADIUS, VIRTUE_SPEED_RADIUS);
		mVulnAmplifier = isLevelTwo() ? VIRTUE_VULN + CharmManager.getLevelPercentDecimal(player, CHARM_VULN) : 0;
		mVulnDuration = CharmManager.getDuration(player, CHARM_VULN_DURATION, VIRTUE_VULN_DURATION);
		mHealth = CharmManager.calculateFlatAndPercentValue(player, CHARM_HEALTH, isLevelOne() ? VIRTUE_HEALTH_1 : VIRTUE_HEALTH_2);
		mMinimumHeals = VIRTUE_MINIMUM_HEALS;
		mCooldown = CharmManager.getDuration(player, CHARM_COOLDOWN, VIRTUE_COOLDOWN);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new KeeperVirtueCS());
	}

	public enum VirtueMode {
		INACTIVE,
		ACTIVE_GENERIC,
		ACTIVE_COMBAT,
		ACTIVE_SUPPORT
	}

	@Override
	public void invalidate() {
		if (mBoss != null) {
			playerVirtueMap.remove(mBoss);
			mBoss.remove();
			mBoss = null;
			mTarget = null;
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (isOnCooldown()) {
			return;
		}
		if (mBoss != null && !mBoss.isValid()) {
			mBoss = null;
		}
		if (mBoss == null) {
			mBoss = (Allay) LibraryOfSoulsIntegration.summon(mPlayer.getLocation().add(mPlayer.getLocation().getDirection().setY(0).normalize().multiply(2)).add(0, 1.5, 0), mCosmetic.getLosName());
			if (mBoss == null) {
				MMLog.warning("Failed to summon KeeperVirtue");
				return;
			}

			KeeperVirtueBoss keeperVirtueBoss = BossUtils.getBossOfClass(mBoss, KeeperVirtueBoss.class);
			if (keeperVirtueBoss == null) {
				MMLog.warning("Failed to get KeeperVirtueBoss");
				return;
			}
			keeperVirtueBoss.spawn(mPlayer, mDamage, mAttackDelay, mAttackDrain, mHealing, mHealDelay, mHealDrain, mVulnAmplifier, mVulnDuration, this, mCosmetic);
			ClientModHandler.updateAbility(mPlayer, ClassAbility.KEEPER_VIRTUE);
			EntityUtils.setMaxHealthAndHealth(mBoss, mHealth);
			GlowingManager.startGlowing(mBoss, mCosmetic.getGlowColor(1), 40, GlowingManager.PLAYER_ABILITY_PRIORITY, mPlayer::equals, "KeeperVirtueGlowing");
			if (mMode == VirtueMode.INACTIVE) {
				changeMode(VirtueMode.INACTIVE);
			}

			cancelOnDeath(new BukkitRunnable() {
				int mTicksElapsed = 0;
				double mRadian = FastUtils.randomDoubleInRange(0, Math.PI);
				int mHeals = 0;

				@Override
				public void run() {
					if (mBoss == null || !mBoss.isValid() || !mPlayer.isValid() || !mPlayer.isOnline() || mBoss.getWorld() != mPlayer.getWorld()) {
						if (mBoss != null) {
							invalidate();
						}
						this.cancel();
						return;
					}
					boolean isLockedDamage = mModeIsLocked && mMode == VirtueMode.ACTIVE_COMBAT;
					boolean isLockedHealing = mModeIsLocked && mMode == VirtueMode.ACTIVE_SUPPORT;

					mTarget = mBoss.getTarget();
					Location pLoc = mPlayer.getLocation();
					List<Player> nearbyPlayers = EntityUtils.getNearestPlayers(pLoc, mDetectionRange);
					nearbyPlayers.remove(mPlayer);
					if (!nearbyPlayers.isEmpty() && !(mTarget instanceof Player) && mBoss != null && !isLockedDamage) {
						// Reset target if a player is found while targeting a mob
						nearbyPlayers.removeIf(player -> mPlugin.mEffectManager.getEffects(player, PercentHeal.class).stream().anyMatch(percentHeal -> percentHeal.getValue() < -0.995));
						nearbyPlayers.removeIf(player -> player.getHealth() > EntityUtils.getMaxHealth(player) * mHealLowerThreshold);
						nearbyPlayers.removeIf(playerVirtueMap::containsValue);
						if (!nearbyPlayers.isEmpty()) {
							mTarget = null;
						}
					}
					if (mMode != VirtueMode.INACTIVE && (mTarget == null || mTarget.isDead() || mTarget.getHealth() <= 0 || mTarget.getLocation().distance(pLoc) > mDetectionRange)) {
						// Reset target
						mTarget = null;
						if (mBoss != null) {
							mBoss.setTarget(null);
						}
						playerVirtueMap.remove(mBoss);
						if (!nearbyPlayers.isEmpty() && !isLockedDamage) {
							Collections.shuffle(nearbyPlayers);
							Player randomPlayer = nearbyPlayers.get(0);
							if (randomPlayer != null) {
								changeMode(VirtueMode.ACTIVE_SUPPORT);
								mBoss.setTarget(randomPlayer);
								mTarget = randomPlayer;
								playerVirtueMap.put(mBoss, randomPlayer);
								mHeals = 0;
							}
						}
						List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(pLoc, mDetectionRange, mBoss);
						if (!nearbyMobs.isEmpty() && mTarget == null && mBoss != null && !isLockedHealing) {
							nearbyMobs.removeIf(mob -> !LocationUtils.hasLineOfSight(Objects.requireNonNull(mBoss), mob));
							nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
							nearbyMobs.removeIf(mob -> DamageUtils.isImmuneToDamage(mob, DamageEvent.DamageType.MAGIC));
							nearbyMobs.removeIf(mob -> !Crusade.enemyTriggersAbilities(mob));
							if (!nearbyMobs.isEmpty()) {
								Collections.shuffle(nearbyMobs);
								LivingEntity randomMob = nearbyMobs.get(0);
								if (nearbyMobs.contains(EntityUtils.getEntityAtCursor(mPlayer, mDetectionRange))) {
									randomMob = EntityUtils.getEntityAtCursor(mPlayer, mDetectionRange);
								}
								if (randomMob != null) {
									changeMode(VirtueMode.ACTIVE_COMBAT);
									mBoss.setTarget(randomMob);
									mTarget = randomMob;
								}
							}
						}
					}

					// movement
					if (mBoss == null) {
						mTarget = null;
						return;
					}
					Location allayLoc = mBoss.getLocation();
					if (mTarget != null && !mTarget.isDead() && (mMode == VirtueMode.ACTIVE_COMBAT || mMode == VirtueMode.ACTIVE_SUPPORT)) {
						Vector direction = LocationUtils.getDirectionTo(LocationUtils.getEntityCenter(mTarget), allayLoc);
						double yDiff = (mTarget.getLocation().getY() - mBoss.getLocation().getY()) * 0.25;
						if (yDiff > direction.getY()) {
							direction.setY(yDiff);
						}
						allayLoc.setDirection(direction);
						double range = mActionRange + 0.5 * mTarget.getWidth();
						// Slow it down when close
						if (allayLoc.distance(LocationUtils.getEntityCenter(mTarget)) < range) {
							direction.multiply(Math.max(0, (allayLoc.distance(LocationUtils.getEntityCenter(mTarget)) - range + 2) / 2));
						}
						// set speed
						allayLoc.add(direction.multiply(mMoveSpeed / 20));
						// attack
						if (new Hitbox.SphereHitbox(allayLoc, range - 2).getBoundingBox().overlaps(mTarget.getBoundingBox())) {
							if (mMode == VirtueMode.ACTIVE_COMBAT) {
								mBoss.attack(mTarget);
							} else if (mMode == VirtueMode.ACTIVE_SUPPORT && mTarget instanceof Player player && playerVirtueMap.get(mBoss) == player) {
								mHeals += keeperVirtueBoss.healPlayer(player);
								if (player.getHealth() >= EntityUtils.getMaxHealth(player) * mHealUpperThreshold && (mHeals >= mMinimumHeals || player.getHealth() >= EntityUtils.getMaxHealth(player))) {
									mBoss.setTarget(null);
									mTarget = null;
									playerVirtueMap.remove(mBoss);
									mHeals = 0;
								}
							}
						}
					} else {
						// Follow player if there's no valid targets around
						Location playerLoc = LocationUtils.getEntityCenter(mPlayer);
						Vector direction = LocationUtils.getDirectionTo(playerLoc, allayLoc);
						Vector direction2 = direction.clone();
						Location allayLoc2 = allayLoc.clone();
						allayLoc2.setY(playerLoc.getY());
						double distance = allayLoc2.distance(playerLoc);
						// Teleport to the player when very far away
						if (distance > 2 * mDetectionRange) {
							allayLoc = playerLoc.clone().subtract(direction2.multiply(2));
						} else {
							if (distance < 4) {
								direction2.multiply(Math.max(0, 0.5 * (distance - 2))).setY(direction.getY());
							} else {
								direction2.multiply(Math.pow(1.2, Math.min(4, distance - 4))).setY(direction.getY());
							}
							allayLoc.add(direction2.clone().multiply(mMoveSpeed / 20));
						}
						allayLoc.setDirection(direction.clone().setY(0));
					}
					// bobbing
					mBoss.teleport(allayLoc.clone().add(0, FastMath.sin(mRadian) * 0.05, 0));
					mRadian += Math.PI / 20D; // Finishes a sin bob in (20 * 2) ticks
					mTicksElapsed++;
				}
			}.runTaskTimer(mPlugin, 0, 1));
		}

		if (mBoss != null) {
			ClientModHandler.updateAbility(mPlayer, ClassAbility.KEEPER_VIRTUE);
			mPlugin.mEffectManager.addEffect(mBoss, "KeeperVirtueHealing", new PercentHeal(10, 0) {
				@Override
				public boolean entityRegainHealthEvent(EntityRegainHealthEvent event) {
					// If inactive, regenerate 2 hp/s (default allay regen). If active but without a target, regenerate 1 hp/s. If active with a target, don't regenerate.
					if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
						event.setAmount(mRegeneration * event.getAmount() * (mMode == VirtueMode.INACTIVE ? 1 : mTarget == null ? 0.5 : 0));
						ClientModHandler.updateAbility(mPlayer, ClassAbility.KEEPER_VIRTUE);
					}
					return true;
				}
			});
			if (mMode == VirtueMode.INACTIVE) {
				List<Player> players = isLevelTwo() ? EntityUtils.getNearestPlayers(mPlayer.getLocation(), mSpeedRadius) : List.of(mPlayer);
				for (Player p : players) {
					mPlugin.mEffectManager.addEffect(p, "KeeperVirtueSpeed", new PercentSpeed(10, mSpeedAmplifier, "KeeperVirtueSpeed").displaysTime(false));
				}
			}
			double percent = getRemainingAbilityHealth() / getInitialAbilityHealth();
			GlowingManager.startGlowing(mBoss, mCosmetic.getGlowColor(percent), 10, GlowingManager.PLAYER_ABILITY_PRIORITY, mPlayer::equals, "KeeperVirtueGlowing");
		}
	}

	public static boolean allayBelongsTo(Allay allay, Player player) {
		KeeperVirtueBoss keeperVirtueBoss = BossUtils.getBossOfClass(allay, KeeperVirtueBoss.class);
		return keeperVirtueBoss != null && keeperVirtueBoss.mPlayer == player;
	}

	public boolean changeModeCast(VirtueMode mode) {
		if (mBoss == null || Bukkit.getCurrentTick() - mLastToggleTick < 5) {
			return false;
		}
		KeeperVirtueBoss keeperVirtueBoss = BossUtils.getBossOfClass(mBoss, KeeperVirtueBoss.class);
		if (keeperVirtueBoss == null) {
			MMLog.warning("Failed to get KeeperVirtueBoss");
			return false;
		}
		keeperVirtueBoss.resetActionTicks();
		if (mMode == VirtueMode.INACTIVE) {
			changeMode(mode);
			mCosmetic.changeModeCast(mBoss, true);
			if (mode != VirtueMode.ACTIVE_GENERIC) {
				mModeIsLocked = true;
			}
		} else {
			changeMode(VirtueMode.INACTIVE);
			mCosmetic.changeModeCast(mBoss, false);
			mModeIsLocked = false;
		}
		mLastToggleTick = Bukkit.getCurrentTick();
		return true;
	}

	public void changeMode(VirtueMode newMode) {
		if (mBoss == null) {
			return;
		}
		mBoss.getEquipment().setItemInMainHand(mCosmetic.getHeldItem(newMode));
		if (newMode == VirtueMode.INACTIVE) {
			mPlugin.mEffectManager.clearEffects(mBoss, "KeeperVirtueHealing");
		}
		playerVirtueMap.remove(mBoss);
		mMode = newMode;
		mTarget = null;
		mBoss.setTarget(null);
	}

	@Override
	public @Nullable String getMode() {
		VirtueMode mode;
		if (!mModeIsLocked) {
			// If not locked, use generic always
			mode = (mMode == VirtueMode.INACTIVE ? VirtueMode.INACTIVE : VirtueMode.ACTIVE_GENERIC);
		} else {
			mode = mMode;
		}
		return mode.name().toLowerCase(Locale.ROOT);
	}

	private static Description<KeeperVirtue> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Passively have a Virtue with ")
			.add(a -> a.mHealth, VIRTUE_HEALTH_1, false, Ability::isLevelOne)
			.add(" health follow you, and while inactive it regenerates ")
			.add(a -> a.mRegeneration, VIRTUE_REGENERATION)
			.add(" health every 0.5s and grants you ")
			.addPercent(a -> a.mSpeedAmplifier, VIRTUE_SPEED)
			.add(" speed. You can heal it with Hallowed Beam at any time. ")
			.addTrigger(2)
			.add(" to toggle it active, making it seek out players within ")
			.add(a -> a.mDetectionRange, VIRTUE_DETECTION_RANGE)
			.add(" blocks under ")
			.addPercent(a -> a.mHealLowerThreshold, VIRTUE_HEAL_LOWER_THRESHOLD)
			.add(" health, healing them for ")
			.addPercent(a -> a.mHealing, VIRTUE_HEALING)
			.add(" of their max health every ")
			.addDuration(a -> a.mHealDelay, VIRTUE_HEAL_DELAY)
			.add("s at least ")
			.add(a -> a.mMinimumHeals, VIRTUE_MINIMUM_HEALS)
			.add(" times and until they're up to ")
			.addPercent(a -> a.mHealUpperThreshold, VIRTUE_HEAL_UPPER_THRESHOLD)
			.add(" or higher, draining ")
			.add(a -> a.mHealDrain, VIRTUE_HEAL_DRAIN)
			.add(" of its health per heal. If no players are found, it instead seeks out Heretics, dealing R2: " + VIRTUE_DAMAGE_R2 + " / R3: ")
			.add(a -> ServerProperties.getAbilityEnhancementsEnabled(a.mPlayer) ? a.mDamage : VIRTUE_DAMAGE_R3, VIRTUE_DAMAGE_R3)
			.add(" magic damage to them and draining ")
			.add(a -> a.mAttackDrain, VIRTUE_ATTACK_DRAIN)
			.add(" health every ")
			.addDuration(a -> a.mAttackDelay, VIRTUE_ATTACK_DELAY)
			.add("s. The Virtue regenerates half as much while active with no target. If the Virtue dies, it respawns after ")
			.addDuration(a -> a.mCooldown, VIRTUE_COOLDOWN)
			.add("s.");
	}

	private static Description<KeeperVirtue> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The Virtue now has ")
			.add(a -> a.mHealth, VIRTUE_HEALTH_2, false, Ability::isLevelTwo)
			.add(" health. The speed bonus when inactive is now granted to all players in a ")
			.add(a -> a.mSpeedRadius, VIRTUE_SPEED_RADIUS)
			.add(" block radius around you. Mobs attacked by the Virtue are inflicted with ")
			.addPercent(a -> a.mVulnAmplifier, VIRTUE_VULN, false, Ability::isLevelTwo)
			.add(" vulnerability for ")
			.addDuration(a -> a.mVulnDuration, VIRTUE_VULN_DURATION)
			.add("s.");
	}

	@Override
	public double getInitialAbilityHealth() {
		return mBoss == null ? -1 : mHealth;
	}

	@Override
	public double getRemainingAbilityHealth() {
		return mBoss == null ? -1 : mBoss.getHealth();
	}

	// Trickery to use the UMM duration indicator as a health bar - multiply health by 1000 for both more accuracy and no bar sliding
	@Override
	public int getInitialAbilityDuration() {
		return mBoss == null ? -1 : (int) (1000 * mHealth);
	}

	@Override
	public int getRemainingAbilityDuration() {
		return mBoss == null ? -1 : (int) (1000 * mBoss.getHealth());
	}
}
