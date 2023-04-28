package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class TabooCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TABOO;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void periodicEffects(Player player, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			new PartialParticle(Particle.DAMAGE_INDICATOR, player.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0).spawnAsPlayerBuff(player);
			new PartialParticle(Particle.SQUID_INK, player.getEyeLocation(), 1, 0.2, 0.2, 0.2, 0).spawnAsPlayerBuff(player);
			AbilityUtils.playPassiveAbilitySound(player, player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 1);
		}
	}

	public void healEffects(Player player) {
		player.getWorld().playSound(player.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 1, 1.2f);
		new PartialParticle(Particle.HEART, player.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(player);
	}

	public void toggle(Player player, boolean active) {
		if (active) {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WANDERING_TRADER_DRINK_POTION, SoundCategory.PLAYERS, 1, 0.9f);
		} else {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.8f, 1.2f);
		}
	}

}
