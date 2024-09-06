package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.hexfall.VoodooBindings;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpellVoodooExecution extends Spell {

	private final Plugin mPlugin;
	private final int mCooldown;
	private final Location mSpawnLoc;

	public SpellVoodooExecution(Plugin plugin, int cooldown, Location spawnLoc) {
		mPlugin = plugin;
		mCooldown = cooldown;
		mSpawnLoc = spawnLoc;
	}

	@Override
	public void run() {
		for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
			mPlugin.mEffectManager.clearEffects(player, VoodooBindings.GENERIC_NAME);
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
