package com.playmonumenta.plugins.enchantments.evasions;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.other.EvasionEnchant;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.BossUtils.BossAbilityDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Evasion implements BaseEnchantment {

	private static String PROPERTY_NAME = ChatColor.GRAY + "Evasion";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onHurtByEntity(Plugin plugin, Player player, int level, EntityDamageByEntityEvent event) {
		EvasionEnchant evasion = (EvasionEnchant) AbilityManager.getManager().getPlayerAbility(player, EvasionEnchant.class);
		if (evasion != null && EntityUtils.getRealFinalDamage(event) > 0) {
			evasion.mChance += (8 * level);
		}
	}

	@Override
	public void onBossDamage(Plugin plugin, Player player, int level, BossAbilityDamageEvent event) {
		EvasionEnchant evasion = (EvasionEnchant) AbilityManager.getManager().getPlayerAbility(player, EvasionEnchant.class);
		if (evasion != null) {
			evasion.mChance += (8 * level);
		}
	}

}
