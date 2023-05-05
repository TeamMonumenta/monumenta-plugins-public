package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class TransmutationRingCS implements CosmeticSkill {

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.2f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TRANSMUTATION_RING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOWSTONE_DUST;
	}

	public void startEffect(Player player, Location center, double radius) {
		center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 3f, 0.35f);
	}

	public void periodicEffect(Player player, Location center, double radius, int tick, int maxTicks, int maximumPotentialTicks) {
		PPCircle particles = new PPCircle(Particle.REDSTONE, center, radius).data(GOLD_COLOR);
		particles.count((int) Math.floor(120 * radius / 5)).location(center.clone().add(0, 0.25, 0)).spawnAsPlayerActive(player);
		particles.count((int) Math.floor(30 * radius / 5)).location(center.clone().add(0, 0.75, 0)).spawnAsPlayerActive(player);
		particles.count((int) Math.floor(15 * radius / 5)).location(center.clone().add(0, 1.25, 0)).spawnAsPlayerActive(player);
		particles.count((int) Math.floor(7 * radius / 5)).location(center.clone().add(0, 2, 0)).spawnAsPlayerActive(player);
	}

	public void effectOnKill(Player mPlayer, Location loc) {
		mPlayer.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1, 2);
	}

}
