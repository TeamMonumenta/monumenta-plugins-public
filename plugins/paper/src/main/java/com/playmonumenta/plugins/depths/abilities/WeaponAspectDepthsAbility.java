package com.playmonumenta.plugins.depths.abilities;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Player;

public abstract class WeaponAspectDepthsAbility extends DepthsAbility {

	public WeaponAspectDepthsAbility(Plugin plugin, Player player, DepthsAbilityInfo<? extends WeaponAspectDepthsAbility> info) {
		super(plugin, player, info);
	}

}
