package com.playmonumenta.plugins.itemstats.gui;

import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.attributes.Armor;
import com.playmonumenta.plugins.itemstats.enchantments.Protection;
import com.playmonumenta.plugins.itemstats.enchantments.RegionScalingDamageDealt;
import com.playmonumenta.plugins.itemstats.enchantments.RegionScalingDamageTaken;
import com.playmonumenta.plugins.itemstats.enchantments.SecondWind;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.itemstats.enchantments.WorldlyProtection;
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
	final ItemStatManager.PlayerItemStats mPlayerItemStats = new ItemStatManager.PlayerItemStats(ItemStatUtils.Region.VALLEY);
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

	double get(ItemStatUtils.AttributeType attr) {
		return mPlayerItemStats.getItemStats().get(attr.getItemStat());
	}

	double get(ItemStatUtils.EnchantmentType ench) {
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

	double getInfusion(ItemStatUtils.InfusionType infusion) {
		if (infusion == ItemStatUtils.InfusionType.SHATTERED) {
			return mPlayerItemStats.getItemStats().get(infusion.getItemStat());
		}
		PlayerItemStatsGUI.InfusionSetting setting = getInfusionSetting();
		if (setting == PlayerItemStatsGUI.InfusionSetting.ENABLED || setting == PlayerItemStatsGUI.InfusionSetting.ENABLED_FULL) {
			if (setting != PlayerItemStatsGUI.InfusionSetting.ENABLED_FULL
				    && PlayerItemStatsGUI.InfusionSetting.CONDITIONAL_DELVE_INFUSIONS.contains(infusion)) {
				return 0;
			}
			double value = mPlayerItemStats.getItemStats().get(infusion.getItemStat());
			if (ItemStatUtils.InfusionType.DELVE_INFUSIONS.contains(infusion) && infusion != ItemStatUtils.InfusionType.UNDERSTANDING) {
				return DelveInfusionUtils.getModifiedLevel((int) value, (int) mPlayerItemStats.getItemStats().get(ItemStatUtils.InfusionType.UNDERSTANDING.getItemStat()));
			}
			return value;
		} else if (setting.mInfusionType == infusion) {
			if (setting == PlayerItemStatsGUI.InfusionSetting.VITALITY) {
				return 20;
			}
			return 24;
		} else {
			if (setting == PlayerItemStatsGUI.InfusionSetting.VITALITY && infusion == ItemStatUtils.InfusionType.TENACITY) {
				return 4;
			}
			return 0;
		}
	}

	double getMainhandAttributeAmount(ItemStatUtils.AttributeType type, ItemStatUtils.Operation operation) {
		ItemStack mainhand = getItem(PSGUIEquipment.MAINHAND);
		return mainhand == null ? 0 : ItemStatUtils.getAttributeAmount(mainhand, type, operation, ItemStatUtils.Slot.MAINHAND);
	}

	double getAttributeAmount(ItemStatUtils.AttributeType type, ItemStatUtils.Operation operation) {
		double result = 0;
		for (PSGUIEquipment slot : PSGUIEquipment.values()) {
			ItemStack item = getItem(slot);
			result += ItemStatUtils.getAttributeAmount(item, type, operation, slot.mSlot);
		}
		return result;
	}

	double getAttributeAmount(ItemStatUtils.AttributeType type, double base) {
		return getAttributeAmount(type, base, 0);
	}

	double getAttributeAmount(ItemStatUtils.AttributeType type, double base, double additionalModifier) {
		return (base + getAttributeAmount(type, ItemStatUtils.Operation.ADD)) * (1 + getAttributeAmount(type, ItemStatUtils.Operation.MULTIPLY) + additionalModifier);
	}

	ItemStatUtils.Region getMaximumRegion(boolean mainhand, ItemStatUtils.Region defaultRegion) {
		return (mainhand ? Stream.of(PSGUIEquipment.MAINHAND) : Arrays.stream(PSGUIEquipment.values()).filter(slot -> slot != PSGUIEquipment.MAINHAND))
			       .map(slot -> ItemStatUtils.getRegion(getItem(slot)))
			       .filter(region -> region == ItemStatUtils.Region.VALLEY || region == ItemStatUtils.Region.ISLES || region == ItemStatUtils.Region.RING)
			       .max(Comparator.naturalOrder())
			       .orElse(defaultRegion);
	}

	int getRegionScaling(Player player, boolean mainhand) {
		return (mainhand ? Stream.of(PSGUIEquipment.MAINHAND) : Arrays.stream(PSGUIEquipment.values()).filter(slot -> slot != PSGUIEquipment.MAINHAND))
			       .mapToInt(slot -> (int) ItemStatManager.getEffectiveRegionScaling(player, getItem(slot), mPlayerItemStats.getRegion(), 0, 1, 2))
			       .max()
			       .orElse(0);
	}

	boolean hasMaxShatteredItemEquipped() {
		for (PSGUIEquipment slot : PSGUIEquipment.values()) {
			ItemStack item = getItem(slot);
			if (item != null && Shattered.isMaxShatter(item)) {
				return true;
			}
		}
		return false;
	}

	double getDamageDealtMultiplier() {

		double result = 1.0;

		if (getInfusion(ItemStatUtils.InfusionType.SHATTERED) > 0) {
			result *= Shattered.getDamageDealtMultiplier(hasMaxShatteredItemEquipped());
		}

		result *= RegionScalingDamageDealt.DAMAGE_DEALT_MULTIPLIER[getRegionScaling(mPlayer, true)];

		result *= Choler.getDamageDealtMultiplier(getInfusion(ItemStatUtils.InfusionType.CHOLER));
		result *= Execution.getDamageDealtMultiplier(getInfusion(ItemStatUtils.InfusionType.EXECUTION));
		result *= Vengeful.getDamageDealtMultiplier(getInfusion(ItemStatUtils.InfusionType.VENGEFUL));
		result *= Decapitation.getDamageDealtMultiplier(getInfusion(ItemStatUtils.InfusionType.DECAPITATION));

		return result;
	}

	double getDamageTakenMultiplier(@Nullable Protection protection, @Nullable Protection inverseProtection) {
		boolean region2 = mPlayerItemStats.getRegion().compareTo(ItemStatUtils.Region.ISLES) >= 0;

		double damageMultiplier = 1;
		if (protection != null && inverseProtection != null) {
			double armor = get(ItemStatUtils.AttributeType.ARMOR);
			double agility = get(ItemStatUtils.AttributeType.AGILITY);

			double armorBonus = 0;
			double agilityBonus = 0;
			for (PSGUISecondaryStat stat : mSettings.mSecondaryStatEnabled) {
				if (stat.isArmorModifier()) {
					armorBonus += Shielding.ARMOR_BONUS_PER_LEVEL * get(stat.getEnchantmentType());
				} else {
					agilityBonus += Shielding.ARMOR_BONUS_PER_LEVEL * get(stat.getEnchantmentType());
				}
			}

			boolean adaptability = get(ItemStatUtils.EnchantmentType.ADAPTABILITY) > 0;
			double epf = (protection.getEPF() * get(protection.getEnchantmentType()))
				             + (inverseProtection.getEPF() * get(inverseProtection.getEnchantmentType()));

			damageMultiplier = Armor.getDamageMultiplier(armor, armorBonus, agility, agilityBonus,
				Armor.getSecondaryEnchantCap(region2), adaptability, epf, protection.getType().isEnvironmental());
		}

		// when Steadfast is enabled, also include Second Wind in calculation
		if (mSettings.mSecondaryStatEnabled.contains(PSGUISecondaryStat.STEADFAST)) {
			damageMultiplier *= SecondWind.getDamageMultiplier(get(ItemStatUtils.EnchantmentType.SECOND_WIND));
		}

		damageMultiplier *= WorldlyProtection.getDamageMultiplier(get(ItemStatUtils.EnchantmentType.WORLDLY_PROTECTION), mPlayerItemStats.getRegion());

		if (protection == null || protection.getEnchantmentType() != ItemStatUtils.EnchantmentType.FEATHER_FALLING) {
			damageMultiplier *= RegionScalingDamageTaken.DAMAGE_TAKEN_MULTIPLIER[getRegionScaling(mPlayer, false)];
		}

		if (getInfusion(ItemStatUtils.InfusionType.SHATTERED) > 0) {
			damageMultiplier *= Shattered.getDamageTakenMultiplier(hasMaxShatteredItemEquipped());
		}

		damageMultiplier *= Tenacity.getDamageTakenMultiplier(getInfusion(ItemStatUtils.InfusionType.TENACITY));
		damageMultiplier *= Carapace.getDamageTakenMultiplier(getInfusion(ItemStatUtils.InfusionType.CARAPACE));
		damageMultiplier *= Fueled.getDamageTakenMultiplier(getInfusion(ItemStatUtils.InfusionType.FUELED));

		return damageMultiplier;
	}

}
