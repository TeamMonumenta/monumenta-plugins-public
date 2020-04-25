package com.playmonumenta.plugins.enchantments.evasions;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.other.EvasionEnchant;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.BossUtils.BossAbilityDamageEvent;

public class AbilityEvasion implements BaseEnchantment {

	private static String PROPERTY_NAME = ChatColor.GRAY + "Ability Evasion";
	private static final int EVASION_CAP = 20;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onBossDamage(Plugin plugin, Player player, int level, BossAbilityDamageEvent event) {
		EvasionEnchant evasion = AbilityManager.getManager().getPlayerAbility(player, EvasionEnchant.class);
		if (evasion != null) {
			// Evasion and Ability Evasion add up, so calculate the effective cap accordingly
			evasion.mCounter += Math.min(level * 2,
					EVASION_CAP - PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, Evasion.class));
		}
	}

}
