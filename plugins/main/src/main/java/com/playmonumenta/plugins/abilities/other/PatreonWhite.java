package com.playmonumenta.plugins.abilities.other;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class PatreonWhite extends Ability {
	public PatreonWhite(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean canUse(Player player) {
		int patreon = ScoreboardUtils.getScoreboardValue(player, "Patreon");
		int shinyWhite = ScoreboardUtils.getScoreboardValue(player, "ShinyWhite");
		return shinyWhite > 0 && patreon >= 5;
	}

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			for(Player other : PlayerUtils.getNearbyPlayers(mPlayer, 30, false)) {
				other.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
			}
			mPlayer.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 0.2, 0), 1, 0.25, 0.25, 0.25, 0);
		}
	}
}
