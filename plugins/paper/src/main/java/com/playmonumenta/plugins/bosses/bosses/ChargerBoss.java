package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.TARGETS;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
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
		public ParticlesList PARTICLE_WARNING = ParticlesList.fromString("[(VILLAGER_ANGRY,50)]");

		@BossParam(help = "Sounds played at the launcher's location when it begins to channel the spell")
		public SoundsList SOUND_WARNING = SoundsList.fromString("[(ENTITY_ELDER_GUARDIAN_CURSE,1,1.5)]");

		@BossParam(help = "Particles spawned along the charge path")
		public ParticlesList PARTICLE_TELL = ParticlesList.fromString("[(CRIT,2)]");

		@BossParam(help = "Particles spawned on a hit target")
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(BLOCK_CRACK,5,0.4,0.4,0.4,0.4,REDSTONE_BLOCK),(BLOCK_CRACK,12,0.4,0.4,0.4,0.4,REDSTONE_WIRE)]");

		@BossParam(help = "Particles spawned at the start and end locations of the charge")
		public ParticlesList PARTICLE_ROAR = ParticlesList.fromString("[(SMOKE_LARGE,125)]");

		@BossParam(help = "Sound played at the start and end locations of the charge")
		public SoundsList SOUND_ROAR = SoundsList.fromString("[(ENTITY_ENDER_DRAGON_GROWL,1,1.5)]");

		@BossParam(help = "Particles spawned when the charge hits a target")
		public ParticlesList PARTICLE_ATTACK = ParticlesList.fromString("[(FLAME,4,0.5,0.5,0.5,0.075),(CRIT,8,0.5,0.5,0.5,0.75)]");
	}

	public ChargerBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(mBoss, identityTag, new Parameters());

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
			//same object
			//probably an older mob version?
			//build a new target from others config
			p.TARGETS = new EntityTargets(TARGETS.PLAYER, p.DETECTION, false, EntityTargets.Limit.DEFAULT, List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));
			//by default Charger don't take player in stealth.
		}

		final Spell spell = new SpellBaseCharge(mPlugin, mBoss, p.COOLDOWN, p.DURATION, p.STOP_ON_HIT,
			0, 0, 0,
			() -> {
				List<? extends LivingEntity> targetList = p.TARGETS.getTargetsList(mBoss);
				if (p.MIN_DISTANCE > 0) {
					targetList.removeIf(target -> target.getLocation()
						.distanceSquared(mBoss.getLocation()) < p.MIN_DISTANCE * p.MIN_DISTANCE);
				}
				return targetList;
			},
			// Warning sound/particles at boss location and slow boss
			(LivingEntity player) -> {
				p.PARTICLE_WARNING.spawn(mBoss, mBoss.getLocation(), 2d, 2d, 2d);
				p.SOUND_WARNING.play(mBoss.getLocation(), 1f, 1.5f);
				mBoss.setAI(false);
			},
			// Warning particles
			(Location loc) -> p.PARTICLE_TELL.spawn(mBoss, loc, 0.65d, 0.65d, 0.65d),
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				p.PARTICLE_ROAR.spawn(mBoss, mBoss.getLocation(), 0.3d, 0.3d, 0.3d, 0.15d);
				p.SOUND_ROAR.play(mBoss.getLocation(), 1f, 1.5f);
			},
			// Attack hit a player
			(LivingEntity target) -> {
				p.PARTICLE_HIT.spawn(mBoss, target.getEyeLocation(), 0.4d, 0.4d, 0.4d, 0.4d);
				if (p.DAMAGE > 0) {
					BossUtils.blockableDamage(mBoss, target, p.DAMAGE_TYPE, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation(), p.EFFECTS.mEffectList);
				}

				if (p.DAMAGE_PERCENTAGE > 0.0) {
					DamageUtils.damage(mBoss, target, new DamageEvent.Metadata(DamageType.TRUE, null, null, p.SPELL_NAME),
						EntityUtils.getMaxHealth(target) * p.DAMAGE_PERCENTAGE, true, true, true);
				}

				if (p.CHANGE_TARGET && mBoss instanceof Mob mobAI && !(target instanceof Player player && AbilityUtils.isStealthed(player))) {
					mobAI.setTarget(target);
				}

				p.EFFECTS.apply(target, mBoss);
			},
			// Attack particles
			(Location loc) -> p.PARTICLE_ATTACK.spawn(mBoss, loc),
			// Ending particles on boss
			() -> {
				p.PARTICLE_ROAR.spawn(mBoss, mBoss.getLocation(), 0.3, 0.3, 0.3, 0.15);
				p.SOUND_ROAR.play(mBoss.getLocation(), 1f, 1.5f);
				mBoss.setAI(true);
			});

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
