package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class Recoil implements Enchantment {

	@Override
	public String getName() {
		return "Recoil";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RECOIL;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double level, ProjectileLaunchEvent event, Projectile proj) {
		if (event.getEntity() instanceof AbstractArrow && !ItemUtils.isShootableItem(player.getInventory().getItemInOffHand())) {
			if (player.isSneaking()) {
				player.setCooldown(player.getInventory().getItemInMainHand().getType(), (int)(20 * Math.sqrt(level)));
			} else if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_MOBILITY_ABILITIES)) {
				Vector velocity = NmsUtils.getActualDirection(player).multiply(-0.5 * Math.sqrt(level));
				velocity.setY(Math.max(0.1, velocity.getY()));
				player.setVelocity(velocity);
			}
		}
	}

}
