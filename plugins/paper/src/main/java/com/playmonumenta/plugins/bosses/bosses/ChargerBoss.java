package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.TARGETS;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class ChargerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_charger";

	public static class Parameters extends BossParameters {
		//i would very like to set the damage to 0 but for this we need to rework all the mobs
		//with boss_charger in the game...so BIG NOPE
		@BossParam(help = "Damage dealt")
		public int DAMAGE = 15;

		@BossParam(help = "Damage type for use with the DAMAGE parameter")
		public DamageType DAMAGE_TYPE = DamageType.MELEE;

		@BossParam(help = "Time in ticks between the initial spell effects and cast")
		public int DURATION = TICKS_PER_SECOND + 5;

		@BossParam(help = "Time in ticks between the launcher spawning and the first attempt to cast this spell")
		public int DELAY = TICKS_PER_SECOND * 5;

		@BossParam(help = "Range in blocks that the launcher searches for players before this spell can run")
		public int DETECTION = 20;

		@BossParam(help = "Time in ticks the launcher waits before casting any other spell when this spell is cast")
		public int COOLDOWN = TICKS_PER_SECOND * 8;

		@BossParam(help = "Whether the launcher should pierce through hit targets while charging")
		public boolean STOP_ON_HIT = false;

		@BossParam(help = "Percent health True damage dealt")
		public double DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Effects applied to targets hit by the charge")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "Name of the spell used for chat messages")
		public String SPELL_NAME = "Charge";

		@BossParam(help = "Valid targets for this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		@BossParam(help = "Minimum distance between the launcher and target before a charge is initiated")
		public double MIN_DISTANCE = 0.0;

		@BossParam(help = "Whether the launcher should change target to a hit target after casting")
		public boolean CHANGE_TARGET = true;

		//Particle & Sounds!
		@BossParam(help = "Particles spawned at the launcher's location when it begins to channel the spell")
		public ParticlesList PARTICLE_WARNING = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.VILLAGER_ANGRY, 50, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Sounds played at the launcher's location when it begins to channel the spell")
		public SoundsList SOUND_WARNING = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.5f))
			.build();

		@BossParam(help = "Particles spawned along the charge path")
		public ParticlesList PARTICLE_TELL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT, 2, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Particles spawned on a hit target")
		public ParticlesList PARTICLE_HIT = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.BLOCK_CRACK, 5, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_BLOCK.createBlockData()))
			.add(new ParticlesList.CParticle(Particle.BLOCK_CRACK, 12, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData()))
			.build();

		@BossParam(help = "Particles spawned at the start and end locations of the charge")
		public ParticlesList PARTICLE_ROAR = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SMOKE_LARGE, 125, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Sound played at the start and end locations of the charge")
		public SoundsList SOUND_ROAR = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f))
			.build();

		@BossParam(help = "Particles spawned when the charge hits a target")
		public ParticlesList PARTICLE_ATTACK = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FLAME, 4, 0.5, 0.5, 0.5, 0.075))
			.add(new ParticlesList.CParticle(Particle.CRIT, 8, 0.5, 0.5, 0.5, 0.75))
			.build();
	}

	public ChargerBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(mBoss, identityTag, new Parameters());

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
			//same object
			//probably an older mob version?
			//build a new target from others config
			p.TARGETS = new EntityTargets(TARGETS.PLAYER, p.DETECTION, EntityTargets.Limit.DEFAULT, List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT, EntityTargets.PLAYERFILTER.NOT_STEALTHED));
			//by default Charger don't take player in stealth.
		}

		final Spell spell = new SpellBaseCharge(mPlugin, mBoss, p);

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
