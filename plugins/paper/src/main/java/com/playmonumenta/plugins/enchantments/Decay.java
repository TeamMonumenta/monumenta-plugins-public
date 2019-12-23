package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Decay implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Decay";

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
		PotionUtils.applyPotion(player, target, new PotionEffect(PotionEffectType.WITHER, 20 * 4, level - 1, false, true));
		BlockData fallingDustData = Material.ANVIL.createBlockData();
		player.getWorld().spawnParticle(Particle.FALLING_DUST, target.getLocation().add(0, 1, 0), 4, 0.4, 0.5, 0.4, fallingDustData);
	}

}
