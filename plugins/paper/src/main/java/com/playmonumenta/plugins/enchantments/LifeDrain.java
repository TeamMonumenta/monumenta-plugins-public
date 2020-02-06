package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class LifeDrain implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Life Drain";
	private static final double LIFE_DRAIN_CRIT_HEAL = 1;
	private static final double LIFE_DRAIN_HEAL = 0.25;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (PlayerUtils.isCritical(player)) {
			PlayerUtils.healPlayer(player, LIFE_DRAIN_CRIT_HEAL * Math.sqrt(level));
			player.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation(), 3, 0.1, 0.1, 0.1, 0.001);
		} else if (PlayerUtils.isFullyCooled(player)) {
			PlayerUtils.healPlayer(player, LIFE_DRAIN_HEAL * Math.sqrt(level));
			player.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation(), 1, 0.1, 0.1, 0.1, 0.001);
		}
	}
}
