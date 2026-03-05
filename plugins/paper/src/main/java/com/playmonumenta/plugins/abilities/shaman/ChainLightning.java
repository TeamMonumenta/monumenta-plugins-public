package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
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

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

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
		mCharges = getChargesOffCooldown();

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
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Fire a bolt of lightning that damages")
			.addLine("and bounces to other nearby mobs.")
			.addLine()
			.addLine("The lightning can bounce to nearby")
			.addLine("*Totems* to extend its range.").styles(Shaman.TOTEM_COLOR)
			.addLine()
			.addStat("Damage: %d1e (s)")
				.statValues(stat((a, p) -> a.mFinalDamage, (a, p) -> DAMAGE_1 + BONUS_DAMAGE_1 * AbilityUtils.getEffectiveTotalSkillPoints(p)))
			.addStat("Max Mobs: %d1")
				.statValues(stat(a -> a.mTargets, TARGETS_1))
			.addStat("Bounce Range: %r")
				.statValues(stat(a -> a.mBounceRange, BOUNCE_RANGE))
			.addStat("Charges: %d")
				.statValues(stat(a -> a.mMaxCharges, CHARGES))
			.addStat("Cooldown: %t (per charge)")
				.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<ChainLightning> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Chain Lightning*'s damage").styles(UNDERLINED)
			.addLine("and maximum mobs hit.")
			.addLine()
			.addStatComparison("Damage: %d1e -> %d2e (s)")
				.statValues(stat((a, p) -> DAMAGE_1 + BONUS_DAMAGE_1 * AbilityUtils.getEffectiveTotalSkillPoints(p)),
					stat((a, p) -> a.mFinalDamage, (a, p) -> DAMAGE_2 + BONUS_DAMAGE_2 * AbilityUtils.getEffectiveTotalSkillPoints(p)))
			.addStatComparison("Max Mobs: %d1 -> %d2")
				.statValues(stat(TARGETS_1), stat(a -> a.mTargets, TARGETS_2))
			.addDashedLine();
	}

	private static Description<ChainLightning> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Increase *Chain Lightning*'s damage by an").styles(UNDERLINED)
			.addLine("additional +%d (s).")
				.statValues(stat(a -> a.mEnhanceBonusDmg, DAMAGE_3))
			.addLine()
			.addIfElse((a, p) -> a == null || a.isLevelOne(),
				desc -> desc.addStatComparison("Damage: %d1 -> %d3 (s)")
					.statValues(stat((a, p) -> DAMAGE_1 + BONUS_DAMAGE_1 * AbilityUtils.getEffectiveTotalSkillPoints(p)),
						stat((a, p) -> a.mFinalDamage, (a, p) -> DAMAGE_1 + BONUS_DAMAGE_1 * AbilityUtils.getEffectiveTotalSkillPoints(p) + DAMAGE_3)),
				desc -> desc.addStatComparison("Damage: %d2 -> %d3 (s)")
					.statValues(stat((a, p) -> DAMAGE_2 + BONUS_DAMAGE_2 * AbilityUtils.getEffectiveTotalSkillPoints(p)),
						stat((a, p) -> a.mFinalDamage, (a, p) -> DAMAGE_2 + BONUS_DAMAGE_2 * AbilityUtils.getEffectiveTotalSkillPoints(p) + DAMAGE_3)))
			.addLine()
			.addLine("When *Chain Lightning* bounces to a *Totem*,").styles(UNDERLINED, Shaman.TOTEM_COLOR)
			.addLine("instantly trigger that *Totem's* effect at").styles(Shaman.TOTEM_COLOR)
			.addLine("%p effectiveness for damaging *Totems*,").styles(Shaman.TOTEM_COLOR)
				.statValues(stat(a -> a.mNegativeEfficiency, ENHANCE_OFFENSIVE_EFFICIENCY))
			.addLine("and %p effectiveness for non-damaging")
				.statValues(stat(a -> a.mPositiveEfficiency, ENHANCE_SUPPORT_EFFICIENCY))
			.addLine("*Totems*.").styles(Shaman.TOTEM_COLOR)
			.addDashedLine();
	}
}
