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
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class ImmortalPassengerBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_immortalpassenger";
	public static final int detectionRange = 40;

	private static final ImmutableList<Class<? extends Effect>> COPIED_EFFECTS = ImmutableList.of(InfernoDamage.class, CustomDamageOverTime.class);

	public static class Parameters extends BossParameters {
		@BossParam(help = "Whether or not damage taken by this mount is redirected to its passenger")
		public boolean TRANSFER_DAMAGE = true;
	}

	private final boolean mTransferDamage;

	private double mPassengerDamageThisTick = 0;
	private double mVehicleDamageThisTick = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ImmortalPassengerBoss(plugin, boss);
	}

	public ImmortalPassengerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mTransferDamage = p.TRANSFER_DAMAGE;

		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> {
				if (boss.getVehicle() == null) {
					boss.setHealth(0);
					boss.remove();
				}
			}, 1, true)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mTransferDamage && mBoss.getVehicle() instanceof LivingEntity vehicle && event.getSource() != null) {
			mPassengerDamageThisTick += event.getDamage();
			// Do this at the end of the tick so we can't miss the passenger being damaged
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (mVehicleDamageThisTick < mPassengerDamageThisTick) {
					DamageUtils.damage(null, vehicle, DamageEvent.DamageType.OTHER, mPassengerDamageThisTick - mVehicleDamageThisTick, null, false);
				}
				mPassengerDamageThisTick = 0;
			}, 0);
		}
		event.setDamage(0);
	}

	@Override
	public void bossPassengerHurt(DamageEvent event) {
		if (mTransferDamage && event.getDamagee() == mBoss.getVehicle()) {
			mVehicleDamageThisTick = Math.max(mVehicleDamageThisTick, event.getDamage());
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> mVehicleDamageThisTick = 0, 1);
		}
	}

	@Override
	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		if (mTransferDamage && mBoss.getVehicle() instanceof LivingEntity vehicle && COPIED_EFFECTS.contains(event.getEffect().getClass())) {
			event.setEntity(vehicle);
		}
	}

	@Override
	public void bossIgnited(int ticks) {
		if (mTransferDamage) {
			if (mBoss.getVehicle() instanceof LivingEntity vehicle) {
				EntityUtils.setFireTicksIfLower(ticks, vehicle);
			}
			mBoss.setFireTicks(0);
		}
	}

}

