package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class EnergizingElixirCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ENERGIZING_ELIXIR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.RABBIT_FOOT;
	}

	public void activate(Player player) {
		World world = player.getWorld();
		new PartialParticle(Particle.TOTEM, player.getLocation().clone().add(0, 1, 0), 50, 1.5, 1, 1.5, 0).spawnAsPlayerActive(player);
		world.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.PLAYERS, 1, 0);
	}

	public void displayCloud(Player player, Location loc, double radius, double height, boolean isGruesome, @Nullable AlchemistPotions alchemistPotions) {
		if (alchemistPotions == null) {
			return;
		}
		Particle.DustOptions dustOptions = new Particle.DustOptions(
			isGruesome ? alchemistPotions.mCosmetic.getGruesomeColor() : alchemistPotions.mCosmetic.getBrutalColor(),
			0.5f);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, height / 2, 0))
			.count(30)
			.delta(radius / 2, height / 2, radius / 2)
			.data(dustOptions)
			.spawnAsPlayerBuff(player);
	}
}
