package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Hex Eater - When you hit with a Melee attack, +X damage per debuff on the target per level
 */

public class HexEater implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Hex Eater";

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
		int effects = PotionUtils.getNegativeEffects(target).size();

		if (EntityUtils.isStunned(target)) {
			effects++;
		}

		if (EntityUtils.isConfused(target)) {
			effects++;
		}

		if (effects > 0) {
			event.setDamage(event.getDamage() + (level * effects));
			player.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.001);
		}
	}

}
