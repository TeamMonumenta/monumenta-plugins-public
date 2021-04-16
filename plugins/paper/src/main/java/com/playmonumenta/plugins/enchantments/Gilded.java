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
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;



public class Gilded implements BaseEnchantment {
	private static final Set<UUID> NO_SELF_PARTICLES = new HashSet<>();

	private static final Particle.DustOptions COLOUR_GOLD = new Particle.DustOptions(Color.fromRGB(221, 214, 5), 1f);


	@Override
	public String getProperty() {
		return ChatColor.GRAY + "Gilded";
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.INVENTORY);
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
			Particle.REDSTONE,
			EntityUtils.getHalfEyeLocation(player),
			4 + Math.max(10, level),
			0.3,
			0.5,
			0.3,
			COLOUR_GOLD
		);
	}

	@Override
	public boolean hasOnSpawn() {
		return true;
	}

	@Override
	public void onSpawn(Plugin plugin, @NotNull Item item, int level) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!item.isValid()) { // Ensure item is not gone, is not despawned?
					this.cancel();
				}

				item.getWorld().spawnParticle(Particle.REDSTONE, EntityUtils.getHalfHeightLocation(item), 1, 0.1, 0.1, 0.1, COLOUR_GOLD);
			}
		}.runTaskTimer(plugin, 10, 6);
	}
}