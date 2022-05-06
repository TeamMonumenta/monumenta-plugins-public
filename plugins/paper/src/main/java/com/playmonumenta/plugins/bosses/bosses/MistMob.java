package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class MistMob extends BossAbilityGroup {

	public static final int detectionRange = 20;
	public static final String identityTag = "MistMob";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) {
		return new MistMob(plugin, boss);
	}

	public MistMob(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		if (event == null || event.getEntity() == null) {
			//If the mob explodes it hits this method, but the event cannot grab the entity, so to prevent null pointers, this is needed
			return;
		}
		Entity entity = event.getEntity();
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at " + entity.getUniqueId() + " run function monumenta:dungeons/mist/mob_death");
	}
}
