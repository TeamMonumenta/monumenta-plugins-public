package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellConstructAggro extends Spell {

	private LivingEntity mBoss;

	public SpellConstructAggro(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		Creature creature = (Creature) mBoss;
		if (!(creature.getTarget() instanceof Player)) {
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 30, true);
			Collections.shuffle(players);
			if (!players.isEmpty()) {
				creature.setTarget(players.get(0));
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
