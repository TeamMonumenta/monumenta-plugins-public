package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSummon;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class MobRisingBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_mob_rising";

	public static class Parameters extends BossParameters {

		@BossParam(help = "not written")
		public int DETECTION = 100;
		@BossParam(help = "not written")
		public int DELAY = 80;
		@BossParam(help = "not written")
		public int COOLDOWN = 160;
		@BossParam(help = "not written")
		public int DURATION = 80;
		@BossParam(help = "not written")
		public double RANGE = 20;
		@BossParam(help = "not written")
		public boolean CAN_BE_STOPPED = false;
		@BossParam(help = "not written")
		public boolean CAN_MOVE = false;
		@BossParam(help = "not written")
		public boolean SINGLE_TARGET = false;
		@BossParam(help = "not written")
		public int MOB_NUMBER = 0;
		@BossParam(help = "not written")
		public float DEPTH = 2.5f;
		@BossParam(help = "not written")
		public boolean SELF_GLOWING = true;
		@BossParam(help = "not written")
		public boolean SUMMON_GLOWING = true;

		@BossParam(help = "not written")
		public LoSPool MOB_POOL = LoSPool.EMPTY;
		@BossParam(help = "not written")
		public EntityTargets TARGETS = EntityTargets.GENERIC_SELF_TARGET;
		@BossParam(help = "not written")
		public SoundsList SOUNDS = SoundsList.EMPTY;
		@BossParam(help = "not written")
		public ParticlesList PARTICLES = ParticlesList.fromString("[(SPELL_INSTANT,2,0.5,0.5,0.5,0)]");
	}

	public MobRisingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (p.MOB_POOL != LoSPool.EMPTY) {
			Spell spell = new SpellBaseSummon(
				plugin,
				boss,
				p.COOLDOWN,
				p.DURATION,
				p.RANGE,
				p.DEPTH,
				p.CAN_BE_STOPPED,
				p.CAN_MOVE,
				p.SINGLE_TARGET,
				() -> p.MOB_NUMBER,
				() -> {
					if (ZoneUtils.hasZoneProperty(boss.getLocation(), ZoneUtils.ZoneProperty.NO_SUMMONS)) {
						return new ArrayList<>();
					}
					return p.TARGETS.getTargetsLocationList(boss);
				},
				(Location loc, int times) -> {
					if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_SUMMONS)) {
						return null;
					}
					return p.MOB_POOL.spawn(loc);
				},
				(LivingEntity bos, Location loc, int ticks) -> {
					if (p.SELF_GLOWING && ticks == 0) {
						bos.setGlowing(true);
					}

					if (p.SOUNDS != SoundsList.EMPTY) {
						p.SOUNDS.play(bos.getLocation());
					}

					p.PARTICLES.spawn(boss, boss.getLocation());

					if (p.SELF_GLOWING && ticks >= p.DURATION) {
						bos.setGlowing(false);
					}

				},
				(LivingEntity mob, Location loc, int ticks) -> {
					if (p.SUMMON_GLOWING && ticks == 0) {
						mob.setGlowing(true);
					}
					p.PARTICLES.spawn(boss, boss.getLocation());

					if (p.SUMMON_GLOWING && ticks >= p.DURATION) {
						mob.setGlowing(false);
					}
				});

			super.constructBoss(spell, p.DETECTION, null, p.DELAY);
		} else {
			Plugin.getInstance().getLogger().warning("[MobRisingBoss] tried to summon a boss with default LoSPool MOB_POOL = EMPTY, boss name=" + boss.getName());
		}
	}

}
