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

/**
 * @deprecated use boss_auraeffect instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_auraeffect
 * /bos var Tags add boss_auraeffect[effect=SLOW]
 * </pre></blockquote>
 * @G3m1n1Boy
 */
public class AuraLargeSlownessBoss extends BossAbilityGroup {
	public static final String identityTag = "SlownessAura";
	public static final int detectionRange = 45;

	private static final Particle.DustOptions SLOWNESS_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2f);

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraLargeSlownessBoss(plugin, boss);
	}

	public AuraLargeSlownessBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(boss, 35, 20, 35, 20, Particle.REDSTONE, SLOWNESS_COLOR,
			                  (Player player) -> {
			                      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0, true, true));
			                  })
		);

		boss.setRemoveWhenFarAway(false);
		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}
