package com.playmonumenta.plugins.abilities.other;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class PatreonPurple extends Ability {
	public PatreonPurple(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean canUse(Player player) {
		int patreon = ScoreboardUtils.getScoreboardValue(player, "Patreon");
		int shinyPurple = ScoreboardUtils.getScoreboardValue(player, "ShinyPurple");
		return shinyPurple > 0 && patreon >= 10;
	}

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			mWorld.spawnParticle(Particle.DRAGON_BREATH, mPlayer.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
		}
	}
}
