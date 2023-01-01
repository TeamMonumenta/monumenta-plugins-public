package com.playmonumenta.plugins.itemstats.gui;

import com.playmonumenta.plugins.itemstats.enchantments.Aptitude;
import com.playmonumenta.plugins.itemstats.enchantments.BlastFragility;
import com.playmonumenta.plugins.itemstats.enchantments.BlastProtection;
import com.playmonumenta.plugins.itemstats.enchantments.FallFragility;
import com.playmonumenta.plugins.itemstats.enchantments.FeatherFalling;
import com.playmonumenta.plugins.itemstats.enchantments.FireFragility;
import com.playmonumenta.plugins.itemstats.enchantments.FireProtection;
import com.playmonumenta.plugins.itemstats.enchantments.Ineptitude;
import com.playmonumenta.plugins.itemstats.enchantments.LifeDrain;
import com.playmonumenta.plugins.itemstats.enchantments.MagicFragility;
import com.playmonumenta.plugins.itemstats.enchantments.MagicProtection;
import com.playmonumenta.plugins.itemstats.enchantments.MeleeFragility;
import com.playmonumenta.plugins.itemstats.enchantments.MeleeProtection;
import com.playmonumenta.plugins.itemstats.enchantments.ProjectileFragility;
import com.playmonumenta.plugins.itemstats.enchantments.ProjectileProtection;
import com.playmonumenta.plugins.itemstats.enchantments.Regeneration;
import com.playmonumenta.plugins.itemstats.enchantments.RegionScalingDamageTaken;
import com.playmonumenta.plugins.itemstats.enchantments.Sustenance;
import com.playmonumenta.plugins.itemstats.infusions.Ardor;
import com.playmonumenta.plugins.itemstats.infusions.Epoch;
import com.playmonumenta.plugins.itemstats.infusions.Expedite;
import com.playmonumenta.plugins.itemstats.infusions.Focus;
import com.playmonumenta.plugins.itemstats.infusions.Grace;
import com.playmonumenta.plugins.itemstats.infusions.Nutriment;
import com.playmonumenta.plugins.itemstats.infusions.Pennate;
import com.playmonumenta.plugins.itemstats.infusions.Perspicacity;
import com.playmonumenta.plugins.itemstats.infusions.Soothing;
import com.playmonumenta.plugins.itemstats.infusions.Unyielding;
import com.playmonumenta.plugins.itemstats.infusions.Vigor;
import com.playmonumenta.plugins.itemstats.infusions.Vitality;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.text.DecimalFormat;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

enum PSGUIStat {

	// health and healing
	HEALTH("Max Health", Formatting.NUMBER, stats -> stats.getAttributeAmount(ItemStatUtils.AttributeType.MAX_HEALTH, 20) * (1 + Vitality.HEALTH_MOD_PER_LEVEL * stats.getInfusion(ItemStatUtils.InfusionType.VITALITY))),
	HEALING_RATE("Healing Rate", Formatting.PERCENT, stats -> Sustenance.getHealingMultiplier(stats.get(ItemStatUtils.EnchantmentType.SUSTENANCE), stats.get(ItemStatUtils.EnchantmentType.CURSE_OF_ANEMIA))
		                                                          * Nutriment.getHealingMultiplier(stats.getInfusion(ItemStatUtils.InfusionType.NUTRIMENT))),
	EFFECTIVE_HEALING_RATE("Effective Healing Rate", Formatting.PERCENT, stats -> HEALING_RATE.get(stats) * 20.0 / HEALTH.get(stats)),
	REGENERATION("Regeneration per second", Formatting.NUMBER, stats -> ((4 * Regeneration.healPer5Ticks(stats.get(ItemStatUtils.EnchantmentType.REGENERATION)))
		                                                                     + (stats.getInfusion(ItemStatUtils.InfusionType.SOOTHING) * Soothing.HEAL_PER_LEVEL)) * HEALING_RATE.get(stats)),
	EFFECTIVE_REGENERATION("Regeneration in %HP/s", Formatting.PERCENT, stats -> REGENERATION.get(stats) / HEALTH.get(stats)),
	LIFE_DRAIN("Life Drain on crit", Formatting.NUMBER, stats -> LifeDrain.LIFE_DRAIN_CRIT_HEAL * Math.sqrt(stats.get(ItemStatUtils.EnchantmentType.LIFE_DRAIN)) * HEALING_RATE.get(stats)),
	EFFECTIVE_LIFE_DRAIN("Life Drain on crit in %HP", Formatting.PERCENT, stats -> LIFE_DRAIN.get(stats) / HEALTH.get(stats)),

	// These stats are damage taken, but get displayed as damage reduction
	MELEE_DAMAGE_TAKEN("Melee", Formatting.ONE_MINUS_PERCENT, stats -> stats.getDamageTakenMultiplier(new MeleeProtection(), new MeleeFragility()), false, Formatting.DR_CHANGE_FORMAT),
	PROJECTILE_DAMAGE_TAKEN("Projectile", Formatting.ONE_MINUS_PERCENT, stats -> stats.getDamageTakenMultiplier(new ProjectileProtection(), new ProjectileFragility()), false, Formatting.DR_CHANGE_FORMAT),
	MAGIC_DAMAGE_TAKEN("Magic", Formatting.ONE_MINUS_PERCENT, stats -> stats.getDamageTakenMultiplier(new MagicProtection(), new MagicFragility()), false, Formatting.DR_CHANGE_FORMAT),
	BLAST_DAMAGE_TAKEN("Blast", Formatting.ONE_MINUS_PERCENT, stats -> stats.getDamageTakenMultiplier(new BlastProtection(), new BlastFragility()), false, Formatting.DR_CHANGE_FORMAT),
	FIRE_DAMAGE_TAKEN("Fire", Formatting.ONE_MINUS_PERCENT, stats -> stats.getDamageTakenMultiplier(new FireProtection(), new FireFragility()), false, Formatting.DR_CHANGE_FORMAT),
	FALL_DAMAGE_TAKEN("Fall", Formatting.ONE_MINUS_PERCENT, stats -> stats.getDamageTakenMultiplier(new FeatherFalling(), new FallFragility())
		                                                                 * Pennate.getFallDamageResistance(stats.getInfusion(ItemStatUtils.InfusionType.PENNATE)), false, Formatting.DR_CHANGE_FORMAT),
	AILMENT_DAMAGE_TAKEN("Ailment", Formatting.ONE_MINUS_PERCENT, stats -> stats.getDamageTakenMultiplier(null, null), false, Formatting.DR_CHANGE_FORMAT),

	// These stats are effective damage taken, but get displayed as effective damage reduction
	EFFECTIVE_MELEE_DAMAGE_TAKEN("Melee", Formatting.ONE_MINUS_PERCENT, stats -> MELEE_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, Formatting.DR_CHANGE_FORMAT),
	EFFECTIVE_PROJECTILE_DAMAGE_TAKEN("Projectile", Formatting.ONE_MINUS_PERCENT, stats -> PROJECTILE_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, Formatting.DR_CHANGE_FORMAT),
	EFFECTIVE_MAGIC_DAMAGE_TAKEN("Magic", Formatting.ONE_MINUS_PERCENT, stats -> MAGIC_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, Formatting.DR_CHANGE_FORMAT),
	EFFECTIVE_BLAST_DAMAGE_TAKEN("Blast", Formatting.ONE_MINUS_PERCENT, stats -> BLAST_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, Formatting.DR_CHANGE_FORMAT),
	EFFECTIVE_FIRE_DAMAGE_TAKEN("Fire", Formatting.ONE_MINUS_PERCENT, stats -> FIRE_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, Formatting.DR_CHANGE_FORMAT),
	EFFECTIVE_FALL_DAMAGE_TAKEN("Fall", Formatting.ONE_MINUS_PERCENT, stats -> FALL_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, Formatting.DR_CHANGE_FORMAT),
	EFFECTIVE_AILMENT_DAMAGE_TAKEN("Ailment", Formatting.ONE_MINUS_PERCENT, stats -> AILMENT_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, Formatting.DR_CHANGE_FORMAT),

	// effective health
	MELEE_EHP("Melee", Formatting.NUMBER, stats -> HEALTH.get(stats) / MELEE_DAMAGE_TAKEN.get(stats)),
	PROJECTILE_EHP("Projectile", Formatting.NUMBER, stats -> HEALTH.get(stats) / PROJECTILE_DAMAGE_TAKEN.get(stats)),
	MAGIC_EHP("Magic", Formatting.NUMBER, stats -> HEALTH.get(stats) / MAGIC_DAMAGE_TAKEN.get(stats)),
	BLAST_EHP("Blast", Formatting.NUMBER, stats -> HEALTH.get(stats) / BLAST_DAMAGE_TAKEN.get(stats)),
	FIRE_EHP("Fire", Formatting.NUMBER, stats -> HEALTH.get(stats) / FIRE_DAMAGE_TAKEN.get(stats)),
	FALL_EHP("Fall", Formatting.NUMBER, stats -> HEALTH.get(stats) / FALL_DAMAGE_TAKEN.get(stats)),
	AILMENT_EHP("Ailment", Formatting.NUMBER, stats -> HEALTH.get(stats) / AILMENT_DAMAGE_TAKEN.get(stats)),

	// effective health multipliers
	// TODO this was suggested, but would need to fit into the GUI somehow
	// Maybe make it so clicking one of the other stats (e.g. damage reduction) switches its display to these?
	MELEE_EHM("Melee", Formatting.NUMBER, stats -> 1.0 / MELEE_DAMAGE_TAKEN.get(stats)),
	PROJECTILE_EHM("Projectile", Formatting.NUMBER, stats -> 1.0 / PROJECTILE_DAMAGE_TAKEN.get(stats)),
	MAGIC_EHM("Magic", Formatting.NUMBER, stats -> 1.0 / MAGIC_DAMAGE_TAKEN.get(stats)),
	BLAST_EHM("Blast", Formatting.NUMBER, stats -> 1.0 / BLAST_DAMAGE_TAKEN.get(stats)),
	FIRE_EHM("Fire", Formatting.NUMBER, stats -> 1.0 / FIRE_DAMAGE_TAKEN.get(stats)),
	FALL_EHM("Fall", Formatting.NUMBER, stats -> 1.0 / FALL_DAMAGE_TAKEN.get(stats)),
	AILMENT_EHM("Ailment", Formatting.NUMBER, stats -> 1.0 / AILMENT_DAMAGE_TAKEN.get(stats)),

	// melee
	ATTACK_DAMAGE_ADD("+flat Attack Damage", Formatting.NUMBER, stats -> stats.get(ItemStatUtils.AttributeType.ATTACK_DAMAGE_ADD) - stats.getMainhandAttributeAmount(ItemStatUtils.AttributeType.ATTACK_DAMAGE_ADD, ItemStatUtils.Operation.ADD)),
	ATTACK_DAMAGE_MULTIPLY("+% Attack Damage", Formatting.PERCENT_MODIFIER, stats -> stats.get(ItemStatUtils.AttributeType.ATTACK_DAMAGE_MULTIPLY)
		                                                                                 * stats.getDamageDealtMultiplier()
		                                                                                 * (1 + Vigor.DAMAGE_MOD_PER_LEVEL * stats.getInfusion(ItemStatUtils.InfusionType.VIGOR))),
	TOTAL_ATTACK_DAMAGE("Total Attack Damage", Formatting.NUMBER, stats -> (1 + stats.get(ItemStatUtils.AttributeType.ATTACK_DAMAGE_ADD)) * ATTACK_DAMAGE_MULTIPLY.get(stats)),
	ATTACK_SPEED("Attack Speed", Formatting.NUMBER, stats -> stats.getAttributeAmount(ItemStatUtils.AttributeType.ATTACK_SPEED, 4)
		                                                         * (1 + (Grace.ATKS_BONUS * stats.getInfusion(ItemStatUtils.InfusionType.GRACE)))),

	// projectile
	PROJECTILE_DAMAGE_ADD("+flat Projectile Damage", Formatting.NUMBER, stats -> stats.get(ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD) - stats.getMainhandAttributeAmount(ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD, ItemStatUtils.Operation.ADD)),
	PROJECTILE_DAMAGE_MULTIPLY("+% Projectile Damage", Formatting.PERCENT_MODIFIER, stats -> stats.get(ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_MULTIPLY)
		                                                                                         * stats.getDamageDealtMultiplier()
		                                                                                         * (1 + Focus.DAMAGE_MOD_PER_LEVEL * stats.getInfusion(ItemStatUtils.InfusionType.FOCUS))),
	TOTAL_PROJECTILE_DAMAGE("Total Projectile Damage", Formatting.NUMBER, stats -> stats.get(ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD) * PROJECTILE_DAMAGE_MULTIPLY.get(stats)),
	PROJECTILE_SPEED("Projectile Speed", Formatting.NUMBER, stats -> stats.get(ItemStatUtils.AttributeType.PROJECTILE_SPEED)),
	PROJECTILE_RATE("Shoot/Throw Rate", Formatting.NUMBER, stats -> {
		double throwRate = stats.get(ItemStatUtils.AttributeType.THROW_RATE);
		if (throwRate != 0) {
			return throwRate;
		}
		ItemStack mainhand = stats.getItem(PSGUIEquipment.MAINHAND);
		if (mainhand == null) {
			return 0;
		}
		if (mainhand.getType() == Material.BOW) {
			return 1;
		} else if (mainhand.getType() == Material.CROSSBOW) {
			return Math.max(0, 1.25 - mainhand.getEnchantmentLevel(Enchantment.QUICK_CHARGE) * 0.25);
		}
		return 0;
	}),

	// magic
	SPELL_POWER("Spell Power", Formatting.PERCENT, stats -> stats.get(ItemStatUtils.AttributeType.SPELL_DAMAGE)),
	MAGIC_DAMAGE_ADD("+flat Magic Damage", Formatting.NUMBER, stats -> stats.get(ItemStatUtils.AttributeType.MAGIC_DAMAGE_ADD)),
	MAGIC_DAMAGE_MULTIPLY("+% Magic Damage", Formatting.PERCENT_MODIFIER, stats -> stats.get(ItemStatUtils.AttributeType.MAGIC_DAMAGE_MULTIPLY)
		                                                                               * stats.getDamageDealtMultiplier()
		                                                                               * (1 + Perspicacity.DAMAGE_MOD_PER_LEVEL * stats.getInfusion(ItemStatUtils.InfusionType.PERSPICACITY))),
	TOTAL_SPELL_DAMAGE("Total Spell Damage %", Formatting.PERCENT, stats -> SPELL_POWER.get(stats) * MAGIC_DAMAGE_MULTIPLY.get(stats)),
	COOLDOWN_MULTIPLIER("Cooldown Multiplier", Formatting.PERCENT, stats -> (1 + Aptitude.getCooldownPercentage(stats.get(ItemStatUtils.EnchantmentType.APTITUDE)))
		                                                                        * (1 + Ineptitude.getCooldownPercentage(stats.get(ItemStatUtils.EnchantmentType.INEPTITUDE)))
		                                                                        * (1 + Epoch.getCooldownPercentage(stats.getInfusion(ItemStatUtils.InfusionType.EPOCH))), false),
	// misc
	ARMOR("Total Armor", Formatting.NUMBER, stats -> stats.get(ItemStatUtils.AttributeType.ARMOR)),
	AGILITY("Total Agility", Formatting.NUMBER, stats -> stats.get(ItemStatUtils.AttributeType.AGILITY)),
	MOVEMENT_SPEED("Movement Speed", Formatting.PERCENT,
		stats -> stats.getAttributeAmount(ItemStatUtils.AttributeType.SPEED, 0.1,
			RegionScalingDamageTaken.SPEED_EFFECT[stats.getRegionScaling(stats.mPlayer, false)]
				+ Ardor.getMovementSpeedBonus(stats.getInfusion(ItemStatUtils.InfusionType.ARDOR))
				+ Expedite.getMovementSpeedBonus(stats.getInfusion(ItemStatUtils.InfusionType.EXPEDITE), Expedite.MAX_STACKS)) / 0.1),
	KNOCKBACK_RESISTANCE("Knockback Resistance", Formatting.PERCENT, stats -> Math.min(1, stats.getAttributeAmount(ItemStatUtils.AttributeType.KNOCKBACK_RESISTANCE, 0)
		                                                                                      + Unyielding.getKnockbackResistance(stats.getInfusion(ItemStatUtils.InfusionType.UNYIELDING)))),
	THORNS_DAMAGE("Thorns Damage", Formatting.NUMBER, stats -> stats.get(ItemStatUtils.AttributeType.THORNS) * stats.getDamageDealtMultiplier()),
	MINING_SPEED("Mining Speed", Formatting.NUMBER, stats -> ItemUtils.getMiningSpeed(stats.getItem(PSGUIEquipment.MAINHAND)) * (stats.getRegionScaling(stats.mPlayer, true) > 0 ? 0.3 : 1)),

	;

	// Has to be an inner class as static fields cannot be declared before enum constants
	private static final class Formatting {

		private static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("0.##");
		private static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("0.##%");
		private static final DecimalFormat PERCENT_CHANGE_FORMATTER = new DecimalFormat("+0.##%;-0.##%");

		private static final DoubleFunction<String> PERCENT = PERCENT_FORMATTER::format;
		@SuppressWarnings("UnnecessaryLambda")
		private static final DoubleFunction<String> ONE_MINUS_PERCENT = d -> PERCENT_FORMATTER.format(1 - d);
		@SuppressWarnings("UnnecessaryLambda")
		private static final DoubleFunction<String> PERCENT_MODIFIER = d -> PERCENT_FORMATTER.format(d - 1);
		private static final DoubleFunction<String> NUMBER = NUMBER_FORMATTER::format;
		@SuppressWarnings("UnnecessaryLambda")
		private static final DoubleFunction<String> DR_CHANGE_FORMAT = d -> PERCENT_CHANGE_FORMATTER.format(d) + " damage taken";

	}

	private final String mName;
	private final DoubleFunction<String> mFormat;
	private final ToDoubleFunction<PSGUIStats> mStatFunc;
	private final boolean mLargerIsBetter;
	private final DoubleFunction<String> mChangeFormat;

	PSGUIStat(String name, DoubleFunction<String> format, ToDoubleFunction<PSGUIStats> statFunc) {
		this(name, format, statFunc, true);
	}

	PSGUIStat(String name, DoubleFunction<String> format, ToDoubleFunction<PSGUIStats> statFunc, boolean largerIsBetter) {
		this(name, format, statFunc, largerIsBetter, Formatting.PERCENT_CHANGE_FORMATTER::format);
	}

	PSGUIStat(String name, DoubleFunction<String> format, ToDoubleFunction<PSGUIStats> statFunc, boolean largerIsBetter, DoubleFunction<String> changeFormat) {
		mName = name;
		mFormat = format;
		mStatFunc = statFunc;
		mLargerIsBetter = largerIsBetter;
		mChangeFormat = changeFormat;
	}

	public double get(PSGUIStats stats) {
		return stats.mStatCache.computeIfAbsent(this, k -> mStatFunc.applyAsDouble(stats));
	}

	public Component getDisplay(PSGUIStats stats, @Nullable PSGUIStats otherStats) {
		double value = get(stats);
		Component comp = Component.text(mName + ": ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
			                 .append(Component.text(mFormat.apply(value), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		if (otherStats != null) {
			double otherValue = get(otherStats);
			NamedTextColor color;
			if (otherValue > value) {
				color = mLargerIsBetter ? NamedTextColor.GREEN : NamedTextColor.RED;
			} else if (otherValue < value) {
				color = mLargerIsBetter ? NamedTextColor.RED : NamedTextColor.GREEN;
			} else {
				color = NamedTextColor.WHITE;
			}
			comp = comp.append(Component.text(String.format(" \u2192 %s", mFormat.apply(otherValue)), color).decoration(TextDecoration.ITALIC, false));
			double relativeValue = value == otherValue ? 0 : otherValue / value - 1;
			if (!Double.isInfinite(relativeValue)) {
				comp = comp.append(Component.text(String.format(" (%s)", mChangeFormat.apply(relativeValue)), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
			}
		}
		return comp;
	}
}
