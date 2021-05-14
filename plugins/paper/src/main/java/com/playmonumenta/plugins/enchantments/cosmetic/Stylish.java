package com.playmonumenta.plugins.enchantments.cosmetic;

import java.util.EnumSet;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseSpawnableItemEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;

import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;



public class Stylish implements BaseSpawnableItemEnchantment {
	public static final Particle PARTICLE = Particle.SMOKE_NORMAL;
	public static final double PARTICLE_EXTRA = 0;

	@Override
	public String getProperty() {
		return "Stylish";
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND);
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		double wideWidthDelta = PartialParticle.getWidthDelta(player) * 2 * 1.1;
		new PartialParticle(
			PARTICLE,
			LocationUtils.getHeightLocation(player, 0.75),
			Constants.QUARTER_TICKS_PER_SECOND,
			wideWidthDelta,
			PartialParticle.getHeightDelta(player),
			wideWidthDelta,
			PARTICLE_EXTRA
		).spawnAsPlayer(player, true);
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
					LocationUtils.getHalfHeightLocation(item),
					1,
					PartialParticle.getWidthDelta(item) * 2,
					PARTICLE_EXTRA
				).spawnFull();
			}
		}.runTaskTimer(
			plugin,
			Constants.QUARTER_TICKS_PER_SECOND,
			Constants.QUARTER_TICKS_PER_SECOND
		);
	}
}