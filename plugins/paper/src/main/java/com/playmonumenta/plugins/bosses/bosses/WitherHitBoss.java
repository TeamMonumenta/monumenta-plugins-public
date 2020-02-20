package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WitherHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_witherhit";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WitherHitBoss(plugin, boss);
	}

	public WitherHitBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super.constructBoss(plugin, identityTag, boss, null, null, detectionRange, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		LivingEntity target = (LivingEntity) event.getEntity();
		target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1, false, true));
	}
}

