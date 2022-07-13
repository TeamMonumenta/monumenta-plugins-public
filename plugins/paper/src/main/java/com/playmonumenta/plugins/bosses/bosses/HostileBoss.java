package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import java.util.Collections;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class HostileBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_hostile";

	@BossParam(help = "Make this LivingEntity fight player")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Attack of this mob")
		public double DAMAGE = 0;

		@BossParam(help = "Attack % of this mob")
		public double DAMAGE_PERCENTAGE = 0;

		@BossParam(help = "Damage type of this mob attack")
		public DamageEvent.DamageType TYPE = DamageEvent.DamageType.MELEE;

		@BossParam(help = "Particles summon at player eye")
		public ParticlesList PARTICLES = ParticlesList.EMPTY;

		@BossParam(help = "Sounds player when deal damage")
		public SoundsList SOUNDS = SoundsList.EMPTY;

		public String SPELL_NAME = "";
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HostileBoss(plugin, boss);
	}

	public HostileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		try {
			if (boss instanceof Creature creature) {
				NmsUtils.getVersionAdapter().setAggressive(creature, (LivingEntity target) -> {
					if (p.DAMAGE != 0) {
						BossUtils.blockableDamage(mBoss, target, p.TYPE, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation(), 0);
					}

					if (p.DAMAGE_PERCENTAGE != 0) {
						BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE);
					}

					if (p.SOUNDS != SoundsList.EMPTY) {
						p.SOUNDS.play(target.getEyeLocation());
					}

					if (p.PARTICLES != ParticlesList.EMPTY) {
						p.PARTICLES.spawn(boss, target.getEyeLocation());
					}

				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//this will make the mobs get damage from abilities
			boss.addScoreboardTag("Hostile");
		}
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}
}
