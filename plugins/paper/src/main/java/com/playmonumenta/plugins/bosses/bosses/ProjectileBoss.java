package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit.LIMITSENUM;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.TARGETS;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_projectile";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DAMAGE = 0;

		@BossParam(help = "not written")
		public int DISTANCE = 64;

		@BossParam(help = "not written")
		public double SPEED = 0.4;

		@BossParam(help = "not written")
		public int DETECTION = 24;

		@BossParam(help = "Delay of the first spell, then cooldown is used to determinate when this spell will cast again")
		public int DELAY = 20 * 5;

		@BossParam(help = "not written")
		public int COOLDOWN = 20 * 10;

		@BossParam(help = "not written")
		public boolean LINGERS = true;

		@BossParam(help = "not written")
		public double HITBOX_LENGTH = 0.5;

		@BossParam(help = "not written")
		public boolean SINGLE_TARGET = true;

		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "not written")
		public boolean LAUNCH_TRACKING = true;

		@BossParam(help = "not written")
		public double TURN_RADIUS = Math.PI / 30;

		@BossParam(help = "not written")
		public boolean COLLIDES_WITH_OTHERS = false;

		@BossParam(help = "not written")
		public boolean COLLIDES_WITH_BLOCKS = true;

		@BossParam(help = "Delay on each single cast between sound_start and the actual cast of the projectile")
		public int SPELL_DELAY = Integer.MAX_VALUE;

		@BossParam(help = "Let you choose the targets of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		@BossParam(help = "Effects applied to the player when he got hit")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		//particle & sound used!
		@BossParam(help = "Sound played at the start")
		public SoundsList SOUND_START = SoundsList.fromString("[(ENTITY_BLAZE_AMBIENT,1.5,1)]");

		@BossParam(help = "Particle used when launching the projectile")
		public ParticlesList PARTICLE_LAUNCH = ParticlesList.fromString("[(EXPLOSION_LARGE,1)]");

		@BossParam(help = "Sound used when launching the projectile")
		public SoundsList SOUND_LAUNCH = SoundsList.fromString("[(ENTITY_BLAZE_SHOOT,0.5,0.5)]");

		@BossParam(help = "Particle used for the projectile")
		public ParticlesList PARTICLE_PROJECTILE = ParticlesList.fromString("[(FLAME, 4, 0.05, 0.05, 0.05, 0.1),(SMOKE_LARGE, 3, 0.25, 0.25, 0.25)]");

		@BossParam(help = "Sound summoned every 2 sec on the projectile location")
		public SoundsList SOUND_PROJECTILE = SoundsList.fromString("[(ENTITY_BLAZE_BURN,0.5,0.2)]");

		@BossParam(help = "Particle used when the projectile hit something")
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(CLOUD,50,0,0,0,0.25)]");

		@BossParam(help = "Sound used when the projectile hit something")
		public SoundsList SOUND_HIT = SoundsList.fromString("[(ENTITY_GENERIC_DEATH,0.5,0.5)]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ProjectileBoss(plugin, boss);
	}


	public ProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		int lifetimeTicks = (int) (p.DISTANCE/p.SPEED);

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
			//same object
			//probably an older mob version?
			//build a new target from others config
			p.TARGETS = new EntityTargets(TARGETS.PLAYER, p.DETECTION, false, p.SINGLE_TARGET ? new Limit(1) : new Limit(LIMITSENUM.ALL));
			//by default LaserBoss don't take player in stealt.
		}

		if (p.SPELL_DELAY == Integer.MAX_VALUE) {
			p.SPELL_DELAY = p.DELAY;
		}
		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSeekingProjectile(plugin, boss, p.LAUNCH_TRACKING, p.COOLDOWN, p.SPELL_DELAY,
					p.SPEED, p.TURN_RADIUS, lifetimeTicks, p.HITBOX_LENGTH, p.COLLIDES_WITH_BLOCKS, p.LINGERS, 0, p.COLLIDES_WITH_OTHERS,
					//spell targets
					() -> {
						return p.TARGETS.getTargetsList(mBoss);
					},
					// Initiate Aesthetic
					(World world, Location loc, int ticks) -> {
						PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, p.SPELL_DELAY, 0));
						p.SOUND_START.play(loc);
					},
					// Launch Aesthetic
					(World world, Location loc, int ticks) -> {
						p.PARTICLE_LAUNCH.spawn(loc);
						p.SOUND_LAUNCH.play(loc);
					},
					// Projectile Aesthetic
					(World world, Location loc, int ticks) -> {
						p.PARTICLE_PROJECTILE.spawn(loc, 0.1, 0.1, 0.1, 0.1);
						if (ticks % 40 == 0) {
							p.SOUND_PROJECTILE.play(loc);
						}
					},
					// Hit Action
					(World world, LivingEntity target, Location loc) -> {
						p.SOUND_HIT.play(loc, 0.5f, 0.5f);
						p.PARTICLE_HIT.spawn(loc, 0d, 0d, 0d, 0.25d);

						if (target != null) {
							if (p.DAMAGE > 0) {
								BossUtils.blockableDamage(boss, target, DamageType.MAGIC, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation());
							}

							if (p.DAMAGE_PERCENTAGE > 0.0) {
								BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, p.SPELL_NAME);
							}
							p.EFFECTS.apply(target, boss);
						}

					})
		));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);

	}
}
