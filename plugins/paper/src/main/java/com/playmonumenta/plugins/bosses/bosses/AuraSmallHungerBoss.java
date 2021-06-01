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
 * /bos var Tags add boss_auraeffect[effect=HUNGER,RADIUS=12,HEIGHT=7,particleNumber=16,COLORRED=58,COLORGREEN=160,COLORBLUE=25]]
 * </pre></blockquote>
 * @G3m1n1Boy
 */
public class AuraSmallHungerBoss extends BossAbilityGroup {
	public static final String identityTag = "aura_hunger";
	public static final int detectionRange = 40;

	private static final Particle.DustOptions HUNGER_COLOR = new Particle.DustOptions(Color.fromRGB(58, 160, 25), 2f);

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraSmallHungerBoss(plugin, boss);
	}

	public AuraSmallHungerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(boss, 12, 7, 12, 16, Particle.REDSTONE, HUNGER_COLOR,
			                  (Player player) -> {
			                      player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 1, true, true));
			                  })
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}
