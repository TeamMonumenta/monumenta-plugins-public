package com.playmonumenta.plugins.itemstats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.itemstats.attributes.Armor;
import com.playmonumenta.plugins.itemstats.enchantments.Aptitude;
import com.playmonumenta.plugins.itemstats.enchantments.BlastProtection;
import com.playmonumenta.plugins.itemstats.enchantments.Cloaked;
import com.playmonumenta.plugins.itemstats.enchantments.Ethereal;
import com.playmonumenta.plugins.itemstats.enchantments.Evasion;
import com.playmonumenta.plugins.itemstats.enchantments.FeatherFalling;
import com.playmonumenta.plugins.itemstats.enchantments.FireProtection;
import com.playmonumenta.plugins.itemstats.enchantments.Guard;
import com.playmonumenta.plugins.itemstats.enchantments.Ineptitude;
import com.playmonumenta.plugins.itemstats.enchantments.LifeDrain;
import com.playmonumenta.plugins.itemstats.enchantments.MagicProtection;
import com.playmonumenta.plugins.itemstats.enchantments.MeleeProtection;
import com.playmonumenta.plugins.itemstats.enchantments.Poise;
import com.playmonumenta.plugins.itemstats.enchantments.ProjectileProtection;
import com.playmonumenta.plugins.itemstats.enchantments.Protection;
import com.playmonumenta.plugins.itemstats.enchantments.ProtectionOfTheDepths;
import com.playmonumenta.plugins.itemstats.enchantments.Reflexes;
import com.playmonumenta.plugins.itemstats.enchantments.Regeneration;
import com.playmonumenta.plugins.itemstats.enchantments.RegionScalingDamageDealt;
import com.playmonumenta.plugins.itemstats.enchantments.RegionScalingDamageTaken;
import com.playmonumenta.plugins.itemstats.enchantments.SecondWind;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.itemstats.enchantments.Steadfast;
import com.playmonumenta.plugins.itemstats.enchantments.Sustenance;
import com.playmonumenta.plugins.itemstats.enchantments.Tempo;
import com.playmonumenta.plugins.itemstats.infusions.Ardor;
import com.playmonumenta.plugins.itemstats.infusions.Carapace;
import com.playmonumenta.plugins.itemstats.infusions.Choler;
import com.playmonumenta.plugins.itemstats.infusions.Epoch;
import com.playmonumenta.plugins.itemstats.infusions.Execution;
import com.playmonumenta.plugins.itemstats.infusions.Expedite;
import com.playmonumenta.plugins.itemstats.infusions.Focus;
import com.playmonumenta.plugins.itemstats.infusions.Nutriment;
import com.playmonumenta.plugins.itemstats.infusions.Pennate;
import com.playmonumenta.plugins.itemstats.infusions.Perspicacity;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.itemstats.infusions.Tenacity;
import com.playmonumenta.plugins.itemstats.infusions.Unyielding;
import com.playmonumenta.plugins.itemstats.infusions.Vengeful;
import com.playmonumenta.plugins.itemstats.infusions.Vigor;
import com.playmonumenta.plugins.itemstats.infusions.Vitality;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Operation;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class PlayerItemStatsGUI extends CustomInventory {

	private static class Settings {
		private final EnumSet<SecondaryStat> mSecondaryStatEnabled = EnumSet.noneOf(SecondaryStat.class);
	}

	private enum InfusionSetting {
		DISABLED("Ignore all infusions in calculations", null),
		ENABLED("Respect existing infusions on items", null),
		ENABLED_FULL("Existing infusions + delve infusions fully active", null),
		VITALITY("20 Vitality + 4 Tenacity", InfusionType.VITALITY),
		TENACITY("24 Tenacity", InfusionType.TENACITY),
		VIGOR("24 Vigor", InfusionType.VIGOR),
		FOCUS("24 Focus", InfusionType.FOCUS),
		PERSPICACITY("24 Perspicacity", InfusionType.PERSPICACITY),
		;

		/**
		 * Set of infusions that are only considered active with the setting {@link #ENABLED_FULL}
		 */
		private static final ImmutableSet<InfusionType> CONDITIONAL_DELVE_INFUSIONS = ImmutableSet.of(
			InfusionType.ARDOR,
			InfusionType.CARAPACE,
			InfusionType.CHOLER,
			InfusionType.EXECUTION,
			InfusionType.EXPEDITE,
			InfusionType.MITOSIS,
			InfusionType.VENGEFUL
		);

		private final String mDescription;
		private final @Nullable InfusionType mInfusionType;

		InfusionSetting(String description, @Nullable InfusionType infusionType) {
			this.mDescription = description;
			this.mInfusionType = infusionType;
		}
	}

	private static class Stats {
		private final Player mPlayer;
		private final PlayerItemStats mPlayerItemStats = new PlayerItemStats(ItemStatUtils.Region.VALLEY);
		private final EnumMap<Equipment, ItemStack> mEquipment = new EnumMap<>(Equipment.class);
		private final EnumMap<Equipment, ItemStack> mDisplayedEquipment = new EnumMap<>(Equipment.class);
		private final EnumMap<Equipment, ItemStack> mOriginalEquipment = new EnumMap<>(Equipment.class);
		private final EnumMap<Equipment, ItemStack> mOriginalDisplayedEquipment = new EnumMap<>(Equipment.class);
		private final EnumMap<Stat, Double> mStatCache = new EnumMap<>(Stat.class);
		private final @Nullable Stats mMainStats;
		private final Settings mSettings;
		private InfusionSetting mInfusionSetting = InfusionSetting.DISABLED;

		private Stats(Player player, @Nullable Stats mainStats, Settings settings) {
			mPlayer = player;
			mMainStats = mainStats;
			mSettings = settings;
		}

		private double get(AttributeType attr) {
			return mPlayerItemStats.getItemStats().get(attr.getItemStat());
		}

		private double get(AttributeType attr, double defaultValue) {
			return mPlayerItemStats.getItemStats().get(attr.getItemStat(), defaultValue);
		}

		private double get(EnchantmentType ench) {
			return mPlayerItemStats.getItemStats().get(ench.getItemStat());
		}

		private @Nullable ItemStack getItem(Equipment slot) {
			ItemStack result = mEquipment.get(slot);
			if ((result == null || result.getType() == Material.AIR) && mMainStats != null) {
				return mMainStats.mEquipment.get(slot);
			}
			return result;
		}

		private InfusionSetting getInfusionSetting() {
			if (mInfusionSetting == InfusionSetting.DISABLED && mMainStats != null) {
				return mMainStats.mInfusionSetting;
			}
			return mInfusionSetting;
		}

		private double getInfusion(InfusionType infusion) {
			if (infusion == InfusionType.SHATTERED) {
				return mPlayerItemStats.getItemStats().get(infusion.getItemStat());
			}
			InfusionSetting setting = getInfusionSetting();
			if (setting == InfusionSetting.ENABLED || setting == InfusionSetting.ENABLED_FULL) {
				if (setting != InfusionSetting.ENABLED_FULL
					&& InfusionSetting.CONDITIONAL_DELVE_INFUSIONS.contains(infusion)) {
					return 0;
				}
				double value = mPlayerItemStats.getItemStats().get(infusion.getItemStat());
				if (InfusionType.DELVE_INFUSIONS.contains(infusion) && infusion != InfusionType.UNDERSTANDING) {
					return DelveInfusionUtils.getModifiedLevel((int) value, (int) mPlayerItemStats.getItemStats().get(InfusionType.UNDERSTANDING.getItemStat()));
				}
				return value;
			} else if (setting.mInfusionType == infusion) {
				if (setting == InfusionSetting.VITALITY) {
					return 20;
				}
				return 24;
			} else {
				if (setting == InfusionSetting.VITALITY && infusion == InfusionType.TENACITY) {
					return 4;
				}
				return 0;
			}
		}

		private double getMainhandAttributeAmount(AttributeType type, Operation operation) {
			ItemStack mainhand = getItem(Equipment.MAINHAND);
			return mainhand == null ? 0 : ItemStatUtils.getAttributeAmount(mainhand, type, operation, Slot.MAINHAND);
		}

		private double getAttributeAmount(AttributeType type, Operation operation) {
			double result = 0;
			for (Equipment slot : Equipment.values()) {
				ItemStack item = getItem(slot);
				result += ItemStatUtils.getAttributeAmount(item, type, operation, slot.mSlot);
			}
			return result;
		}

		private double getAttributeAmount(AttributeType type, double base) {
			return getAttributeAmount(type, base, 0);
		}

		private double getAttributeAmount(AttributeType type, double base, double additionalModifier) {
			return (base + getAttributeAmount(type, Operation.ADD)) * (1 + getAttributeAmount(type, Operation.MULTIPLY) + additionalModifier);
		}

		private ItemStatUtils.Region getMaximumRegion(boolean mainhand) {
			return getMaximumRegion(mainhand, ItemStatUtils.Region.VALLEY);
		}

		private ItemStatUtils.Region getMaximumRegion(boolean mainhand, ItemStatUtils.Region defaultRegion) {
			return (mainhand ? Stream.of(Equipment.MAINHAND) : Arrays.stream(Equipment.values()).filter(slot -> slot != Equipment.MAINHAND))
				       .map(slot -> ItemStatUtils.getRegion(getItem(slot)))
				       .filter(region -> region == ItemStatUtils.Region.VALLEY || region == ItemStatUtils.Region.ISLES || region == ItemStatUtils.Region.RING)
				       .max(Comparator.naturalOrder())
				       .orElse(defaultRegion);
		}

		private int getRegionScaling(Player player, boolean mainhand) {
			return (mainhand ? Stream.of(Equipment.MAINHAND) : Arrays.stream(Equipment.values()).filter(slot -> slot != Equipment.MAINHAND))
				       .mapToInt(slot -> (int) ItemStatManager.getRegionScaling(player, ItemStatUtils.getRegion(getItem(slot)), mPlayerItemStats.getRegion(), 0, 1, 2))
				       .max()
				       .orElse(0);
		}

		private boolean hasMaxShatteredItemEquipped() {
			for (Equipment slot : Equipment.values()) {
				ItemStack item = getItem(slot);
				if (item != null && Shattered.isMaxShatter(item)) {
					return true;
				}
			}
			return false;
		}

		private double getTotalDamageDealtMultiplier() {

			double result = 1.0;

			if (getInfusion(InfusionType.SHATTERED) > 0) {
				result *= Shattered.getDamageDealtMultiplier(hasMaxShatteredItemEquipped());
			}

			result *= RegionScalingDamageDealt.DAMAGE_DEALT_MULTIPLIER[getRegionScaling(mPlayer, true)];

			result *= Choler.getDamageDealtMultiplier(getInfusion(InfusionType.CHOLER));
			result *= Execution.getDamageDealtMultiplier(getInfusion(InfusionType.EXECUTION));
			result *= Vengeful.getDamageDealtMultiplier(getInfusion(InfusionType.VENGEFUL));

			return result;
		}
	}

	static double getDamageTakenMultiplier(Stats stats, @Nullable Protection protection) {
		boolean region2 = stats.mPlayerItemStats.getRegion().compareTo(ItemStatUtils.Region.ISLES) >= 0;

		double damageMultiplier = 1;
		if (protection != null) {
			double armor = stats.get(AttributeType.ARMOR);
			double agility = stats.get(AttributeType.AGILITY);

			double armorBonus = 0;
			double agilityBonus = 0;
			for (SecondaryStat stat : stats.mSettings.mSecondaryStatEnabled) {
				if (stat.isArmorModifier()) {
					armorBonus += Shielding.ARMOR_BONUS_PER_LEVEL * stats.get(stat.getEnchantmentType());
				} else {
					agilityBonus += Shielding.ARMOR_BONUS_PER_LEVEL * stats.get(stat.getEnchantmentType());
				}
			}

			boolean adaptability = stats.get(EnchantmentType.ADAPTABILITY) > 0;
			double epf = protection.getEPF() * stats.get(protection.getEnchantmentType());

			damageMultiplier = Armor.getDamageMultiplier(armor, armorBonus, agility, agilityBonus,
				Armor.getSecondaryEnchantCap(region2), adaptability, epf, protection.getType().isEnvironmental());
		}

		// when Steadfast is enabled, also include Second Wind in calculation
		if (stats.mSettings.mSecondaryStatEnabled.contains(SecondaryStat.STEADFAST)) {
			damageMultiplier *= SecondWind.getDamageMultiplier(stats.get(EnchantmentType.SECOND_WIND));
		}

		if (stats.get(EnchantmentType.PROTECTION_OF_THE_DEPTHS) > 0) {
			damageMultiplier *= ProtectionOfTheDepths.getDamageMultiplier(region2);
		}

		if (protection == null || protection.getEnchantmentType() != EnchantmentType.FEATHER_FALLING) {
			damageMultiplier *= RegionScalingDamageTaken.DAMAGE_TAKEN_MULTIPLIER[stats.getRegionScaling(stats.mPlayer, false)];
		}

		if (stats.getInfusion(InfusionType.SHATTERED) > 0) {
			damageMultiplier *= Shattered.getDamageTakenMultiplier(stats.hasMaxShatteredItemEquipped());
		}

		damageMultiplier *= Tenacity.getDamageTakenMultiplier(stats.getInfusion(InfusionType.TENACITY));
		damageMultiplier *= Carapace.getDamageTakenMultiplier(stats.getInfusion(InfusionType.CARAPACE));

		return damageMultiplier;
	}

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

	private enum Stat {

		// health and healing
		HEALTH("Max Health", NUMBER, stats -> stats.getAttributeAmount(AttributeType.MAX_HEALTH, 20) * (1 + Vitality.HEALTH_MOD_PER_LEVEL * stats.getInfusion(InfusionType.VITALITY))),
		HEALING_RATE("Healing Rate", PERCENT, stats -> Sustenance.getHealingMultiplier(stats.get(EnchantmentType.SUSTENANCE), stats.get(EnchantmentType.CURSE_OF_ANEMIA))
			* Nutriment.getHealingMultiplier(stats.getInfusion(InfusionType.NUTRIMENT))),
		EFFECTIVE_HEALING_RATE("Effective Healing Rate", PERCENT, stats -> HEALING_RATE.get(stats) * 20.0 / HEALTH.get(stats)),
		REGENERATION("Regeneration per second", NUMBER, stats -> 4 * Regeneration.healPer5Ticks(stats.get(EnchantmentType.REGENERATION)) * HEALING_RATE.get(stats)),
		EFFECTIVE_REGENERATION("Regeneration in %HP/s", PERCENT, stats -> REGENERATION.get(stats) / HEALTH.get(stats)),
		LIFE_DRAIN("Life Drain on crit", NUMBER, stats -> LifeDrain.LIFE_DRAIN_CRIT_HEAL * Math.sqrt(stats.get(EnchantmentType.LIFE_DRAIN)) * HEALING_RATE.get(stats)),
		EFFECTIVE_LIFE_DRAIN("Life Drain on crit in %HP", PERCENT, stats -> LIFE_DRAIN.get(stats) / HEALTH.get(stats)),

		// These stats are damage taken, but get displayed as damage reduction
		MELEE_DAMAGE_TAKEN("Melee", ONE_MINUS_PERCENT, stats -> getDamageTakenMultiplier(stats, new MeleeProtection()), false, DR_CHANGE_FORMAT),
		PROJECTILE_DAMAGE_TAKEN("Projectile", ONE_MINUS_PERCENT, stats -> getDamageTakenMultiplier(stats, new ProjectileProtection()), false, DR_CHANGE_FORMAT),
		MAGIC_DAMAGE_TAKEN("Magic", ONE_MINUS_PERCENT, stats -> getDamageTakenMultiplier(stats, new MagicProtection()), false, DR_CHANGE_FORMAT),
		BLAST_DAMAGE_TAKEN("Blast", ONE_MINUS_PERCENT, stats -> getDamageTakenMultiplier(stats, new BlastProtection()), false, DR_CHANGE_FORMAT),
		FIRE_DAMAGE_TAKEN("Fire", ONE_MINUS_PERCENT, stats -> getDamageTakenMultiplier(stats, new FireProtection()), false, DR_CHANGE_FORMAT),
		FALL_DAMAGE_TAKEN("Fall", ONE_MINUS_PERCENT, stats -> getDamageTakenMultiplier(stats, new FeatherFalling())
			* Pennate.getFallDamageResistance(stats.getInfusion(InfusionType.PENNATE)), false, DR_CHANGE_FORMAT),
		AILMENT_DAMAGE_TAKEN("Ailment", ONE_MINUS_PERCENT, stats -> getDamageTakenMultiplier(stats, null), false, DR_CHANGE_FORMAT),

		// These stats are effective damage taken, but get displayed as effective damage reduction
		EFFECTIVE_MELEE_DAMAGE_TAKEN("Melee", ONE_MINUS_PERCENT, stats -> MELEE_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_PROJECTILE_DAMAGE_TAKEN("Projectile", ONE_MINUS_PERCENT, stats -> PROJECTILE_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_MAGIC_DAMAGE_TAKEN("Magic", ONE_MINUS_PERCENT, stats -> MAGIC_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_BLAST_DAMAGE_TAKEN("Blast", ONE_MINUS_PERCENT, stats -> BLAST_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_FIRE_DAMAGE_TAKEN("Fire", ONE_MINUS_PERCENT, stats -> FIRE_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_FALL_DAMAGE_TAKEN("Fall", ONE_MINUS_PERCENT, stats -> FALL_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_AILMENT_DAMAGE_TAKEN("Ailment", ONE_MINUS_PERCENT, stats -> AILMENT_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),

		// effective health
		MELEE_EHP("Melee", NUMBER, stats -> HEALTH.get(stats) / MELEE_DAMAGE_TAKEN.get(stats)),
		PROJECTILE_EHP("Projectile", NUMBER, stats -> HEALTH.get(stats) / PROJECTILE_DAMAGE_TAKEN.get(stats)),
		MAGIC_EHP("Magic", NUMBER, stats -> HEALTH.get(stats) / MAGIC_DAMAGE_TAKEN.get(stats)),
		BLAST_EHP("Blast", NUMBER, stats -> HEALTH.get(stats) / BLAST_DAMAGE_TAKEN.get(stats)),
		FIRE_EHP("Fire", NUMBER, stats -> HEALTH.get(stats) / FIRE_DAMAGE_TAKEN.get(stats)),
		FALL_EHP("Fall", NUMBER, stats -> HEALTH.get(stats) / FALL_DAMAGE_TAKEN.get(stats)),
		AILMENT_EHP("Ailment", NUMBER, stats -> HEALTH.get(stats) / AILMENT_DAMAGE_TAKEN.get(stats)),

		// effective health multipliers
		// TODO this was suggested, but would need to fit into the GUI somehow
		// Maybe make it so clicking one of the other stats (e.g. damage reduction) switches its display to these?
		MELEE_EHM("Melee", NUMBER, stats -> 1.0 / MELEE_DAMAGE_TAKEN.get(stats)),
		PROJECTILE_EHM("Projectile", NUMBER, stats -> 1.0 / PROJECTILE_DAMAGE_TAKEN.get(stats)),
		MAGIC_EHM("Magic", NUMBER, stats -> 1.0 / MAGIC_DAMAGE_TAKEN.get(stats)),
		BLAST_EHM("Blast", NUMBER, stats -> 1.0 / BLAST_DAMAGE_TAKEN.get(stats)),
		FIRE_EHM("Fire", NUMBER, stats -> 1.0 / FIRE_DAMAGE_TAKEN.get(stats)),
		FALL_EHM("Fall", NUMBER, stats -> 1.0 / FALL_DAMAGE_TAKEN.get(stats)),
		AILMENT_EHM("Ailment", NUMBER, stats -> 1.0 / AILMENT_DAMAGE_TAKEN.get(stats)),

		// melee
		ATTACK_DAMAGE_ADD("+flat Attack Damage", NUMBER, stats -> stats.get(AttributeType.ATTACK_DAMAGE_ADD) - stats.getMainhandAttributeAmount(AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD)),
		ATTACK_DAMAGE_MULTIPLY("+% Attack Damage", PERCENT_MODIFIER, stats -> stats.get(AttributeType.ATTACK_DAMAGE_MULTIPLY, 1)
			* stats.getTotalDamageDealtMultiplier()
			* (1 + Vigor.DAMAGE_MOD_PER_LEVEL * stats.getInfusion(InfusionType.VIGOR))),
		TOTAL_ATTACK_DAMAGE("Total Attack Damage", NUMBER, stats -> (1 + stats.get(AttributeType.ATTACK_DAMAGE_ADD)) * ATTACK_DAMAGE_MULTIPLY.get(stats)),
		ATTACK_SPEED("Attack Speed", NUMBER, stats -> stats.getAttributeAmount(AttributeType.ATTACK_SPEED, 4)),

		// projectile
		PROJECTILE_DAMAGE_ADD("+flat Projectile Damage", NUMBER, stats -> stats.get(AttributeType.PROJECTILE_DAMAGE_ADD) - stats.getMainhandAttributeAmount(AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD)),
		PROJECTILE_DAMAGE_MULTIPLY("+% Projectile Damage", PERCENT_MODIFIER, stats -> stats.get(AttributeType.PROJECTILE_DAMAGE_MULTIPLY, 1)
			* stats.getTotalDamageDealtMultiplier()
			* (1 + Focus.DAMAGE_MOD_PER_LEVEL * stats.getInfusion(InfusionType.FOCUS))),
		TOTAL_PROJECTILE_DAMAGE("Total Projectile Damage", NUMBER, stats -> stats.get(AttributeType.PROJECTILE_DAMAGE_ADD) * PROJECTILE_DAMAGE_MULTIPLY.get(stats)),
		PROJECTILE_SPEED("Projectile Speed", NUMBER, stats -> {
			ItemStack mainhand = stats.getItem(Equipment.MAINHAND);
			if (ItemUtils.isBowOrTrident(mainhand)) {
				// bows, crossbows, and tridents have the final projectile speed lowered by one in ProjectileSpeed.onLaunchProjectile
				return stats.get(AttributeType.PROJECTILE_SPEED, 1) - 1;
			} else {
				return stats.get(AttributeType.PROJECTILE_SPEED, 0);
			}
		}),
		PROJECTILE_RATE("Shoot/Throw Rate", NUMBER, stats -> {
			double throwRate = stats.get(AttributeType.THROW_RATE, 0);
			if (throwRate != 0) {
				return throwRate;
			}
			ItemStack mainhand = stats.getItem(Equipment.MAINHAND);
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
		SPELL_POWER("Spell Power", PERCENT, stats -> stats.get(AttributeType.SPELL_DAMAGE)),
		MAGIC_DAMAGE_ADD("+flat Magic Damage", NUMBER, stats -> stats.get(AttributeType.MAGIC_DAMAGE_ADD)),
		MAGIC_DAMAGE_MULTIPLY("+% Magic Damage", PERCENT_MODIFIER, stats -> stats.get(AttributeType.MAGIC_DAMAGE_MULTIPLY, 1)
			                                                                    * stats.getTotalDamageDealtMultiplier()
			                                                                    * (1 + Perspicacity.DAMAGE_MOD_PER_LEVEL * stats.getInfusion(InfusionType.PERSPICACITY))),
		TOTAL_SPELL_DAMAGE("Total Spell Damage %", PERCENT, stats -> SPELL_POWER.get(stats) * MAGIC_DAMAGE_MULTIPLY.get(stats)),
		COOLDOWN_MULTIPLIER("Cooldown Multiplier", PERCENT, stats -> (1 + Aptitude.getCooldownPercentage(stats.get(EnchantmentType.APTITUDE)))
			                                                             * (1 + Ineptitude.getCooldownPercentage(stats.get(EnchantmentType.INEPTITUDE)))
			                                                             * (1 + Epoch.getCooldownPercentage(stats.getInfusion(InfusionType.EPOCH))), false),
		// misc
		ARMOR("Total Armor", NUMBER, stats -> stats.get(AttributeType.ARMOR)),
		AGILITY("Total Agility", NUMBER, stats -> stats.get(AttributeType.AGILITY)),
		MOVEMENT_SPEED("Movement Speed", PERCENT,
			stats -> stats.getAttributeAmount(AttributeType.SPEED, 0.1,
				RegionScalingDamageTaken.SPEED_EFFECT[stats.getRegionScaling(stats.mPlayer, false)]
					+ Ardor.getMovementSpeedBonus(stats.getInfusion(InfusionType.ARDOR))
					+ Expedite.getMovementSpeedBonus(stats.getInfusion(InfusionType.EXPEDITE), Expedite.MAX_STACKS)) / 0.1),
		KNOCKBACK_RESISTANCE("Knockback Resistance", PERCENT, stats -> Math.min(1, stats.getAttributeAmount(AttributeType.KNOCKBACK_RESISTANCE, 0)
			                                                                           + Unyielding.getKnockbackResistance(stats.getInfusion(InfusionType.UNYIELDING)))),
		THORNS_DAMAGE("Thorns Damage", NUMBER, stats -> stats.get(AttributeType.THORNS) * stats.getTotalDamageDealtMultiplier()),
		MINING_SPEED("Mining Speed", NUMBER, stats -> ItemUtils.getMiningSpeed(stats.getItem(Equipment.MAINHAND)) * (stats.getRegionScaling(stats.mPlayer, true) > 0 ? 0.3 : 1)),

		;

		private final String mName;
		private final DoubleFunction<String> mFormat;
		private final ToDoubleFunction<Stats> mStatFunc;
		private final boolean mLargerIsBetter;
		private final DoubleFunction<String> mChangeFormat;

		Stat(String name, DoubleFunction<String> format, ToDoubleFunction<Stats> statFunc) {
			this(name, format, statFunc, true);
		}

		Stat(String name, DoubleFunction<String> format, ToDoubleFunction<Stats> statFunc, boolean largerIsBetter) {
			this(name, format, statFunc, largerIsBetter, PERCENT_CHANGE_FORMATTER::format);
		}

		Stat(String name, DoubleFunction<String> format, ToDoubleFunction<Stats> statFunc, boolean largerIsBetter, DoubleFunction<String> changeFormat) {
			mName = name;
			mFormat = format;
			mStatFunc = statFunc;
			mLargerIsBetter = largerIsBetter;
			mChangeFormat = changeFormat;
		}

		public double get(Stats stats) {
			return stats.mStatCache.computeIfAbsent(this, k -> mStatFunc.applyAsDouble(stats));
		}

		public Component getDisplay(Stats stats, @Nullable Stats otherStats) {
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

	private static class StatItem {
		private final int mSlot;
		private final Material mIcon;
		private final Component mName;
		private final List<Stat> mDisplayedStats;

		private StatItem(int mSlot, Material mIcon, Component mName, Stat... displayedStats) {
			this.mSlot = mSlot;
			this.mIcon = mIcon;
			this.mName = mName;
			this.mDisplayedStats = List.of(displayedStats);
		}

		public ItemStack getDisplay(Stats stats, @Nullable Stats otherStats) {
			ItemStack icon = getCleanItem(new ItemStack(mIcon, 1));
			ItemMeta meta = icon.getItemMeta();
			meta.displayName(mName);
			meta.lore(mDisplayedStats.stream().map(stat -> stat.getDisplay(stats, otherStats)).toList());
			icon.setItemMeta(meta);
			ItemUtils.setPlainName(icon);
			return icon;
		}
	}

	private static final StatItem HEALTH_INFO = new StatItem(13, Material.APPLE,
		Component.text("Health and Healing", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.HEALTH, Stat.HEALING_RATE, Stat.EFFECTIVE_HEALING_RATE, Stat.REGENERATION, Stat.EFFECTIVE_REGENERATION, Stat.LIFE_DRAIN, Stat.EFFECTIVE_LIFE_DRAIN);

	private static final StatItem HEALTH_NORMALIZED_DAMAGE_RESISTANCE = new StatItem(21, Material.GLOWSTONE_DUST,
		Component.text("Health Normalized Damage Reduction", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.EFFECTIVE_MELEE_DAMAGE_TAKEN, Stat.EFFECTIVE_PROJECTILE_DAMAGE_TAKEN, Stat.EFFECTIVE_MAGIC_DAMAGE_TAKEN,
		Stat.EFFECTIVE_BLAST_DAMAGE_TAKEN, Stat.EFFECTIVE_FIRE_DAMAGE_TAKEN, Stat.EFFECTIVE_FALL_DAMAGE_TAKEN, Stat.EFFECTIVE_AILMENT_DAMAGE_TAKEN);

	private static final StatItem DAMAGE_RESISTANCE = new StatItem(22, Material.SUGAR,
		Component.text("Damage Reduction", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.MELEE_DAMAGE_TAKEN, Stat.PROJECTILE_DAMAGE_TAKEN, Stat.MAGIC_DAMAGE_TAKEN, Stat.BLAST_DAMAGE_TAKEN, Stat.FIRE_DAMAGE_TAKEN, Stat.FALL_DAMAGE_TAKEN, Stat.AILMENT_DAMAGE_TAKEN);

	private static final StatItem EFFECTIVE_HEALTH = new StatItem(23, Material.REDSTONE,
		Component.text("Effective Health", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.MELEE_EHP, Stat.PROJECTILE_EHP, Stat.MAGIC_EHP, Stat.BLAST_EHP, Stat.FIRE_EHP, Stat.FALL_EHP, Stat.AILMENT_EHP);

	private static final StatItem MELEE_INFO = new StatItem(30, Material.IRON_SWORD,
		Component.text("Melee", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		/*Stat.ATTACK_DAMAGE_ADD,*/ Stat.ATTACK_DAMAGE_MULTIPLY, Stat.TOTAL_ATTACK_DAMAGE, Stat.ATTACK_SPEED);

	private static final StatItem PROJECTILE_INFO = new StatItem(31, Material.BOW,
		Component.text("Projectile", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		/*Stat.PROJECTILE_DAMAGE_ADD,*/ Stat.PROJECTILE_DAMAGE_MULTIPLY, Stat.TOTAL_PROJECTILE_DAMAGE, Stat.PROJECTILE_SPEED, Stat.PROJECTILE_RATE);

	private static final StatItem MAGIC_INFO = new StatItem(32, Material.BLAZE_ROD,
		Component.text("Magic", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.SPELL_POWER, /*Stat.MAGIC_DAMAGE_ADD,*/ Stat.MAGIC_DAMAGE_MULTIPLY, Stat.TOTAL_SPELL_DAMAGE, Stat.COOLDOWN_MULTIPLIER);

	private static final StatItem MISC_INFO = new StatItem(40, Material.LEATHER_BOOTS,
		Component.text("Misc", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.ARMOR, Stat.AGILITY, Stat.MOVEMENT_SPEED, Stat.KNOCKBACK_RESISTANCE, Stat.THORNS_DAMAGE, Stat.MINING_SPEED);

	private static final StatItem[] STAT_ITEMS = {
		HEALTH_INFO,
		DAMAGE_RESISTANCE, HEALTH_NORMALIZED_DAMAGE_RESISTANCE, EFFECTIVE_HEALTH,
		MELEE_INFO, PROJECTILE_INFO, MAGIC_INFO,
		MISC_INFO
	};

	private enum SecondaryStat {
		SHIELDING(0, Material.NAUTILUS_SHELL, EnchantmentType.SHIELDING, true, """
			Gain (Level*20%%) effective Armor
			when taking damage from an enemy within %s blocks.
			Taking damage that would stun a shield
			halves Shielding reduction for %s seconds.""".formatted(Shielding.DISTANCE, StringUtils.ticksToSeconds(Shielding.DISABLE_DURATION))),
		POISE(1, Material.LILY_OF_THE_VALLEY, EnchantmentType.POISE, true, """
			Gain (Level*20%%) effective Armor
			when above %s%% Max Health.""".formatted(StringUtils.multiplierToPercentage(Poise.MIN_HEALTH_PERCENT))),
		INURE(2, Material.NETHERITE_SCRAP, EnchantmentType.INURE, true, """
			Gain (Level*20%) effective Armor
			when taking the same type of mob damage consecutively
			(Melee, Projectile, Blast, or Magic)."""),
		STEADFAST(3, Material.LEAD, EnchantmentType.STEADFAST, true, """
			Scaling with percent health missing,
			gain up to (Level*20%%) effective Armor
			(%s%% armor per 1%% health lost, up to 20%% armor).
			Also calculates bonus from Second Wind when enabled.""".formatted(Steadfast.BONUS_SCALING_RATE)),
		GUARD(9, Material.SHULKER_SHELL, EnchantmentType.GUARD, true, """
			Gain (Level*20%%) effective Armor
			after blocking an attack with a shield.
			The duration lasts for %ss if blocked
			from offhand, and %ss from mainhand.""".formatted(
			StringUtils.ticksToSeconds(Guard.PAST_HIT_DURATION_TIME_OFFHAND), StringUtils.ticksToSeconds(Guard.PAST_HIT_DURATION_TIME_MAINHAND))),
		ETHEREAL(5, Material.PHANTOM_MEMBRANE, EnchantmentType.ETHEREAL, false, """
			Gain (Level*20%%) effective Agility
			on hits taken within %s seconds of any previous hit.""".formatted(StringUtils.ticksToSeconds(Ethereal.PAST_HIT_DURATION_TIME))),
		REFLEXES(6, Material.ENDER_EYE, EnchantmentType.REFLEXES, false, """
			Gain (Level*20%%) effective Agility
			when there are %s or more enemies within %s blocks.""".formatted(Reflexes.MOB_CAP, Reflexes.RADIUS)),
		EVASION(7, Material.ELYTRA, EnchantmentType.EVASION, false, """
			Gain (Level*20%%) effective Agility
			when taking damage from a source further
			than %s blocks from the player.""".formatted(Evasion.DISTANCE)),
		TEMPO(8, Material.CLOCK, EnchantmentType.TEMPO, false, """
			Gain (Level*20%%) effective Agility
			on the first hit taken after
			%s seconds of taking no damage.
			Half of the bonus is granted after
			%s seconds of taking no damage.""".formatted(
			StringUtils.ticksToSeconds(Tempo.PAST_HIT_DURATION_TIME),
			StringUtils.ticksToSeconds(Tempo.PAST_HIT_DURATION_TIME_HALF)
		)),
		CLOAKED(17, Material.BLACK_DYE, EnchantmentType.CLOAKED, false, """
			Gain (Level*20%%) effective Agility
			when there are %s or less enemies within %s blocks.""".formatted(Cloaked.MOB_CAP, Cloaked.RADIUS));

		private final int mSlot;
		private final Material mIcon;
		private final String mName;
		private final boolean mIsArmorModifier;
		private final EnchantmentType mEnchantmentType;
		private final String mDescription;

		SecondaryStat(int slot, Material icon, EnchantmentType enchantmentType, boolean isArmorModifier, String description) {
			mSlot = slot;
			mIcon = icon;
			mName = enchantmentType.getName();
			mEnchantmentType = enchantmentType;
			mIsArmorModifier = isArmorModifier;
			mDescription = description;
		}

		public int getSlot() {
			return mSlot;
		}

		public Material getIcon() {
			return mIcon;
		}

		public String getName() {
			return mName;
		}

		public EnchantmentType getEnchantmentType() {
			return mEnchantmentType;
		}

		public boolean isArmorModifier() {
			return mIsArmorModifier;
		}

		public Component getDisplay(boolean enabled) {
			return Component.text(String.format("Calculate Bonus from %s%s", mName, enabled ? " - Enabled" : " - Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		}

		public List<Component> getDisplayLore() {
			return Arrays.stream(mDescription.split("\n")).map(line -> (Component) Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList();
		}
	}

	private enum Equipment {
		MAINHAND(46, 52, 0, "Main Hand", EquipmentSlot.HAND, Slot.MAINHAND),
		OFFHAND(19, 25, 40, "Off Hand", EquipmentSlot.OFF_HAND, Slot.OFFHAND),
		HEAD(18, 26, 39, "Head", EquipmentSlot.HEAD, Slot.HEAD),
		CHEST(27, 35, 38, "Chest", EquipmentSlot.CHEST, Slot.CHEST),
		LEGS(36, 44, 37, "Legs", EquipmentSlot.LEGS, Slot.LEGS),
		FEET(45, 53, 36, "Feet", EquipmentSlot.FEET, Slot.FEET);

		private final int mLeftSlot;
		private final int mRightSlot;
		private final int mPlayerInventorySlot;
		private final String mName;
		private final EquipmentSlot mEquipmentSlot;
		private final Slot mSlot;
		private final ImmutableList<Component> mLore = ImmutableList.of(
			Component.text("Click here, then click an item to compare builds.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
			Component.text("Right click to restore the initial item.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

		Equipment(int leftSlot, int rightSlot, int playerInventorySlot, String name, EquipmentSlot equipmentSlot, Slot slot) {
			mLeftSlot = leftSlot;
			mRightSlot = rightSlot;
			mPlayerInventorySlot = playerInventorySlot;
			mName = name;
			mEquipmentSlot = equipmentSlot;
			mSlot = slot;
		}

		public Material getIcon() {
			return Material.ITEM_FRAME;
		}

		public Component getDisplay(boolean selected) {
			return Component.text(String.format("%s Slot%s", mName, selected ? " (Selected)" : ""), selected ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		}

		public ImmutableList<Component> getLore() {
			return mLore;
		}
	}

	private static final int REGION_SETTING_SLOT = 4;
	private static final ImmutableMap<ItemStatUtils.Region, ItemStack> REGION_ICONS = ImmutableMap.of(
		ItemStatUtils.Region.VALLEY, ItemUtils.parseItemStack("{id:\"minecraft:cyan_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"sc\",Color:3},{Pattern:\"mc\",Color:11},{Pattern:\"flo\",Color:15},{Pattern:\"bts\",Color:11},{Pattern:\"tts\",Color:11}]},HideFlags:63,display:{Name:'{\"text\":\"Calculation Region: King\\'s Valley\",\"italic\":false,\"bold\":true,\"color\":\"aqua\"}'}}}"),
		ItemStatUtils.Region.ISLES, ItemUtils.parseItemStack("{id:\"minecraft:green_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"gru\",Color:5},{Pattern:\"bo\",Color:13},{Pattern:\"mr\",Color:13},{Pattern:\"mc\",Color:5}]},HideFlags:63,display:{Name:'{\"text\":\"Calculation Region: Celsian Isles\",\"italic\":false,\"bold\":true,\"color\":\"green\"}'}}}"),
		ItemStatUtils.Region.RING, ItemUtils.parseItemStack("{id:\"minecraft:white_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:ss,Color:12},{Pattern:bts,Color:13},{Pattern:tts,Color:13},{Pattern:gra,Color:8},{Pattern:ms,Color:13},{Pattern:gru,Color:7},{Pattern:flo,Color:15},{Pattern:mc,Color:0}]},display:{Name:'{\"bold\":true,\"italic\":false,\"underlined\":false,\"color\":\"white\",\"text\":\"Calculation Region: Architect\\\\u0027s Ring\"}'}}}")
	);

	private static final int SWAP_EQUIPMENT_SET_SLOT = 49;
	private static final int INFUSION_SETTINGS_LEFT_SLOT = 12;
	private static final int INFUSION_SETTINGS_RIGHT_SLOT = 14;

	private final Settings mSettings = new Settings();
	private final Stats mLeftStats;
	private final Stats mRightStats;

	private final boolean mShowVanity;

	private boolean mSelectedRightEquipmentSet;
	private @Nullable Equipment mSelectedEquipmentsSlot = null;

	public PlayerItemStatsGUI(Player player) {
		this(player, null);
	}

	public PlayerItemStatsGUI(Player player, @Nullable Player otherPlayer) {
		super(player, 54, Component.text("Player Stats Calculator", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		mShowVanity = Plugin.getInstance().mVanityManager.getData(player).mGuiVanityEnabled;
		mLeftStats = new Stats(player, null, mSettings);
		mRightStats = new Stats(player, mLeftStats, mSettings);

		setEquipmentFromPlayer(false, player);
		if (otherPlayer != null) {
			setEquipmentFromPlayer(true, otherPlayer);
		}
		ItemStatUtils.Region region = Stream.of(mLeftStats.getMaximumRegion(false, ServerProperties.getRegion()), mRightStats.getMaximumRegion(false, ServerProperties.getRegion()))
			                              .max(Comparator.naturalOrder())
			                              .orElse(ServerProperties.getRegion());
		mLeftStats.mPlayerItemStats.setRegion(region);
		mRightStats.mPlayerItemStats.setRegion(region);
		generateInventory();
	}

	private void setEquipmentFromPlayer(boolean right, Player player) {
		Stats stats = right ? mRightStats : mLeftStats;

		stats.mEquipment.clear();
		stats.mDisplayedEquipment.clear();
		for (Equipment slot : Equipment.values()) {
			setEquipmentFromPlayer(stats, player, slot);
		}

		stats.mOriginalEquipment.clear();
		stats.mOriginalEquipment.putAll(stats.mEquipment);
		stats.mOriginalDisplayedEquipment.clear();
		stats.mOriginalDisplayedEquipment.putAll(stats.mDisplayedEquipment);
	}

	private void setEquipmentFromPlayer(Stats stats, Player player, Equipment slot) {
		ItemStack item = player.getInventory().getItem(slot.mEquipmentSlot);
		setEquipment(stats, slot, item);
		stats.mDisplayedEquipment.put(slot, getPlayerItemWithVanity(player, slot.mEquipmentSlot));
	}

	private @Nullable ItemStack getPlayerItemWithVanity(Player player, EquipmentSlot slot) {
		return getPlayerItemWithVanity(player, slot, mShowVanity);
	}

	public static @Nullable ItemStack getPlayerItemWithVanity(Player player, EquipmentSlot slot, boolean withVanity) {
		ItemStack item = player.getInventory().getItem(slot);
		if (item != null && item.getType() != Material.AIR && withVanity) {
			VanityManager.VanityData vanityData = Plugin.getInstance().mVanityManager.getData(player);
			if (vanityData.getEquipped(slot) != null) {
				ItemStack vanityItem = ItemUtils.clone(item);
				if (VanityManager.isInvisibleVanityItem(vanityData.getEquipped(slot))) {
					ItemMeta meta = vanityItem.getItemMeta();
					if (meta != null) {
						List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
						lore.add(0, Component.text("Invisibility vanity skin applied", NamedTextColor.GOLD));
						meta.lore(lore);
						vanityItem.setItemMeta(meta);
					}
				} else {
					VanityManager.applyVanity(vanityItem, vanityData, slot, false);
				}
				return vanityItem;
			}
		}
		return item;
	}

	private void setEquipment(Stats stats, Equipment slot, ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		item = ItemUtils.clone(item);
		stats.mEquipment.put(slot, item);
		stats.mDisplayedEquipment.put(slot, item);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClick() == ClickType.DOUBLE_CLICK) {
			return;
		}

		int slot = event.getSlot();
		Inventory inv = event.getClickedInventory();
		if (inv == null) {
			return;
		}

		if (inv.equals(mInventory)) {
			if (slot == SWAP_EQUIPMENT_SET_SLOT) {
				EnumMap<Equipment, ItemStack> leftEquipment = new EnumMap<>(mLeftStats.mEquipment);
				mLeftStats.mEquipment.clear();
				mLeftStats.mEquipment.putAll(mRightStats.mEquipment);
				mRightStats.mEquipment.clear();
				mRightStats.mEquipment.putAll(leftEquipment);
				EnumMap<Equipment, ItemStack> leftDisplayEquipment = new EnumMap<>(mLeftStats.mDisplayedEquipment);
				mLeftStats.mDisplayedEquipment.clear();
				mLeftStats.mDisplayedEquipment.putAll(mRightStats.mDisplayedEquipment);
				mRightStats.mDisplayedEquipment.clear();
				mRightStats.mDisplayedEquipment.putAll(leftDisplayEquipment);
				InfusionSetting leftInfusionSetting = mLeftStats.mInfusionSetting;
				mLeftStats.mInfusionSetting = mRightStats.mInfusionSetting;
				mRightStats.mInfusionSetting = leftInfusionSetting;
				generateInventory();
				return;
			}

			if (slot == REGION_SETTING_SLOT) {
				ItemStatUtils.Region region = mLeftStats.mPlayerItemStats.getRegion() == ItemStatUtils.Region.VALLEY ? ItemStatUtils.Region.ISLES
					                              : mLeftStats.mPlayerItemStats.getRegion() == ItemStatUtils.Region.ISLES ? ItemStatUtils.Region.RING
						                                : ItemStatUtils.Region.VALLEY;
				mLeftStats.mPlayerItemStats.setRegion(region);
				mRightStats.mPlayerItemStats.setRegion(region);
				generateInventory();
				return;
			}

			for (SecondaryStat stat : SecondaryStat.values()) {
				if (slot == stat.getSlot()) {
					if (!mSettings.mSecondaryStatEnabled.remove(stat)) {
						mSettings.mSecondaryStatEnabled.add(stat);
					}
					generateInventory();
					return;
				}
			}

			if (slot == INFUSION_SETTINGS_LEFT_SLOT || slot == INFUSION_SETTINGS_RIGHT_SLOT) {
				Stats stats = (slot == INFUSION_SETTINGS_LEFT_SLOT ? mLeftStats : mRightStats);
				stats.mInfusionSetting = InfusionSetting.values()[(stats.mInfusionSetting.ordinal() + (event.getClick().isLeftClick() ? 1 : InfusionSetting.values().length - 1)) % InfusionSetting.values().length];
				generateInventory();
				return;
			}

			for (Equipment equipment : Equipment.values()) {
				if (slot == equipment.mRightSlot) {
					if (event.isRightClick()) {
						mRightStats.mEquipment.put(equipment, mRightStats.mOriginalEquipment.get(equipment));
						mRightStats.mDisplayedEquipment.put(equipment, mRightStats.mOriginalDisplayedEquipment.get(equipment));
					} else {
						if (mSelectedEquipmentsSlot == equipment && mSelectedRightEquipmentSet) {
							mSelectedEquipmentsSlot = null;
						} else if (mRightStats.mEquipment.get(equipment) == null) {
							mSelectedEquipmentsSlot = equipment;
							mSelectedRightEquipmentSet = true;
						} else {
							mRightStats.mEquipment.remove(equipment);
							mRightStats.mDisplayedEquipment.remove(equipment);
						}
					}

					generateInventory();
					return;
				}
				if (slot == equipment.mLeftSlot) {
					if (event.isRightClick()) {
						mLeftStats.mEquipment.put(equipment, mLeftStats.mOriginalEquipment.get(equipment));
						mLeftStats.mDisplayedEquipment.put(equipment, mLeftStats.mOriginalDisplayedEquipment.get(equipment));
					} else {
						if (mSelectedEquipmentsSlot == equipment && !mSelectedRightEquipmentSet) {
							mSelectedEquipmentsSlot = null;
						} else if (mLeftStats.mEquipment.get(equipment) == null) {
							mSelectedEquipmentsSlot = equipment;
							mSelectedRightEquipmentSet = false;
						} else {
							mLeftStats.mEquipment.remove(equipment);
							mLeftStats.mDisplayedEquipment.remove(equipment);
						}
					}

					generateInventory();
					return;
				}
			}
		} else {
			ItemStack clickedItem = inv.getItem(slot);
			if (clickedItem != null && clickedItem.getType() != Material.AIR) {
				ItemStack item = new ItemStack(clickedItem);
				if (ShulkerEquipmentListener.isEquipmentBox(item)
					&& item.getItemMeta() instanceof BlockStateMeta meta
					&& meta.getBlockState() instanceof ShulkerBox shulker) {
					Stats stats;
					if (event.getClick().isShiftClick()) {
						stats = event.getClick().isLeftClick() ? mRightStats : mLeftStats;
					} else if (mSelectedEquipmentsSlot != null) {
						stats = mSelectedRightEquipmentSet ? mRightStats : mLeftStats;
					} else {
						return;
					}
					mSelectedEquipmentsSlot = null;

					for (Equipment equipment : Equipment.values()) {
						Integer shulkerSlot = ShulkerEquipmentListener.getShulkerSlot(equipment.mPlayerInventorySlot);
						if (shulkerSlot == null) {
							continue;
						}
						ItemStack shulkerItem = shulker.getInventory().getItem(shulkerSlot);
						if (shulkerItem != null && shulkerItem.getType() != Material.AIR) {
							setEquipment(stats, equipment, shulkerItem);
						}
					}
					generateInventory();
					return;
				}
				Equipment targetSlot = null;
				boolean targetRightSet = false;
				if (event.getClick().isShiftClick()) {
					targetRightSet = event.getClick().isLeftClick();
					for (Equipment equipment : new Equipment[] {Equipment.HEAD, Equipment.CHEST, Equipment.LEGS, Equipment.FEET}) {
						if (isValid(equipment, item.getType())) {
							targetSlot = equipment;
							break;
						}
					}
					if (targetSlot == null) {
						if (ItemStatUtils.hasAttributeInSlot(item, Slot.OFFHAND)) {
							targetSlot = Equipment.OFFHAND;
						} else {
							targetSlot = Equipment.MAINHAND;
						}
					}
				} else if (mSelectedEquipmentsSlot != null) {
					if (isValid(mSelectedEquipmentsSlot, item.getType())) {
						targetSlot = mSelectedEquipmentsSlot;
						targetRightSet = mSelectedRightEquipmentSet;
					}
				}
				if (targetSlot != null) {
					setEquipment(targetRightSet ? mRightStats : mLeftStats, targetSlot, item);
					mSelectedEquipmentsSlot = null;
					generateInventory();
				}
			}
		}
	}

	private boolean isValid(Equipment equipment, Material material) {
		return equipment == Equipment.MAINHAND
			|| equipment == Equipment.OFFHAND
			|| equipment.mEquipmentSlot == material.getEquipmentSlot();
	}

	private static ItemStack getCleanItem(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);
		return item;
	}

	private static ItemStack makePlaceholderItem(Equipment equipment, boolean selected) {
		ItemStack empty = getCleanItem(new ItemStack(equipment.getIcon(), 1));
		ItemMeta meta = empty.getItemMeta();
		meta.displayName(equipment.getDisplay(selected));
		if (selected) {
			meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
		}
		empty.setItemMeta(meta);
		empty.lore(equipment.getLore());
		return empty;
	}

	private static @Nullable ItemStack getWarningIcon(Stats stats) {
		List<Component> warnings = new ArrayList<>();
		if (stats.getRegionScaling(stats.mPlayer, false) > 0) {
			warnings.add(Component.text("Build has equipment of a later region.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		if (stats.getRegionScaling(stats.mPlayer, true) > 0) {
			warnings.add(Component.text("Build has mainhand of a later region.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		if (stats.get(EnchantmentType.CURSE_OF_CORRUPTION) > 1) {
			warnings.add(Component.text("Build has more than one Curse of Corruption item.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		if (stats.get(EnchantmentType.TWO_HANDED) > 0
			&& stats.getItem(Equipment.OFFHAND) != null
			&& ItemStatUtils.getEnchantmentLevel(stats.getItem(Equipment.OFFHAND), EnchantmentType.WEIGHTLESS) == 0) {
			warnings.add(Component.text("Build has a Two Handed item, but a non-Weightless offhand.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		if (warnings.isEmpty()) {
			return null;
		}
		ItemStack item = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Warning!", NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		meta.lore(warnings);
		item.setItemMeta(meta);
		ItemUtils.setPlainName(item);
		return item;
	}

	private void generateInventory() {

		for (Stats stats : new Stats[] {mLeftStats, mRightStats}) {
			stats.mStatCache.clear();
			stats.mPlayerItemStats.updateStats(stats.getItem(Equipment.MAINHAND), stats.getItem(Equipment.OFFHAND),
				stats.getItem(Equipment.HEAD), stats.getItem(Equipment.CHEST), stats.getItem(Equipment.LEGS), stats.getItem(Equipment.FEET), (Player) mInventory.getHolder(), true);
		}

		ItemStack swapItem = new ItemStack(Material.ARMOR_STAND, 1);
		ItemMeta swapItemMeta = swapItem.getItemMeta();
		swapItemMeta.displayName(Component.text("Swap Equipment Sets", NamedTextColor.WHITE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		swapItem.setItemMeta(swapItemMeta);
		mInventory.setItem(SWAP_EQUIPMENT_SET_SLOT, swapItem);

		mInventory.setItem(REGION_SETTING_SLOT, REGION_ICONS.get(mLeftStats.mPlayerItemStats.getRegion()));

		for (StatItem statItem : STAT_ITEMS) {
			mInventory.setItem(statItem.mSlot, statItem.getDisplay(mLeftStats, mRightStats.mEquipment.isEmpty() && mRightStats.mInfusionSetting == InfusionSetting.DISABLED ? null : mRightStats));
		}

		for (SecondaryStat stat : SecondaryStat.values()) {
			ItemStack item = getCleanItem(new ItemStack(stat.getIcon(), 1));
			ItemMeta meta = item.getItemMeta();
			if (mSettings.mSecondaryStatEnabled.contains(stat)) {
				meta.displayName(stat.getDisplay(true));
				meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
			} else {
				meta.displayName(stat.getDisplay(false));
			}
			meta.lore(stat.getDisplayLore());
			item.setItemMeta(meta);
			mInventory.setItem(stat.getSlot(), item);
		}

		BiFunction<Boolean, InfusionSetting, ItemStack> makeInfusionSettingsItem = (left, selectedSetting) -> {
			ItemStack item = new ItemStack(Material.ANVIL);
			ItemMeta meta = item.getItemMeta();
			meta.displayName(Component.text("Infusion Settings", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("Click to select how infusion are", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("treated by this calculator.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Selected option:", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			for (InfusionSetting setting : InfusionSetting.values()) {
				String line;
				if (setting == InfusionSetting.DISABLED && !left) {
					line = "Use option chosen on the left build";
				} else {
					line = setting.mDescription;
				}
				boolean selected = setting == selectedSetting;
				lore.add(Component.text((selected ? "+ " : "- ") + line, selected ? NamedTextColor.GREEN : NamedTextColor.GRAY)
					         .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, selected));
			}
			meta.lore(lore);
			item.setItemMeta(meta);
			return item;
		};
		mInventory.setItem(INFUSION_SETTINGS_LEFT_SLOT, makeInfusionSettingsItem.apply(true, mLeftStats.mInfusionSetting));
		mInventory.setItem(INFUSION_SETTINGS_RIGHT_SLOT, makeInfusionSettingsItem.apply(false, mRightStats.mInfusionSetting));

		mInventory.setItem(28, getWarningIcon(mLeftStats));
		mInventory.setItem(34, getWarningIcon(mRightStats));

		// Set plain name/lore tags. This must be before equipment items are added to not break vanity.
		for (ItemStack item : mInventory) {
			if (item != null) {
				ItemUtils.setPlainTag(item);
			}
		}

		for (Equipment equipment : Equipment.values()) {
			ItemStack leftItem = mLeftStats.mDisplayedEquipment.get(equipment);
			mInventory.setItem(equipment.mLeftSlot, leftItem != null && leftItem.getType() != Material.AIR ? leftItem
				                                        : makePlaceholderItem(equipment, equipment == mSelectedEquipmentsSlot && !mSelectedRightEquipmentSet));

			ItemStack rightItem = mRightStats.mDisplayedEquipment.get(equipment);
			mInventory.setItem(equipment.mRightSlot, rightItem != null && rightItem.getType() != Material.AIR ? rightItem
				                                         : makePlaceholderItem(equipment, equipment == mSelectedEquipmentsSlot && mSelectedRightEquipmentSet));
		}

		GUIUtils.fillWithFiller(mInventory, Material.BLACK_STAINED_GLASS_PANE);

	}
}
