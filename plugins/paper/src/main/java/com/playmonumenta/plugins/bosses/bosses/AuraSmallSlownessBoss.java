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
 * /bos var Tags add boss_auraeffect[effect=SLOW,RADIUS=8,HEIGHT=5,particleNumber=14]
 * </pre></blockquote>
 * @G3m1n1Boy
 */
public class AuraSmallSlownessBoss extends BossAbilityGroup {
	public static final String identityTag = "aura_slowness";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraSmallSlownessBoss(plugin, boss);
	}

	public AuraSmallSlownessBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(boss, 8, 5, 8, 14, null, null,
			                  (Player player) -> {
			                      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 1, true, true));
			                  })
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}
