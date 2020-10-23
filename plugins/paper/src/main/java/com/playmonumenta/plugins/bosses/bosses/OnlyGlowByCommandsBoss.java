package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public class OnlyGlowByCommandsBoss extends BossAbilityGroup {

	public static String identityTag = "boss_no_class_glowing";
	public static int detectionRange = 100;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) {
		return new OnlyGlowByCommandsBoss(plugin, boss);
	}

	public OnlyGlowByCommandsBoss(Plugin plugin, LivingEntity boss) {
		super.constructBoss(plugin, identityTag, boss, null, null, detectionRange, null);
	}

	@Override
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		if (event.getAction() == Action.ADDED && event.getNewEffect() != null && event.getNewEffect().getType() == PotionEffectType.GLOWING && event.getCause() != Cause.COMMAND) {
			event.setCancelled(true);
		}
	}
}
