package com.playmonumenta.plugins.itemstats.gui;

import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.attributes.Armor;
import com.playmonumenta.plugins.itemstats.enchantments.Protection;
import com.playmonumenta.plugins.itemstats.enchantments.RegionScalingDamageDealt;
import com.playmonumenta.plugins.itemstats.enchantments.RegionScalingDamageTaken;
import com.playmonumenta.plugins.itemstats.enchantments.SecondWind;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.itemstats.enchantments.WorldlyProtection;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.infusions.Carapace;
import com.playmonumenta.plugins.itemstats.infusions.Choler;
import com.playmonumenta.plugins.itemstats.infusions.Decapitation;
import com.playmonumenta.plugins.itemstats.infusions.Execution;
import com.playmonumenta.plugins.itemstats.infusions.Fueled;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.itemstats.infusions.Tenacity;
import com.playmonumenta.plugins.itemstats.infusions.Vengeful;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

class PSGUIStats {
	final Player mPlayer;
	final ItemStatManager.PlayerItemStats mPlayerItemStats = new ItemStatManager.PlayerItemStats(Region.VALLEY);
	final EnumMap<PSGUIEquipment, ItemStack> mEquipment = new EnumMap<>(PSGUIEquipment.class);
	final EnumMap<PSGUIEquipment, ItemStack> mDisplayedEquipment = new EnumMap<>(PSGUIEquipment.class);
	final EnumMap<PSGUIEquipment, ItemStack> mOriginalEquipment = new EnumMap<>(PSGUIEquipment.class);
	final EnumMap<PSGUIEquipment, ItemStack> mOriginalDisplayedEquipment = new EnumMap<>(PSGUIEquipment.class);
	final EnumMap<PSGUIStat, Double> mStatCache = new EnumMap<>(PSGUIStat.class);
	final @Nullable PSGUIStats mMainStats;
	final PlayerItemStatsGUI.Settings mSettings;
	PlayerItemStatsGUI.InfusionSetting mInfusionSetting = PlayerItemStatsGUI.InfusionSetting.DISABLED;

	PSGUIStats(Player player, @Nullable PSGUIStats mainStats, PlayerItemStatsGUI.Settings settings) {
		mPlayer = player;
		mMainStats = mainStats;
		mSettings = settings;
	}

	double get(AttributeType attr) {
		return mPlayerItemStats.getItemStats().get(attr.getItemStat());
	}

	double get(EnchantmentType ench) {
		return mPlayerItemStats.getItemStats().get(ench.getItemStat());
	}

	@Nullable ItemStack getItem(PSGUIEquipment slot) {
		ItemStack result = mEquipment.get(slot);
		if ((result == null || result.getType() == Material.AIR) && mMainStats != null) {
			return mMainStats.mEquipment.get(slot);
		}
		return result;
	}

	private PlayerItemStatsGUI.InfusionSetting getInfusionSetting() {
		if (mInfusionSetting == PlayerItemStatsGUI.InfusionSetting.DISABLED && mMainStats != null) {
			return mMainStats.mInfusionSetting;
		}
		return mInfusionSetting;
	}

	double getInfusion(InfusionType infusion) {
		if (infusion == InfusionType.SHATTERED) {
			return mPlayerItemStats.getItemStats().get(infusion.getItemStat());
		}
		PlayerItemStatsGUI.InfusionSetting setting = getInfusionSetting();
		if (setting == PlayerItemStatsGUI.InfusionSetting.ENABLED || setting == PlayerItemStatsGUI.InfusionSetting.ENABLED_FULL) {
			if (setting != PlayerItemStatsGUI.InfusionSetting.ENABLED_FULL
				    && PlayerItemStatsGUI.InfusionSetting.CONDITIONAL_DELVE_INFUSIONS.contains(infusion)) {
				return 0;
			}
			double value = mPlayerItemStats.getItemStats().get(infusion.getItemStat());
			if (infusion.isDelveInfusion() && value > 0) {
				return Math.min(value, DelveInfusionUtils.MAX_LEVEL + 1);
			}
			return value;
		} else if (setting.mInfusionType == infusion) {
			if (setting == PlayerItemStatsGUI.InfusionSetting.VITALITY) {
				return 20;
			}
			return 24;
		} else {
			if (setting == PlayerItemStatsGUI.InfusionSetting.VITALITY && infusion == InfusionType.TENACITY) {
				return 4;
			}
			return 0;
		}
	}

	double getMainhandAttributeAmount(AttributeType type, Operation operation) {
		ItemStack mainhand = getItem(PSGUIEquipment.MAINHAND);
		return mainhand == null ? 0 : ItemStatUtils.getAttributeAmount(mainhand, type, operation, Slot.MAINHAND);
	}

	double getAttributeAmount(AttributeType type, Operation operation) {
		double result = 0;
		for (PSGUIEquipment slot : PSGUIEquipment.values()) {
			ItemStack item = getItem(slot);
			result += ItemStatUtils.getAttributeAmount(item, type, operation, slot.mSlot);
		}
		return result;
	}

	double getAttributeAmount(AttributeType type, double base) {
		return getAttributeAmount(type, base, 0);
	}

	double getAttributeAmount(AttributeType type, double base, double additionalModifier) {
		return (base + getAttributeAmount(type, Operation.ADD)) * (1 + getAttributeAmount(type, Operation.MULTIPLY) + additionalModifier);
	}

	Region getMaximumRegion(boolean mainhand, Region defaultRegion) {
		return (mainhand ? Stream.of(PSGUIEquipment.MAINHAND) : Arrays.stream(PSGUIEquipment.values()).filter(slot -> slot != PSGUIEquipment.MAINHAND))
			       .map(slot -> ItemStatUtils.getRegion(getItem(slot)))
			       .filter(region -> region == Region.VALLEY || region == Region.ISLES || region == Region.RING)
			       .max(Comparator.naturalOrder())
			       .orElse(defaultRegion);
	}

	int getRegionScaling(Player player, boolean mainhand) {
		return (mainhand ? Stream.of(PSGUIEquipment.MAINHAND) : Arrays.stream(PSGUIEquipment.values()).filter(slot -> slot != PSGUIEquipment.MAINHAND))
				.mapToInt(slot -> (int) ItemStatManager.getEffectiveRegionScaling(player, getItem(slot), mPlayerItemStats.getRegion(), false, 0, 1, 2))
				.max()
				.orElse(0);
	}

	int getShatteredItemEquipped() {
		int totalLevel = 0;
		for (PSGUIEquipment slot: PSGUIEquipment.values()) {
			ItemStack item = getItem(slot);
			if (item != null) {
				totalLevel += Shattered.getShatterLevel(item);
			}
		}
		return totalLevel;
	}

	double getAdditiveDamageDealtMultiplier() {
		double result = 1.0;

		result += Choler.getDamageDealtMultiplier(getInfusion(InfusionType.CHOLER)) - 1;
		result += Execution.getDamageDealtMultiplier(getInfusion(InfusionType.EXECUTION)) - 1;
		result += Vengeful.getDamageDealtMultiplier(getInfusion(InfusionType.VENGEFUL)) - 1;
		result += Decapitation.getDamageDealtMultiplier(getInfusion(InfusionType.DECAPITATION)) - 1;

		return result;
	}

	double getMultiplicativeDamageDealtMultiplier() {
		double result = 1.0;

		if (getInfusion(InfusionType.SHATTERED) > 0) {
			result *= (1 - Shattered.getMultiplier(getShatteredItemEquipped()));
		}

		result *= RegionScalingDamageDealt.DAMAGE_DEALT_MULTIPLIER[getRegionScaling(mPlayer, true)];

		return result;
	}

	double getDamageTakenMultiplier(@Nullable Protection protection, @Nullable Protection inverseProtection) {
		boolean region2 = mPlayerItemStats.getRegion().compareTo(Region.ISLES) >= 0;

		double damageMultiplier = 1;
		if (protection != null && inverseProtection != null) {
			double armor = get(AttributeType.ARMOR);
			double agility = get(AttributeType.AGILITY);

			double armorBonus = 0;
			double agilityBonus = 0;
			for (PSGUISecondaryStat stat : mSettings.mSecondaryStatEnabled) {
				if (stat.isArmorModifier()) {
					armorBonus += Shielding.ARMOR_BONUS_PER_LEVEL * get(stat.getEnchantmentType());
				} else {
					agilityBonus += Shielding.ARMOR_BONUS_PER_LEVEL * get(stat.getEnchantmentType());
				}
			}

			boolean adaptability = get(EnchantmentType.ADAPTABILITY) > 0;
			double epf = (protection.getEPF() * get(protection.getEnchantmentType()))
				             + (inverseProtection.getEPF() * get(inverseProtection.getEnchantmentType()));

			damageMultiplier = Armor.getDamageMultiplier(armor, armorBonus, agility, agilityBonus,
				Armor.getSecondaryEnchantCap(region2), adaptability, epf, protection.getType().isEnvironmental());
		}

		// when Steadfast is enabled, also include Second Wind in calculation
		if (mSettings.mSecondaryStatEnabled.contains(PSGUISecondaryStat.STEADFAST)) {
			damageMultiplier *= SecondWind.getDamageMultiplier(get(EnchantmentType.SECOND_WIND));
		}

		damageMultiplier *= WorldlyProtection.getDamageMultiplier(get(EnchantmentType.WORLDLY_PROTECTION), mPlayerItemStats.getRegion());

		if (protection == null || protection.getEnchantmentType() != EnchantmentType.FEATHER_FALLING) {
			damageMultiplier *= RegionScalingDamageTaken.DAMAGE_TAKEN_MULTIPLIER[getRegionScaling(mPlayer, false)];
		}

		if (getInfusion(InfusionType.SHATTERED) > 0) {
			damageMultiplier *= (1 + Shattered.getMultiplier(getShatteredItemEquipped()));
		}

		damageMultiplier *= Tenacity.getDamageTakenMultiplier(getInfusion(InfusionType.TENACITY));
		damageMultiplier *= Carapace.getDamageTakenMultiplier(getInfusion(InfusionType.CARAPACE));
		damageMultiplier *= Fueled.getDamageTakenMultiplier(getInfusion(InfusionType.FUELED));

		return damageMultiplier;
	}

}
