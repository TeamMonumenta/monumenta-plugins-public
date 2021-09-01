package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAura;
import com.playmonumenta.plugins.utils.BossUtils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class AuraEffectBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_auraeffect";

	public static class Parameters {
		public int RADIUS = 35;
		public int HEIGHT = 20;
		public int DETECTION = 45;

		/** Particles summoned in the air */
		public ParticlesList PARTICLE = ParticlesList.fromString("[(redstone,20,0,0,0,#ffffff,2.0)]");
		/** Particles summoned near the boss */
		public ParticlesList PARTICLE_MOB = ParticlesList.fromString("[(redstone,2,1,1,1,#ffffff,2.0)]");

		/** Effects applied to the player when inside the range*/
		public EffectsList EFFECTS = EffectsList.EMPTY;
	}


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraEffectBoss(plugin, boss);
	}

	public AuraEffectBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(boss, p.RADIUS, p.HEIGHT, p.RADIUS,
				(Entity entity) -> {
					p.PARTICLE.spawn(entity.getLocation(), p.RADIUS / 2, p.HEIGHT / 2, p.RADIUS / 2);
					p.PARTICLE_MOB.spawn(entity.getLocation().clone().add(0, 1, 0));
				},
				(p.EFFECTS == EffectsList.EMPTY ? null :
					(Player player) -> {
						p.EFFECTS.apply(player, mBoss);
					}
				)
			)
		);
		super.constructBoss(null, passiveSpells, p.DETECTION, null);
	}
}
