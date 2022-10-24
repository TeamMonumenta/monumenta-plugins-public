package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseShatter;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

public class ShatterBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_shatter";

	// Shatter Params:
	// Radius
	// Targets
	// Duration
	// NumLines
	// BlockMaterial
	// CastAction
	// TickAction
	// HitAction
	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DAMAGE = 0;
		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;
		@BossParam(help = "Effect applied to players hit by the explosion")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "Horizontal Velocity of player when hit")
		public float HORIZONTAL_KNOCKBACK = 0;
		@BossParam(help = "Vertical Velocity of player when hit")
		public float VERTICAL_KNOCKBACK = 0;
		@BossParam(help = "not written")
		public int DETECTION = 40;
		@BossParam(help = "not written")
		public double RADIUS = 10;
		@BossParam(help = "Number of Shatter Lines, out of a 360 degree angle.")
		public int NUM_LINES = 4;
		@BossParam(help = "Delay of first spell cast")
		public int DELAY = 100;
		@BossParam(help = "Duration of the spell cast")
		public int DURATION = 50;
		@BossParam(help = "How often the spell is cast")
		public int COOLDOWN = 7 * 20;
		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		@BossParam(help = "Let you choose the first target of this spell")
		public EntityTargets LAUNCH_TARGET = EntityTargets.GENERIC_ONE_PLAYER_TARGET;
		@BossParam(help = "The material used for the replaced blocks")
		public Material INDICATOR_MATERIAL = Material.CRIMSON_HYPHAE;

		@BossParam(help = "Particle when player gets hit Spawns")
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(CRIT_MAGIC,30,2,2,2,1.5)]");

		@BossParam(help = "Sound summoned at boss location when starting the ability. Goes up in Pitch.")
		public SoundsList SOUND_WARNING = SoundsList.fromString("[(ENTITY_IRON_GOLEM_HURT,10,1)]");
		@BossParam(help = "Sound summoned at boss location when ability is launched.")
		public SoundsList SOUND_LAUNCH = SoundsList.fromString("[(ENTITY_ENDER_DRAGON_GROWL,10,0),(ENTITY_GENERIC_EXPLODE, 10,0.5),(ENTITY_ZOMBIE_BREAK_WOODEN_DOOR,10,0.5)]");
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ShatterBoss(plugin, boss);
	}

	public ShatterBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseShatter(
				plugin,
				boss,
				p.RADIUS,
				p.COOLDOWN,
				p.DURATION,
				p.NUM_LINES,
				p.INDICATOR_MATERIAL,
				() -> {
					// Targets of spell launch
					return p.LAUNCH_TARGET.getTargetsList(boss);
				},
				(LivingEntity bosss, Location loc, float pitch) -> {
					p.SOUND_WARNING.play(loc, 10, pitch);
				},
				(LivingEntity bosss, Location loc) -> {
					p.SOUND_LAUNCH.play(loc);
				},
				(LivingEntity bosss, LivingEntity target, Location loc) -> {
					// Hit Action
					p.PARTICLE_HIT.spawn(bosss, target.getLocation());

					if (p.DAMAGE > 0) {
						BossUtils.blockableDamage(boss, target, DamageEvent.DamageType.MAGIC, p.DAMAGE, p.SPELL_NAME, loc);
					}

					if (p.DAMAGE_PERCENTAGE > 0.0) {
						BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, p.SPELL_NAME);
					}

					p.EFFECTS.apply(target, boss);

					MovementUtils.knockAway(boss.getLocation(), target, p.HORIZONTAL_KNOCKBACK, p.VERTICAL_KNOCKBACK, false);
				}
			)
		));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}
