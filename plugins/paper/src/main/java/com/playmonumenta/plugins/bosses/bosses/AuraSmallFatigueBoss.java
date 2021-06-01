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
 * /bos var Tags add boss_auraeffect[effect=SLOW_DIGGING,RADIUS=8,HEIGHT=5,particleNumber=14,COLORRED=255,COLORGREEN=232,COLORBLUE=160]
 * </pre></blockquote>
 * @G3m1n1Boy
 */
public class AuraSmallFatigueBoss extends BossAbilityGroup {
	public static final String identityTag = "aura_fatigue";
	public static final int detectionRange = 40;
	private static final Particle.DustOptions FATIGUE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 232, 160), 2f);

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraSmallFatigueBoss(plugin, boss);
	}

	public AuraSmallFatigueBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(boss, 8, 5, 8, 14, Particle.REDSTONE, FATIGUE_COLOR,
			                  (Player player) -> {
			                      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 60, 1, true, true));
			                  })
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}
