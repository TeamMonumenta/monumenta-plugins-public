package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellBaseAura;

public class AuraLargeSlownessBoss extends BossAbilityGroup {
	public static final String identityTag = "SlownessAura";
	public static final int detectionRange = 45;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraLargeSlownessBoss(plugin, boss);
	}

	public AuraLargeSlownessBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(mBoss, 35, 20, 35, 10, Particle.FALLING_DUST, Material.ANVIL.createBlockData(),
			                  (Player player) -> {
			                      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0, true, true));
			                  })
		);

		mBoss.setRemoveWhenFarAway(false);
		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
