package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.bosses.bosses.ImperialConstruct;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellConstructAggro extends Spell {

	private final LivingEntity mBoss;
	private final ImperialConstruct mConstruct;

	public SpellConstructAggro(LivingEntity boss, ImperialConstruct construct) {
		mBoss = boss;
		mConstruct = construct;
	}

	@Override
	public void run() {
		if (mBoss instanceof Creature creature && (!(creature.getTarget() instanceof Player prevTarget) || !mConstruct.isInArena(prevTarget))) {
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 30, true);
			players.removeIf(p -> !mConstruct.isInArena(p));
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
