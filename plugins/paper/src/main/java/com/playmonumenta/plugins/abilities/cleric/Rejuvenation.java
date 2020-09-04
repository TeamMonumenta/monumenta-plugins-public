package com.playmonumenta.plugins.abilities.cleric;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Rejuvenation extends Ability {

	private static final int RADIUS = 12;
	private static final int HEAL_INTERVAL_1 = 20 * 5;
	private static final int HEAL_INTERVAL_2 = 20 * 3;
	private static final double PERCENT_HEAL = 0.05;

	private static final Map<Player, Integer> LAST_HEAL_TICK = new HashMap<Player, Integer>();

	private final int mHealInterval;

	private int mTimer = 0;

	public Rejuvenation(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Rejuvenation");
		mInfo.mScoreboardId = "Rejuvenation";
		mInfo.mShorthandName = "Rjv";
		mInfo.mDescriptions.add("You and all other players in a 12 block radius regenerate 5% of their max health every 5 seconds.");
		mInfo.mDescriptions.add("You and all other players in a 12 block radius regenerate 5% of their max health every 3 seconds.");
		mHealInterval = getAbilityScore() == 1 ? HEAL_INTERVAL_1 : HEAL_INTERVAL_2;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && !mPlayer.isDead()) {
			mTimer += 20;
			if (mTimer % mHealInterval == 0) {
				for (Player player : PlayerUtils.playersInRange(mPlayer, RADIUS, true)) {
					// Don't buff players that have their class disabled or who have PvP enabled
					if (player.getScoreboardTags().contains("disable_class") || AbilityManager.getManager().isPvPEnabled(player)) {
						continue;
					}

					Integer lastHealTick = LAST_HEAL_TICK.get(player);
					if (lastHealTick == null || player.getTicksLived() - LAST_HEAL_TICK.get(player) >= mHealInterval) {
						LAST_HEAL_TICK.put(player, player.getTicksLived());

						double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						if (player.getHealth() != maxHealth) {
							PlayerUtils.healPlayer(player, PERCENT_HEAL * maxHealth);
							mWorld.spawnParticle(Particle.HEART, (player.getLocation()).add(0, 2, 0), 1, 0.07, 0.07, 0.07, 0.001);
						}
					}
				}
			}
		}
	}

}
