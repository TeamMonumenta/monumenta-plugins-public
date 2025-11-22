package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.EntityGlowEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public class NoGlowingBoss extends BossAbilityGroup {

	public static String identityTag = "boss_no_glowing";
	public static int detectionRange = 100;

	public NoGlowingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void bossGlowed(EntityGlowEvent event) {
		if (event.getGlowingPriority() == GlowingManager.PLAYER_ABILITY_PRIORITY) {
			event.setCancelled(true);
		}
	}

	@Override
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		if (event.getAction() == Action.ADDED && event.getNewEffect() != null && event.getNewEffect().getType().equals(PotionEffectType.GLOWING)) {
			event.setCancelled(true);
		}
	}
}
