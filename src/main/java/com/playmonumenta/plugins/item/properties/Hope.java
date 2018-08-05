package com.playmonumenta.plugins.item.properties;

import java.lang.reflect.Field;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.particlelib.ParticleEffect;

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
			@Override
			public void run() {
				ParticleEffect.SPELL_INSTANT.display(0.2f, 0.2f, 0.2f, 0, 3, item.getLocation(), 40);
				if (item == null || item.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 10, tickPeriod);
	}
}
