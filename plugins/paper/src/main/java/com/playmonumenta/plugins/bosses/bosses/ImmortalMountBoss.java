package com.playmonumenta.plugins.bosses.bosses;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class ImmortalMountBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_immortalmount";
	public static final int detectionRange = 40;

	private static final ImmutableList<Class<? extends Effect>> COPIED_EFFECTS = ImmutableList.of(InfernoDamage.class, CustomDamageOverTime.class);

	public static class Parameters extends BossParameters {
		@BossParam(help = "Whether or not damage taken by this mount is redirected to its passenger")
		public boolean TRANSFER_DAMAGE = true;
	}

	private final boolean mTransferDamage;

	private @MonotonicNonNull LivingEntity mPassenger;
	private double mMountDamageThisTick = 0;
	private double mPassengerDamageThisTick = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ImmortalMountBoss(plugin, boss);
	}

	public ImmortalMountBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mTransferDamage = p.TRANSFER_DAMAGE;

		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> {
				List<Entity> passengers = boss.getPassengers();
				if (passengers.size() == 0) {
					boss.setHealth(0);
					boss.remove();
				} else {
					for (Entity entity : passengers) {
						if (entity instanceof LivingEntity livingEntity) {
							if (livingEntity.isDead()) {
								boss.removePassenger(entity);
							}
							// Arbitrarily choose a living entity to be the "main" passenger
							if (mPassenger == null) {
								mPassenger = livingEntity;
							}
						}
					}
				}
			}, 1, true)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mTransferDamage && mPassenger != null && event.getSource() != null) {
			mMountDamageThisTick += event.getDamage();
			// Do this at the end of the tick so we can't miss the passenger being damaged
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (mPassengerDamageThisTick < mMountDamageThisTick) {
					DamageUtils.damage(null, mPassenger, DamageEvent.DamageType.OTHER, mMountDamageThisTick - mPassengerDamageThisTick, null, false);
				}
				mMountDamageThisTick = 0;
			}, 0);
		}
		event.setDamage(0);
	}

	@Override
	public void bossPassengerHurt(DamageEvent event) {
		if (mTransferDamage && event.getDamagee() == mPassenger) {
			mPassengerDamageThisTick = Math.max(mPassengerDamageThisTick, event.getDamage());
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPassengerDamageThisTick = 0, 1);
		}
	}

	@Override
	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		if (mTransferDamage && mPassenger != null && COPIED_EFFECTS.contains(event.getEffect().getClass())) {
			event.setEntity(mPassenger);
		}
	}

	@Override
	public void bossIgnited(int ticks) {
		if (mTransferDamage) {
			if (mPassenger != null) {
				EntityUtils.setFireTicksIfLower(ticks, mPassenger);
			}
			mBoss.setFireTicks(0);
		}
	}

}
