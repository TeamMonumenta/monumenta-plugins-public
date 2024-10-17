package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBombToss;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/* TODO: Merge this with BossGrenadeLauncher */
public class BombTossNoBlockBreakBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bombtossnoblockbreak";
	public static final int detectionRange = 20;

	private static final int EXPLOSION_RADIUS = 4;

	public BombTossNoBlockBreakBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Spell spell = new SpellBombToss(plugin, boss, detectionRange, EXPLOSION_RADIUS, 1, 50, false, false);

		super.constructBoss(spell, detectionRange);
	}
}
