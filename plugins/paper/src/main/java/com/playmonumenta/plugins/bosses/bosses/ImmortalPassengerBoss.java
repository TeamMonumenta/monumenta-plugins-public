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

	private static final ImmutableList<Class<? extends Effect>> COPIED_EFFECTS = ImmutableList.of(InfernoDamage.class, CustomDamageOverTime.class);

	public static class Parameters extends BossParameters {
		@BossParam(help = "Whether or not damage taken by this mount is redirected to its passenger")
		public boolean TRANSFER_DAMAGE = true;

		@BossParam(help = "detection range of this ability")
		public int DETECTION = 40;
	}

	private final boolean mTransferDamage;

	private double mPassengerDamageThisTick = 0;
	private double mVehicleDamageThisTick = 0;

	public ImmortalPassengerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mTransferDamage = p.TRANSFER_DAMAGE;
		// these bosses break when reloaded (won't be passengers to the correct boss)
		EntityUtils.setRemoveEntityOnUnload(boss);
		boss.setRemoveWhenFarAway(true);

		List<Spell> passiveSpells = List.of(
			new SpellRunAction(() -> {
				if (boss.getVehicle() == null || boss.getVehicle().getScoreboardTags().contains("boss_immortalmount")) {
					boss.setHealth(0);
					boss.remove();
				}
			}, 1, true)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, p.DETECTION, null, 0, 1);
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
		event.setFlatDamage(0);
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
