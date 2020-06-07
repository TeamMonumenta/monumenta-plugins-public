package com.playmonumenta.plugins.abilities.warlock;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Harvester extends Ability {

	public Harvester(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Harvester of the Damned");
		mInfo.mScoreboardId = "Harvester";
		mInfo.mShorthandName = "HotD";
		mInfo.mDescriptions.add("Whenever an enemy dies within 8 blocks of you, reduce the cooldown of your skills by .5s.");
		mInfo.mDescriptions.add("You also heal for 5% of your max health when an enemy dies within 8 blocks of you.");
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		int level = getAbilityScore();
		World world = mPlayer.getWorld();
		mPlugin.mTimers.updateCooldowns(mPlayer, 10);
		if (level > 1) {
			PlayerUtils.healPlayer(mPlayer, 1);
		}
		world.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 9, 0.35, 0.45, 0.35, 0.001);
	}

	@Override
	public double entityDeathRadius() {
		return 8;
	}

}
