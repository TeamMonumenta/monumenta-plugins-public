package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TabooCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TABOO;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void periodicEffects(Player player, boolean twoHertz, boolean oneSecond, int ticks, double currentSelfDamage, double absorptionLossThreshold) {
		Location loc = player.getLocation();
		if (oneSecond) {
			new PartialParticle(Particle.FALLING_DUST, loc.clone().add(0, player.getHeight() / 2, 0), 5, 0.25, 0.2, 0.25, 0).data(Material.WARPED_HYPHAE.createBlockData()).spawnAsPlayerBuff(player);
			new PartialParticle(Particle.FALLING_OBSIDIAN_TEAR, loc.clone().add(0, player.getHeight() / 2, 0), 10, 0.25, 0.2, 0.25, 0).spawnAsPlayerBuff(player);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 1);
		}

		if (currentSelfDamage >= absorptionLossThreshold) {
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_GRAVEL_BREAK, 0.2f, 0.5f);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CHORUS_FLOWER_GROW, 2f, 1f);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CHORUS_FLOWER_DEATH, 2f, 1f);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CHORUS_FLOWER_DEATH, 2f, 1f);
		} else if (currentSelfDamage >= absorptionLossThreshold / 2 && twoHertz) {
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_GRAVEL_BREAK, 0.25f, 0.5f);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CHORUS_FLOWER_GROW, 1.5f, 1f);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CHORUS_FLOWER_GROW, 1.5f, 1f);
		} else if (oneSecond) {
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_GRAVEL_BREAK, 0.2f, 0.5f);
		}
	}

	public void toggle(Player player, boolean active) {
		if (active) {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WANDERING_TRADER_DRINK_POTION, SoundCategory.PLAYERS, 1, 0.9f);
		} else {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.8f, 1.2f);
		}
	}

	public void notifyAbsorptionLossStart(Player player, Plugin plugin) {
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (player.isDead() || !player.isValid() || !player.isOnline()) {
					cancel();
					return;
				}

				player.playSound(player, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 1.5f);
				player.playSound(player, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 1.5f);
				player.playSound(player, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 1.125f);
				player.playSound(player, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 1.125f);
				player.playSound(player, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.75f);
				player.playSound(player, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.75f);
				mT++;
				if (mT >= 3) {
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 20);
	}
}
