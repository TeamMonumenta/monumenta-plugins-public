package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAura;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class AuraEffectBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_auraeffect";

	@BossParam(help = "Applies effects to players within an aura around the entity")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Spherical effect radius")
		public int RADIUS = 35;

		@BossParam(help = "Only apply to players within this much y value")
		public int HEIGHT = 20;

		@BossParam(help = "Effects only run if a player is within this range")
		public int DETECTION = 45;

		@BossParam(help = "Delay after spawning before starting effects")
		public int DELAY = 20;

		@BossParam(help = "How often effects will be applied")
		public int PASSIVE_RATE = PASSIVE_RUN_INTERVAL_DEFAULT;

		@BossParam(help = "Particles summoned in the effect area")
		public ParticlesList PARTICLE = ParticlesList.fromString("[(REDSTONE,20,0,0,0,0,#ffffff,2.0)]");

		@BossParam(help = "Particles summoned near the entity")
		public ParticlesList PARTICLE_ENTITY = ParticlesList.fromString("[(REDSTONE,2,1,1,1,0,#ffffff,2.0)]");

		@BossParam(help = "Effects applied to the player when inside the range")
		public EffectsList EFFECTS = EffectsList.EMPTY;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraEffectBoss(plugin, boss);
	}

	public AuraEffectBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(boss, p.RADIUS, p.HEIGHT, p.RADIUS,
				(Entity entity) -> {
					p.PARTICLE.spawn(entity.getLocation(), p.RADIUS / 2, p.HEIGHT / 2, p.RADIUS / 2);
					p.PARTICLE_ENTITY.spawn(entity.getLocation().clone().add(0, 1, 0));
				},
				(p.EFFECTS == EffectsList.EMPTY ? null :
					(Player player) -> {
						p.EFFECTS.apply(player, mBoss);
					}
				)
			)
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, p.DETECTION, null, p.DELAY, p.PASSIVE_RATE);
	}
}
