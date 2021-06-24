package com.playmonumenta.plugins.enchantments.abilities;

import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;

import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.ItemUtils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;



public abstract class BaseAbilityEnchantment implements BaseEnchantment {
	private final String mPropertyName;
	private final EnumSet<ItemSlot> mSlots;

	public BaseAbilityEnchantment(String propertyName, EnumSet<ItemSlot> validSlots) {
		mPropertyName = propertyName;
		mSlots = validSlots;
	}

	@Override
	public String getProperty() {
		return mPropertyName;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return mSlots;
	}

	@Override
	public boolean canNegativeLevel() {
		return true;
	}

	@Override
	public int getPlayerItemLevel(
		@NotNull ItemStack itemStack,
		@NotNull Player player,
		@NotNull ItemSlot itemSlot
	) {
		@NotNull List<@NotNull String> plainLoreLines = ItemUtils.getPlainLore(itemStack);
		for (@NotNull String plainLore : plainLoreLines) {
			if (plainLore.contains(getProperty())) {
				return parseValue(plainLore);
			}
		}

		return 0;
	}

	private int parseValue(String loreLine) {
		//Whether effect is being added to or subtracted
		boolean add = loreLine.contains("+");
		//Parse the value from the line

		loreLine = loreLine.split("\\+|-")[1];
		@SuppressWarnings("resource")
		Scanner s = new Scanner(loreLine).useDelimiter("\\D+");
		if (s.hasNextInt()) {
			int sint = s.nextInt();
			//If it's a negative effect
			if (!add) {
				sint = sint * -1;
			}
			s.close();
			return sint;
		}

		s.close();



		// TODO: Error handling if enchant is malformed

		return 0;
	}

	public static float getRadius(Player player, float baseRadius, Class<? extends BaseEnchantment> c) {
		int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, c);
		return baseRadius * (float) ((level / 100.0) + 1);
	}

	public static float getExtraDamage(Player player, Class<? extends BaseEnchantment> c) {
		int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, c);
		return level;
	}

	public static float getExtraDuration(Player player, Class<? extends BaseEnchantment> c) {
		int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, c);
		return level * 20;
	}

	public static float getExtraPercentDamage(Player player, Class<? extends BaseEnchantment> c, float baseDamage) {
		int percentage = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, c);
		return baseDamage * (1 + (percentage / 100f));
	}

	public static float getCooldown(Player player, float baseCooldown, Class<? extends BaseEnchantment> c) {
		int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, c);
		return baseCooldown * (float) ((level / 100.0) + 1);
	}

	public static float getLevel(Player player, Class<? extends BaseEnchantment> c) {
		int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, c);
		return level;
	}

	public static float getSpellDamage(Player player, int baseDamage) {
		return getSpellDamage(player, (float)baseDamage);
	}

	public static float getSpellDamage(Player player, float baseDamage) {
		int percentage = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, SpellPower.class);
		return baseDamage * (1 + (percentage / 100f));
	}

	public static float getExtraPercentHealing(Player player, Class<? extends BaseEnchantment> c, float baseHealing) {
		int percentage = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, c);
		return baseHealing * (1 + (percentage / 100f));
	}

	public static float getExtraPercent(Player player, Class<? extends BaseEnchantment> c, float base) {
		int percentage = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, c);
		return base * (1 + (percentage / 100f));
	}
}
