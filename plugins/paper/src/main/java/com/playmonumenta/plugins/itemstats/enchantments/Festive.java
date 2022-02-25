package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Festive implements Infusion {

	private static final int TICK_PERIOD = 5;
	private static final Particle.DustOptions FESTIVE_RED_COLOR = new Particle.DustOptions(Color.fromRGB(255, 98, 71), 1.0f);
	private static final Particle.DustOptions FESTIVE_GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(75, 200, 0), 1.0f);
	private static final Set<UUID> NO_SELF_PARTICLES = new HashSet<>();

	@Override
	public String getName() {
		return "Festive";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.FESTIVE;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		if (player.getScoreboardTags().contains("noSelfParticles")) {
			NO_SELF_PARTICLES.add(player.getUniqueId());
		} else {
			NO_SELF_PARTICLES.remove(player.getUniqueId());
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
			return;
		}

		final Location loc = player.getLocation().add(0, 1, 0);
		if (NO_SELF_PARTICLES.contains(player.getUniqueId())) {
			for (Player other : PlayerUtils.otherPlayersInRange(player, 30, true)) {
				other.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + (int) value), 0.4, 0.4, 0.4, FESTIVE_RED_COLOR);
				other.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + (int) value), 0.4, 0.4, 0.4, FESTIVE_GREEN_COLOR);
				other.spawnParticle(Particle.SNOWBALL, loc, Math.max(3, (int) value), 0.4, 0.4, 0.4, 0);
			}
		} else {
			World world = player.getWorld();
			world.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + (int) value), 0.4, 0.4, 0.4, FESTIVE_RED_COLOR);
			world.spawnParticle(Particle.REDSTONE, loc, Math.max(6, 2 + (int) value), 0.4, 0.4, 0.4, FESTIVE_GREEN_COLOR);
			world.spawnParticle(Particle.SNOWBALL, loc, Math.max(3, (int) value), 0.4, 0.4, 0.4, 0);
		}
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, double value) {
		new BukkitRunnable() {
			int mTicks = 0;
			World mWorld = item.getWorld();

			@Override
			public void run() {
				final Location loc = item.getLocation().add(0, 0.15, 0);
				if (!loc.isChunkLoaded() || item.isDead() || !item.isValid()) {
					this.cancel();
					return;
				}

				mWorld.spawnParticle(Particle.REDSTONE, loc, 3, 0.2, 0.2, 0.2, FESTIVE_RED_COLOR);
				mWorld.spawnParticle(Particle.REDSTONE, loc, 3, 0.2, 0.2, 0.2, FESTIVE_GREEN_COLOR);
				mWorld.spawnParticle(Particle.SNOWBALL, loc, 3, 0.2, 0.2, 0.2, 0);

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
