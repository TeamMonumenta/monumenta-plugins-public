package com.playmonumenta.plugins.portals;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PortalAFKCheck extends BukkitRunnable {

	Player mPlayer;

	public PortalAFKCheck(Player p) {
		mPlayer = p;
	}

	@Override
	public void run() {
		PortalManager.clearPortal(mPlayer, 1);
		PortalManager.clearPortal(mPlayer, 2);
	}
}
