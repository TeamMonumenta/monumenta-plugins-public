package com.playmonumenta.plugins.bosses.bosses;

import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FocusFireBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_focusfire";
	public static final int detectionRange = 15;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FocusFireBoss(plugin, boss);
	}

	public FocusFireBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		//If we hit a player
		if (damagee instanceof Player player) {
			//Set all nearby mobs to target them
			for (LivingEntity le : EntityUtils.getNearbyMobs(mBoss.getLocation(), detectionRange)) {
				if (le instanceof Mob mob) {
					mob.setTarget(player);
				}
			}
			//Let the players know something happened
			player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.3f, 0.9f);
			player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 25, 1.5, 1.5, 1.5);
		}
	}
}
