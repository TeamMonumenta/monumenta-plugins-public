package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public class SpellRutenThreat extends Spell {

	private final LivingEntity mBoss;
	private final int mIntervalTicks;
	private final Location mSpawnLoc;
	private int mTicks;

	public SpellRutenThreat(LivingEntity boss, int intervalTicks, Location mSpawnLoc) {
		mBoss = boss;
		mIntervalTicks = intervalTicks;
		this.mSpawnLoc = mSpawnLoc;
		mTicks = 0;
	}

	@Override
	public void run() {
		if (mTicks++ > mIntervalTicks) {
			mTicks = 0;
			List<Player> players = HexfallUtils.getPlayersInRuten(mSpawnLoc);
			if (!players.isEmpty()) {
				((Mob) mBoss).setTarget(players.get(FastUtils.randomIntInRange(0, players.size() - 1)));
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
