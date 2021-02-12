package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Stylish implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Stylish";
	private static final Set<UUID> NO_SELF_PARTICLES = new HashSet<>();

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public int getLevelFromItem(ItemStack item, Player player) {
		if (player.getScoreboardTags().contains("noSelfParticles")) {
			NO_SELF_PARTICLES.add(player.getUniqueId());
		} else {
			NO_SELF_PARTICLES.remove(player.getUniqueId());
		}
		return getLevelFromItem(item);
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		if (NO_SELF_PARTICLES.contains(player.getUniqueId())) {
			for (Player other : PlayerUtils.playersInRange(player, 30, false)) {
				other.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1.5, 0), 5, 0.4, 0.4, 0.4, 0);
			}
		} else {
			player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(0, 1.5, 0), 5, 0.4, 0.4, 0.4, 0);
		}
	}
}
