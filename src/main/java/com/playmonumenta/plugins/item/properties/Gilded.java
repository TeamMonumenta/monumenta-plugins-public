package com.playmonumenta.plugins.item.properties;

import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.particlelib.ParticleEffect;
import com.playmonumenta.plugins.utils.particlelib.ParticleEffect.OrdinaryColor;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;

public class Gilded implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Gilded";
	private static final int tickPeriod = 6;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.INVENTORY);
	}

	@Override
	public boolean hasOnSpawn() {
		return true;
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		switch (level) {
		case 0:
			break;
		case 1:
			ParticleUtils.playColorEffect(ParticleEffect.REDSTONE, 191, 166, 51, 0.3, 0.5, 0.3, player.getLocation().add(0, 0.8, 0), 4);
			break;
		case 2:
			ParticleUtils.playColorEffect(ParticleEffect.REDSTONE, 210, 191, 76, 0.3, 0.5, 0.3, player.getLocation().add(0, 0.8, 0), 5);
			break;
		case 3:
		default:
			ParticleUtils.playColorEffect(ParticleEffect.REDSTONE, 229, 229, 128, 0.3, 0.5, 0.3, player.getLocation().add(0, 0.8, 0), 7);
			break;
		}
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		new BukkitRunnable() {
			int numTicks = 0;

			@Override
			public void run() {
				ParticleEffect.REDSTONE.display(new OrdinaryColor(191, 165, 51), item.getLocation(), 40);
				if (item == null || item.isDead()) {
					this.cancel();
				}

				// Very infrequently check if the item is still actually there
				numTicks++;
				if (numTicks > 200) {
					numTicks = 0;
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 10, tickPeriod);
	}
}
