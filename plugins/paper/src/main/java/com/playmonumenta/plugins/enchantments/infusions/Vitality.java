package com.playmonumenta.plugins.enchantments.infusions;

import java.util.Collection;
import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class Vitality implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Vitality";
	public static final String MODIFIER = "VitalityMod";
	private static final double HP_PCT_PER_LEVEL = 0.01;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		if (ServerProperties.getInfusionsEnabled()) {
			return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
		} else {
			return EnumSet.noneOf(ItemSlot.class);
		}
	}

	@Override
	public void applyProperty(Plugin plugin, Player player, int level) {
		if (player != null) {
			removeProperty(plugin, player);
			double healthBoostPct = HP_PCT_PER_LEVEL * level;
			AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			if (maxHealth != null) {
				AttributeModifier mod = new AttributeModifier(MODIFIER, healthBoostPct,
				                                              AttributeModifier.Operation.MULTIPLY_SCALAR_1);
				maxHealth.addModifier(mod);
			}
		}
	}

	@Override
	public void removeProperty(Plugin plugin, Player player) {
		AttributeInstance ai = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		if (ai != null) {
			Collection<AttributeModifier> modTemp = ai.getModifiers();
			for (AttributeModifier mod : modTemp) {
				if (mod != null && mod.getName().equals(MODIFIER)) {
					ai.removeModifier(mod);
				}
			}
		}
	}
}
