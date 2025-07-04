package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellDevour;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class DevourBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_devour";

	public static class Parameters extends BossParameters {
		// Timing Related Parameters
		@BossParam(help = "Number of ticks before ability can be casted.")
		public int DELAY = 0;
		@BossParam(help = "Number of ticks at the start of ability cast before we begin the loop.")
		public int INITIAL_DELAY = 0;
		@BossParam(help = "Number of ticks before actual fangs spawn after particles tells. It also is the duration of particle tells.")
		public int INDICATOR_DELAY = 20;
		@BossParam(help = "Number of ticks between each iteration.")
		public int ITERATION_DELAY = 2;
		@BossParam(help = "Number of iterations.")
		public int NUM_ITERATION = 5;
		@BossParam(help = "Ability Cooldown in Ticks.")
		public int COOLDOWN = 12 * 20;

		// Range Related Parameters
		@BossParam(help = "Range at which player must be within for ability to execute.")
		public int DETECTION = 30;
		@BossParam(help = "Initial Radius for First set of Fangs.")
		public double INITIAL_RADIUS = 1.0;
		@BossParam(help = "Total amount of loops of Fangs we do (similar to thickness). Loop distance is based on RADIUS_INCREMENT.")
		public int RING_THICKNESS = 1;
		@BossParam(help = "Distance between each set of fangs.")
		public double RADIUS_INCREMENT = 1.5;
		@BossParam(help = "Start angle, in degrees.")
		public double INITIAL_ANGLE = -7.5;
		@BossParam(help = "Final angle, in degrees.")
		public double FINAL_ANGLE = 7.5;
		@BossParam(help = "Distance along ring between two fangs (Think of it like an Angle Threshold, but it will scale with radii).")
		public double RING_SPACING = 1.15;
		@BossParam(help = "Y-level offset.")
		public double Y_OFFSET = 0.0;

		// Damage Parameters
		@BossParam(help = "Amount of damage each fang will deal")
		public double DAMAGE = 10;
		@BossParam(help = "Target of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_ONE_PLAYER_TARGET;

		// Visual and Sound Tells Parameters
		@BossParam(help = "Sound played at the beginning of Cast.")
		public SoundsList SOUND_INITIAL = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 3.0f, 1.0f))
			.build();
		@BossParam(help = "Sound played at the beginning of indicators.")
		public SoundsList SOUND_INDICATOR = SoundsList.EMPTY;
		@BossParam(help = "Particles spawn at beginning of Cast.")
		public ParticlesList PARTICLES_INITIAL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_WITCH, 20, 0.0, 0.0, 0.0, 0.0))
			.build();
		@BossParam(help = "Particles spawn at beginning of indicators at every fang spot.")
		public ParticlesList PARTICLES_INDICATOR = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 2, 0.0, 0.5, 0.0, 0.5, new Particle.DustOptions(Color.BLACK, 1.0f)))
			.build();
		@BossParam(help = "Offset for Particles that spawn at beginning of indicators at every fang spot.")
		public double PARTICLES_INDICATOR_Y_OFFSET = 0;
		@BossParam(help = "If false, evoker fangs are silenced and don't do the chomping sound.")
		public boolean SOUNDS_EVOKER_FANGS = true;
	}

	private final SpellDevour mSpell;

	public DevourBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new DevourBoss.Parameters());

		mSpell = new SpellDevour((com.playmonumenta.plugins.Plugin) plugin, boss, p);

		super.constructBoss(mSpell, p.DETECTION, null, p.DELAY);
	}
}