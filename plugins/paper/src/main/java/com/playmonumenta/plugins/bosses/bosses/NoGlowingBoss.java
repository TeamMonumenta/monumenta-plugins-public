package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public class NoGlowingBoss extends BossAbilityGroup {

	public static String identityTag = "boss_no_glowing";
	public static int detectionRange = 100;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) {
		return new NoGlowingBoss(plugin, boss);
	}

	public NoGlowingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		if (event.getAction() == Action.ADDED && event.getNewEffect() != null && event.getNewEffect().getType().equals(PotionEffectType.GLOWING)) {
			event.setCancelled(true);
		}
	}
}
