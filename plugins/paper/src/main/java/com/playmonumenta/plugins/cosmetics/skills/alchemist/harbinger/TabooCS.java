package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
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

	public void periodicEffects(Player player, boolean twoHertz, boolean oneSecond, int ticks, boolean inBurst) {
		if (oneSecond) {
			new PartialParticle(Particle.FALLING_DUST, player.getLocation().clone().add(0, player.getHeight() / 2, 0), 5, 0.25, 0.2, 0.25, 0).data(Material.WARPED_HYPHAE.createBlockData()).spawnAsPlayerBuff(player);
			new PartialParticle(Particle.FALLING_OBSIDIAN_TEAR, player.getLocation().clone().add(0, player.getHeight() / 2, 0), 10, 0.25, 0.2, 0.25, 0).spawnAsPlayerBuff(player);
			AbilityUtils.playPassiveAbilitySound(player, player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 1);

			if (inBurst) {
				new PPCircle(Particle.FLAME, player.getLocation().clone().add(0, 0.1, 0), 2).count(50).ringMode(false).spawnAsPlayerBuff(player);
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
			}
		}
	}

	public void burstEffects(Player player) {
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1, 1.6f);
		player.getWorld().playSound(player.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 1, 1.2f);
		new PartialParticle(Particle.DAMAGE_INDICATOR, player.getLocation().clone().add(0, player.getHeight() / 2, 0), 20, 0.2, 0.2, 0.2, 0).spawnAsPlayerBuff(player);
		new PPCircle(Particle.FLAME, player.getLocation().clone().add(0, 0.1, 0), 2).count(50).ringMode(false).spawnAsPlayerBuff(player);
	}

	public void unburstEffects(Player player) {
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1, 1.7f);
	}

	public void toggle(Player player, boolean active) {
		if (active) {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WANDERING_TRADER_DRINK_POTION, SoundCategory.PLAYERS, 1, 0.9f);
		} else {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.8f, 1.2f);
		}
	}

}
