package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.scheduler.BukkitRunnable;

public class Chronology {

	public static final String DESCRIPTION = "Spawners spawn faster.";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("All spawners spawn twice as fast.")
		};
	}

	public static void applyModifiers(CreatureSpawner spawner, int level) {
		if (level != 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					spawner.setDelay(Math.max(1, FastUtils.RANDOM.nextInt(spawner.getMinSpawnDelay(), spawner.getMaxSpawnDelay() + 1) / 2 - 5));
					spawner.update(false, false);
				}
			}.runTaskLater(Plugin.getInstance(), 5);
		}
	}
}
