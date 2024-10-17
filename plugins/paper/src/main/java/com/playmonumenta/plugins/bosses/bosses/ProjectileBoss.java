package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit.LIMITSENUM;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.TARGETS;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public class ProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_projectile";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Damage to apply to a hit entity")
		public int DAMAGE = 0;

		@BossParam(help = "Maximum distance the projectile can travel")
		public int DISTANCE = 64;

		@BossParam(help = "Projectile speed in blocks per tick")
		public double SPEED = 0.4;

		@BossParam(help = "Radius in blocks that this boss will check before doing anything")
		public int DETECTION = 24;

		@BossParam(help = "Initial delay in ticks for the first cast of this spell")
		public int DELAY = 20 * 5;

		@BossParam(help = "Period in ticks between the start of the last charge and next start.")
		public int COOLDOWN = 20 * 10;

		@BossParam(help = "Prevents the projectile from being despawned early in some situations")
		public boolean LINGERS = true;

		@BossParam(help = "Length in blocks of the projectile hitbox")
		public double HITBOX_LENGTH = 0.5;

		@BossParam(help = "Deprecated. Use the targets filter instead", deprecated = true)
		public boolean SINGLE_TARGET = true;

		@BossParam(help = "Percent true damage to apply to a hit entity")
		public double DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Boolean on if the projectile can damage entities or not")
		public boolean DAMAGE_PLAYER_ONLY = false;

		@BossParam(help = "Track target during the channel time")
		public boolean LAUNCH_TRACKING = true;

		@BossParam(help = "Angular velocity (in radians) of the projectile while tracking a target")
		public double TURN_RADIUS = Math.PI / 30;

		@BossParam(help = "Collide with entities besides the initial target")
		public boolean COLLIDES_WITH_OTHERS = false;

		@BossParam(help = "Collide with solid blocks")
		public boolean COLLIDES_WITH_BLOCKS = true;

		@BossParam(help = "Percent speed reduction to apply to the projectile while traveling in liquids")
		public double SPEED_LIQUID = 0.5;

		@BossParam(help = "Percent speed reduction to apply to the projectile while traveling in solid blocks")
		public double SPEED_BLOCKS = 0.125;

		@BossParam(help = "Delay in ticks on each cast between sound_start and the actual cast of the projectile")
		public int SPELL_DELAY = Integer.MAX_VALUE;

		@BossParam(help = "Mob glowing color")
		public String COLOR = "red";

		@BossParam(help = "Conditions to choose valid entities of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT;
		//note: this object is only used to show the default value while using /bosstag add boss_projectile[targets=[...]]

		@BossParam(help = "Effects applied to hit entities")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		@BossParam(help = "Amount of casts of the projectile per cooldown")
		public int CHARGE = 1;

		@BossParam(help = "Interval in ticks between charge casts")
		public int CHARGE_INTERVAL = 40;

		@BossParam(help = "Left offset from mob's eye to projectile start point")
		public double OFFSET_LEFT = 0;

		@BossParam(help = "Up offset from mob's eye to projectile start point")
		public double OFFSET_UP = 0;

		@BossParam(help = "Front offset from mob's eye to projectile start point")
		public double OFFSET_FRONT = 0;

		@BossParam(help = "Amount of projectiles to be launched in a flat plane")
		public int SPLIT = 1;

		@BossParam(help = "Angle in degrees between split projectiles")
		public double SPLIT_ANGLE = 30;

		@BossParam(help = "Mirror launched projectiles. 0=None, 1=L-R, 2=F-B, 3=Both")
		public int MIRROR = 0;

		@BossParam(help = "Force launch at a yaw degree offset from boss' sight. [-180, 180] is valid.")
		public double FIX_YAW = 200.0;

		@BossParam(help = "Force launch at a fixed pitch degree. [-90, 90] is valid.")
		public double FIX_PITCH = 100.0;

		@BossParam(help = "Apply excess healing as absorption to hit entities")
		public boolean OVERHEAL = false;

		@BossParam(help = "Health to heal hit entities")
		public double HEAL_AMOUNT = 0;

		@BossParam(help = "Scalar value to use when generating an AoE at the location a projectile hits a target or blocks. Only works with players")
		public double AOE_RADIUS = 0;

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

	public ProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(mBoss, identityTag, new Parameters());
		final int lifetimeTicks = (int) (p.DISTANCE / p.SPEED);

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT) {
			//same object
			//probably an older mob version?
			//build a new target from others config
			p.TARGETS = new EntityTargets(TARGETS.PLAYER, p.DETECTION, false, p.SINGLE_TARGET ? new Limit(1) : new Limit(LIMITSENUM.ALL), List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));
			//by default ProjectileBoss doesn't take player in stealth and need line of sight.
		}

		if (p.SPELL_DELAY == Integer.MAX_VALUE) {
			p.SPELL_DELAY = p.DELAY;
		}

		if (p.MIRROR > 3 || p.MIRROR < 0) {
			p.MIRROR = 0;
		}

		final Spell spell = new SpellBaseSeekingProjectile(mPlugin, mBoss, p.LAUNCH_TRACKING, p.CHARGE, p.CHARGE_INTERVAL,
			p.COOLDOWN, p.SPELL_DELAY, p.OFFSET_LEFT, p.OFFSET_UP, p.OFFSET_FRONT, p.MIRROR, p.FIX_YAW, p.FIX_PITCH,
			p.SPLIT, p.SPLIT_ANGLE, p.SPEED, p.TURN_RADIUS, lifetimeTicks, p.HITBOX_LENGTH, p.LINGERS, p.COLLIDES_WITH_BLOCKS,
			p.SPEED_LIQUID, p.SPEED_BLOCKS, p.COLLIDES_WITH_OTHERS, 0,
			//spell targets
			() -> p.TARGETS.getTargetsList(mBoss),
			// Initiate Aesthetic
			(World world, Location loc, int ticks) -> {
				if (p.SPELL_DELAY > 0) {
					GlowingManager.startGlowing(mBoss, NamedTextColor.NAMES.valueOr(p.COLOR, NamedTextColor.RED), p.SPELL_DELAY, GlowingManager.BOSS_SPELL_PRIORITY);
				}
				p.SOUND_START.play(loc);
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				p.PARTICLE_LAUNCH.spawn(mBoss, loc);
				p.SOUND_LAUNCH.play(loc);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {
				p.PARTICLE_PROJECTILE.spawn(mBoss, loc, 0.1, 0.1, 0.1, 0.1);
				if (ticks % 40 == 0) {
					p.SOUND_PROJECTILE.play(loc);
				}
			},
			// Hit Action
			(World world, @Nullable LivingEntity target, Location loc, @Nullable Location prevLoc) -> {
				if (!p.DAMAGE_PLAYER_ONLY || target instanceof Player) {
					p.SOUND_HIT.play(loc, 0.5f, 0.5f);
					p.PARTICLE_HIT.spawn(mBoss, loc, 0d, 0d, 0d, 0.25d);

					if (target != null) {
						onHitActions(p, mBoss, target, prevLoc);
					}

					if (p.AOE_RADIUS > 0) {
						/* TODO: This could be generalized to work with all LivingEntities with some effort. I only
						 *  made it work with players since I'm strictly importing functionality from KineticProjectileBoss */
						final List<Player> hitPlayers = new Hitbox.AABBHitbox(world,
							new BoundingBox().shift(loc).expand(p.AOE_RADIUS)).getHitPlayers(true);
						hitPlayers.removeIf(player -> player == target);
						hitPlayers.forEach(player -> onHitActions(p, mBoss, player, prevLoc));
					}
				}
			});

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}

	private void onHitActions(final Parameters p, final LivingEntity launcher, final LivingEntity affected, final @Nullable Location prevLoc) {
		if (p.DAMAGE > 0) {
			BossUtils.blockableDamage(launcher, affected, DamageType.MAGIC, p.DAMAGE, p.SPELL_NAME, prevLoc);
		}

		if (p.DAMAGE_PERCENTAGE > 0.0) {
			BossUtils.bossDamagePercent(mBoss, affected, p.DAMAGE_PERCENTAGE, prevLoc, p.SPELL_NAME);
		}

		if (p.HEAL_AMOUNT > 0) {
			final double healed = EntityUtils.healMob(affected, p.HEAL_AMOUNT);
			if (p.OVERHEAL && healed < p.HEAL_AMOUNT) {
				AbsorptionUtils.addAbsorption(affected, p.HEAL_AMOUNT - healed, p.HEAL_AMOUNT, -1);
			}
		}
		p.EFFECTS.apply(affected, launcher);
	}
}
