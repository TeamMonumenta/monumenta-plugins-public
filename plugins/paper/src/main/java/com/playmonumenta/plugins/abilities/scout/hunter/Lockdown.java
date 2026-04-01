package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.LockdownCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.TwoHanded;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Lockdown extends Ability implements AbilityWithChargesOrStacks, AbilityWithDuration {
	private static final String LOADING_NAME = "LockdownLoadingEffect";

	private static final double DAMAGE_L1_R2 = 22;
	private static final double DAMAGE_L2_R2 = 26;
	private static final double DAMAGE_L1_R3 = 26;
	private static final double DAMAGE_L2_R3 = 30;
	private static final int SHOT_COUNT = 3;
	private static final float KNOCKBACK = 0.4f;
	private static final int KILL_BONUS = 1;
	private static final double MAX_DISTANCE = 36;
	private static final double RADIUS = 0.1;
	private static final int INITIAL_LOAD_TIME = Constants.TICKS_PER_SECOND; // 1s
	private static final int LOAD_TIME = 5; // 0.25s
	private static final int COOLDOWN = Constants.TICKS_PER_SECOND * 8;
	private static final int MIDAIR_DURATION = Constants.TICKS_PER_SECOND * 2;

	public static final String CHARM_SHOT_COUNT = "Lockdown Shots";
	public static final String CHARM_KNOCKBACK = "Lockdown Knockback";
	public static final String CHARM_DAMAGE = "Lockdown Damage";
	public static final String CHARM_MAX_DISTANCE = "Lockdown Max Distance";
	public static final String CHARM_CHARGE_TIME = "Lockdown Charge Time";
	public static final String CHARM_INITIAL_CHARGE_TIME = "Lockdown Initial Charge Time";
	public static final String CHARM_RADIUS = "Lockdown Radius";
	public static final String CHARM_PIERCE = "Lockdown Pierce";
	public static final String CHARM_MIDAIR_DURATION = "Lockdown Duration Per Shot";
	public static final String CHARM_KILL_BONUS = "Lockdown Shot Kill Extension";
	public static final String CHARM_COOLDOWN = "Lockdown Cooldown";

	public static final AbilityInfo<Lockdown> INFO =
		new AbilityInfo<>(Lockdown.class, "Lockdown", Lockdown::new)
			.linkedSpell(ClassAbility.LOCKDOWN)
			.scoreboardId("Lockdown")
			.shorthandName("Ld")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Enter a stance that suspends you, recast to shoot.")
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Lockdown::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.ENDER_EYE);

	private int mShiftingTime = 0;

	private final double mDamageFlat;
	private final float mKnockback;
	private final int mPierce;
	private final double mDistance;
	private final int mInitialChargeTime;
	private final int mChargeTime;
	private final double mRadius;
	private final int mKillExtension;
	private final LockdownCS mCosmetic;
	private final int mMaxCharges;
	private final int mDurationPerShot;
	private @Nullable Sharpshooter mSharpshooter;

	private int mCharges = 0;
	private boolean mReady = false;
	private int mDuration = 0;
	private @Nullable BukkitTask mRunnable = null;

	public Lockdown(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		boolean isR3 = ServerProperties.getAbilityEnhancementsEnabled(player);
		final double damageL1 = isR3 ? DAMAGE_L1_R3 : DAMAGE_L1_R2;
		final double damageL2 = isR3 ? DAMAGE_L2_R3 : DAMAGE_L2_R2;

		mDamageFlat = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? damageL1 : damageL2);
		mMaxCharges = SHOT_COUNT + (int) CharmManager.getLevel(mPlayer, CHARM_SHOT_COUNT);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		mPierce = (int) CharmManager.getLevel(mPlayer, CHARM_PIERCE);
		mDistance = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MAX_DISTANCE, MAX_DISTANCE);
		mChargeTime = CharmManager.getDuration(mPlayer, CHARM_CHARGE_TIME, LOAD_TIME);
		mInitialChargeTime = CharmManager.getDuration(mPlayer, CHARM_INITIAL_CHARGE_TIME, INITIAL_LOAD_TIME);
		mKillExtension = KILL_BONUS + (int) CharmManager.getLevel(mPlayer, CHARM_KILL_BONUS);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mDurationPerShot = CharmManager.getDuration(mPlayer, CHARM_MIDAIR_DURATION, MIDAIR_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new LockdownCS());

		Bukkit.getScheduler().runTask(plugin, () -> mSharpshooter = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Sharpshooter.class));
	}

	private void createLockdownRunnable() {
		mRunnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mDuration <= 0
					|| !mPlayer.isOnline()
					|| AbilityManager.getManager().getPlayerAbilities(mPlayer).isSilenced()
					|| AbilityManager.getManager().getPlayerAbility(mPlayer, Lockdown.class) == null) {
					disableLockdown();
					this.cancel();
					return;
				}

				applyEffects(mPlayer);
				mCosmetic.lockdownTick(mPlayer, mCharges, mMaxCharges, mT++);

				if (!mReady) {
					mCosmetic.lockdownCharging(mPlayer,
						(double) mShiftingTime / (mCharges != mMaxCharges ? mChargeTime : mInitialChargeTime));
				}

				mDuration--;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
		cancelOnDeath(mRunnable);
	}

	@Override
	public void invalidate() {
		if (mRunnable != null) {
			mRunnable.cancel();
		}
	}

	private void load() {
		if (mReady ||
			isOnCooldown() ||
			mPlugin.mEffectManager.hasEffect(mPlayer, LOADING_NAME)) {
			return;
		}

		mShiftingTime = 0;
		int chargeTime = mChargeTime;

		if (!isActive()) {
			chargeTime = mInitialChargeTime;
			mDuration = mInitialChargeTime + mDurationPerShot;
			mCharges = mMaxCharges;
			showChargesMessage();
			ClientModHandler.updateAbility(mPlayer, ClassAbility.LOCKDOWN);
			createLockdownRunnable();
		}

		mPlugin.mEffectManager.addEffect(mPlayer, LOADING_NAME, new Aesthetics(chargeTime,
			(entity, fourHertz, twoHertz, oneHertz) -> {
			},
			(entity) -> {
				if (isActive()) {
					mReady = true;
					mCosmetic.lockdownCharged(mPlayer);
				}
			}).deleteOnAbilityUpdate(true));
	}

	private boolean cast() {
		if (isOnCooldown() ||
			(mPlugin.mItemStatManager.getEnchantmentLevel(mPlayer, EnchantmentType.TWO_HANDED) > 0 && TwoHanded.checkForOffhand(mPlugin, mPlayer))) {
			return false;
		}

		if (!mReady) {
			load();
			return true;
		}

		mCharges--;
		showChargesMessage();
		mReady = false;

		shootLockdown();
		return true;
	}

	private void shootLockdown() {
		Location loc = mPlayer.getEyeLocation().subtract(0, 0.2, 0);
		Vector dir = NmsUtils.getVersionAdapter().getActualDirection(mPlayer);

		World world = loc.getWorld();
		mCosmetic.lockdownShoot(mPlugin, mPlayer, loc);

		List<LivingEntity> struckMobs = new ArrayList<>();

		RayTraceResult result = world.rayTrace(loc, dir, mDistance, FluidCollisionMode.NEVER, true, 0.45,
			this::lockdownTargetApplicable);

		int hits = 0;
		while (hits <= mPierce) {
			// if we hit a block then just stop
			if (result == null || result.getHitBlock() != null) {
				break;
			}

			Entity hitEntity = result.getHitEntity();
			if (hitEntity instanceof LivingEntity le) {
				struckMobs.add(le);
				hits++;
			}

			result = world.rayTrace(loc, dir, mDistance, FluidCollisionMode.NEVER, true, mRadius,
				e -> lockdownTargetApplicable(e) && !struckMobs.contains((LivingEntity) e));
		}

		Location endLoc = result == null ?
			loc.clone().add(dir.multiply(mDistance)) :
			result.getHitPosition().toLocation(world);

		hitStruckMobs(struckMobs);

		if (struckMobs.isEmpty()) {
			mCosmetic.lockdownMiss(mPlayer);
			mDuration = 0;
		} else {
			mCosmetic.lockdownSuccess(mPlayer);
			if (mCharges > 0) {
				mDuration = mDurationPerShot + mChargeTime;
				load();
			} else {
				disableLockdown();
			}
		}

		Bukkit.getScheduler().runTask(mPlugin, () -> mCosmetic.lockdownParticleLine(mPlayer, loc, endLoc, 0.05, mDistance));
		ClientModHandler.updateAbility(mPlayer, this);
	}

	private boolean lockdownTargetApplicable(Entity e) {
		return (WindBomb.isWindBomb(e)
			|| (EntityUtils.isHostileMob(e)
			&& !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG)))
			&& !e.isDead()
			&& e.isValid();
	}

	private void hitStruckMobs(List<LivingEntity> struckMobs) {
		if (mSharpshooter != null) {
			if (struckMobs.isEmpty()) {
				mSharpshooter.miss();
			} else {
				mSharpshooter.addStacks(2);
			}
		}

		for (LivingEntity e : struckMobs) {
			mCosmetic.lockdownHit(mPlayer, e);

			if (WindBomb.attemptHit(e)) {
				continue;
			}

			MovementUtils.knockAway(mPlayer.getLocation(), e, mKnockback, true);
			DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.PROJECTILE_SKILL,
				mDamageFlat, ClassAbility.LOCKDOWN, true);

			if (isLevelTwo() && e.isDead()) {
				mCharges = Math.min(mMaxCharges, mCharges + mKillExtension);
				showChargesMessage();
				ClientModHandler.updateAbility(mPlayer, ClassAbility.LOCKDOWN);
			}
		}
	}

	private static void applyEffects(Player player) {
		Plugin.getInstance().mEffectManager.addEffect(player, "LockdownKnockbackRes",
			new PercentKnockbackResist(5, 1, "LockdownKnockbackRes"));

		if (ZoneUtils.hasZoneProperty(player.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return;
		}

		if (PlayerUtils.isOnGround(player)) {
			Plugin.getInstance().mEffectManager.addEffect(player, "LockdownSlowness",
				new PercentSpeed(5, -0.5, "LockdownSlowness"));
		} else {
			Plugin.getInstance().mPotionManager.addPotion(player, PotionManager.PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.LEVITATION, 5, -1, false, false));
		}
	}

	@Override
	public void playerItemHeldEvent(PlayerItemHeldEvent e) {
		if (isActive()) {
			disableLockdown();
		}
	}

	private boolean isActive() {
		return mRunnable != null;
	}

	private void disableLockdown() {
		putOnCooldown();
		mCharges = 0;
		mDuration = 0;
		mReady = false;
		if (mRunnable != null) {
			mRunnable.cancel();
			mRunnable = null;
		}
		mPlugin.mEffectManager.clearEffects(mPlayer, LOADING_NAME);
		showChargesMessage();
		ClientModHandler.updateAbility(mPlayer, ClassAbility.LOCKDOWN);
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDurationPerShot;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return mDuration;
	}

	private static Description<Lockdown> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Enter *Lockdown* after %t, suspending you").styles(UNDERLINED)
			.statValues(stat(a -> a.mInitialChargeTime, INITIAL_LOAD_TIME))
			.addLine("in the air and loading %d shots.")
			.statValues(stat(a -> a.mMaxCharges, SHOT_COUNT))
			.addLine()
			.addLine("Recast *Lockdown* to fire a shot. Missing").styles(UNDERLINED)
			.addLine("a shot will immediately end *Lockdown*.").styles(UNDERLINED)
			.addLine("(Swap weapons to cancel Lockdown)")
			.addLine()
			.addStat("Damage: %d1 (p)")
			.statValues(perRegion(a -> a.mDamageFlat, DAMAGE_L1_R2, DAMAGE_L1_R3))
			.addIf((a, p) -> a != null && a.mPierce > 0, desc -> desc
				.addStat("Pierce: %d")
				.statValues(stat(a -> a.mPierce, 0)))
			.addStat("Max Distance: %r")
			.statValues(stat(a -> a.mDistance, MAX_DISTANCE))
			.addStat("Cooldown: %t")
			.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<Lockdown> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Lockdown*'s damage.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (p)")
			.statValues(perRegion(DAMAGE_L1_R2, DAMAGE_L1_R3), perRegion(a -> a.mDamageFlat, DAMAGE_L2_R2, DAMAGE_L2_R3))
			.addLine()
			.addLine("Extend *Lockdown* by %d shot upon kill.").styles(UNDERLINED)
			.statValues(stat(a -> a.mKillExtension, KILL_BONUS))
			.addDashedLine();
	}
}
