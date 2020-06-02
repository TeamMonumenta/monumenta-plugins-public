package com.playmonumenta.plugins.abilities.cleric;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class ClericPassive extends Ability {

	private static final int PASSIVE_HEAL_AMOUNT = 1;
	private static final int PASSIVE_HEAL_RADIUS = 5;
	private static final double PASSIVE_HP_THRESHOLD = 10.0;
	private static final String CLERICPASSIVE_METADATA_KEY = "ClericPassiveMetadataKey";

	private int timer = 0;

	public ClericPassive(Plugin plugin, World world, Player player) {
		super(plugin, world, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 3;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			//  Don't trigger this if dead!
			if (!mPlayer.isDead()) {
				// 5 ticks because it triggers on four hertz.
				timer += 5;
				if (timer % 60 == 0) {
					// Passive Heal Radius
					World world = mPlayer.getWorld();
					for (Player p : PlayerUtils.playersInRange(mPlayer, PASSIVE_HEAL_RADIUS, false)) {
						if (MetadataUtils.checkOnceThisTick(mPlugin, p, CLERICPASSIVE_METADATA_KEY)) {
							if (p.getHealth() <= PASSIVE_HP_THRESHOLD) {
								PlayerUtils.healPlayer(p, PASSIVE_HEAL_AMOUNT);
								world.spawnParticle(Particle.HEART, (p.getLocation()).add(0, 2, 0), 1, 0.03, 0.03, 0.03, 0.001);
							}
						}
					}
				}
			}
		}
	}

}
