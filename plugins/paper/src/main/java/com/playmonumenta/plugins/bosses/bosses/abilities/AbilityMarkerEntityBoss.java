package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;

public class AbilityMarkerEntityBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_ability_marker_entity";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AbilityMarkerEntityBoss(plugin, boss);
	}

	public AbilityMarkerEntityBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		boss.setInvulnerable(true);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), 0, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		event.setCancelled(true);
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		event.setCancelled(true);
	}

}
