package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class SpellBaseNova extends SpellBaseAoE {

	public SpellBaseNova(Plugin plugin, Entity launcher, int radius, int duration, int cooldown, boolean canMoveWhileCasting,
		Sound chargeSound, ChargeAuraAction chargeAuraAction, ChargeCircleAction chargeCircleAction,
		OutburstAction outburstAction, CircleOutburstAction circleOutburstAction, DealDamageAction dealDamageAction) {
		super(plugin, launcher, radius, duration, cooldown, canMoveWhileCasting, chargeSound, 1f, 1, chargeAuraAction, chargeCircleAction, outburstAction,
	circleOutburstAction, dealDamageAction);
	}
}
