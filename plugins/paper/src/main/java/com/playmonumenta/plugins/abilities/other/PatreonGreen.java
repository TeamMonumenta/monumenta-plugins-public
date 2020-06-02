package com.playmonumenta.plugins.abilities.other;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class PatreonGreen extends Ability {
	private boolean mNoSelfParticles = false;

	public PatreonGreen(Plugin plugin, World world, Player player) {
		super(plugin, world, player, null);

		if (player != null) {
			mNoSelfParticles = player.getScoreboardTags().contains("noSelfParticles");
		} else {
			mNoSelfParticles = false;
		}
	}

	@Override
	public boolean canUse(Player player) {
		int patreon = ScoreboardUtils.getScoreboardValue(player, "Patreon");
		int shinyGreen = ScoreboardUtils.getScoreboardValue(player, "ShinyGreen");
		return shinyGreen > 0 && patreon >= 20;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			if (mNoSelfParticles) {
				for (Player other : PlayerUtils.playersInRange(mPlayer, 30, false)) {
					other.spawnParticle(Particle.VILLAGER_HAPPY, mPlayer.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				}
			} else {
				mWorld.spawnParticle(Particle.VILLAGER_HAPPY, mPlayer.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
			}
		}
	}
}
