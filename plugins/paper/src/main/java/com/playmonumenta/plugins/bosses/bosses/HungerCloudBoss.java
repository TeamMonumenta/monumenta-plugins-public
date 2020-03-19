package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellMobEffect;

public class HungerCloudBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_hungercloud";
	public static final int detectionRange = 100;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HungerCloudBoss(plugin, boss);
	}

	public HungerCloudBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
			new SpellMobEffect(mBoss, new PotionEffect(PotionEffectType.HUNGER, 600, 99, false, false))
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
