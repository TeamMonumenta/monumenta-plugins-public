package com.playmonumenta.plugins.timers;

import java.util.EnumSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;

import static java.awt.Color.HSBtoRGB;

public class ShowMarkerTimer {
	private static final EnumSet<GameMode> VALID_GAMEMODES = EnumSet.of(GameMode.CREATIVE, GameMode.SPECTATOR);

	public static void update() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!VALID_GAMEMODES.contains(player.getGameMode())) {
				continue;
			}
			for (Marker marker : player.getLocation().getNearbyEntitiesByType(Marker.class, 64.0d)) {
				Location markerLoc = marker.getLocation();
				UUID markerUuid = marker.getUniqueId();

				float hue = floatFloorMod(Bukkit.getCurrentTick() / 360.0f, 1.0f);
				hue += floatFloorMod((float) markerLoc.getX() / 16.0f, 1.0f);
				hue += floatFloorMod((float) markerLoc.getY() / 16.0f, 1.0f);
				hue += floatFloorMod((float) markerLoc.getZ() / 16.0f, 1.0f);
				hue = floatFloorMod(hue, 1.0f);
				float saturation = 1.0f - 0.6f * floatFloorMod((float) markerUuid.getMostSignificantBits() / 1024.0f, 1.0f);
				float value = 1.0f - 0.6f * floatFloorMod((float) markerUuid.getLeastSignificantBits() / 1024.0f, 1.0f);
				int rgb = 0x00ffffff & HSBtoRGB(hue, saturation, value);
				Color color = Color.fromRGB(rgb);

				player.spawnParticle(Particle.REDSTONE,
					markerLoc,
					1,
					0.0,
					0.0,
					0.0,
					new Particle.DustOptions(color, 0.3f));
			}
		}
	}

	private static float floatFloorMod(float x, float y) {
		return x - ((float) Math.floor(x/y) * y);
	}
}
