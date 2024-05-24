package com.playmonumenta.plugins.cosmetics.skills.scout;

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

public class SwiftnessCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SWIFTNESS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.RABBIT_FOOT;
	}

	public void swiftnessEnhancement(Player player) {
		Location loc = player.getLocation();
		World world = player.getWorld();
		new PartialParticle(Particle.CLOUD, loc, 40, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_WITCH_THROW, SoundCategory.PLAYERS, 1.25f, 2f);
	}

	public void toggleJumpBoostOff(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.6f);
	}

	public void toggleJumpBoostOn(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.6f);
	}
}
