package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcanePotionsCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ArcaneTabooCS extends TabooCS {

	public static final String NAME = "Arcane Taboo";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Using your own life force to fuel potions",
			"is forbidden by most alchemist circles.",
			"The power gained is vast, but fleeting.",
			"And why consider it, when there are so many",
			"lesser life forms one can use as sacrifices instead?"
		);
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public void periodicEffects(Player player, World world, Location loc, boolean twoHertz, boolean oneSecond, int ticks, boolean inBurst) {
		if (oneSecond) {
			AbilityUtils.playPassiveAbilitySound(player, player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 1);

			new PPCircle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, -0.25, 0), 0.5)
				.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
				.directionalMode(true).delta(0, 1, 0).extra(1)
				.spawnAsPlayerActive(player);
			new PPCircle(Particle.FALLING_OBSIDIAN_TEAR, loc.clone().add(0, 0.75, 0), 0.5)
				.count(3)
				.spawnAsPlayerActive(player);
		}
	}

	@Override
	public void burstEffects(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1, 1.6f);
		world.playSound(loc, Sound.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 1, 1.2f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1, 1);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1, 1);
		ArcanePotionsCS.drawAlchemyCircle(player, loc, 6, 5, false, ArcanePotionsCS.BISMUTH, true);
	}

	@Override
	public void unburstEffects(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1, 1.7f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1, 1);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1, 1);
	}

}
