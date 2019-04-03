package com.playmonumenta.bossfights.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.bossfights.Plugin;

public class IceAspectBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_iceaspect";
	public static final int detectionRange = 50;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new IceAspectBoss(plugin, boss);
	}

	public IceAspectBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super.constructBoss(plugin, identityTag, boss, null, null, detectionRange, null);
	}

	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		LivingEntity target = (LivingEntity) event.getEntity();
		target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 1, false, true));
	}
}
