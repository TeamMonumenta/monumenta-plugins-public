package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;



public class Retrieval implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Retrieval";
	private static final float RETRIEVAL_CHANCE = 0.1f;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if ((proj.getType() == EntityType.ARROW || proj.getType() == EntityType.SPECTRAL_ARROW) && FastUtils.RANDOM.nextDouble() < RETRIEVAL_CHANCE * level) {
			boolean refunded = AbilityUtils.refundArrow(player, (AbstractArrow) proj);
			if (refunded) {
				player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.3f, 1.0f);
			}
		}
	}
}
