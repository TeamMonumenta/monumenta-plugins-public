package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.ChainLightningCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ChainLightning extends MultipleChargeAbility {
	private static final int COOLDOWN = 6 * 20;
	private static final int CHARGES = 2;
	private static final int TARGETS_1 = 3;
	private static final int TARGETS_2 = 5;
	private static final int INITIAL_RANGE = 9;
	private static final int BOUNCE_RANGE = 6;
	private static final double DAMAGE_1 = 2.5;
	private static final double BONUS_DAMAGE_1 = 0.25;
	private static final double DAMAGE_2 = 4;
	private static final double BONUS_DAMAGE_2 = 0.4;
	private static final int DAMAGE_3 = 3;
	private static final float KNOCKBACK = 0.2f;
	public static final double ENHANCE_SUPPORT_EFFICIENCY = 0.3;
	public static final double ENHANCE_OFFENSIVE_EFFICIENCY = 0.8;

	public static final String CHARM_COOLDOWN = "Chain Lightning Cooldown";
	public static final String CHARM_DAMAGE = "Chain Lightning Damage";
	public static final String CHARM_ENHANCEMENT_DAMAGE = "Chain Lightning Enhancement Bonus Damage";
	public static final String CHARM_RADIUS = "Chain Lightning Bounce Radius";
	public static final String CHARM_TARGETS = "Chain Lightning Targets";
	public static final String CHARM_CHARGES = "Chain Lightning Charges";
	public static final String CHARM_KNOCKBACK = "Chain Lightning Knockback";
	public static final String CHARM_INITIAL_RANGE = "Chain Lightning Initial Range";
	public static final String CHARM_SUPPORT_TOTEM_EFFICIENCY = "Chain Lightning Non-Damaging Totem Efficiency";
	public static final String CHARM_OFFENSIVE_TOTEM_EFFICIENCY = "Chain Lightning Damaging Totem Efficiency";

	public static final AbilityInfo<ChainLightning> INFO =
		new AbilityInfo<>(ChainLightning.class, "Chain Lightning", ChainLightning::new)
			.linkedSpell(ClassAbility.CHAIN_LIGHTNING)
			.scoreboardId("ChainLightning")
			.shorthandName("CL")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Flings lightning at mobs in a medium radius in front of you, bouncing between mobs and totems.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast1", "cast", ChainLightning::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false).keyOptions(AbilityTrigger.KeyOptions.NO_PROJECTILE_WEAPON)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.addTrigger(new AbilityTriggerInfo<>("cast2", "alternative cast", ChainLightning::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false).keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.BLAZE_ROD);

	private final double mBounceRange;
	private final int mTargets;
	private final double mBaseDamage;
	private final double mEnhanceBonusDmg;
	private final double mFinalDamage;
	private final double mInitialRange;
	private int mLastCastTicks = 0;
	private final double mPositiveEfficiency;
	private final double mNegativeEfficiency;
	private final float mKnockback;
	private final ChainLightningCS mCosmetic;

	private final List<LivingEntity> mHitTargets = new ArrayList<>();

	public ChainLightning(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mEnhanceBonusDmg = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_DAMAGE, DAMAGE_3);
		mBaseDamage = (isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		double mSkillPointDamage = AbilityUtils.getEffectiveTotalSkillPoints(player) * (isLevelOne() ? BONUS_DAMAGE_1 : BONUS_DAMAGE_2);
		mFinalDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mBaseDamage + mSkillPointDamage + (isEnhanced() ? mEnhanceBonusDmg : 0));

		mMaxCharges = CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getTrackedCharges();

		mBounceRange = CharmManager.getRadius(mPlayer, CHARM_RADIUS, BOUNCE_RANGE);
		mTargets = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_TARGETS, isLevelOne() ? TARGETS_1 : TARGETS_2);
		mInitialRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_INITIAL_RANGE, INITIAL_RANGE);
		mPositiveEfficiency = ENHANCE_SUPPORT_EFFICIENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SUPPORT_TOTEM_EFFICIENCY);
		mNegativeEfficiency = ENHANCE_OFFENSIVE_EFFICIENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_OFFENSIVE_TOTEM_EFFICIENCY);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ChainLightningCS());
	}

	public boolean cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		if (ticks - mLastCastTicks <= 10 || mCharges <= 0) {
			return false;
		}
		mLastCastTicks = ticks;
		mHitTargets.clear();
		mHitTargets.add(mPlayer);

		Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), mInitialRange, Math.toRadians(16))
			.union(Hitbox.approximateCone(mPlayer.getEyeLocation(), 3, Math.toRadians(24)))
			.union(new Hitbox.SphereHitbox(mPlayer.getLocation(), 1.25));

		List<LivingEntity> nearbyMobs = hitbox.getHitMobs();
		nearbyMobs.removeIf(mob -> !(mPlayer.hasLineOfSight(mob.getEyeLocation()) || mPlayer.hasLineOfSight(mob.getLocation())));
		nearbyMobs.sort(Comparator.comparing(e -> e.getLocation().distance(mPlayer.getLocation())));
		List<LivingEntity> nearbyTotems = new ArrayList<>(ShamanPassiveManager.getTotemList(mPlayer));
		nearbyTotems.removeIf(totem -> !hitbox.intersects(totem.getBoundingBox()));
		if (!nearbyMobs.isEmpty()) {
			mHitTargets.add(nearbyMobs.getFirst());
			if (!nearbyTotems.isEmpty()) {
				LivingEntity totem = FastUtils.getRandomElement(nearbyTotems);
				mHitTargets.add(totem);
			}
			startChain(nearbyMobs.getFirst(), true);
		} else {
			if (!nearbyTotems.isEmpty()) {
				LivingEntity totem = FastUtils.getRandomElement(nearbyTotems);
				mHitTargets.add(totem);
				startChain(totem, false);
			}
		}

		return true;
	}

	private void startChain(LivingEntity starterEntity, boolean foundMob) {
		Location lastTarget = starterEntity.getLocation();
		LivingEntity nextTarget;
		LivingEntity possibleTotem;
		boolean atLeastOneMob = foundMob;
		int safetyCounter = 0;
		Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), mInitialRange * 3, Math.toRadians(50))
			.union(Hitbox.approximateCone(mPlayer.getEyeLocation(), 3, Math.toRadians(60)))
			.union(new Hitbox.SphereHitbox(mPlayer.getLocation(), 1));
		List<LivingEntity> nearbyMobs = hitbox.getHitMobs();
		while (currentBounces(mHitTargets) <= mTargets && safetyCounter <= 40) {
			safetyCounter++;
			nextTarget = locateMobInRange(lastTarget, nearbyMobs);
			possibleTotem = locateTotemInRange(lastTarget, nearbyMobs);
			if (nextTarget != null) {
				mHitTargets.add(nextTarget);
				atLeastOneMob = true;
				if (possibleTotem != null) {
					mHitTargets.add(possibleTotem);
					lastTarget = possibleTotem.getLocation();
				} else {
					lastTarget = nextTarget.getLocation();
				}
			} else {
				if (possibleTotem != null) {
					mHitTargets.add(possibleTotem);
					lastTarget = possibleTotem.getLocation();
				} else {
					break;
				}
			}
		}
		if (!atLeastOneMob) {
			return;
		}
		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (!consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;
		mCosmetic.chainLightningSound(mPlayer);
		for (int i = 0; i < mHitTargets.size() - 1; i++) {
			LivingEntity target = mHitTargets.get(i + 1);
			if (target != null) {
				DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.MAGIC, mFinalDamage, ClassAbility.CHAIN_LIGHTNING, true, false);

				if (!(target instanceof ArmorStand)) {
					MovementUtils.knockAway(mPlayer.getLocation(), target, mKnockback, 0.6f * mKnockback, true);
				}

				mCosmetic.chainLightningCast(mPlayer, mHitTargets, target, i);

				if (isEnhanced() && target instanceof ArmorStand) {
					for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
						if (abil instanceof TotemAbility totemAbility
							&& totemAbility.mDisplayName.equals(target.getName())) {
							totemAbility.pulse(target.getLocation(),
								mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer), true);
						}
					}
				}
			}
		}
		mHitTargets.clear();
	}

	private @Nullable LivingEntity locateMobInRange(Location loc, List<LivingEntity> nearbyMobs) {
		List<LivingEntity> possibleMobs = new ArrayList<>(nearbyMobs);
		possibleMobs.removeIf(mob -> !(mPlayer.hasLineOfSight(mob.getEyeLocation()) || mPlayer.hasLineOfSight(mob.getLocation())));
		possibleMobs.sort(Comparator.comparing(e -> e.getLocation().distance(loc)));
		for (LivingEntity entity : mHitTargets) {
			possibleMobs.removeIf(mob -> entity.getUniqueId().equals(mob.getUniqueId()));
		}
		if (!possibleMobs.isEmpty()) {
			Collections.shuffle(possibleMobs);
			return possibleMobs.getFirst();
		}
		return null;
	}

	private @Nullable LivingEntity locateTotemInRange(Location loc, List<LivingEntity> nearbyMobs) {
		List<LivingEntity> totemList = new ArrayList<>(ShamanPassiveManager.getTotemList(mPlayer));
		totemList.removeIf(totem -> !nearbyMobs.contains(totem));
		totemList.sort(Comparator.comparing(e -> e.getLocation().distance(loc)));
		for (LivingEntity entity : mHitTargets) {
			totemList.removeIf(totem -> totem.getUniqueId().equals(entity.getUniqueId()));
		}
		if (!totemList.isEmpty()) {
			return totemList.getFirst();
		}
		return null;
	}

	private int currentBounces(List<LivingEntity> targetList) {
		int totalBounces = targetList.size();
		int totalTotems = 0;
		for (LivingEntity target : targetList) {
			if (target instanceof ArmorStand) {
				totalTotems++;
			}
		}
		return totalBounces - totalTotems;
	}

	private static Description<ChainLightning> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger(0)
			.add(" or ")
			.addTrigger(1)
			.add(" to cast a beam of lightning, bouncing between up to ")
			.add(a -> a.mTargets, TARGETS_1, false, Ability::isLevelOne)
			.add(" mobs within line of sight of each other and within ")
			.add(a -> a.mBounceRange, BOUNCE_RANGE)
			.add(" blocks of the last target hit, dealing ")
			.add(a -> a.mBaseDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" + (")
			.add(BONUS_DAMAGE_1)
			.add(" * Skill Points) magic damage to each. The beam can also bounce to nearby totems without consuming a hit target. Charges: ")
			.add(a -> a.mMaxCharges, CHARGES)
			.add(".")
			.addCooldown(COOLDOWN);
	}

	private static Description<ChainLightning> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mBaseDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" + (")
			.add(BONUS_DAMAGE_2)
			.add(" * Skill Points). Up to ")
			.add(a -> a.mTargets, TARGETS_2, false, Ability::isLevelTwo)
			.add(" mobs can now be targeted.");
	}

	private static Description<ChainLightning> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Each totem the beam bounces off of now instantly pulses its effects at ")
			.addPercent(a -> a.mNegativeEfficiency, ENHANCE_OFFENSIVE_EFFICIENCY)
			.add(" efficiency for damaging totems and ")
			.addPercent(a -> a.mPositiveEfficiency, ENHANCE_SUPPORT_EFFICIENCY)
			.add(" efficiency for non-damaging totems. These extra activations do not interact with the stacking buffs from consecutive pulses on Flame Totem. The base magic damage of Chain Lightning is also increased by ")
			.add(a -> a.mEnhanceBonusDmg, DAMAGE_3)
			.add(".");
	}
}
