package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

import net.md_5.bungee.api.ChatColor;

//Ethereal: Increases I-Frames of a player by 1 tick per level. Default I-Frames is 20.
public class Ethereal implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Ethereal";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		new BukkitRunnable() {
			@Override
			public void run() {
				//Invulnerability frames are weird. This line guarentees that I-frames only extend if the timer has reset.
				if (player.getNoDamageTicks() == 19 || player.getNoDamageTicks() == 20) {
					player.setNoDamageTicks(20 + level);
				}
			}
		}.runTaskLater(plugin, 0);
	}

	@Override
	public void removeProperty(Plugin plugin, Player player) {
		player.setNoDamageTicks(20);
	}
}
