package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellProjectileDeflection;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class LivingBladeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_livingblade";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LivingBladeBoss(plugin, boss);
	}

	public LivingBladeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseParticleAura(boss, 1,
				(LivingEntity mob) -> {
					new PartialParticle(Particle.SPELL_INSTANT, mob.getLocation().add(0, 1, 0), 8, 0.2, 0.4, 0.2, 0).spawnAsEntityActive(boss);
				}),
			new SpellProjectileDeflection(boss)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}
