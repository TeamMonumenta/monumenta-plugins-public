package com.playmonumenta.plugins.cosmetics.punches;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public interface PlayerPunch {
	void run(Player bully, Player victim);

	void broadcastPunchMessage(Player bully, Player victim, List<Player> playersInWorld, boolean isRemotePunch);

	Material getDisplayItem();
}
