package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SwiftnessCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SWIFTNESS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.RABBIT_FOOT;
	}

	public void swiftnessDoubleJump(final Player player, final Location blockLoc) {
		new PartialParticle(Particle.CLOUD, blockLoc, 20, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(player);

		player.getWorld().playSound(blockLoc, Sound.ENTITY_BREEZE_IDLE_AIR, SoundCategory.PLAYERS, 0.2f, 1.4f);
		player.getWorld().playSound(blockLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.7f, 2f);
	}

	public void swiftnessDash(final Player player, Location loc) {
		player.getWorld().playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.PLAYERS, 0.6f, 2.0f);
		player.getWorld().playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.4f, 1.1f);
		player.getWorld().playSound(loc, Sound.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.8f, 1.1f);

		ParticleUtils.drawParticleCircleExplosion(player, player.getEyeLocation(), 0, 0.85, 0, -90,
			10, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
	}

	public void swiftnessDashTick(Player player, Vector dir) {
		new PartialParticle(Particle.CLOUD, LocationUtils.getHalfHeightLocation(player))
			.count(3)
			.delta(0.25, 0.5, 0.25)
			.spawnAsPlayerActive(player);
	}

	public void toggleJumpBoostOff(final Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.6f);
	}

	public void toggleJumpBoostOn(final Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.6f);
	}
}
