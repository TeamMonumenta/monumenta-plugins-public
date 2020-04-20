package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellProjectileDeflection;

public class ProjectileDeflectionBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_projdeflect";
	public static final int detectionRange = 40;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LivingBladeBoss(plugin, boss);
	}

	public ProjectileDeflectionBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		List<Spell> passiveSpells = Arrays.asList(
			new SpellProjectileDeflection(mBoss)
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
