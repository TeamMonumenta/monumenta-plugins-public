package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
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
	public int getLevelFromItem(ItemStack item) {
		return ServerProperties.getClassSpecializationsEnabled() ? 0 : BaseEnchantment.super.getLevelFromItem(item);
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		event.setDamage(event.getDamage() * DAMAGE_TAKEN_MULTIPLIER);
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		plugin.mEffectManager.addEffect(player, SPEED_EFFECT_NAME, new PercentSpeed(20, SPEED_EFFECT, SPEED_EFFECT_NAME));
	}

}
