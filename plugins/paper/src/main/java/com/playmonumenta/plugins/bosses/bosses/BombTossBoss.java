package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBombToss;
import org.bukkit.entity.LivingEntity;

/* TODO: Merge this with BossGrenadeLauncher */
public class BombTossBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bombtoss";
	public static final int detectionRange = 20;

	public BombTossBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Spell spell = new SpellBombToss(plugin, boss, detectionRange);

		super.constructBoss(spell, detectionRange);
	}
}
