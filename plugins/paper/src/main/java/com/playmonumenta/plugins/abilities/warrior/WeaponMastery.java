package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.WeaponMasteryCS;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.SweepingEdge;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class WeaponMastery extends Ability {
	private static final double AXE_1_DAMAGE_FLAT = 2;
	private static final double AXE_2_DAMAGE_FLAT = 4;
	private static final double SWORD_2_DAMAGE_FLAT = 1;
	private static final double AXE_1_DAMAGE = 0.05;
	private static final double AXE_2_DAMAGE = 0.1;
	private static final double SWORD_2_DAMAGE = 0.1;
	private static final double WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE = 0.1;
	private static final double SWORD_WEAKEN = 0.1;
	private static final double ENHANCED_DAMAGE = 0.1;
	private static final int SWORD_WEAKEN_DURATION = 4 * 20;
	private static final double AXE_SPEED = 0.15;
	private static final String SPEED_EFFECT = "WeaponMasterySpeedEffect";

	public static final String CHARM_REDUCTION = "Weapon Mastery Damage Reduction";
	public static final String CHARM_WEAKEN = "Weapon Mastery Weaken";
	public static final String CHARM_DURATION = "Weapon Mastery Duration";
	public static final String CHARM_SPEED = "Weapon Mastery Speed";

	public static final AbilityInfo<WeaponMastery> INFO =
		new AbilityInfo<>(WeaponMastery.class, "Weapon Mastery", WeaponMastery::new)
			.scoreboardId("WeaponMastery")
			.shorthandName("WM")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Increase total damage dealt with axes and swords and gain resistance while holding a sword.")
			.displayItem(Material.STONE_SWORD);

	private final double mDamageBonusAxeFlat;
	private final double mDamageBonusSwordFlat;
	private final double mDamageBonusAxe;
	private final double mDamageBonusSword;
	private final double mDamageReduction;
	private final int mWeakenDuration;
	private final double mWeaken;
	private final double mAttackSpeed;
	private final WeaponMasteryCS mCosmetic;

	public WeaponMastery(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageBonusAxeFlat = isLevelOne() ? AXE_1_DAMAGE_FLAT : AXE_2_DAMAGE_FLAT;
		mDamageBonusSwordFlat = isLevelOne() ? 0 : SWORD_2_DAMAGE_FLAT;
		double enhancementDamage = (isEnhanced() ? ENHANCED_DAMAGE : 0);
		mDamageBonusAxe = (isLevelOne() ? AXE_1_DAMAGE : AXE_2_DAMAGE) + enhancementDamage;
		mDamageBonusSword = (isLevelOne() ? 0 : SWORD_2_DAMAGE) + enhancementDamage;
		mDamageReduction = WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REDUCTION);
		mWeakenDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, SWORD_WEAKEN_DURATION);
		mWeaken = SWORD_WEAKEN + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
		mAttackSpeed = AXE_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new WeaponMasteryCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_ENCH) {
			double flatDamageRatio = 1;
			if (event.getType() == DamageType.MELEE_ENCH) {
				if (event.getAbility() == ClassAbility.ARCANE_THRUST) {
					double arcaneThrust = mPlugin.mItemStatManager.getEnchantmentLevel(mPlayer, EnchantmentType.ARCANE_THRUST);
					flatDamageRatio = arcaneThrust / (arcaneThrust + 1);
				} else if (event.getAbility() == ClassAbility.SWEEPING_EDGE) {
					double sweepingEdge = mPlugin.mItemStatManager.getEnchantmentLevel(mPlayer, EnchantmentType.SWEEPING_EDGE);
					flatDamageRatio = sweepingEdge * SweepingEdge.TRANSFER_COEFFICIENT;
				}
			}
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (ItemUtils.isAxe(mainHand)) {
				event.addUnmodifiableDamage(flatDamageRatio * mDamageBonusAxeFlat);
				event.updateDamageWithMultiplier(1 + mDamageBonusAxe);
				mCosmetic.weaponMasteryAxeHit(mPlayer);
			} else if (ItemUtils.isSword(mainHand)) {
				event.addUnmodifiableDamage(flatDamageRatio * mDamageBonusSwordFlat);
				event.updateDamageWithMultiplier(1 + mDamageBonusSword);
				mCosmetic.weaponMasterySwordHit(mPlayer);
				if (isEnhanced()) {
					EntityUtils.applyWeaken(mPlugin, mWeakenDuration, mWeaken, enemy);
					mCosmetic.weaponMasteryBleedApply(mPlayer);
				}
			}
		}
		return false; // only changes event damage
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (isEnhanced() && ItemUtils.isAxe(mPlayer.getInventory().getItemInMainHand())) {
			mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT, new PercentSpeed(6, mAttackSpeed, SPEED_EFFECT).displaysTime(false).deleteOnAbilityUpdate(true));
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())) {
			event.setFlatDamage(event.getDamage() * (1 - mDamageReduction));
		}
	}

	private static Description<WeaponMastery> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("You gain ")
			.addPercent(a -> a.mDamageReduction, WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE)
			.add(" resistance while holding a sword. Additionally, your axe damage is increased by an unscalable ")
			.add(a -> a.mDamageBonusAxeFlat, AXE_1_DAMAGE_FLAT, false, Ability::isLevelOne)
			.add(" plus ")
			.addPercent(a -> a.mDamageBonusAxe - (a.isEnhanced() ? ENHANCED_DAMAGE : 0), AXE_1_DAMAGE, false, Ability::isLevelOne)
			.add(" of final damage dealt.");
	}

	private static Description<WeaponMastery> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your sword swing's damage is increased by an unscalable ")
			.add(a -> a.mDamageBonusSwordFlat, SWORD_2_DAMAGE_FLAT, false, Ability::isLevelTwo)
			.add(" plus ")
			.addPercent(a -> a.mDamageBonusSword - (a.isEnhanced() ? ENHANCED_DAMAGE : 0), SWORD_2_DAMAGE, false, Ability::isLevelTwo)
			.add(" of final damage dealt. The axe damage buffs are increased to an unscalable ")
			.add(a -> a.mDamageBonusAxeFlat, AXE_2_DAMAGE_FLAT, false, Ability::isLevelTwo)
			.add(" plus ")
			.addPercent(a -> a.mDamageBonusAxe - (a.isEnhanced() ? ENHANCED_DAMAGE : 0), AXE_2_DAMAGE, false, Ability::isLevelTwo)
			.add(".");
	}

	private static Description<WeaponMastery> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Gain ")
			.addPercent(ENHANCED_DAMAGE)
			.add(" final damage when using either a sword or an axe. Apply ")
			.addPercent(a -> a.mWeaken, SWORD_WEAKEN)
			.add(" weaken for ")
			.addDuration(a -> a.mWeakenDuration, SWORD_WEAKEN_DURATION)
			.add(" seconds when using a sword. Gain ")
			.addPercent(a -> a.mAttackSpeed, AXE_SPEED)
			.add(" speed when holding an axe.");
	}
}
