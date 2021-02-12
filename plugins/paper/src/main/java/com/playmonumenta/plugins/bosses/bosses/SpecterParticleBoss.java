package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellSpecterParticle;

public class SpecterParticleBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_specterparticle";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SpecterParticleBoss(plugin, boss);
	}

	public SpecterParticleBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = Arrays.asList(
			new SpellSpecterParticle(boss)
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}
