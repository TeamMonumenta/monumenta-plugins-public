package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class RegionScalingDamageTaken implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.DARK_GRAY + "Celsian Isles : ";

	private static final String SPEED_EFFECT_NAME = "RegionScalingPercentSpeedEffect";
	private static final double SPEED_EFFECT = -0.1;

	private static final double DAMAGE_TAKEN_MULTIPLIER = 3;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public int getPlayerItemLevel(ItemStack itemStack, Player player, ItemSlot itemSlot) {
		return ServerProperties.getClassSpecializationsEnabled() ? 0 : BaseEnchantment.super.getPlayerItemLevel(itemStack, player, itemSlot);
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		if (event.getCause() == DamageCause.VOID) {
			return;
		}

		event.setDamage(event.getDamage() * DAMAGE_TAKEN_MULTIPLIER);

		if (event.getDamage() > 1000000) {
			event.setDamage(1000000);
		}

		if (event.getCause() == DamageCause.POISON) {
			event.setDamage(Math.min(event.getDamage(), Math.max(player.getHealth() - 1, 0)));
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		plugin.mEffectManager.addEffect(player, SPEED_EFFECT_NAME, new PercentSpeed(20, SPEED_EFFECT, SPEED_EFFECT_NAME));
	}

}
