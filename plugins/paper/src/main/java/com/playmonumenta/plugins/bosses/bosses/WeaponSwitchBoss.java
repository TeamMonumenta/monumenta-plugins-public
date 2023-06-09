package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.SpellWeaponSwitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

public class WeaponSwitchBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_weaponswitch";
	public static final int detectionRange = 35;

	public WeaponSwitchBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob mob)) {
			throw new Exception("boss_weaponswitch only works on mobs!");
		}

		super.constructBoss(new SpellWeaponSwitch(mob), detectionRange);
	}
}
