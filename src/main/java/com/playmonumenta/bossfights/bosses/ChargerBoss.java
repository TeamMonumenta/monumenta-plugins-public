package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;

import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.SpellGenericCharge;

public class ChargerBoss extends BossAbilityGroup
{
	public static final String identityTag = "boss_charger";
	public static final int detectionRange = 20;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception
	{
		return new ChargerBoss(plugin, boss);
	}

	public ChargerBoss(Plugin plugin, LivingEntity boss)
	{
		mBoss = boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		                                                 new SpellGenericCharge(plugin, mBoss, detectionRange, 15.0F)
		                                             ));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}
