package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
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

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perLevel;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

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
	private final double mSpeed;
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
		mSpeed = AXE_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
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
			mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT, new PercentSpeed(6, mSpeed, SPEED_EFFECT).displaysTime(false).deleteOnAbilityUpdate(true));
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
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("While holding a sword, gain resistance.")
			.addLine()
			.addStat("Sword Effect: +%p Resistance")
				.statValues(stat(a -> a.mDamageReduction, WEAPON_MASTERY_SWORD_DAMAGE_RESISTANCE))
			.addLine()
			.addLine("While holding an axe, your attacks deal")
			.addLine("increased damage.")
			.addLine()
			.addStat("Axe Damage Boost: +%d1e + %p1e (m)")
				.statValues(stat(a -> a.mDamageBonusAxeFlat, AXE_1_DAMAGE_FLAT), stat(a -> a.mDamageBonusAxe, AXE_1_DAMAGE))
			.addDashedLine();
	}

	private static Description<WeaponMastery> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("While holding a sword, your attacks now")
			.addLine("deal increased damage as well.")
			.addLine()
			.addStat("Sword Damage Boost: +%d2e + %p2e (m)")
				.statValues(stat(a -> a.mDamageBonusSwordFlat, SWORD_2_DAMAGE_FLAT), stat(a -> a.mDamageBonusSword, SWORD_2_DAMAGE))
			.addLine()
			.addLine("While holding an axe, your attacks deal")
			.addLine("even more damage.")
			.addLine()
			.addStatComparison("Axe Damage Boost: +%d1e + %p1e -> +%d2e + %p2e (m)")
				.statValues(stat(AXE_1_DAMAGE_FLAT), stat(AXE_1_DAMAGE),
					stat(a -> a.mDamageBonusAxeFlat, AXE_2_DAMAGE_FLAT), stat(a -> a.mDamageBonusAxe, AXE_2_DAMAGE))
			.addDashedLine();
	}

	private static Description<WeaponMastery> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Increase *Weapon Mastery*'s damage boost").styles(UNDERLINED)
			.addLine("for both weapons by an additional %p.")
				.statValues(stat(ENHANCED_DAMAGE))
			.addLine()
			.addStatComparison("Sword Damage Boost: %d + %p -> +%d3 + %p3 (m)")
			.statValues(perLevel(0, SWORD_2_DAMAGE_FLAT), perLevel(0, SWORD_2_DAMAGE),
				perLevel(a -> a.mDamageBonusSwordFlat, 0, SWORD_2_DAMAGE_FLAT), perLevel(a -> a.mDamageBonusSword, ENHANCED_DAMAGE, SWORD_2_DAMAGE + ENHANCED_DAMAGE))
			.addStatComparison("Axe Damage Boost: %d + %p -> +%d3 + %p3 (m)")
			.statValues(perLevel(AXE_1_DAMAGE_FLAT, AXE_2_DAMAGE_FLAT), perLevel(AXE_1_DAMAGE, AXE_2_DAMAGE),
				perLevel(a -> a.mDamageBonusAxeFlat, AXE_1_DAMAGE_FLAT, AXE_2_DAMAGE_FLAT), perLevel(a -> a.mDamageBonusAxe, AXE_1_DAMAGE + ENHANCED_DAMAGE, AXE_2_DAMAGE + ENHANCED_DAMAGE))
			.addLine()
			.addLine("While holding a sword, your attacks weaken mobs.")
			.addLine()
			.addStat("Effect: %p Weakness for %t")
				.statValues(stat(a -> a.mWeaken, SWORD_WEAKEN), stat(a -> a.mWeakenDuration, SWORD_WEAKEN_DURATION))
			.addLine()
			.addLine("While holding an axe, gain speed.")
			.addLine()
			.addStat("Effect: +%p Speed")
				.statValues(stat(a -> a.mSpeed, AXE_SPEED))
			.addDashedLine();
	}
}
