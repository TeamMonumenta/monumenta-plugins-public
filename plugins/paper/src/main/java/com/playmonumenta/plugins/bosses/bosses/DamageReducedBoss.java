package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class DamageReducedBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_damage_reduced";
	public static final int detectionRange = 50;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DamageReducedBoss(plugin, boss);
	}

	public DamageReducedBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (event.getDamage() < 4) {
			Location loc = mBoss.getLocation();
			loc.getWorld().playSound(loc, Sound.ENTITY_SHULKER_HURT_CLOSED, SoundCategory.HOSTILE, 1f, 1f);
			new PartialParticle(Particle.ENCHANTMENT_TABLE, loc.add(0, 1.4, 0), 20, 0, 0, 0, 0.4).spawnAsEntityActive(mBoss);
			event.setCancelled(true);
		} else {
			event.setDamage(event.getDamage() - 4);
		}
	}
}

