package com.playmonumenta.bossfights.bosses;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.bossfights.Plugin;

public class DamageReducedBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_damage_reduced";
	public static final int detectionRange = 50;

	private final LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DamageReducedBoss(plugin, boss);
	}

	public DamageReducedBoss(Plugin plugin, LivingEntity boss) throws Exception {
		mBoss = boss;

		super.constructBoss(plugin, identityTag, boss, null, null, detectionRange, null);
	}

	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (event.getFinalDamage() < 4) {
			Location loc = mBoss.getLocation();
			loc.getWorld().playSound(loc, Sound.ENTITY_SHULKER_HURT_CLOSED, SoundCategory.HOSTILE, 1f, 1f);
			loc.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, loc.add(0, 1.4, 0), 20, 0, 0, 0, 0.4);
			event.setCancelled(true);
		} else {
			event.setDamage(event.getFinalDamage() - 4);
		}
	}
}

