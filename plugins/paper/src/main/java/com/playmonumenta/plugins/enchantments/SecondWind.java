package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.effects.PercentDamageReceived;

public class SecondWind implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Second Wind";
	private static final String DAMAGE_RESIST_NAME =  "SecondWindDamageReduction";
	private static final double DAMAGE_RESIST = 0.1;
	private static final double HEALTH_LIMIT = 0.5;
	
	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		double hp = player.getHealth() / player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		if (hp <= HEALTH_LIMIT) {
			plugin.mEffectManager.addEffect(player, DAMAGE_RESIST_NAME, new PercentDamageReceived(20, DAMAGE_RESIST * level));
		}
	}
}
