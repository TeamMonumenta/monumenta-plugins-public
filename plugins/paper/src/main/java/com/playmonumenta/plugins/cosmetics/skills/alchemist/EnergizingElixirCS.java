package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class EnergizingElixirCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ENERGIZING_ELIXIR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.RABBIT_FOOT;
	}

	public void activate(Player player, int newStacks, boolean manualCast) {
		if (!manualCast) {
			stackDecayEffect(player, newStacks);
			return;
		}
		World world = player.getWorld();
		new PartialParticle(Particle.TOTEM, player.getLocation().clone().add(0, 1, 0), 50, 1.5, 1, 1.5, 0).spawnAsPlayerActive(player);
		world.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.PLAYERS, 1, 0);
	}

	public void toggleRecastOff(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.6f);
	}

	public void toggleRecastOn(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.6f);
	}

	public void stackDecayEffect(Player player, int newStacks) {
		World world = player.getWorld();
		new PartialParticle(Particle.TOTEM, player.getLocation().clone().add(0, 1, 0), 10, 0.3, 1, 0.3, 0).spawnAsPlayerActive(player);
		world.playSound(player.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 0.6f, 0);
	}
}
