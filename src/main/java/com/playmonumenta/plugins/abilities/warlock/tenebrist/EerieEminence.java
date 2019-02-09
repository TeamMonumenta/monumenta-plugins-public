package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.PotionEffectApplyEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Eerie Eminence: Provides an AoE debuff aura around the player
 * that applies the last debuff (level 1 of debuff, ex: slowness
 * 2 —> slowness 1, or 10% for Vulnerability) you have applied to
 * everything around you in a 6/8 block radius for 4s. At level 2,
 * it provides the opposite effect to other players (not including
 * self) in the range. (Wither -> Regen, Slowness -> Speed, Vulnerability
 * -> Resistance)
 */

public class EerieEminence extends Ability {

	public EerieEminence(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "EerieEminence";
	}

	@Override
	public void PotionApplyEvent(PotionEffectApplyEvent event) {
		double radius = getAbilityScore() == 1 ? 6 : 8;
		int amp = event.getEffect().getType() == PotionEffectType.UNLUCK ? 1 : 0;
		for (Mob mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), radius)) {
			mob.addPotionEffect(new PotionEffect(event.getEffect().getType(), 20 * 4, amp));
		}
		for (Player player : PlayerUtils.getNearbyPlayers(mPlayer.getLocation(), radius)) {
			PotionEffectType type = null;
			PotionEffect effect = event.getEffect();
			if (effect.getType().equals(PotionEffectType.WEAKNESS)) {
				type = PotionEffectType.INCREASE_DAMAGE;
			} else if (effect.getType().equals(PotionEffectType.SLOW)) {
				type = PotionEffectType.SPEED;
			} else if (effect.getType().equals(PotionEffectType.WITHER) || effect.getType().equals(PotionEffectType.POISON)) {
				type = PotionEffectType.REGENERATION;
			} else if (effect.getType().equals(PotionEffectType.UNLUCK)) {
				type = PotionEffectType.DAMAGE_RESISTANCE;
			}
			if (type != null) {
				player.addPotionEffect(new PotionEffect(type, 20 * 4, 0));
			}
		}
	}

}
