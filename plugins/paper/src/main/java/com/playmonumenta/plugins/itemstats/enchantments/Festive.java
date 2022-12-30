package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Festive implements Infusion {

	private static final int TICK_PERIOD = 5;
	private static final Particle.DustOptions FESTIVE_RED_COLOR = new Particle.DustOptions(Color.fromRGB(255, 98, 71), 1.0f);
	private static final Particle.DustOptions FESTIVE_GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(75, 200, 0), 1.0f);

	@Override
	public String getName() {
		return "Festive";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.FESTIVE;
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
			return;
		}
		int level = (int) value;
		final Location loc = player.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.REDSTONE, loc, Math.max(6, 2 + level), 0.4, 0.4, 0.4, 0, FESTIVE_RED_COLOR)
			.minimumMultiplier(false)
			.spawnAsPlayerPassive(player);
		new PartialParticle(Particle.REDSTONE, loc, Math.max(6, 2 + level), 0.4, 0.4, 0.4, 0, FESTIVE_GREEN_COLOR)
			.minimumMultiplier(false)
			.spawnAsPlayerPassive(player);
		new PartialParticle(Particle.SNOWBALL, loc, Math.max(3, level), 0.4, 0.4, 0.4, 0)
			.minimumMultiplier(false)
			.spawnAsPlayerPassive(player);
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, double value) {
		new BukkitRunnable() {
			int mTicks = 0;
			final World mWorld = item.getWorld();

			@Override
			public void run() {
				final Location loc = item.getLocation().add(0, 0.15, 0);
				if (!loc.isChunkLoaded() || item.isDead() || !item.isValid()) {
					this.cancel();
					return;
				}

				new PartialParticle(Particle.REDSTONE, loc, 3, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR).spawnFull();
				new PartialParticle(Particle.REDSTONE, loc, 3, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR).spawnFull();
				new PartialParticle(Particle.SNOWBALL, loc, 3, 0.2, 0.2, 0.2, 0).spawnFull();

				// Very infrequently check if the item is still actually there
				mTicks++;
				if (mTicks > 100) {
					mTicks = 0;
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 10, TICK_PERIOD);
	}
}
