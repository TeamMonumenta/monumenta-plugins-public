package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import java.util.Collections;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class HostileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_hostile";

	@BossParam(help = "Force a Creature to target and attempt to damage Players")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Damage amount per attack")
		public double DAMAGE = 0;

		@BossParam(help = "Percent Health True damage to apply on hit")
		public double DAMAGE_PERCENTAGE = 0;

		@BossParam(help = "Damage type to use with the DAMAGE param")
		public DamageEvent.DamageType TYPE = DamageEvent.DamageType.MELEE;

		@BossParam(help = "Particles summoned on the target when hit")
		public ParticlesList PARTICLES = ParticlesList.EMPTY;

		@BossParam(help = "Sounds played when damage is dealt")
		public SoundsList SOUNDS = SoundsList.EMPTY;

		@BossParam(help = "Name of the attack that shows up in chat")
		public String SPELL_NAME = "";

		@BossParam(help = "How long the attack breaks shields for. If 0, attack doesn't break shields")
		public int SHIELD_BREAK_TICKS = 0;

		@BossParam(help = "Melee Attack Range. I would not recommend using this for anything that is ranged.")
		public double MELEE_ATTACK_RANGE = 0;
	}

	public HostileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		try {
			if (boss instanceof Creature creature) {
				NmsUtils.getVersionAdapter().setAggressive(creature, (LivingEntity target) -> {
					if (p.DAMAGE > 0) {
						BossUtils.blockableDamage(mBoss, target, p.TYPE, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation(), p.SHIELD_BREAK_TICKS);
					}

					if (p.DAMAGE_PERCENTAGE > 0) {
						DamageUtils.damage(mBoss, target, new DamageEvent.Metadata(DamageEvent.DamageType.TRUE,
								null, null, p.SPELL_NAME), p.DAMAGE_PERCENTAGE * EntityUtils.getMaxHealth(target),
							false, true, true);
					}

					if (p.SOUNDS != SoundsList.EMPTY) {
						p.SOUNDS.play(target.getEyeLocation());
					}

					if (p.PARTICLES != ParticlesList.EMPTY) {
						p.PARTICLES.spawn(boss, target.getEyeLocation());
					}

				}, p.MELEE_ATTACK_RANGE);
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
