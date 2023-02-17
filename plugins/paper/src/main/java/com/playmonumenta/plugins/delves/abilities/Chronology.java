package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.scheduler.BukkitRunnable;

public class Chronology {

	public static final String DESCRIPTION = "Spawners spawn faster.";

	public static String[] rankDescription(int level) {
		return new String[]{
			"All spawners spawn twice as fast."
		};
	}

	public static void applyModifiers(CreatureSpawner spawner, int level) {
		if (level == 0) {
			return;
		} else {
			new BukkitRunnable() {
				@Override
				public void run() {
					spawner.setDelay(Math.max(1, (spawner.getMaxSpawnDelay() + spawner.getMinSpawnDelay()) / 4 - 5));
					spawner.update();
				}
			}.runTaskLater(Plugin.getInstance(), 5);

		}
	}
}
