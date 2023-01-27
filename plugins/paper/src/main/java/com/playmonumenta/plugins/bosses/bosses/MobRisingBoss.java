package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSummon;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class MobRisingBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_mob_rising";

	public static class Parameters extends BossParameters {

		public int DETECTION = 100;
		public int DELAY = 80;
		public int COOLDOWN = 160;
		public int DURATION = 80;
		public int RANGE = 20;
		public boolean CAN_BE_STOPPED = false;
		public boolean CAN_MOVE = false;
		public boolean SINGLE_TARGET = false;
		public int MOB_NUMBER = 0;
		public float DEPTH = 2.5f;

		public LoSPool MOB_POOL = LoSPool.EMPTY;

		public EntityTargets TARGETS = EntityTargets.GENERIC_SELF_TARGET;

		public SoundsList SOUNDS = SoundsList.EMPTY;

	}


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new MobRisingBoss(plugin, boss);
	}

	public MobRisingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (p.MOB_POOL != LoSPool.EMPTY) {
			SpellManager activeSpells = new SpellManager(List.of(
					new SpellBaseSummon(
							plugin,
							boss,
							p.COOLDOWN,
							p.DURATION,
							p.RANGE,
							p.DEPTH,
							p.CAN_BE_STOPPED,
							p.CAN_MOVE,
							p.SINGLE_TARGET,
							() -> {
								return p.MOB_NUMBER;
							},
							() -> {
								if (ZoneUtils.hasZoneProperty(boss.getLocation(), ZoneUtils.ZoneProperty.RESIST_5)
										&& !ZoneUtils.hasZoneProperty(boss.getLocation(), ZoneUtils.ZoneProperty.BLITZ)) {
									return new ArrayList<Location>();
								}
								return p.TARGETS.getTargetsLocationList(boss);
							},
							(Location loc, int times) -> {
								if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.RESIST_5)
										&& !ZoneUtils.hasZoneProperty(boss.getLocation(), ZoneUtils.ZoneProperty.BLITZ)) {
									return null;
								}
								return p.MOB_POOL.spawn(loc);
							},
							(LivingEntity bos, Location loc, int ticks) -> {
								if (ticks == 0) {
									bos.setGlowing(true);
								}

								if (p.SOUNDS != SoundsList.EMPTY) {
									p.SOUNDS.play(bos.getLocation());
								}

								new PartialParticle(Particle.SPELL_INSTANT, loc, 2, 0.5, 0.5, 0.5, 0).spawnAsEntityActive(boss);

								if (ticks >= p.DURATION) {
									bos.setGlowing(false);
								}

							},
							(LivingEntity mob, Location loc, int ticks) -> {
								if (ticks == 0) {
									mob.setGlowing(true);
								}
								new PartialParticle(Particle.SPELL_INSTANT, loc, 2, 0.5, 0.5, 0.5, 0).spawnAsEntityActive(boss);

								if (ticks >= p.DURATION) {
									mob.setGlowing(false);
								}
							})
			));


			super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
		} else {
			Plugin.getInstance().getLogger().warning("[MobRisingBoss] tried to summon a boss with default LoSPool MOB_POOL = EMPTY");
		}
	}

}
