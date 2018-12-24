package com.playmonumenta.plugins.abilities.cleric;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class ClericPassive extends Ability {

	private static final int PASSIVE_HEAL_AMOUNT = 1;
	private static final int PASSIVE_HEAL_RADIUS = 5;
	private static final double PASSIVE_HP_THRESHOLD = 10.0;

	public ClericPassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 3;
	}

	@Override
	public void PeriodicTrigger(boolean twoHertz, boolean oneSecond, boolean twoSeconds, boolean fourtySeconds, boolean sixtySeconds, int originalTime) {
		//  Don't trigger this if dead!
		if (!mPlayer.isDead()) {
			boolean threeSeconds = ((originalTime % 3) == 0);
			if (threeSeconds) {
				// Passive Heal Radius
				World world = mPlayer.getWorld();
				for (Player p : PlayerUtils.getNearbyPlayers(mPlayer, PASSIVE_HEAL_RADIUS, false)) {
					if (p.getHealth() <= PASSIVE_HP_THRESHOLD) {
						PlayerUtils.healPlayer(p, PASSIVE_HEAL_AMOUNT);
						world.spawnParticle(Particle.HEART, (p.getLocation()).add(0, 2, 0), 1, 0.03, 0.03, 0.03, 0.001);
					}
				}
			}
		}
	}

}
