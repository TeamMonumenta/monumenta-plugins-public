package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;



public class Stylish implements BaseEnchantment {
	private static final Set<UUID> NO_SELF_PARTICLES = new HashSet<>();

	@Override
	public String getProperty() {
		return ChatColor.GRAY + "Stylish";
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public int getLevelFromItem(ItemStack item, Player player) {
		UUID playerUuid = player.getUniqueId();
		if (PlayerUtils.isNoSelfParticles(player)) {
			NO_SELF_PARTICLES.add(playerUuid);
		} else {
			NO_SELF_PARTICLES.remove(playerUuid);
		}

		return getLevelFromItem(item);
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		PlayerUtils.spawnHideableParticles(
			NO_SELF_PARTICLES.contains(player.getUniqueId()),
			player,
			Particle.SMOKE_NORMAL,
			EntityUtils.getHeightLocation(player, 0.75),
			5,
			0.4,
			0
		);
	}
}