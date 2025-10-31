package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.SmokescreenCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.scheduler.BukkitRunnable;

public class Smokescreen extends Ability implements AbilityWithDuration {

	private static final int SMOKESCREEN_RANGE = 6;
	private static final int SMOKESCREEN_RANGE_2 = 8;
	private static final int SMOKESCREEN_EFFECT_DURATION = 8 * 20;
	private static final double SMOKESCREEN_SLOWNESS_AMPLIFIER = 0.2;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.4;
	private static final int SMOKESCREEN_COOLDOWN = 20 * 20;
	private static final int ENHANCEMENT_SMOKECLOUD_DURATION = 8 * 20;
	private static final double VELOCITY = 0.85;
	private static final double ENHANCEMENT_DAMAGE = 12;

	public static final String CHARM_SLOW = "Smokescreen Slowness Amplifier";
	public static final String CHARM_WEAKEN = "Smokescreen Weakness Amplifier";
	public static final String CHARM_COOLDOWN = "Smokescreen Cooldown";
	public static final String CHARM_RANGE = "Smokescreen Range";
	public static final String CHARM_ENHANCEMENT_DURATION = "Smokescreen Pool Duration";
	public static final String CHARM_EFFECT_DURATION = "Smokescreen Effect Duration";
	public static final String CHARM_DAMAGE = "Smokescreen Enhancement Damage";

	public static final AbilityInfo<Smokescreen> INFO =
		new AbilityInfo<>(Smokescreen.class, "Smokescreen", Smokescreen::new)
			.linkedSpell(ClassAbility.SMOKESCREEN)
			.scoreboardId("SmokeScreen")
			.shorthandName("Smk")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Weaken and slow nearby mobs.")
			.cooldown(SMOKESCREEN_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Smokescreen::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.DEAD_TUBE_CORAL);

	private final double mWeakenEffect;
	private final double mSlownessEffect;
	private final int mPoolDuration;
	private final int mEffectDuration;
	private final double mRadius;
	private final double mDamage;

	private final SmokescreenCS mCosmetic;

	private int mCurrDuration = -1;

	public Smokescreen(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKEN) + (isLevelOne() ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2);
		mSlownessEffect = SMOKESCREEN_SLOWNESS_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOW);
		mPoolDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCEMENT_DURATION, ENHANCEMENT_SMOKECLOUD_DURATION);
		mEffectDuration = CharmManager.getDuration(mPlayer, CHARM_EFFECT_DURATION, SMOKESCREEN_EFFECT_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RANGE, isLevelOne() ? SMOKESCREEN_RANGE : SMOKESCREEN_RANGE_2);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, ENHANCEMENT_DAMAGE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SmokescreenCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		World world = mPlayer.getWorld();

		ThrowableProjectile proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, VELOCITY, mCosmetic.getProjectileName(), null, LocationUtils.isLocationInWater(mPlayer.getLocation()));
		int cd = getModifiedCooldown();
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT > cd) {
					proj.remove();
					this.cancel();
				}

				if (proj.isDead()) {
					Location loc = proj.getLocation();
					applyEffects(loc);
					mCosmetic.smokescreenEffects(mPlayer, world, loc, mRadius);

					if (isEnhanced()) {
						residualDebuffs(loc);

						List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRadius);
						mobs.forEach(mob -> DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true));
					}

					this.cancel();
				}
				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		putOnCooldown();

		ClientModHandler.updateAbility(mPlayer, this);
		return true;
	}

	private void residualDebuffs(Location loc) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT > mPoolDuration) {
					this.cancel();
				} else {
					if (mT > 0) {
						mCosmetic.residualEnhanceEffects(mPlayer, world, loc, mRadius);
						applyEffects(loc);
					}
					mT += 20;
					mCurrDuration += 20;
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, Smokescreen.this);
			}
		}.runTaskTimer(mPlugin, 0, 20);
	}

	private void applyEffects(Location loc) {
		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRadius)) {
			EntityUtils.applySlow(mPlugin, mEffectDuration, mSlownessEffect, mob);
			EntityUtils.applyWeaken(mPlugin, mEffectDuration, mWeakenEffect, mob);
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return isEnhanced() ? mPoolDuration : 0;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 && isEnhanced() ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	private static Description<Smokescreen> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to launch a projectile which releases a cloud of smoke, afflicting all mobs within ")
			.add(a -> a.mRadius, SMOKESCREEN_RANGE, false, Ability::isLevelOne)
			.add(" blocks with ")
			.addDuration(SMOKESCREEN_EFFECT_DURATION)
			.add(" seconds of ")
			.addPercent(a -> a.mWeakenEffect, WEAKEN_EFFECT_1, false, Ability::isLevelOne)
			.add(" weaken and ")
			.addPercent(a -> a.mSlownessEffect, SMOKESCREEN_SLOWNESS_AMPLIFIER)
			.add(" slowness.")
			.addCooldown(SMOKESCREEN_COOLDOWN);
	}

	private static Description<Smokescreen> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The weaken debuff is increased to ")
			.addPercent(a -> a.mWeakenEffect, WEAKEN_EFFECT_2, false, Ability::isLevelTwo)
			.add(" and the radius is increased to ")
			.add(a -> a.mRadius, SMOKESCREEN_RANGE_2, false, Ability::isLevelTwo)
			.add(" blocks.");
	}

	private static Description<Smokescreen> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The smoke cloud will now deal ")
			.add(a -> a.mDamage, ENHANCEMENT_DAMAGE)
			.add(" melee damage on impact and additionally leave a persistent cloud on the ground for ")
			.addDuration(a -> a.mPoolDuration, ENHANCEMENT_SMOKECLOUD_DURATION)
			.add(" seconds after activating. Mobs in the cloud gain the debuffs for ")
			.addDuration(SMOKESCREEN_EFFECT_DURATION)
			.add(" seconds, pulsing every second.");
	}
}
