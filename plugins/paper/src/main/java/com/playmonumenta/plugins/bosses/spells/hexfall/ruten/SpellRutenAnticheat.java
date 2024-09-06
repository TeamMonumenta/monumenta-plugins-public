package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellRutenAnticheat extends Spell {

	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private static final double RADIUS = 32.5;
	private static final double SIZE_UP = 19.0;
	private static final double SIZE_DOWN = 6.0;

	public SpellRutenAnticheat(LivingEntity boss, Location spawnLoc) {
		mBoss = boss;
		mSpawnLoc = spawnLoc;
	}

	@Override
	public void run() {
		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, Ruten.detectionRange, true)) {
			if (!HexfallUtils.playerInRuten(player) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
				Location location = player.getLocation();
				double y = location.getY();
				double sy = mSpawnLoc.getY();

				if (y <= sy + SIZE_UP && y >= sy - SIZE_DOWN && LocationUtils.xzDistance(mSpawnLoc, location) <= RADIUS) {
					PlayerUtils.killPlayer(player, mBoss, null, true, true, true);
					AuditListener.logPlayer("[Hexfall] " + player.getName() + " was inside a Ru'Ten boss fight when they shouldn't have been.");
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
