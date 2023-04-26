package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.particle.PPCircle;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class ParticleRingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_particlering";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Radius of the ring.")
		public double RADIUS = 3;

		@BossParam(help = "Particle list. Count is multiplied by density (useful if there are multiple particles).")
		public ParticlesList PARTICLE = new ParticlesList(List.of(new ParticlesList.CParticle(Particle.END_ROD)));

		@BossParam(help = "Frequency at which the ring is displayed, in ticks.")
		public int FREQUENCY = 5;

		@BossParam(help = "Density of the ring. Density = Particle Count / (2pi * Radius) (in ring mode)")
		public double DENSITY = 5;

		@BossParam(help = "Height of the ring relative to mob's foot level.")
		public double HEIGHT = 0;

		@BossParam(help = "Whether only a ring is displayed (TRUE) or a filled in circle is displayed (FALSE).")
		public boolean RING_MODE = true;

		@BossParam(help = "Detection range.")
		public int DETECTION = 30;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ParticleRingBoss(plugin, boss);
	}

	public ParticleRingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		SpellBaseParticleAura.ParticleEffect[] particleEffects = p.PARTICLE.getParticleList().stream()
			.map(c -> new PPCircle(c.mParticle, boss.getLocation(), p.RADIUS).countPerMeter(p.DENSITY * c.mCount).delta(c.mDx, c.mDy, c.mDz).ringMode(p.RING_MODE).extra(c.mVelocity).data(c.mExtra2))
			.map(ppc -> (SpellBaseParticleAura.ParticleEffect) b -> ppc.location(b.getLocation().clone().add(0, p.HEIGHT, 0)).spawnAsEnemy())
			.toArray(SpellBaseParticleAura.ParticleEffect[]::new);
		List<Spell> passiveSpells = List.of(new SpellBaseParticleAura(boss, p.FREQUENCY, particleEffects));

		super.constructBoss(SpellManager.EMPTY, passiveSpells, p.DETECTION, null);
	}
}
