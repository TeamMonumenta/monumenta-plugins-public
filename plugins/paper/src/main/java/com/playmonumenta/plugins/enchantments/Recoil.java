package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class Recoil implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Recoil";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (player.isSneaking()) {
			player.setCooldown(player.getInventory().getItemInMainHand().getType(), (int)(20 * Math.sqrt(level)));
		} else if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			Vector velocity = player.getLocation().getDirection().multiply(-0.5 * Math.sqrt(level));
			player.setVelocity(velocity.setY(Math.max(0.1, velocity.getY())));
		}
	}

}
