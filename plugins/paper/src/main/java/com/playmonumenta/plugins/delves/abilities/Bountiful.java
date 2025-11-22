package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import net.kyori.adventure.text.Component;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class Bountiful {
	public static final String DESCRIPTION = "Spawners have larger ranges.";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Most spawner ranges are doubled."),
		};
	}

	public static void applyModifiers(CreatureSpawner spawner, int level) {
		if (level != 0 && !spawner.hasMetadata("BountifulChecked")) {
			spawner.setMetadata("BountifulChecked", new FixedMetadataValue(Plugin.getInstance(), true));
			new BukkitRunnable() {
				@Override
				public void run() {
					spawner.setRequiredPlayerRange(spawner.getRequiredPlayerRange() * 2);
					spawner.update(false, false);
				}
			}.runTaskLater(Plugin.getInstance(), 5);
		}
	}
}
