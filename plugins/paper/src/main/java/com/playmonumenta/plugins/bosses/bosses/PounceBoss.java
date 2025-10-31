package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.AbstractMap;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class PounceBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_pounce";

	@BossParam(help = "The launcher gains the ability to jump then slam at targets")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Name of the spell used for chat messages")
		public String SPELL_NAME = "Meteor Slam";
		@BossParam(help = "Minimum distance between the launcher and target before a slam is initiated")
		public int MIN_RANGE = 0;
		@BossParam(help = "Range in blocks that the launcher searches for players to target with this spell")
		public int DETECTION = 32;
		@BossParam(help = "Time in ticks between the launcher spawning and the first attempt to cast this spell")
		public int DELAY = TICKS_PER_SECOND * 5;
		@BossParam(help = "Upwards velocity applied to the launcher")
		public int JUMP_HEIGHT = 1;
		@BossParam(help = "How far the launcher runs before leaping")
		public int RUN_DISTANCE = 0;
		@BossParam(help = "Time in ticks the launcher waits before casting any other spell when this spell is cast")
		public int COOLDOWN = TICKS_PER_SECOND * 8;
		@BossParam(help = "Adjusts the distance of the leap by changing the launcher's velocity at the start of the jump")
		public double VELOCITY_MULTIPLIER = 0.5;

		@BossParam(help = "Radius in blocks in which players are damaged when the launcher lands")
		public double DAMAGE_RADIUS = 3;
		@BossParam(help = "Blast Damage dealt")
		public double DAMAGE = 0;
		@BossParam(help = "Percent health True damage dealt")
		public double DAMAGE_PERCENT = 0;
		@BossParam(help = "Effects applied to players hit by the pounce")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		//Particles & Sounds
		@BossParam(help = "Sounds played at the launcher's location when it begins to channel the spell")
		public SoundsList SOUND_START = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f))
			.build();

		@BossParam(help = "Particles spawned at the launcher's location when it begins to channel the spell")
		public ParticlesList PARTICLE_START = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.LAVA, 15, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Particles spawned at the start of the leap")
		public ParticlesList PARTICLE_LEAP = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.LAVA, 15, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Sounds played at the start of the leap")
		public SoundsList SOUND_LEAP = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_HORSE_JUMP, 1.0f, 1.0f))
			.build();

		@BossParam(help = "Particles spawned while the launcher is leaping")
		public ParticlesList PARTICLE_LEAPING = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 4, 0.5, 0.5, 0.5, 0.0, new Particle.DustOptions(Color.WHITE, 1.0f)))
			.build();

		@BossParam(help = "Particles spawned as a ring when the launcher collides with a hit player or block")
		public ParticlesList PARTICLE_RING = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FLAME, 1, 0.1, 0.1, 0.1, 0.1))
			.add(new ParticlesList.CParticle(Particle.CLOUD, 1, 0.1, 0.1, 0.1, 0.1))
			.build();

		@BossParam(help = "Sounds played when the launcher collides with a hit player or block")
		public SoundsList SOUND_HIT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.0f))
			.add(new SoundsList.CSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.25f))
			.build();

		@BossParam(help = "Particles spawned when the launcher collides with a hit player or block")
		public ParticlesList PARTICLE_HIT = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FLAME, 60, 0.0, 0.0, 0.0, 0.2))
			.add(new ParticlesList.CParticle(Particle.EXPLOSION_NORMAL, 20, 0.0, 0.0, 0.0, 0.3))
			.add(new ParticlesList.CParticle(Particle.LAVA, 27, 3.0, 0.25, 3.0, 0.0))
			.build();
	}

	public PounceBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(mBoss, identityTag, new Parameters());
		Spell spell = new SpellBaseSlam(mPlugin, mBoss, p.JUMP_HEIGHT, p.DETECTION, p.MIN_RANGE, p.RUN_DISTANCE,
			p.COOLDOWN, p.VELOCITY_MULTIPLIER,
			(World world, Location loc) -> {
				mBoss.setMetadata(BlockPlacerBoss.PREVENT_BLOCK_PLACEMENT, new FixedMetadataValue(mPlugin, null));
				p.SOUND_START.play(loc, 1f, 1f);
				p.PARTICLE_START.spawn(mBoss, loc, 1d, 0d, 1d);
			},
			(World world, Location loc) -> {
				p.SOUND_LEAP.play(loc, 1f, 1f);
				p.PARTICLE_LEAP.spawn(mBoss, loc, 1d, 0f, 1d, 0d);
			},
			(World world, Location loc) -> p.PARTICLE_LEAPING.spawn(mBoss, loc, 0.5, 0.5, 0.5, 1d),
			(World world, @Nullable Player player, Location loc, Vector dir) -> {
				mBoss.removeMetadata(BlockPlacerBoss.PREVENT_BLOCK_PLACEMENT, mPlugin);
				ParticleUtils.explodingRingEffect(mPlugin, loc, 4, 1, 4,
					List.of(new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) ->
						p.PARTICLE_RING.spawn(mBoss, loc, 0.1, 0.1, 0.1, 0.1))
					));
				p.SOUND_HIT.play(loc, 1, 1);
				p.PARTICLE_HIT.spawn(mBoss, loc);

				for (final Player playerInRange : PlayerUtils.playersInRange(loc, p.DAMAGE_RADIUS, true)) {
					if (p.DAMAGE > 0.0) {
						DamageUtils.damage(mBoss, playerInRange, new DamageEvent.Metadata(DamageType.BLAST, null,
							null, p.SPELL_NAME), p.DAMAGE, false, true, true);
					}

					if (p.DAMAGE_PERCENT > 0.0) {
						DamageUtils.damage(mBoss, playerInRange, new DamageEvent.Metadata(DamageType.TRUE, null,
								null, p.SPELL_NAME), p.DAMAGE_PERCENT * EntityUtils.getMaxHealth(playerInRange),
							true, true, true);
					}
					p.EFFECTS.apply(playerInRange, mBoss);
				}
			}
		);
		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
