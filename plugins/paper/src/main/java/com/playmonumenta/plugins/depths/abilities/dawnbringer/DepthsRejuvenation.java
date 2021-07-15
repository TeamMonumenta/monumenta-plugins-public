package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class DepthsRejuvenation extends DepthsAbility {

	public static final String ABILITY_NAME = "Rejuvenation";
	public static final int[] HEAL_INTERVAL = {100, 90, 80, 70, 60};
	public static final int RADIUS = 12;
	public static final double PERCENT_HEAL = .05;

	private static final Map<UUID, Integer> LAST_HEAL_TICK = new HashMap<>();

	private int mHealInterval;

	private int mTimer = 0;

	public DepthsRejuvenation(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.NETHER_STAR;
		mTree = DepthsTree.SUNLIGHT;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		mHealInterval = HEAL_INTERVAL[mRarity - 1];
		if (oneSecond && !mPlayer.isDead()) {
			mTimer += 20;
			if (mTimer % mHealInterval == 0) {
				for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), RADIUS, true)) {
					// Prevents stacking
					boolean highestLevel = true;
					for (Player checkPlayer : PlayerUtils.playersInRange(player.getLocation(), RADIUS, true)) {
						int teammateRarity = DepthsManager.getInstance().getPlayerLevelInAbility(ABILITY_NAME, checkPlayer);
						if (checkPlayer == mPlayer || teammateRarity == 0) {
							continue;
						} else if (teammateRarity >= mRarity) {
							highestLevel = false;
							break;
						}
					}
					Integer lastHealTick = LAST_HEAL_TICK.get(player.getUniqueId());
					if (lastHealTick == null || player.getTicksLived() - LAST_HEAL_TICK.get(player.getUniqueId()) >= mHealInterval && highestLevel) {
						LAST_HEAL_TICK.put(player.getUniqueId(), player.getTicksLived());

						double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						if (player.getHealth() != maxHealth) {
							PlayerUtils.healPlayer(player, PERCENT_HEAL * maxHealth);
							player.getWorld().spawnParticle(Particle.HEART, (player.getLocation()).add(0, 2, 0), 1, 0.07, 0.07, 0.07, 0.001);
						}
					}
				}
			}
		}
	}

	@Override
	public String getDescription(int rarity) {
		return "All players within " + RADIUS + " blocks of you (including yourself) heal " + DepthsUtils.roundPercent(PERCENT_HEAL) + "% of their max health every " + DepthsUtils.getRarityColor(rarity) + HEAL_INTERVAL[rarity - 1] / 20.0 + ChatColor.WHITE + " seconds. A given player will only be healed by the highest Rejuvenation that affects them.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SUNLIGHT;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.PASSIVE;
	}
}

