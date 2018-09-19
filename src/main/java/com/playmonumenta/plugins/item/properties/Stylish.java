package com.playmonumenta.plugins.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;

public class Stylish implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Stylish";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		world.spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1.5, 0), 5, 0.4, 0.4, 0.4, 0);
	}
}
