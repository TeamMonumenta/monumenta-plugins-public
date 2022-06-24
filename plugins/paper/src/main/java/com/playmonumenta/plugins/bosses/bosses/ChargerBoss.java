package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.TARGETS;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

public class ChargerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_charger";

	public static class Parameters extends BossParameters {
		//i would very like to set the damage to 0 but for this we need to rework all the mobs
		//with boss_charger in the game...so BIG NOPE
		@BossParam(help = "not written")
		public int DAMAGE = 15;

		@BossParam(help = "not written")
		public DamageType DAMAGE_TYPE = DamageType.MELEE;

		@BossParam(help = "not written")
		public int DURATION = 25;

		@BossParam(help = "not written")
		public int DELAY = 5 * 20;

		@BossParam(help = "not written", deprecated = true)
		public int DETECTION = 20;

		@BossParam(help = "not written")
		public int COOLDOWN = 8 * 20;

		@BossParam(help = "not written")
		public boolean STOP_ON_HIT = false;

		@BossParam(help = "not written")
		public boolean TARGET_FURTHEST = false;

		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Effects applied to players hit by the charge")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		@BossParam(help = "Let you choose the targets of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		@BossParam(help = "Whenever this mob should change target to charge target after casting")
		public boolean CHANGE_TARGET = true;

		//Particle & Sounds!
		@BossParam(help = "Particle summoned at boss location when starting the ability")
		public ParticlesList PARTICLE_WARNING = ParticlesList.fromString("[(VILLAGER_ANGRY,50)]");

		@BossParam(help = "Sound summoned at boss location when starting the ability")
		public SoundsList SOUND_WARNING = SoundsList.fromString("[(ENTITY_ELDER_GUARDIAN_CURSE,1,1.5)]");

		@BossParam(help = "Particle to show the player where the boss want to charge")
		public ParticlesList PARTICLE_TELL = ParticlesList.fromString("[(CRIT,2)]");

		@BossParam(help = "Particle summon when the ability hit a player")
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(BLOCK_CRACK,5,0.4,0.4,0.4,0.4,REDSTONE_BLOCK),(BLOCK_CRACK,12,0.4,0.4,0.4,0.4,REDSTONE_WIRE)]");

		@BossParam(help = "Particle summoned at the start and end of the charge")
		public ParticlesList PARTICLE_ROAR = ParticlesList.fromString("[(SMOKE_LARGE,125)]");

		@BossParam(help = "Sound summoned at the start and end of the charge")
		public SoundsList SOUND_ROAR = SoundsList.fromString("[(ENTITY_ENDER_DRAGON_GROWL,1,1.5)]");

		@BossParam(help = "Particle summoned when the charge hit a player")
		public ParticlesList PARTICLE_ATTACK = ParticlesList.fromString("[(FLAME,4,0.5,0.5,0.5,0.075),(CRIT,8,0.5,0.5,0.5,0.75)]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ChargerBoss(plugin, boss);
	}

	public ChargerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
			//same object
			//probably an older mob version?
			//build a new target from others config
			p.TARGETS = new EntityTargets(TARGETS.PLAYER, p.DETECTION, false, EntityTargets.Limit.DEFAULT, List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));
			//by default Charger don't take player in stealth.
		}


		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseCharge(plugin, boss, p.COOLDOWN, p.DURATION, p.STOP_ON_HIT,
			0, 0, 0,
			() -> {
				return p.TARGETS.getTargetsList(mBoss);
			},
			// Warning sound/particles at boss location and slow boss
			(LivingEntity player) -> {
				p.PARTICLE_WARNING.spawn(boss, boss.getLocation(), 2d, 2d, 2d);
				p.SOUND_WARNING.play(boss.getLocation(), 1f, 1.5f);
				boss.setAI(false);
			},
			// Warning particles
			(Location loc) -> {
				p.PARTICLE_TELL.spawn(boss, loc, 0.65d, 0.65d, 0.65d);
			},
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				p.PARTICLE_ROAR.spawn(boss, boss.getLocation(), 0.3d, 0.3d, 0.3d, 0.15d);
				p.SOUND_ROAR.play(boss.getLocation(), 1f, 1.5f);
			},
			// Attack hit a player
			(LivingEntity target) -> {
				p.PARTICLE_HIT.spawn(boss, target.getEyeLocation(), 0.4d, 0.4d, 0.4d, 0.4d);
				if (p.DAMAGE > 0) {
					BossUtils.blockableDamage(boss, target, p.DAMAGE_TYPE, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation());
				}

				if (p.DAMAGE_PERCENTAGE > 0.0) {
					BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, p.SPELL_NAME);
				}

				if (p.CHANGE_TARGET && mBoss instanceof Mob mobAI) {
					mobAI.setTarget(target);
				}

				p.EFFECTS.apply(target, mBoss);
			},
			// Attack particles
			(Location loc) -> {
				p.PARTICLE_ATTACK.spawn(boss, loc);
			},
			// Ending particles on boss
			() -> {
				p.PARTICLE_ROAR.spawn(boss, boss.getLocation(), 0.3, 0.3, 0.3, 0.15);
				p.SOUND_ROAR.play(boss.getLocation(), 1f, 1.5f);
				boss.setAI(true);
			})
		));

		super.constructBoss(activeSpells, Collections.emptyList(), (int) (p.TARGETS.getRange() * 2), null, p.DELAY);
	}
}
