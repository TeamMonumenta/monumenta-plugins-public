package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class Recoil implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Recoil";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (event.getEntity() instanceof AbstractArrow && !ItemUtils.isShootableItem(player.getInventory().getItemInOffHand())) {
			if (player.isSneaking()) {
				player.setCooldown(player.getInventory().getItemInMainHand().getType(), 10);
			} else if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_MOBILITY_ABILITIES)) {
				Vector velocity = NmsUtils.getActualDirection(player).multiply(-0.5 * Math.sqrt(level));
				velocity.setY(Math.max(0.1, velocity.getY()));
				player.setVelocity(velocity);
			}
		}
	}

}
