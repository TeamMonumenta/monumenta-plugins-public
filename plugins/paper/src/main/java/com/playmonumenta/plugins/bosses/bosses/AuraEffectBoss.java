package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAura;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class AuraEffectBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_auraeffect";

	@BossParam(help = "Applies effects to players within an aura around the entity")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Radius in blocks where players receive effects")
		public int RADIUS = 35;

		@BossParam(help = "Height clamp in blocks specifically for vertical distance between the launcher and players")
		public int HEIGHT = 20;

		@BossParam(help = "Range in blocks that the launcher searches for players to target with this spell")
		public int DETECTION = 45;

		@BossParam(help = "Time in ticks between the launcher spawning and the first attempt to cast this spell")
		public int DELAY = 20;

		@BossParam(help = "Time period in ticks that governs how often effects are applied")
		public int PASSIVE_RATE = PASSIVE_RUN_INTERVAL_DEFAULT;

		@BossParam(help = "Particles summoned in the effect area")
		public ParticlesList PARTICLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 20, 0.0, 0.0, 0.0, 0.0, new Particle.DustOptions(Color.WHITE, 2.0f)))
			.build();

		@BossParam(help = "Particles summoned at the launcher")
		public ParticlesList PARTICLE_ENTITY = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 2, 1.0, 1.0, 1.0, 0.0, new Particle.DustOptions(Color.WHITE, 2.0f)))
			.build();

		@BossParam(help = "Effects applied to players in range")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "Can the aura be disabled by stuns and silences")
		public boolean CANCELABLE = false;
	}

	public AuraEffectBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		final Parameters p = BossParameters.getParameters(mBoss, identityTag, new Parameters());

		final List<Spell> passiveSpells = List.of(
			new SpellBaseAura(mBoss, p.RADIUS, p.HEIGHT,
				// Summon particles on and around launcher
				(Entity entity) -> {
					p.PARTICLE.spawn(mBoss, LocationUtils.getEntityCenter(entity), p.RADIUS / 2f, p.HEIGHT / 2f, p.RADIUS / 2f);
					p.PARTICLE_ENTITY.spawn(mBoss, LocationUtils.getEntityCenter(entity));
				},
				// Apply effects to players in range
				(p.EFFECTS.equals(EffectsList.EMPTY) ? null : (Player player) -> p.EFFECTS.apply(player, mBoss)),
				p.CANCELABLE
			)
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, p.DETECTION, null, p.DELAY, p.PASSIVE_RATE);
	}
}
