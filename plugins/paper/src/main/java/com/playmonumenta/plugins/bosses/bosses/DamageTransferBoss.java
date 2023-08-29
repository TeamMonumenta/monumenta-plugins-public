package com.playmonumenta.plugins.bosses.bosses;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.InfernoDamage;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class DamageTransferBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_damagetransfer";

	private static final ImmutableList<Class<? extends Effect>> COPIED_EFFECTS = ImmutableList.of(InfernoDamage.class, CustomDamageOverTime.class);

	public static class Parameters extends BossParameters {
		@BossParam(help = "targeted entity/entities. don't have 2 mobs target each other with this ability (it can crash the shard)")
		public EntityTargets TARGETS = EntityTargets.GENERIC_MOB_TARGET.setRange(15);
		@BossParam(help = "detection radius")
		public int DETECTION = 30;

	}

	private double mBossDamageThisTick = 0;
	private @MonotonicNonNull LivingEntity mTarget;

	public DamageTransferBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> {
				List<? extends LivingEntity> targets = p.TARGETS.getTargetsList(mBoss);
				mTarget = targets.get(0);
			}, 1, true)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, p.DETECTION, null, 0, 1);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mTarget != null && !mTarget.isDead() && event.getSource() != null) {
			mBossDamageThisTick += event.getDamage();
			// Do this at the end of the tick so we can't miss the passenger being damaged
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				DamageUtils.damage(null, mTarget, event.getType(), mBossDamageThisTick, null, false);
				mBossDamageThisTick = 0;
			}, 0);
		} else {
			DamageUtils.damage(null, mBoss, event.getType(), event.getDamage(), null, false);
		}
		event.setDamage(0);
	}

	@Override
	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		if (mTarget != null && !mTarget.isDead() && COPIED_EFFECTS.contains(event.getEffect().getClass())) {
			event.setEntity(mTarget);
		} else {
			event.setEntity(mBoss);
		}
	}

	@Override
	public void bossIgnited(int ticks) {
		if (mTarget != null && !mTarget.isDead()) {
			EntityUtils.setFireTicksIfLower(ticks, mTarget);
			mBoss.setFireTicks(0);
		} else {
			mBoss.setFireTicks(ticks);
		}
	}
}
