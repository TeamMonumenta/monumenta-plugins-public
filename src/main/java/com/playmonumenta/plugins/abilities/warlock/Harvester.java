package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Harvester extends Ability {

	public Harvester(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Harvester";
	}

	@Override
	public void EntityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		int level = getAbilityScore();
		World world = mPlayer.getWorld();
		mPlugin.mTimers.UpdateCooldowns(mPlayer, 8);
		if (level > 1) {
			PlayerUtils.healPlayer(mPlayer, 1);
		}
		world.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 9, 0.35, 0.45, 0.35, 0.001);
	}

	@Override
	public double EntityDeathRadius() {
		return 8;
	}

}
