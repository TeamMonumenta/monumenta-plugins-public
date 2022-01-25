package com.playmonumenta.plugins.bosses.bosses;

import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class VolatileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_volatile";
	public static final int detectionRange = 20;

	private final Creeper mCreeper;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		if (!(boss instanceof Creeper)) {
			throw new Exception("Attempted to give non-creeper the " + identityTag + " ability: " + boss.toString());
		}
		return new VolatileBoss(plugin, boss);
	}

	public VolatileBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Creeper)) {
			throw new Exception("Attempted to give non-creeper the " + identityTag + " ability: " + boss.toString());
		}

		mCreeper = (Creeper)boss;

		// Boss effectively does nothing
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (event.getType() == DamageType.BLAST) {
			mCreeper.explode();
		}
	}
}
