package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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

	public void swiftnessEnhancement(final Player player, final Location blockLoc) {
		new PartialParticle(Particle.CLOUD, blockLoc, 20, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerPassive(player);
		player.playSound(blockLoc, Sound.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.PLAYERS, 0.8f, 1.0f);
	}

	public void toggleJumpBoostOff(final Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.6f);
	}

	public void toggleJumpBoostOn(final Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.6f);
	}
}
