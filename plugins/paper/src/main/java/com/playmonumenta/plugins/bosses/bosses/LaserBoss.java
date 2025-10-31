package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit.LIMITSENUM;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.PLAYERFILTER;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.TARGETS;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

//generalized class for all bosses with laser
public class LaserBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_laser";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DAMAGE = 0;

		@BossParam(help = "not written")
		public int DETECTION = 30;

		@BossParam(help = "not written")
		public int DELAY = 5 * 20;

		@BossParam(help = "not written")
		public int DURATION = 5 * 20;

		@BossParam(help = "not written")
		public int COOLDOWN = 12 * 20;

		@BossParam(help = "When false, laser attack can go through blocks.")
		public boolean CAN_BLOCK = true;

		@BossParam(help = "whether or not the laser will break blocks around where it hits")
		public boolean BREAK_BLOCKS = false;

		@BossParam(help = "if breakblocks = true, this is the radius of the blocks that will be deleted")
		public int BLOCK_BREAK_RADIUS = 0;

		@BossParam(help = "not written")
		public boolean CAN_MOVE = false;

		@BossParam(help = "not written", deprecated = true)
		public boolean SINGLE_TARGET = false;

		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Let you choose the targets of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		@BossParam(help = "Effects apply to player after the laser end")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "LOS name of the mob at the location of the laser's end")
		public LoSPool SPAWNED_MOB_POOL = LoSPool.LibraryPool.EMPTY;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		//particle & sound used!
		@BossParam(help = "Sound used each tick on each player")
		public SoundsList SOUND_TICKS = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_SHULKER_BULLET_HIT, 1.0f, 1.0f))
			.build();

		@BossParam(help = "Particle used for the laser")
		public ParticlesList PARTICLE_LASER = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT, 1, 0.0, 0.0, 0.0, 0.0))
			.add(new ParticlesList.CParticle(Particle.CRIT_MAGIC, 1, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Particle used when the cast is over")
		public ParticlesList PARTICLE_END = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.EXPLOSION_NORMAL, 35, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Sound used when the cast is over")
		public SoundsList SOUND_END = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.6f, 1.5f))
			.build();

		@BossParam(help = "not written")
		public int PARTICLE_FREQUENCY = 1;
		@BossParam(help = "not written")
		public int PARTICLE_CHANCE = 6;
		@BossParam(help = "If the player exceeds this range, the laser gets cancelled. If 0, laser has no max range.")
		public int MAX_RANGE = 0;
	}

	public LaserBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
			//same object
			//probably an older mob version?
			//build a new target from others config
			p.TARGETS = new EntityTargets(TARGETS.PLAYER, p.DETECTION, p.SINGLE_TARGET ? new Limit(1) : new Limit(LIMITSENUM.ALL), List.of(PLAYERFILTER.HAS_LINEOFSIGHT, EntityTargets.PLAYERFILTER.NOT_STEALTHED));
			//by default LaserBoss don't take player in stealth and need LINEOFSIGHT to cast.
		}

		/* I am NOT writing another constructor for these shenanigans - Spy */
		SpellBlockBreak blockBreakAtLaserEnd = new SpellBlockBreak(boss, p.BLOCK_BREAK_RADIUS, p.BLOCK_BREAK_RADIUS, p.BLOCK_BREAK_RADIUS, 0,
			-65, false, true, false, true, true, true, Material.AIR);
		Spell laser = new SpellBaseLaser(plugin, boss, p.DURATION, false, p.COOLDOWN,
			() -> p.TARGETS.getTargetsList(mBoss),
			// Tick action per player
			(LivingEntity target, int ticks, boolean blocked) -> {
				p.SOUND_TICKS.play(target.getLocation(), 0.8f, 0.5f + (ticks / 80f) * 1.5f);
				p.SOUND_TICKS.play(boss.getLocation(), 0.8f, 0.5f + (ticks / 80f) * 1.5f);

				if (ticks == 0 && !p.CAN_MOVE) {
					Plugin.getInstance().mEffectManager.addEffect(boss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
						new BaseMovementSpeedModifyEffect(p.DURATION, -0.75));
				}
			},
			// Particles generated by the laser
			(Location loc) -> p.PARTICLE_LASER.spawn(boss, loc, 0.03, 0.03, 0.03, 0.5d),
			p.PARTICLE_FREQUENCY,
			p.PARTICLE_CHANCE,
			// Damage generated at the end of the attack
			(LivingEntity target, Location loc, boolean blocked) -> {
				p.SOUND_END.play(loc, 0.6f, 1.5f);
				p.PARTICLE_END.spawn(boss, loc, 0, 0, 0, 0.25);
				if (!ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_SUMMONS)) {
					Entity spawn = p.SPAWNED_MOB_POOL.spawn(loc);
					if (spawn != null) {
						summonPlugins(spawn);
					}
				}

				if (p.CAN_BLOCK && blocked) {
					if (p.BREAK_BLOCKS) {
						blockBreakAtLaserEnd.tryToBreakBlocks(loc, 0);
					}
					return;
				}

				if (target != null) {
					if (p.DAMAGE > 0) {
						BossUtils.blockableDamage(boss, target, DamageType.MAGIC, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation(), p.EFFECTS.mEffectList());
					}

					if (p.DAMAGE_PERCENTAGE > 0.0) {
						BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, mBoss.getLocation(), p.SPELL_NAME, p.EFFECTS.mEffectList());
					}

					p.EFFECTS.apply(target, mBoss);
				}


			},
			p.MAX_RANGE);

		List<Spell> spells = List.of(laser, blockBreakAtLaserEnd);
		super.constructBoss(spells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}

	public void summonPlugins(Entity summon) {

	}
}
