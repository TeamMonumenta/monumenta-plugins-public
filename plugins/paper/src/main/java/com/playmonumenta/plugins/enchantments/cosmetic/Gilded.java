package com.playmonumenta.plugins.enchantments.cosmetic;

import java.util.EnumSet;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseSpawnableItemEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;



public class Gilded implements BaseSpawnableItemEnchantment {
	public static final Particle PARTICLE = Particle.REDSTONE;

	// Minecoin Gold colour
	public static final Particle.DustOptions COLOUR_GOLD = new Particle.DustOptions(Color.fromRGB(221, 214, 5), 1f);

	@Override
	public String getProperty() {
		return "Gilded";
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.INVENTORY);
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		if (PremiumVanishIntegration.isInvisible(player)) {
			return;
		}

		double doubleWidthDelta = PartialParticle.getWidthDelta(player) * 2;
		new PartialParticle(
			PARTICLE,
			LocationUtils.getHalfEyeLocation(player),
			// Count of 5 at level 1 like other cosmetic enchants
			// /patron particles (1 for each game tick).
			// Scales to 15 at level 10 cap
			Constants.QUARTER_TICKS_PER_SECOND - 1 + Math.max(10, level),
			doubleWidthDelta,
			PartialParticle.getHeightDelta(player),
			doubleWidthDelta,
			1,
			COLOUR_GOLD
		).spawnAsPlayer(player, true);
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
					LocationUtils.getHalfHeightLocation(item),
					1,
					PartialParticle.getWidthDelta(item) * 2,
					1,
					COLOUR_GOLD
				).spawnFull();
			}
		}.runTaskTimer(
			plugin,
			Constants.QUARTER_TICKS_PER_SECOND,
			Constants.QUARTER_TICKS_PER_SECOND
		);
	}
}