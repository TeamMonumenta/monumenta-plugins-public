package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;



public class LifeDrain implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Life Drain";
	private static final double LIFE_DRAIN_CRIT_HEAL = 1;
	private static final double LIFE_DRAIN_NONCRIT_HEAL_MULTIPLIER = 0.5;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		if (PlayerUtils.isFallingAttack(player)) {
			PlayerUtils.healPlayer(player, LIFE_DRAIN_CRIT_HEAL * Math.sqrt(level));
			player.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation(), 3, 0.1, 0.1, 0.1, 0.001);
		} else {
			PlayerUtils.healPlayer(
				player,
				LIFE_DRAIN_NONCRIT_HEAL_MULTIPLIER
					* Math.sqrt(level)
					// This is * √(attack rate seconds)
					// The same as / √(1 / attack rate seconds)
					// Advancement simply says / √(attack speed)
					* Math.sqrt(player.getCooldownPeriod() / Constants.TICKS_PER_SECOND)
					* player.getCooledAttackStrength(0)
			);

			player.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation(), 1, 0.1, 0.1, 0.1, 0.001);
		}
	}
}