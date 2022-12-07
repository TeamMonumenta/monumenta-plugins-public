package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellWeaponSwitch;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

public class WeaponSwitchBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_weaponswitch";
	public static final int detectionRange = 35;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WeaponSwitchBoss(plugin, boss);
	}

	public WeaponSwitchBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob)) {
			throw new Exception("boss_weaponswitch only works on mobs!");
		}

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellWeaponSwitch((Mob) mBoss)
		));

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, null);
	}
}
