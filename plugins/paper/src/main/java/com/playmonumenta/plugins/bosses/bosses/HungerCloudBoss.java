package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellMobEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

public final class HungerCloudBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_hungercloud";
	public static final int detectionRange = 100;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HungerCloudBoss(plugin, boss);
	}

	public HungerCloudBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = Arrays.asList(
			new SpellMobEffect(boss, new PotionEffect(PotionEffectType.HUNGER, 600, 99, false, false))
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}
