package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellForce;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class ForceBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_force";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written", deprecated = true)
		public int DETECTION = 20;

		@BossParam(help = "not written")
		public int COOLDOWN = 20 * 8;

		@BossParam(help = "not written")
		public int DURATION = 70;

		@BossParam(help = "DEPRECATED. Use TARGETS for choosing the radius instead", deprecated = true)
		public int RADIUS = 5;

		@BossParam(help = "not written")
		public int DELAY = 100;

		@BossParam(help = "DON'T change this unless you also change the TARGETS to Entity or Mob")
		public boolean NEED_PLAYERS = true;

		@BossParam(help = "Targets of this ability")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		@BossParam(help = "Effects applied to the player if he is near the boss (< RADIUS /3) when cast is over")
		public EffectsList EFFECTS_NEAR = EffectsList.fromString("[(pushforce,3),(SLOW,100,2)]");
		@BossParam(help = "Effects applied to the player if he is near the boss (< Radius * 2/3) when cast is over")
		public EffectsList EFFECTS_MIDDLE = EffectsList.fromString("[(pushforce,2.1),(SLOW,100,1)]");
		@BossParam(help = "Effects applied to the player if he is at limit distance to the boss (< Radius) when cast is over")
		public EffectsList EFFECTS_LIMIT = EffectsList.fromString("[(pushforce,1.2),(SLOW,100,0)]");

		//Particles & Sounds!
		@BossParam(help = "Particle summon in the air while the ability is charging")
		public ParticlesList PARTICLE_AIR = ParticlesList.fromString("[(SMOKE_LARGE,1)]");

		@BossParam(help = "Particle summon int the ground while the ability is charging")
		public ParticlesList PARTICLE_CIRCLE = ParticlesList.fromString("[(CRIT_MAGIC,1)]");

		@BossParam(help = "Particle when the ability explode")
		public ParticlesList PARTICLE_EXPLODE = ParticlesList.fromString("[(SMOKE_LARGE,100)]");

		@BossParam(help = "Sound played when the ability explode")
		public SoundsList SOUND_EXPLODE = SoundsList.fromString("[(ENTITY_WITHER_SHOOT,1.5,0.65),(ENTITY_GHAST_SHOOT,1.0,0.5),(ENTITY_GUARDIAN_HURT,1,0.8)]");

		@BossParam(help = "Particle when the ability explode")
		public ParticlesList PARTICLE_CIRCLE_EXPLODE = ParticlesList.fromString("[(SMOKE_LARGE,1,0.1,0.1,0.1,0.3),(SMOKE_NORMAL,2,0.25,0.25,0.25,0.1)]");

		@BossParam(help = "Particle summon at player position when he got hit by the ability")
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(VILLAGER_ANGRY,4,0.25,0.5,0.25)]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ForceBoss(plugin, boss);
	}

	public ForceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
			p.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, p.RADIUS, true, EntityTargets.Limit.DEFAULT);
			//by default Force boss hit all the player in range even the players in stealth
		}
		final double currentRadius = p.TARGETS.getRange();
		SpellManager activeSpells = new SpellManager(List.of(
			new SpellForce(plugin, boss, (int) currentRadius, p.DURATION, p.COOLDOWN, p.NEED_PLAYERS) {

				@Override
				protected void chargeAuraAction(Location loc) {
					p.PARTICLE_AIR.spawn(boss, loc, currentRadius / 2, currentRadius / 2, currentRadius / 2, 0.05);
				}

				@Override
				protected void chargeCircleAction(Location loc) {
					p.PARTICLE_CIRCLE.spawn(boss, loc, 0.25, 0.25, 0.25, 0.1);
				}

				@Override
				protected void outburstAction(Location loc) {
					p.PARTICLE_EXPLODE.spawn(boss, loc, 0.5, 0, 0.5, 0.8f);
					p.SOUND_EXPLODE.play(loc, 1.0f, 0.7f);
				}

				@Override
				protected void circleOutburstAction(Location loc) {
					p.PARTICLE_CIRCLE_EXPLODE.spawn(boss, loc, 0.2, 0.2, 0.2, 0.2);
				}

				@Override
				protected void dealDamageAction(Location loc) {
					for (LivingEntity target : p.TARGETS.getTargetsList(mBoss)) {

						double distance = target.getLocation().distance(loc);
						if (distance < p.TARGETS.getRange() / 3.0) {
							p.EFFECTS_NEAR.apply(target, boss);
						} else if (distance < (p.TARGETS.getRange() * 2.0) / 3.0) {
							p.EFFECTS_MIDDLE.apply(target, boss);
						} else if (distance < p.TARGETS.getRange()) {
							p.EFFECTS_LIMIT.apply(target, boss);
						}
						p.PARTICLE_HIT.spawn(boss, target.getEyeLocation(), 0.25, 0.5, 0.25, 0);
					}
				}

			}));

		super.constructBoss(activeSpells, Collections.emptyList(), (int) (p.TARGETS.getRange() * 2), null, p.DELAY);
	}
}
