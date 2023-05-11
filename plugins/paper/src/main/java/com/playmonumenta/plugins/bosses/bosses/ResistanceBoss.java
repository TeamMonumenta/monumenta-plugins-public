package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class ResistanceBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_resistance";
	public Parameters mParams;

	public static class Parameters extends BossParameters {
		@BossParam(help = "The % or amount the damage is increased or decreased.")
		public double DAMAGE_INCREASE = 0;

		@BossParam(help = "Damage type that is increased.")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.TRUE;

		@BossParam(help = "If it should be a universal increase.")
		public boolean UNIVERSAL_INCREASE = true;

		@BossParam(help = "Additive or Multiply")
		public ItemStatUtils.Operation DAMAGE_INCREASE_TYPE = ItemStatUtils.Operation.MULTIPLY;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ResistanceBoss(plugin, boss);
	}

	public ResistanceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (!event.getType().equals(DamageEvent.DamageType.TRUE)) {
			if (mParams.DAMAGE_INCREASE_TYPE.equals(ItemStatUtils.Operation.MULTIPLY)) {
				if (mParams.UNIVERSAL_INCREASE || mParams.DAMAGE_TYPE.equals(event.getType())) {
					event.setDamage(event.getDamage() * (1 + mParams.DAMAGE_INCREASE));
				}
			} else {
				if (mParams.UNIVERSAL_INCREASE || mParams.DAMAGE_TYPE.equals(event.getType())) {
					event.setDamage(event.getDamage() + mParams.DAMAGE_INCREASE);
				}
			}
		}
	}
}
