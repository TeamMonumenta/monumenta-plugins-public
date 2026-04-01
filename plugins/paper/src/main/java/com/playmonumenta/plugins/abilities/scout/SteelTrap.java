package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.SteelTrapCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.LIGHT_GREY;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class SteelTrap extends Ability implements AbilityWithChargesOrStacks {
	private static final int NOT_LANDED = -1;

	private static final int MAX_CHARGES = 2;
	private static final int TRAP_COOLDOWN = 12 * Constants.TICKS_PER_SECOND;
	private static final double DAMAGE_R1 = 7;
	private static final double DAMAGE_R2 = 12;
	private static final double DAMAGE_R3 = 16;
	private static final double DAMAGE_R4 = 18; // It's true Region 4 exist
	private static final double RADIUS_L1 = 3;
	private static final double RADIUS_L2 = 4;
	private static final int STAGGER_DURATION = 2 * Constants.TICKS_PER_SECOND;
	private static final int DURATION = 10 * Constants.TICKS_PER_SECOND;
	private static final int PRIMING_DURATION_L1 = 30;
	private static final int PRIMING_DURATION_L3 = 15;
	private static final double VELOCITY = 1.1;
	private static final double TRIGGER_RADIUS = 2;
	private static final float KNOCKBACK_HORIZONTAL = 0.75f;
	private static final float KNOCKBACK_VERTICAL = 0.45f;
	private static final double VULN = 0.15;
	private static final int VULN_DURATION = 5 * Constants.TICKS_PER_SECOND;

	public static final String CHARM_COOLDOWN = "Steel Trap Cooldown";
	public static final String CHARM_DAMAGE = "Steel Trap Damage";
	public static final String CHARM_RADIUS = "Steel Trap Radius";
	public static final String CHARM_STAGGER_DURATION = "Steel Trap Stagger Duration";
	public static final String CHARM_DURATION = "Steel Trap Duration";
	public static final String CHARM_PRIMING_DURATION = "Steel Trap Priming Duration";
	public static final String CHARM_VELOCITY = "Steel Trap Velocity";
	public static final String CHARM_CHARGES = "Steel Trap Charge";
	public static final String CHARM_TRIGGER_RADIUS = "Steel Trap Trigger Radius";
	public static final String CHARM_KNOCKBACK = "Steel Trap Knockback";
	public static final String CHARM_VULN = "Steel Trap Vulnerability Amplifier";
	public static final String CHARM_VULN_DURATION = "Steel Trap Vulnerability Duration";

	private final double mDamage;
	private final double mRadius;
	private final int mStaggerDuration;
	private final int mTrapDuration;
	private final int mPrimingDuration;
	private final double mVelocity;
	private final double mTriggerRadius;
	private final int mMaxCharges;
	private final float mKnockbackHorizontal;
	private final float mKnockbackVertical;
	private final double mVulnerability;
	private final int mVulnerabilityDuration;

	private int mCharges;
	private int mLastCastTicks = 0;
	private boolean mWasOnCooldown;

	private final SteelTrapCS mCosmetic;

	private final HashSet<Trap> mEnhancedTrap = new HashSet<>();

	public static final AbilityInfo<SteelTrap> INFO =
		new AbilityInfo<>(SteelTrap.class, "Steel Trap", SteelTrap::new)
			.linkedSpell(ClassAbility.STEEL_TRAP)
			.scoreboardId("SteelTrap")
			.shorthandName("ST")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Throw a trap to stagger mobs.")
			.cooldown(TRAP_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", SteelTrap::cast, new AbilityTrigger(AbilityTrigger.Key.DROP),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.IRON_TRAPDOOR);

	public SteelTrap(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mMaxCharges = MAX_CHARGES + (int) CharmManager.getLevel(player, CHARM_CHARGES);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE,
			isEnhanced() ? DAMAGE_R4 : AbilityUtils.regionalScale(player, DAMAGE_R1, DAMAGE_R2, DAMAGE_R3));
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelOne() ? RADIUS_L1 : RADIUS_L2);
		mTrapDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mPrimingDuration = CharmManager.getDuration(mPlayer, CHARM_PRIMING_DURATION, isEnhanced() ? PRIMING_DURATION_L3 : PRIMING_DURATION_L1);
		mStaggerDuration = CharmManager.getDuration(mPlayer, CHARM_STAGGER_DURATION, STAGGER_DURATION);
		mVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, VELOCITY);
		mTriggerRadius = CharmManager.getRadius(mPlayer, CHARM_TRIGGER_RADIUS, TRIGGER_RADIUS);
		mKnockbackHorizontal = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK_HORIZONTAL);
		mKnockbackVertical = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK_VERTICAL);
		mVulnerability = VULN + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULN);
		mVulnerabilityDuration = CharmManager.getDuration(mPlayer, CHARM_VULN_DURATION, VULN_DURATION);

		mCharges = Math.min(AbilityManager.getManager().getTrackedCharges(mPlayer, ClassAbility.STEEL_TRAP), mMaxCharges);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SteelTrapCS());
	}

	public class Trap {
		private final World mWorld;
		private final boolean mIsUnderwater;
		private final Map<String, BlockDisplay> mTrapDisplay;
		private final Item mPhysicsItem;

		private int mTicks = 0;
		private int mLandingTime = NOT_LANDED;
		private boolean mPrimed = false;
		private boolean mEnhanceTrigger = false;
		private Location mCenter;

		public Trap(Map<String, BlockDisplay> trapDisplay, Item item, boolean underwater) {
			mWorld = mPlayer.getWorld();
			mPhysicsItem = item;
			mCenter = item.getLocation();
			mIsUnderwater = underwater;
			mTrapDisplay = trapDisplay;

			createRunnable();
		}

		private void setEnhanceTrigger() {
			mEnhanceTrigger = true;
		}

		private double getDistance() {
			return mCenter.distance(mPlayer.getLocation());
		}


		private void addToMap() {
			if (isEnhanced()) {
				mEnhancedTrap.add(this);
			}
		}

		private void removeFromMap() {
			if (isEnhanced()) {
				mEnhancedTrap.remove(this);
			}
		}

		public void createRunnable() {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!mPlayer.isOnline()
						|| !mPlayer.isValid()
						|| mPlayer.isDead()) {
						this.cancel();
						return;
					}

					int currTick = Bukkit.getServer().getCurrentTick();

					// Skip landing & priming
					if (mIsUnderwater) {
						if (!mPrimed) {
							mLandingTime = currTick;
							mPrimed = true;
							mCenter = mPhysicsItem.getLocation();
							addToMap();
						}

						if (mPhysicsItem.isValid()) {
							mCenter = mPhysicsItem.getLocation();
							mPhysicsItem.setVelocity(mPhysicsItem.getVelocity().multiply(0.98));
						}

						if (!LocationUtils.isLocationInWater(mCenter)) {
							mPhysicsItem.setVelocity(mPhysicsItem.getVelocity().setY(0));
						}

					}

					if (mLandingTime == NOT_LANDED) {
						mCosmetic.trapMidairTick(mWorld, mPlayer, mPhysicsItem.getLocation());

						if (mPhysicsItem.isOnGround()) {
							mLandingTime = currTick;
							mTicks = 0; // Only used for expiration, should be safe to reset
							mCenter = mPhysicsItem.getLocation().add(0, 0.2, 0);
							mCosmetic.trapLand(mWorld, mPlayer, mCenter, mTrapDisplay, mRadius);
							mPhysicsItem.remove();
						}
					}

					if (!mPrimed && mLandingTime != NOT_LANDED) {
						if (currTick - mLandingTime >= mPrimingDuration) {
							mCosmetic.trapPrimed(mWorld, mPlayer, mCenter, mRadius);
							mPrimed = true;
							addToMap();
						} else {
							mCosmetic.trapPrimingTick(mWorld, mPlayer, mCenter, currTick - mLandingTime, mPrimingDuration, mRadius);
						}
					}

					if (mPrimed) {
						mCosmetic.trapPrimeTick(mWorld, mPlayer, mCenter, mTriggerRadius, mTicks, isEnhanced());

						boolean canDetonate = isEnhanced() ? mEnhanceTrigger
							: !EntityUtils.getNearbyMobs(mCenter, mTriggerRadius).isEmpty();

						if (canDetonate) {
							for (LivingEntity entity : EntityUtils.getNearbyMobs(mCenter, mRadius)) {
								DamageUtils.damage(mPlayer, entity, DamageEvent.DamageType.PROJECTILE_SKILL, mDamage, mInfo.getLinkedSpell(), true);
								EntityUtils.applyStagger(mPlugin, mStaggerDuration, entity);
								MovementUtils.knockAway(mCenter, entity, mKnockbackHorizontal, mKnockbackVertical, true);

								if (isLevelTwo()) {
									EntityUtils.applyVulnerability(mPlugin, mVulnerabilityDuration, mVulnerability, entity);
								}

								HuntingCompanion.staggerApplied(mPlayer, entity);
							}

							if (isEnhanced()
								&& mCenter.getNearbyPlayers(mRadius).contains(mPlayer)
								&& !ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
								MovementUtils.knockAway(mCenter, mPlayer, mKnockbackHorizontal * 2, mKnockbackVertical * 3, false);
							}
							mCosmetic.trapExplode(mWorld, mPlayer, mCenter, mRadius);
							this.cancel();
							return;
						}
					}

					// Expire if the trap...
					// 1: Is past its duration
					// 2: Hasn't landed after 5s
					boolean hasNotLanded = mLandingTime != NOT_LANDED && mTicks > mTrapDuration;
					boolean hasLanded = mLandingTime == NOT_LANDED && mTicks > 100;
					if (hasLanded || hasNotLanded) {
						if (hasLanded) {
							mCosmetic.trapDespawn(mWorld, mPlayer, mCenter);
						}
						this.cancel();
						return;
					}

					mTicks++;
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					removeFromMap();
					mTrapDisplay.values().forEach(Entity::remove);
					mTrapDisplay.clear();
					if (mPhysicsItem.isValid()) {
						mPhysicsItem.remove();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	public boolean cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5) {
			return false;
		}
		mLastCastTicks = ticks;

		if (isEnhanced() && mEnhancedTrap.size() >= mMaxCharges) {
			List<Trap> list = mEnhancedTrap.stream()
				.sorted(Comparator.comparingDouble(Trap::getDistance))
				.toList();

			int count = 0;

			for (Trap trap : list) {
				if (++count > mMaxCharges) {
					break;
				}
				trap.setEnhanceTrigger();
			}

			return true;
		} else if (consumeCharge()) {
			throwTrap();
			return true;
		}

		return false;
	}

	public void throwTrap() {
		Location unrotatedLoc = mPlayer.getEyeLocation();
		unrotatedLoc.setPitch(0);

		Location loc = mPlayer.getEyeLocation();

		HashMap<String, BlockDisplay> trapDisplay;
		Item physicsItem = AbilityUtils.spawnAbilityItem(mPlayer.getWorld(), loc, mCosmetic.getThrownItem(), "TrapPhysicItem", false, mVelocity, false, true);

		boolean isUnderwater = mPlayer.isUnderWater();

		if (isUnderwater) {
			trapDisplay = new HashMap<>(mCosmetic.getUnderwaterBlockDisplayTrap(mPlayer.getWorld(), unrotatedLoc));
			physicsItem.setGravity(false);
		} else {
			trapDisplay = new HashMap<>(mCosmetic.getBlockDisplayTrap(mPlayer.getWorld(), unrotatedLoc));
		}

		for (BlockDisplay display : trapDisplay.values()) {
			display.setInterpolationDuration(2);
			physicsItem.addPassenger(display);
			EntityUtils.setRemoveEntityOnUnload(display);
		}

		World world = mPlayer.getWorld();
		mCosmetic.trapThrow(world, mPlayer, loc);

		new Trap(trapDisplay, physicsItem, isUnderwater);
	}

	private boolean consumeCharge() {
		if (mCharges <= 0) {
			return false;
		}

		mCharges--;
		if (mMaxCharges > 1) {
			showChargesMessage();
		}

		AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.STEEL_TRAP, mCharges);
		ClientModHandler.updateAbility(mPlayer, this);

		if (!isOnCooldown()) {
			putOnCooldown();
		} else { // putOnCooldown calls abilityCastEvent, need to recall if cast while on cooldown
			PlayerUtils.callAbilityCastEvent(mPlayer, this, ClassAbility.STEEL_TRAP, 0);
		}

		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mWasOnCooldown && !isOnCooldown()) {
			mCharges = mMaxCharges;
			AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.STEEL_TRAP, mCharges);

			showOffCooldownMessage();
			ClientModHandler.updateAbility(mPlayer, this);
		}

		mWasOnCooldown = isOnCooldown();

		if (!isOnCooldown() && mCharges != mMaxCharges) {
			putOnCooldown();
		}
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}

	private static Description<SteelTrap> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Throw a *Trap* that primes after %t1e_only.").styles(LIGHT_GREY)
			.statValues(stat(a -> a.mPrimingDuration, PRIMING_DURATION_L1))
			.addLine("(Underwater nets prime instantly)")
			.addLine()
			.addLine("When a mob gets within %d blocks of the *Trap*,").styles(LIGHT_GREY)
			.statValues(stat(a -> a.mTriggerRadius, TRIGGER_RADIUS))
			.addLine("it damages and staggers nearby mobs.")
			.addLine()
			.addStat("Damage: %d1e_only (p)")
			.statValues(perRegion(a -> a.mDamage, DAMAGE_R1, DAMAGE_R2, DAMAGE_R3))
			.addStat("Effect: Stagger for %t")
			.statValues(stat(a -> a.mStaggerDuration, STAGGER_DURATION))
			.addStat("Radius: %r1")
			.statValues(stat(a -> a.mRadius, RADIUS_L1))
			.addStat("Duration: %t")
			.statValues(stat(a -> a.mTrapDuration, DURATION))
			.addStat("Charges: %d")
			.statValues(stat(a -> a.mMaxCharges, MAX_CHARGES))
			.addStat("Cooldown: %t (refreshes all charges at once)")
			.statValues(cooldown(TRAP_COOLDOWN))
			.addDashedLine();
	}

	private static Description<SteelTrap> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Steel Trap*'s radius").styles(UNDERLINED)
			.addLine("and now applies vulnerability.")
			.addLine()
			.addStatComparison("Radius: %r1 -> %r2")
			.statValues(stat(RADIUS_L1), stat(a -> a.mRadius, RADIUS_L2))
			.addStat("Effect : %p Vulnerability for %t")
			.statValues(stat(a -> a.mVulnerability, VULN), stat(a -> a.mVulnerabilityDuration, VULN_DURATION))
			.addDashedLine();
	}

	private static Description<SteelTrap> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Steel Trap* no longer explodes automatically.").styles(UNDERLINED)
			.addLine("Casting when %d *Traps* are primed detonates them.").styles(LIGHT_GREY)
			.statValues(stat(a -> a.mMaxCharges, MAX_CHARGES))
			.addLine()
			.addLine("Increase *Steel Trap*'s damage and").styles(UNDERLINED)
			.addLine("decrease priming duration.")
			.addLine()
			.addStatComparison("Damage: %d1e -> %d3")
			.statValues(perRegion(DAMAGE_R1, DAMAGE_R2, DAMAGE_R3), stat(a -> a.mDamage, DAMAGE_R4))
			.addStatComparison("Priming Duration: %t1e -> %t3")
			.statValues(stat(PRIMING_DURATION_L1), stat(a -> a.mPrimingDuration, PRIMING_DURATION_L3))
			.addDashedLine();
	}
}
