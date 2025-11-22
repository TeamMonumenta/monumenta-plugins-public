package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellOmen;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public final class OmenBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_omen";

	public static class Parameters extends BossParameters {
		@BossParam(help = "spell name")
		public String SPELL_NAME = "Omen";
		@BossParam(help = "telegraph duration in ticks")
		public int TEL_DURATION = 20 * 2;
		@BossParam(help = "telegraph interval in ticks (Only used if instant is set to true)")
		public int TEL_INTERVAL = 4;
		@BossParam(help = "max range of the ground lines")
		public double MAX_RANGE = 24;
		@BossParam(help = "width of the omen")
		public int WIDTH = 5;
		@BossParam(help = "velocity of the ground lines")
		public double VELOCITY = 20;
		@BossParam(help = "")
		public int COOLDOWN = 150;
		@BossParam(help = "")
		public int DELAY = 40;
		@BossParam(help = "detection radius")
		public int DETECTION = 40;
		@BossParam(help = "degree offset of the blades")
		public int DEGREE_OFFSET = 0;
		@BossParam(help = "offset of the tip of the omen")
		public double BLADE_HEAD_OFFSET = 0.0;
		@BossParam(help = "offset of the two tail ends of the omen")
		public double BLADE_TAIL_OFFSET = 0.0;
		@BossParam(help = "blade y offset")
		public double HEIGHT_OFFSET = 1.0;
		@BossParam(help = "how many branches the omen will split into, all equal in angle in between each other (try not to go below 3 or so if you arent using targeting)")
		public int SPLITS = 4;
		@BossParam(help = "angle between each split")
		public int SPLIT_ANGLE = 90;
		@BossParam(help = "spell damage")
		public double DAMAGE = 20;
		@BossParam(help = "spell damage in %")
		public double DAMAGE_PERCENTAGE = 0;
		@BossParam(help = "horizontal knockback velocity")
		public float KB_X = 0.6f;
		@BossParam(help = "vertical knockback velocity")
		public float KB_Y = 0.8f;
		@BossParam(help = "The type of the damage dealt by the attack. Default: MAGIC")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;
		@BossParam(help = "effects given on hit")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "width of the gap between omen particles, across the omen horizontally")
		public double PARTICLE_GAP = 1.0;
		@BossParam(help = "particle gap but for tel")
		public double PARTICLE_GAP_TEL = 1.0;
		@BossParam(help = "size of the center safe spot, in percent. Default: 1.0")
		public double SAFESPOT_SIZE = 1.0;
		@BossParam(help = "height of the blade hitbox. Default: 0.3 blocks")
		public double HITBOX_HEIGHT = 0.3;
		@BossParam(help = "whether the telegraph and attack are instant rather than having travel time")
		public boolean INSTANT = false;
		@BossParam(help = "whether boss can move during the cast")
		public boolean CAN_MOVE = true;
		@BossParam(help = "whether the omen will be directed at a player")
		public boolean DO_TARGETING = false;
		@BossParam(help = "whether the spell respects iframes")
		public boolean RESPECT_IFRAMES = true;
		@BossParam(help = "sound of omen launch")
		public SoundsList SOUND_LAUNCH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.75f))
			.build();
		@BossParam(help = "sound of getting hit")
		public SoundsList SOUND_HIT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 2.0f))
			.build();
		@BossParam(help = "sound that plays throughout the telegraph's duration")
		public SoundsList SOUND_WAVE = SoundsList.EMPTY;
		@BossParam(help = "(VERY OPTIONAL) sound that plays constantly for the duration of the omen's actual travel time")
		public SoundsList SOUND_TEL = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.UI_TOAST_IN, 1.5f, 1.9f))
			.build();
		@BossParam(help = "sound that plays at the start of the spell cast")
		public SoundsList SOUND_WARN = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.0f))
			.build();
		@BossParam(help = "particles of the omen telegraph")
		public ParticlesList PARTICLE_TEL_SWIRL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SOUL_FIRE_FLAME, 1, 0.1, 0.1, 0.1, 0.0))
			.build();
		@BossParam(help = "particles of the omen telegraph")
		public ParticlesList PARTICLE_TEL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 1, 0.1, 0.1, 0.1, 0.0, new Particle.DustOptions(Color.RED, 0.8f)))
			.build();
		@BossParam(help = "particles of the omen")
		public ParticlesList PARTICLE_OMEN = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 1, 0.1, 0.1, 0.1, 0.0, new Particle.DustOptions(Color.fromRGB(0xc700ff), 1.0f)))
			.add(new ParticlesList.CParticle(Particle.SOUL_FIRE_FLAME, 1, 0.1, 0.1, 0.1, 0.0))
			.build();
		@BossParam(help = "targets of the spell, if targeting param is set to true")
		public EntityTargets TARGETS = EntityTargets.GENERIC_ONE_PLAYER_TARGET;

	}

	public OmenBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		OmenBoss.Parameters p = OmenBoss.Parameters.getParameters(boss, identityTag, new OmenBoss.Parameters());

		Spell spell = new SpellOmen(plugin, boss, p);

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}