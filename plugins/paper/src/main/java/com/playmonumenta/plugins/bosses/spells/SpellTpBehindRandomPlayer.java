package com.playmonumenta.plugins.bosses.spells;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

/**
 * @deprecated look inside SpellTpBehindPlayer
 */
public class SpellTpBehindRandomPlayer extends SpellTpBehindPlayer {
	private static final int MAX_RANGE = 80;

	public SpellTpBehindRandomPlayer(Plugin plugin, Entity launcher, int duration) {
		super(plugin, launcher, duration);
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mLauncher.getLocation(), MAX_RANGE);
		while (!players.isEmpty()) {
			Player target = players.get(FastUtils.RANDOM.nextInt(players.size()));

			/* Do not teleport to players in safezones */
			if (ZoneUtils.hasZoneProperty(target, ZoneProperty.RESIST_5)) {
				/* This player is in a safe area - don't tp to them */
				players.remove(target);
			} else {
				launch(target, MAX_RANGE);
				animation(target);
				break;
			}
		}
	}

}
