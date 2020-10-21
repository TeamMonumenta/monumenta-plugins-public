package com.playmonumenta.plugins.abilities.warlock;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Harvester extends Ability {

	private static final int COOLDOWN_REDUCTION = 10;
	private static final double PERCENT_HEAL = 0.05;

	public Harvester(Plugin plugin, Player player) {
		super(plugin, player, "Harvester of the Damned");
		mInfo.mScoreboardId = "Harvester";
		mInfo.mShorthandName = "HotD";
		mInfo.mDescriptions.add("Whenever an enemy dies within 8 blocks of you, reduce the cooldown of your skills by .5s.");
		mInfo.mDescriptions.add("You also heal for 5% of your max health when an enemy dies within 8 blocks of you.");
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 9, 0.35, 0.45, 0.35, 0.001);

		// We want this to run after any relevant abilities have gone on cooldown
		new BukkitRunnable() {
			@Override
			public void run() {
				mPlugin.mTimers.updateCooldowns(mPlayer, COOLDOWN_REDUCTION);
			}
		}.runTaskLater(mPlugin, 0);

		if (getAbilityScore() > 1) {
			PlayerUtils.healPlayer(mPlayer, PERCENT_HEAL * mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		}
	}

	@Override
	public double entityDeathRadius() {
		return 8;
	}

}
