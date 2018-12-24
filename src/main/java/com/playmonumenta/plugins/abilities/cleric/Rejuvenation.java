package com.playmonumenta.plugins.abilities.cleric;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Rejuvenation extends Ability {

	private static final int REJUVENATION_RADIUS = 12;
	private static final int REJUVENATION_HEAL_AMOUNT = 1;

	public Rejuvenation(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 3;
		mInfo.specId = -1;
		mInfo.scoreboardId = "Rejuvenation";
	}

	@Override
	public void PeriodicTrigger(boolean twoHertz, boolean oneSecond, boolean twoSeconds, boolean fourtySeconds, boolean sixtySeconds, int originalTime) {
		//  Don't trigger this if dead!
		if (!mPlayer.isDead()) {
			boolean threeSeconds = ((originalTime % 3) == 0);
			if (threeSeconds) {
				int rejuvenation = getAbilityScore();
				for (Player p : PlayerUtils.getNearbyPlayers(mPlayer, REJUVENATION_RADIUS, true)) {
					// Don't buff players that have their class disabled
					if (p.getScoreboardTags().contains("disable_class")) {
						continue;
					}

					//  If this is us or we're allowing anyone to get it.
					if (p == mPlayer || rejuvenation > 1) {
						double oldHealth = p.getHealth();
						PlayerUtils.healPlayer(p, REJUVENATION_HEAL_AMOUNT);
						if (p.getHealth() > oldHealth) {
							mWorld.spawnParticle(Particle.HEART, (p.getLocation()).add(0, 2, 0), 1, 0.07, 0.07, 0.07, 0.001);
						}
					}
				}
			}
		}
	}

}
