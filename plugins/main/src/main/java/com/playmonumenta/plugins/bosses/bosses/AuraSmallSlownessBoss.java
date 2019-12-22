package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellBaseAura;

public class AuraSmallSlownessBoss extends BossAbilityGroup {
	public static final String identityTag = "aura_slowness";
	public static final int detectionRange = 40;

	LivingEntity mBoss;
	private static final Particle.DustOptions SLOWNESS_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2f);

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraSmallSlownessBoss(plugin, boss);
	}

	public AuraSmallSlownessBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(mBoss, 8, 5, 8, 14, Particle.REDSTONE, SLOWNESS_COLOR,
			                  (Player player) -> {
			                      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1, true, true));
			                  })
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
