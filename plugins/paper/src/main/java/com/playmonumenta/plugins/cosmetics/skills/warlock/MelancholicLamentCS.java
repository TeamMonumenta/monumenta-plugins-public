package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class MelancholicLamentCS implements CosmeticSkill {
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(235, 235, 224), 1.0f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.MELANCHOLIC_LAMENT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GHAST_TEAR;
	}

	public void onCast(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 1.9f, 0.7f);
		world.playSound(loc, Sound.ENTITY_STRAY_AMBIENT, SoundCategory.PLAYERS, 2.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_HORSE_DEATH, SoundCategory.PLAYERS, 0.3f, 1.2f);
		world.playSound(loc, Sound.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED, SoundCategory.PLAYERS, 1.2f, 0.7f);
		world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.5f, 0.3f);
		world.playSound(loc, Sound.ENTITY_GOAT_SCREAMING_PREPARE_RAM, SoundCategory.PLAYERS, 0.5f, 0.4f);

		new PartialParticle(Particle.REDSTONE, loc, 300, 8, 8, 8, 0.125, COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 300, 8, 8, 8, 0.125).spawnAsPlayerActive(player);
	}

	public void enhancementTick(Player otherPlayer, Player user) {
		Location loc = otherPlayer.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 20, 0.5, 0, 0.5, 0.125).spawnAsPlayerActive(user);
	}

	public void onCleanse(Player otherPlayer, Player user) {
		Location loc = otherPlayer.getLocation();
		new PartialParticle(Particle.REDSTONE, loc, 13, 0.25, 2, 0.25, 0.125, COLOR).spawnAsPlayerActive(user);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 13, 0.25, 2, 0.25, 0.125).spawnAsPlayerActive(user);
	}
}
