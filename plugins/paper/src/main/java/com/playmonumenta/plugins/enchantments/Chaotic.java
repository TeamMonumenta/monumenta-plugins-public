package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Chaotic implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Chaotic";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		Random mRandom = new Random();
		int rand = mRandom.nextInt(2 * level + 1) - level;

		if (rand > 0) {
			player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.001);
		}

		if (event.getCause().equals(DamageCause.ENTITY_SWEEP_ATTACK)) {
			rand = rand / 2;
		}

		event.setDamage(Math.max(0, event.getDamage() + rand * player.getCooledAttackStrength(0)));
	}
}
