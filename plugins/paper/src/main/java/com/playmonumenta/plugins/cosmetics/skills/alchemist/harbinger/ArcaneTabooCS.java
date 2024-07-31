package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcanePotionsCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
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
	public void periodicEffects(Player player, boolean twoHertz, boolean oneSecond, int ticks, boolean inBurst) {
		if (oneSecond) {
			Location loc = player.getLocation();
			AbilityUtils.playPassiveAbilitySound(player, player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 1);

			new PPCircle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, -0.25, 0), 0.5)
				.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
				.directionalMode(true).delta(0, 1, 0).extra(1)
				.spawnAsPlayerPassive(player);
			new PPCircle(inBurst ? Particle.FALLING_LAVA : Particle.FALLING_OBSIDIAN_TEAR, loc.clone().add(0, 0.75, 0), 0.5)
				.count(inBurst ? 10 : 3)
				.spawnAsPlayerPassive(player);
		}

		if (inBurst) {
			new PartialParticle(Particle.FALLING_LAVA, player.getLocation().add(0, 0.6, 0), 5)
				.delta(0.2, 0.6, 0.2)
				.spawnAsPlayerPassive(player);
		}
	}

	@Override
	public void burstEffects(Player player) {
		World world = player.getWorld();
		Location loc = player.getLocation();
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.0f, 1.7f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.4f, 0.6f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.7f, 0.4f);
		world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 0.8f, 0.2f);
		world.playSound(loc, "minecraft:block.amethyst_block.resonate", SoundCategory.PLAYERS, 2.0f, 1.7f);

		ArcanePotionsCS.drawAlchemyCircle(player, loc, 6, 5, false, ArcanePotionsCS.BISMUTH, true, false, false);
		new PartialParticle(Particle.FALLING_LAVA, player.getLocation().add(0, 0.6, 0), 20)
			.delta(0.2, 0.6, 0.2)
			.spawnAsPlayerActive(player);
	}

	@Override
	public void unburstEffects(Player player) {
		World world = player.getWorld();
		Location loc = player.getLocation();
		world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_STEP, SoundCategory.PLAYERS, 0.7f, 0.7f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.7f, 0.6f);
		world.playSound(loc, "minecraft:block.amethyst_block.resonate", SoundCategory.PLAYERS, 1.2f, 0.5f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 2.0f, 0.4f);
	}

	@Override
	public void toggle(Player player, boolean active) {
		super.toggle(player, active);
		World world = player.getWorld();
		Location loc = player.getLocation();
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1, 1);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1, 1);

		if (active) {
			ArcanePotionsCS.drawAlchemyCircle(player, player.getLocation(), 3, 5, false, ArcanePotionsCS.BISMUTH, false, false, false);
		} else {
			new PartialParticle(Particle.ENCHANTMENT_TABLE, LocationUtils.getHalfHeightLocation(player), 20)
				.delta(0.2, 0.6, 0.6)
				.spawnAsPlayerActive(player);
		}
	}

}
