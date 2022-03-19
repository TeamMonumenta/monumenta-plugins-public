package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Gilded implements Infusion {

	public static final Particle PARTICLE = Particle.REDSTONE;
	public static final Particle.DustOptions COLOUR_GOLD = new Particle.DustOptions(Color.fromRGB(221, 214, 5), 1f);

	@Override
	public String getName() {
		return "Gilded";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.GILDED;
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
			return;
		}

		double doubleWidthDelta = PartialParticle.getWidthDelta(player) * 2;
		new PartialParticle(PARTICLE, LocationUtils.getHalfEyeLocation(player))
			// Count of 5 at level 1 like other cosmetic enchants
			// /patron particles (1 for each game tick).
			// Scales to 15 at level 10 cap
			.count(Constants.QUARTER_TICKS_PER_SECOND - 1 + Math.max(10, (int) value))
			.delta(doubleWidthDelta, PartialParticle.getHeightDelta(player), doubleWidthDelta)
			.extra(1)
			.data(COLOUR_GOLD)
			.minimumMultiplier(false)
			.spawnAsPlayerPassive(player);
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, double value) {
		new BukkitRunnable() {
			@Override
			public void run() {
				final Location loc = item.getLocation().add(0, 0.15, 0);
				if (!loc.isChunkLoaded() || item.isDead() || !item.isValid()) {
					this.cancel();
					return;
				}

				new PartialParticle(
					PARTICLE,
					LocationUtils.getHalfHeightLocation(item),
					1,
					PartialParticle.getWidthDelta(item) * 2,
					1,
					COLOUR_GOLD,
					false,
					0
				).spawnFull();
			}
		}.runTaskTimer(
			plugin,
			Constants.QUARTER_TICKS_PER_SECOND,
			Constants.QUARTER_TICKS_PER_SECOND
		);
	}
}
