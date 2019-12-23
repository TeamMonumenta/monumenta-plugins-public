package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellProjectileDeflection;

public class LivingBladeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_livingblade";
	public static final int detectionRange = 40;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LivingBladeBoss(plugin, boss);
	}

	public LivingBladeBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		World world = boss.getWorld();
		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseParticleAura(boss, 1,
			                          (LivingEntity mBoss) -> {
			                              world.spawnParticle(Particle.SPELL_INSTANT, mBoss.getLocation().add(0, 1, 0), 8, 0.2, 0.4, 0.2, 0);
			                          }),
			new SpellProjectileDeflection(mBoss)
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
