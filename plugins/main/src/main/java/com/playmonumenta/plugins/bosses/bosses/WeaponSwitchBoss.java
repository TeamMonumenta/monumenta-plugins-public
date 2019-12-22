package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellWeaponSwitch;

public class WeaponSwitchBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_weaponswitch";
	public static final int detectionRange = 35;

	Mob mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WeaponSwitchBoss(plugin, boss);
	}

	public WeaponSwitchBoss(Plugin plugin, LivingEntity boss) throws Exception {
		if (!(boss instanceof Mob)) {
			throw new Exception("boss_weaponswitch only works on mobs!");
		}

		mBoss = (Mob)boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellWeaponSwitch(mBoss)
		));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}
