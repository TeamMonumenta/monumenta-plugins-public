package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellEarthshake;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class EarthshakeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_earthshake";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Explosion radius of the spell")
		public int RADIUS = 4;

		@BossParam(help = "Range at which the spell can be cast")
		public int RANGE = 12;

		@BossParam(help = "not written")
		public int DELAY = 100;

		@BossParam(help = "Blast damage that this spell deals to players")
		public int DAMAGE = 40;

		@BossParam(help = "not written")
		public int COOLDOWN = 160;

		@BossParam(help = "not written")
		public int DETECTION = 20;

		@BossParam(help = "Time between casting the spell and the resulting explosion")
		public int FUSE_TIME = 50;

		@BossParam(help = "Whether the explosion also makes blocks fly around")
		public boolean FLY_BLOCKS = true;

		@BossParam(help = "Chance for a block to be thrown around and/or replaced")
		public double FLY_BLOCKS_CHANCE = 0.5;

		@BossParam(help = "Material to place where blocks were thrown from. If not air, will also work if throwing blocks is disabled and will replace blocks without throwing them.")
		public Material REPLACE_BLOCKS = Material.AIR;

		@BossParam(help = "Players hit will be pushed up by this amount, plus 0.5 if standing close to the center")
		public double KNOCK_UP_SPEED = 1.0;
		public boolean DO_KNOCK_UP = true;

		@BossParam(help = "You should not use this. use TARGETS instead.", deprecated = true)
		public boolean LINE_OF_SIGHT = true;

		@BossParam(help = "target of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT;

		@BossParam(help = "Sound played at the targeted player when the boss starts charging the ability ability")
		public SoundsList SOUND_WARNING = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 5.0f, 0.75f))
			.build();

		@BossParam(help = "Sound played once a second at the spell's target while the spell is charging")
		public SoundsList SOUND_CHARGE_TARGET = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 3.0f, 0.5f))
			.add(new SoundsList.CSound(Sound.BLOCK_GRAVEL_BREAK, 3.0f, 0.5f))
			.build();

		@BossParam(help = "Sound played every tick at the caster while the spell is charging. Pitch is automatically increased by 0.01 every tick")
		public SoundsList SOUND_CHARGE_BOSS = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 0.25f))
			.build();

		@BossParam(help = "Sound played when the explosion happens")
		public SoundsList SOUND_EXPLOSION = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 1.35f))
			.add(new SoundsList.CSound(Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 3.0f, 0.5f))
			.build();

		@BossParam(help = "Sound played when the explosion hits a player")
		public SoundsList SOUND_EXPLOSION_PLAYER = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.0f))
			.build();

		@BossParam(help = "Particles to spawn on explosion.")
		public ParticlesList PARTICLES_EXPLOSION = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CLOUD, 150, 0.0, 0.0, 0.0, 0.5))
			.add(new ParticlesList.CParticle(Particle.LAVA, 35, 2.0, 1.0, 2.0, 0.0))
			.add(new ParticlesList.CParticle(Particle.BLOCK_CRACK, 200, 2.0, 1.0, 2.0, 0.0, Material.DIRT.createBlockData()))
			.add(new ParticlesList.CParticle(Particle.CAMPFIRE_COSY_SMOKE, 35, 2.0, 1.0, 2.0, 0.1))
			.build();

		@BossParam(help = "Particles to spawn on explosion. 100 of these will be spawned, spread over the explosion area.")
		public ParticlesList PARTICLES_EXPLOSION_DIRECTIONAL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SMOKE_LARGE, 0, 0.0, 1.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Particles to spawn at the boss while the spell is charging.")
		public ParticlesList PARTICLES_CHARGE_BOSS = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.DRIP_LAVA, 2, 0.25, 0.45, 0.25, 1.0))
			.build();

		@BossParam(help = "Particles to spawn every tick at the target location while the spell is charging.")
		public ParticlesList PARTICLES_CHARGE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.BLOCK_CRACK, 2, 2.0, 0.1, 2.0, 0.0, Material.STONE.createBlockData()))
			.add(new ParticlesList.CParticle(Particle.LAVA, 2, 0.25, 0.25, 0.25, 0.1))
			.build();

		@BossParam(help = "Particles to spawn every 2 ticks at the target location while the spell is charging.")
		public ParticlesList PARTICLES_CHARGE_TWO_TICKS = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CAMPFIRE_COSY_SMOKE, 4, 4.0, 4.0, 1.0, 0.05))
			.build();

		@BossParam(help = "Particles to spawn every 20 ticks at the target location while the spell is charging.")
		public ParticlesList PARTICLES_CHARGE_TWENTY_TICKS = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.BLOCK_CRACK, 80, 2.0, 0.1, 2.0, 0.0, Material.DIRT.createBlockData()))
			.add(new ParticlesList.CParticle(Particle.CAMPFIRE_COSY_SMOKE, 8, 2.0, 0.1, 2.0, 0.0))
			.build();

		@BossParam(help = "Particles to spawn every 20 ticks at the circular border of the spell's area of effect.")
		public ParticlesList PARTICLES_CHARGE_TWENTY_TICKS_BORDER = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SMOKE_NORMAL, 1, 0.1, 0.1, 0.1, 0.0))
			.build();

	}

	public EarthshakeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT) {
			p.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, p.RANGE, EntityTargets.Limit.DEFAULT_ONE, p.LINE_OF_SIGHT ? List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT, EntityTargets.PLAYERFILTER.NOT_STEALTHED) : List.of(EntityTargets.PLAYERFILTER.NOT_STEALTHED));
		}

		Spell spell = new SpellEarthshake(plugin, boss, p);
		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}