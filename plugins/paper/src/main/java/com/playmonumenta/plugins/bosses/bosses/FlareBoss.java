package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellFlare;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class FlareBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_flare";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Explosion radius of the spell")
		public int RADIUS = 3;

		@BossParam(help = "Range at which the spell can be cast")
		public double RANGE = 20;

		@BossParam(help = "delay before casting")
		public int DELAY = 100;

		@BossParam(help = "Blast damage that this spell deals to players")
		public int DAMAGE = 40;

		@BossParam(help = "time between casts")
		public int COOLDOWN = 160;

		@BossParam(help = "range which players are in for this spell to cast")
		public int DETECTION = 20;
		@BossParam(help = "how many additional flare lines there will be")
		public int SPLITS = 1;
		@BossParam(help = "angle between the splits")
		public int SPLIT_ANGLE = 90;
		@BossParam(help = "starting angle of the flares. the actual angle of splits is a bit tricky, it's tied to the amount of splits there are")
		public double START_ANGLE = 90;
		@BossParam(help = "speed of the telegraph lines")
		public double TEL_SPEED = 1.25;
		@BossParam(help = "number of explosions done before reaching the target")
		public int EXPLOSIONS = 5;
		@BossParam(help = "interval between explosions")
		public int EXPLOSION_INTERVAL = 20;
		@BossParam(help = "Time between casting the spell and the resulting explosions")
		public int FUSE_TIME = 50;
		@BossParam(help = "whether or not the boss is rooted during cast")
		public boolean CAN_MOVE = false;
		@BossParam(help = "Players hit will be pushed up by this amount, plus 0.5 if standing close to the center")
		public double KNOCK_UP_SPEED = 0.3;
		public boolean DO_KNOCK_UP = true;
		@BossParam(help = "You should not use this. use TARGETS instead.", deprecated = true)
		public boolean LINE_OF_SIGHT = true;
		@BossParam(help = "target of this spell (maybe keept this set to 1 if you plan to do multiple splits)")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT;
		@BossParam(help = "Sound played at the targeted player when the boss starts charging the ability ability")
		public SoundsList SOUND_WARNING = SoundsList.fromString("[(BLOCK_ENCHANTMENT_TABLE_USE,4,0.75)]");
		@BossParam(help = "Sound played once a second at the spell's target while the spell is charging")
		public SoundsList SOUND_CHARGE_TARGET = SoundsList.fromString("[(ENTITY_EVOKER_CAST_SPELL,3,0.9)]");
		@BossParam(help = "Sound played every tick at the caster while the spell is charging. Pitch is automatically increased by 0.01 every tick")
		public SoundsList SOUND_CHARGE_BOSS = SoundsList.fromString("[(UI_TOAST_IN,1,0.5)]");
		@BossParam(help = "Sound played when the explosion happens")
		public SoundsList SOUND_EXPLOSION = SoundsList.fromString("[(ENTITY_LIGHTNING_BOLT_IMPACT,2,1.35),(ENTITY_FIREWORK_ROCKET_LAUNCH,2,1.0),(ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,3,1.0)]");
		@BossParam(help = "Sound played when the explosion hits a player")
		public SoundsList SOUND_EXPLOSION_PLAYER = SoundsList.fromString("[(ENTITY_ZOMBIE_VILLAGER_CURE,1,2.0)]");
		@BossParam(help = "Particles for the pillar visual effect")
		public ParticlesList PARTICLES_PILLAR = ParticlesList.fromString("[(END_ROD,5,0.15,0.15,0.15,0.05),(FIREWORKS_SPARK,10,0.2,0.2,0.2,0.05)]");
		@BossParam(help = "Particles to spawn on the ground in a ring for the explosion.")
		public ParticlesList PARTICLES_EXPLOSION_RINGS = ParticlesList.fromString("[(CRIT_MAGIC,5,0.1,0.1,0.1,0.05),(ELECTRIC_SPARK,1,0.5,0.1,0.5,0.0)]");
		@BossParam(help = "Particles to spawn at the boss while the spell is charging.")
		public ParticlesList PARTICLES_CHARGE_BOSS = ParticlesList.fromString("[(ELECTRIC_SPARK,1,0.25,0.45,0.25,1)]");
		@BossParam(help = "particles for the directional indicator for the flares")
		public ParticlesList PARTICLES_FLARE_TEL = ParticlesList.fromString("[(WAX_OFF,3,0.25,0.25,0.25,1)]");
		@BossParam(help = "Particles to spawn every 20 ticks at the circular border of the spell's area of effect.")
		public ParticlesList PARTICLES_CHARGE_TWENTY_TICKS_BORDER = ParticlesList.fromString("[(END_ROD,1,0.1,0.0,0.1,0.05)]");
	}

	public FlareBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT) {
			p.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, p.RANGE, false, EntityTargets.Limit.DEFAULT_ONE, p.LINE_OF_SIGHT ? List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT) : Collections.emptyList());
		}

		Spell spell = new SpellFlare(plugin, boss, p);
		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
