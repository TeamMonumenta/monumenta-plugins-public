package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ArcaneElixirCS extends EnergizingElixirCS {

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public @Nullable String getName() {
		return "Arcane Elixir";
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Brewing performance-enhancing potions",
			"is an ages-old alchemical tradition.",
			"It is however unsportsmanlike to",
			"use them in worldly competitions.");
	}

	@Override
	public void activate(Player player, int newStacks, boolean manualCast) {
		if (manualCast) {
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.PLAYERS, 1, 0);
		}
		ArcanePotionsCS.drawSingleCircle(player, player.getLocation().add(0, 0.25, 0), 0.5, null, Particle.SCRAPE);
	}

	@Override
	public void stackDecayEffect(Player player, int newStacks) {
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 0.6f, 0);
		ArcanePotionsCS.drawSingleCircle(player, player.getLocation().add(0, 0.25, 0), 0.5, null, Particle.SCRAPE);
	}

}
