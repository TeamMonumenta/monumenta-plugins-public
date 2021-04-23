package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;



public class Stylish implements BaseEnchantment {
	@NotNull public static final Particle PARTICLE = Particle.SMOKE_NORMAL;
	public static final double PARTICLE_EXTRA = 0;

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
	public void tick(Plugin plugin, Player player, int level) {
		double width = player.getWidth() / 2 * 1.25;
		new PartialParticle(
			PARTICLE,
			EntityUtils.getHeightLocation(player, 0.75),
			Constants.QUARTER_TICKS_PER_SECOND,
			width,
			player.getHeight() / 4,
			width,
			PARTICLE_EXTRA
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
				if (!item.isValid()) {
					this.cancel();
				}

				new PartialParticle(
					PARTICLE,
					EntityUtils.getHalfHeightLocation(item),
					1,
					item.getWidth() / 2,
					PARTICLE_EXTRA
				).spawn();
			}
		}.runTaskTimer(
			plugin,
			Constants.QUARTER_TICKS_PER_SECOND,
			Constants.QUARTER_TICKS_PER_SECOND
		);
	}
}