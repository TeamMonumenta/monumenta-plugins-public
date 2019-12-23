package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellMobEffect;

public class FireResistantBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_fireresist";
	public static final int detectionRange = 100;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FireResistantBoss(plugin, boss);
	}

	public FireResistantBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		// Immediately apply the effect, don't wait
		Spell fireresist = new SpellMobEffect(mBoss, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0, false, false));
		fireresist.run();

		List<Spell> passiveSpells = Arrays.asList(fireresist);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
