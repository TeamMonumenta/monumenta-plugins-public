package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

public class Choler implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Choler";
	private static final double DAMAGE_MLT_PER_LVL = 0.01;

	@Override
	public @NotNull String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onDamage(@NotNull Plugin plugin, @NotNull Player player, int level, @NotNull LivingEntity enemy, @NotNull EntityDamageByEntityEvent entityDamageByEntityEvent) {
		if (EntityUtils.isStunned(enemy) || EntityUtils.isSlowed(plugin, enemy) || enemy.getFireTicks() > 0) {
			entityDamageByEntityEvent.setDamage(entityDamageByEntityEvent.getDamage() * (1 + (DAMAGE_MLT_PER_LVL * DelveInfusionUtils.getModifiedLevel(plugin, player, level))));
		}
	}

}
