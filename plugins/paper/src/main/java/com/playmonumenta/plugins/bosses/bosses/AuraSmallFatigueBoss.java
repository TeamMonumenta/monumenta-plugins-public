package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAura;

public class AuraSmallFatigueBoss extends BossAbilityGroup {
	public static final String identityTag = "aura_fatigue";
	public static final int detectionRange = 40;

	LivingEntity mBoss;

	private static final Particle.DustOptions FATIGUE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 232, 160), 2f);
	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraSmallFatigueBoss(plugin, boss);
	}

	public AuraSmallFatigueBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(mBoss, 8, 5, 8, 14, Particle.REDSTONE, FATIGUE_COLOR,
			                  (Player player) -> {
			                      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 60, 1, true, true));
			                  })
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
