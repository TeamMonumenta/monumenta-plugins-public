package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.TARGETS;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public final class NovaBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_nova";

	public static class Parameters extends BossParameters {
		@BossParam(help = "you should use TARGETS instead of this param.", deprecated = true)
		public int RADIUS = 9;
		@BossParam(help = "not written")
		public int DAMAGE = 0;
		@BossParam(help = "not written")
		public int DELAY = 100;
		@BossParam(help = "not written")
		public int DURATION = 70;
		@BossParam(help = "not written")
		public int DETECTION = 40;
		@BossParam(help = "not written")
		public int COOLDOWN = 8 * 20;
		@BossParam(help = "not written")
		public boolean CAN_MOVE = false;
		@BossParam(help = "Whether or not the nova attack can be blocked by a shield. (Default = true)")
		public boolean CAN_BLOCK = true;
		@BossParam(help = "You should not use this. use TARGETS instead.", deprecated = true)
		public boolean NEED_LINE_OF_SIGHT = true;

		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Effect applied to players hit by the nova")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		@BossParam(help = "Let you choose the targets of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		//particle & sound used!
		@BossParam(help = "Particle summon on the air")
		public ParticlesList PARTICLE_AIR = ParticlesList.fromString("[(CLOUD,5)]");

		@BossParam(help = "Sound used when charging the ability")
		public Sound SOUND_CHARGE = Sound.ENTITY_WITCH_CELEBRATE;

		@BossParam(help = "Volume for sound used when charging the ability")
		public float SOUND_CHARGE_VOLUME = 1.0f;

		@BossParam(help = "Frequency for sound charge")
		public int SOUND_CHARGE_FREQUENCY = 1;

		@BossParam(help = "Particle summon arround the boss when loading the spell")
		public ParticlesList PARTICLE_LOAD = ParticlesList.fromString("[(CRIT,1)]");

		@BossParam(help = "Sound used when the spell is casted (when explode)")
		public SoundsList SOUND_CAST = SoundsList.fromString("[(ENTITY_WITCH_DRINK,1.5,0.65),(ENTITY_WITCH_DRINK,1.5,0.55)]");

		@BossParam(help = "Particle summoned when the spell explode")
		public ParticlesList PARTICLE_EXPLODE = ParticlesList.fromString("[(CRIT,1,0.1,0.1,0.1,0.3),(CRIT_MAGIC,1,0.25,0.25,0.25,0.1)]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new NovaBoss(plugin, boss);
	}

	public NovaBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
			//same object
			//probably an older mob version?
			//build a new target from others config
			p.TARGETS = new EntityTargets(TARGETS.PLAYER, p.RADIUS, true);
			//by default LaserBoss take player in stealt.
		} else {
			p.NEED_LINE_OF_SIGHT = false;
		}
		SpellManager activeSpells = new SpellManager(List.of(
			new SpellBaseAoE(plugin, boss, (int) p.TARGETS.getRange(), p.DURATION, p.COOLDOWN, p.CAN_MOVE, p.NEED_LINE_OF_SIGHT, p.SOUND_CHARGE, p.SOUND_CHARGE_VOLUME, p.SOUND_CHARGE_FREQUENCY) {
				@Override
				protected void chargeAuraAction(Location loc) {
					p.PARTICLE_AIR.spawn(boss, loc, p.TARGETS.getRange() / 2, p.TARGETS.getRange() / 2, p.TARGETS.getRange() / 2, 0.05);
				}

				@Override
				protected void chargeCircleAction(Location loc) {
					p.PARTICLE_LOAD.spawn(boss, loc, 0.25d, 0.25d, 0.25d, 0.0d);
				}

				@Override
				protected void outburstAction(Location loc) {
					p.SOUND_CAST.play(loc, 1.5f, 0.65f);
				}

				@Override
				protected void circleOutburstAction(Location loc) {
					p.PARTICLE_EXPLODE.spawn(boss, loc, 0.2, 0.2, 0.2, 0.2);
				}

				@Override
				protected void dealDamageAction(Location loc) {
					for (LivingEntity target : p.TARGETS.getTargetsList(mBoss)) {
						if (p.DAMAGE > 0) {
							if (p.CAN_BLOCK) {
								BossUtils.blockableDamage(boss, target, DamageType.MAGIC, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation());
							} else {
								DamageUtils.damage(boss, target, DamageType.MAGIC, p.DAMAGE, null, false, true, p.SPELL_NAME);
							}
						}

						if (p.DAMAGE_PERCENTAGE > 0.0) {
							BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, p.CAN_BLOCK ? mBoss.getLocation() : null, p.SPELL_NAME);
						}
						p.EFFECTS.apply(target, mBoss);
					}
				}
			}));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}
