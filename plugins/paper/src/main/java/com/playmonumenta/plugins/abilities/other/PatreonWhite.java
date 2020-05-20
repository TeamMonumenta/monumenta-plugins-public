package com.playmonumenta.plugins.abilities.other;


import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class PatreonWhite extends Ability {
	private boolean mNoSelfParticles = false;

	public PatreonWhite(Plugin plugin, World world, Player player) {
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
		int shinyWhite = ScoreboardUtils.getScoreboardValue(player, "ShinyWhite");
		return shinyWhite > 0 && patreon >= 5;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			if (mNoSelfParticles) {
				for (Player other : PlayerUtils.playersInRange(mPlayer, 30, false)) {
					other.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				}
			} else {
				mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
			}
		}
	}
}
