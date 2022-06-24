package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellMobHealAoE;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class RejuvenationBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_rejuvenation";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int HEAL = 25;
		@BossParam(help = "not written")
		public int RANGE = 14;
		@BossParam(help = "not written")
		public int DURATION = 80;
		@BossParam(help = "not written")
		public int DETECTION = 20;
		@BossParam(help = "not written")
		public int DELAY = 5 * 20;
		@BossParam(help = "not written")
		public int COOLDOWN = 15 * 20;

		@BossParam(help = "not written")
		public double PARTICLE_RADIUS = 15;
		@BossParam(help = "not written")
		public boolean CAN_MOVE = true;
		@BossParam(help = "not written")
		public boolean OVERHEAL = true;

		@BossParam(help = "Targets of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_MOB_TARGET;


		public ParticlesList PARTICLE_CHARGE_AIR = ParticlesList.fromString("[(SPELL_INSTANT,3)]");

		public ParticlesList PARTICLE_CHARGE_CIRCLE = ParticlesList.fromString("[(SPELL_INSTANT,3)]");

		public SoundsList SOUND_CHARGE = SoundsList.fromString("[(ITEM_TRIDENT_RETURN,0.8)]");

		public SoundsList SOUND_OUTBURST_CIRCLE = SoundsList.fromString("[(ENTITY_ILLUSIONER_CAST_SPELL,3,1.25),(ENTITY_ZOMBIE_VILLAGER_CONVERTED,3,2)]");

		public ParticlesList PARTICLE_OUTBURST_AIR = ParticlesList.fromString("[(FIREWORKS_SPARK,3),(VILLAGER_HAPPY,3,3.5,3.5,3.5,0.5)]");

		public ParticlesList PARTICLE_OUTBURST_CIRCLE = ParticlesList.fromString("[(CRIT_MAGIC,3,0.25,0.25,0.25,0.35),(FIREWORKS_SPARK,2,0.25,0.25,0.25,0.15)]");

		public ParticlesList PARTICLE_HEAL = ParticlesList.fromString("[(FIREWORKS_SPARK,3,0.25,0.5,0.25,0.3),(HEART,3,0.4,0.5,0.4)]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new RejuvenationBoss(plugin, boss);
	}

	public RejuvenationBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (p.TARGETS == EntityTargets.GENERIC_MOB_TARGET) {
			//probably a mob from an older version.
			p.TARGETS = new EntityTargets(EntityTargets.TARGETS.MOB, p.RANGE, false);
			p.PARTICLE_RADIUS = p.RANGE;
		}
		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellMobHealAoE(
				plugin,
				boss,
				p.COOLDOWN,
				p.DURATION,
				p.PARTICLE_RADIUS,
				p.CAN_MOVE,
				() -> {
					return p.TARGETS.getTargetsList(mBoss);
				},
				(Location loc, int ticks) -> {
					p.PARTICLE_CHARGE_AIR.spawn(boss, loc, 3.5, 3.5, 3.5, 0.25);
					if (ticks <= (p.DURATION - 5) && ticks % 2 == 0) {
						p.SOUND_CHARGE.play(mBoss.getLocation(), 0.8f, 0.25f + ((float)ticks / (float)100));
					}
				},
				(Location loc, int ticks) -> {
					p.PARTICLE_CHARGE_CIRCLE.spawn(boss, loc, 0.25, 0.25, 0.25);
				},
				(Location loc, int ticks) -> {
					p.PARTICLE_OUTBURST_AIR.spawn(boss, loc, 3.5, 3.5, 3.5, 0.25);
					p.SOUND_OUTBURST_CIRCLE.play(loc);
				},
				(Location loc, int ticks) -> {
					p.PARTICLE_OUTBURST_CIRCLE.spawn(boss, loc);
				},
				(LivingEntity target) -> {
					p.PARTICLE_HEAL.spawn(boss, target.getEyeLocation());
					double hp = target.getHealth() + p.HEAL;
					double max = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
					if (hp >= max) {
						target.setHealth(max);
						if (p.OVERHEAL) {
							int missing = (int) (hp - max);
							AbsorptionUtils.addAbsorption(target, missing, p.HEAL, -1);
						}
					} else {
						target.setHealth(hp);
					}
				}
				)));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}
