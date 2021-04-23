package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;



public class Gilded implements BaseEnchantment {
	@NotNull public static final Particle PARTICLE = Particle.REDSTONE;
	public static final Particle.DustOptions COLOUR_GOLD = new Particle.DustOptions(Color.fromRGB(221, 214, 5), 1f);

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
	public void tick(Plugin plugin, Player player, int level) {
		double halfWidth = player.getWidth() / 2;
		new PartialParticle(
			PARTICLE,
			EntityUtils.getHalfEyeLocation(player),
			Constants.QUARTER_TICKS_PER_SECOND - 1 + Math.max(10, level), // Count of 5 at level 1 like other enchants/patron (1 for each game tick), scaling to 15 at level 10 cap
			halfWidth,
			player.getHeight() / 4,
			halfWidth,
			COLOUR_GOLD
		).spawnHideable(player);
	}

	@Override
	public boolean hasOnSpawn() {
		return true;
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!item.isValid()) { // Ensure item is not gone, is not despawned?
					this.cancel();
				}

				new PartialParticle(
					PARTICLE,
					EntityUtils.getHalfHeightLocation(item),
					1,
					item.getWidth() / 2, // Dropped items 0.25 size cube, offsets like vanilla /particle
					COLOUR_GOLD
				).spawn();
			}
		}.runTaskTimer(
			plugin,
			Constants.QUARTER_TICKS_PER_SECOND,
			Constants.QUARTER_TICKS_PER_SECOND
		);
	}
}