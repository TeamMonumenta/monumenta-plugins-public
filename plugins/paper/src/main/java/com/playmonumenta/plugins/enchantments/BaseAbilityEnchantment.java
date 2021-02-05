package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.tracking.PlayerTracking;

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
	public EnumSet<ItemSlot> validSlots() {
		return mSlots;
	}

	@Override
	public boolean negativeLevelsAllowed() {
		return true;
	}

	@Override
	public int getLevelFromItem(ItemStack item) {
		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				List<String> lore = meta.getLore();
				if (lore != null && !lore.isEmpty()) {
					for (String loreEntry : lore) {
						if (loreEntry.contains(mPropertyName)) {
							return parseValue(loreEntry);
						}
					}
				}
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

	public static float getCooldown(Player player, float baseCooldown, Class<? extends BaseEnchantment> c) {
		int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, c);
		return baseCooldown * (float) ((level / 100.0) + 1);
	}

	public static float getLevel(Player player, Class<? extends BaseEnchantment> c) {
		int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, c);
		return level;
	}

	//Float version
	public static float getSpellDamage(Player player, float baseDamage) {
		int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, SpellDamage.class);
		return baseDamage * (float) ((level / 100.0) + 1);
	}

	//Int to float version
	public static float getSpellDamage(Player player, int baseDamage) {
		int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, SpellDamage.class);
		return baseDamage * (float) ((level / 100.0) + 1);
	}
}
