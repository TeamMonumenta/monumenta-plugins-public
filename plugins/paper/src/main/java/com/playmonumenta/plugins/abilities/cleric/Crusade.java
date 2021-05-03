package com.playmonumenta.plugins.abilities.cleric;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.World;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.events.CustomDamageEvent;

public class Crusade extends Ability {

	public Crusade(Plugin plugin, Player player) {
		super(plugin, player, "Crusade");
		mInfo.mScoreboardId = "Crusade";
		mInfo.mShorthandName = "Crs";
		mInfo.mDescriptions.add("All ability damage against undead is increased by 33%.");
		mInfo.mDescriptions.add("Humanoid enemies (anything affected by Duelist) count as Undead for Cleric skills.");
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		LivingEntity mob = (LivingEntity) event.getDamaged();
		World world = mob.getWorld();
		if (EntityUtils.isUndead(mob) || (getAbilityScore() == 2 && EntityUtils.isHumanoid(mob))) {
			world.spawnParticle(Particle.CRIT_MAGIC, mob.getEyeLocation(), 10, 0.25, 0.5, 0.25, 0);
			double originalDamage = event.getDamage();
			double modifiedDamage = originalDamage * 1.33;
			event.setDamage(modifiedDamage);
		}
	}
}
