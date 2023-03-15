package com.playmonumenta.plugins.player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * This class ensures that players receive saturation updates even at full health + full hunger
 */
public class PlayerSaturationTracker {

	public static void startTracking(Plugin plugin) {
		Map<UUID, Float> saturationLevels = new HashMap<>();
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				float newSaturation = player.getSaturation();
				if (player.getHealth() < EntityUtils.getMaxHealth(player)
					    || player.getFoodLevel() < 20) {
					// Health updates are sent automatically in these conditions, so just update the tracked value
					saturationLevels.put(player.getUniqueId(), newSaturation);
				} else {
					Float oldSaturation = saturationLevels.get(player.getUniqueId());
					// Full health and hunger: no saturation updates are sent. Manually send them whenever the saturation changes by 0.5 or more.
					if (oldSaturation != null
						    && Math.abs(oldSaturation - newSaturation) >= 0.5) {
						player.sendHealthUpdate();
						saturationLevels.put(player.getUniqueId(), newSaturation);
					}
				}
			}
			saturationLevels.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
		}, 10, 10);
	}

}
