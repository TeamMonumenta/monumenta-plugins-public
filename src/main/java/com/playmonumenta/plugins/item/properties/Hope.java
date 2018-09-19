package com.playmonumenta.plugins.item.properties;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;

import java.lang.reflect.Field;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

public class Hope implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Hope";

	/* How much longer an item lasts per level */
	private static final int extraMinutesPerLevel = 5;
	private static final int tickPeriod = 6;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean hasOnSpawn() {
		return true;
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		item.setInvulnerable(true);

		/*
		 * Use reflection to modify the item entity's age field directly.
		 * This is because setTicksLived() doesn't seem to affect despawn time
		 * Basically an exact copy of code from here:
		 * https://www.spigotmc.org/threads/settickslived-not-working.240140/
		 */
		try {
			Field itemField = item.getClass().getDeclaredField("item");
			Field ageField;
			Object entityItem;

			itemField.setAccessible(true);
			entityItem = itemField.get(item);

			ageField = entityItem.getClass().getDeclaredField("age");
			ageField.setAccessible(true);

			ageField.set(entityItem, -1 * extraMinutesPerLevel * Constants.TICKS_PER_MINUTE * level);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}

		new BukkitRunnable() {
			int numTicks = 0;

			@Override
			public void run() {
				item.getWorld().spawnParticle(Particle.SPELL_INSTANT, item.getLocation(), 3, 0.2, 0.2, 0.2, 0);
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
