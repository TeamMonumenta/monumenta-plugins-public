package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Rejuvenation extends Ability {

	private static final int RADIUS = 12;
	private static final int HEAL_INTERVAL_1 = 20 * 5;
	private static final int HEAL_INTERVAL_2 = 20 * 3;
	private static final double PERCENT_HEAL = 0.05;

	private static final Map<UUID, Integer> LAST_HEAL_TICK = new HashMap<>();

	private final int mHealInterval;

	private int mTimer = 0;

	public Rejuvenation(Plugin plugin, Player player) {
		super(plugin, player, "Rejuvenation");
		mInfo.mScoreboardId = "Rejuvenation";
		mInfo.mShorthandName = "Rjv";
		mInfo.mDescriptions.add("You and all other players in a 12 block radius regenerate 5% of their max health every 5 seconds.");
		mInfo.mDescriptions.add("You and all other players in a 12 block radius regenerate 5% of their max health every 3 seconds.");
		mHealInterval = isLevelOne() ? HEAL_INTERVAL_1 : HEAL_INTERVAL_2;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && mPlayer != null && !mPlayer.isDead()) {
			mTimer += 20;
			if (mTimer % mHealInterval == 0) {
				for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), RADIUS, true)) {
					// Don't buff players that have their class disabled or who have PvP enabled
					if (player.getScoreboardTags().contains("disable_class") || AbilityManager.getManager().isPvPEnabled(player)) {
						continue;
					}

					Integer lastHealTick = LAST_HEAL_TICK.get(player.getUniqueId());
					if (lastHealTick == null || player.getTicksLived() - lastHealTick >= mHealInterval) {
						LAST_HEAL_TICK.put(player.getUniqueId(), player.getTicksLived());

						double maxHealth = EntityUtils.getMaxHealth(player);
						if (player.getHealth() != maxHealth) {
							PlayerUtils.healPlayer(mPlugin, player, PERCENT_HEAL * maxHealth, mPlayer);
							player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 1, 0.07, 0.07, 0.07, 0.001);
						}
					}
				}
			}
		}
	}

}
