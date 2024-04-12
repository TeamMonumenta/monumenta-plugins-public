package com.playmonumenta.plugins.cosmetics.finishers;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface PlayingFinisher {
	UUID playerUuid();

	void registerKill(Entity killedMob, Location loc);

	void cancel();
}
