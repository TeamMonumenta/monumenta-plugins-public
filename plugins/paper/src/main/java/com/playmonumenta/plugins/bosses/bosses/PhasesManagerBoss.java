package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.BossPhasesList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class PhasesManagerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_phases_manager";

	public static class Parameters extends BossParameters {
		public BossPhasesList PHASES = BossPhasesList.emptyPhaseList();
	}

	private final Parameters mParam;

	public PhasesManagerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = BossParameters.getParameters(boss, identityTag, new Parameters());
		mParam.PHASES.onSpawn(boss);

		Spell spell = new Spell() {
			int mTicks = 0;

			@Override
			public void run() {
				mParam.PHASES.tick(mBoss, mTicks);
				mTicks += 5;
			}

			@Override
			public int cooldownTicks() {
				return 5;
			}
		};

		super.constructBoss(SpellManager.EMPTY, List.of(spell), -1, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		mParam.PHASES.onHurt(mBoss, event.getSource(), event);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		mParam.PHASES.onDamage(mBoss, event.getDamagee(), event);
	}

	@Override
	public void death(EntityDeathEvent event) {
		mParam.PHASES.onDeath(mBoss);
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		mParam.PHASES.onBossCastAbility(mBoss, event);
	}

	public void onCustomTrigger(String key) {
		mParam.PHASES.onCustom(mBoss, key);
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PhasesManagerBoss(plugin, boss);
	}
}
