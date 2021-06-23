package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Hopeless implements BaseSpawnableItemEnchantment {
	private static final Particle.DustOptions DARK_RED_PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);
	private static String PROPERTY_NAME = ChatColor.GRAY + "Hopeless";
	private static final Set<UUID> NO_SELF_PARTICLES = new HashSet<>();

	private static final int TICK_PERIOD = 6;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public int getPlayerItemLevel(ItemStack itemStack, Player player, ItemSlot itemSlot) {
		if (player.getScoreboardTags().contains("noSelfParticles")) {
			NO_SELF_PARTICLES.add(player.getUniqueId());
		} else {
			NO_SELF_PARTICLES.remove(player.getUniqueId());
		}
		return BaseSpawnableItemEnchantment.super.getPlayerItemLevel(itemStack, player, itemSlot);
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		//Runs dark red particles while wearing, subject to no self particle PEB option
		if (NO_SELF_PARTICLES.contains(player.getUniqueId())) {
			for (Player other : PlayerUtils.otherPlayersInRange(player, 30, true)) {
				other.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1.5, 0), 3, 0.4, 0.4, 0.4, 0, DARK_RED_PARTICLE_COLOR);
			}
		} else {
			player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1.5, 0), 3, 0.4, 0.4, 0.4, 0, DARK_RED_PARTICLE_COLOR);
		}
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		//Functionally the same as Hope and actually runs in conjunction, since the enchant parser parses Hopeless as both Hopeless and Hope enchanted
		//Only runs cosmetic particles
		new BukkitRunnable() {

			@Override
			public void run() {
				item.getWorld().spawnParticle(Particle.REDSTONE, item.getLocation(), 5, 0.2, 0.2, 0.2, 0, DARK_RED_PARTICLE_COLOR);
				if (item == null || item.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 10, TICK_PERIOD);
	}
}
