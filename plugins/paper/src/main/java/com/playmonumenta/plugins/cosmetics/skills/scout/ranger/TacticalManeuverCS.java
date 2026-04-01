package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class TacticalManeuverCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TACTICAL_MANEUVER;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STRING;
	}

	public void maneuverStartEffect(World world, Player mPlayer, Vector dir) {
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 2);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 1.7f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 63, 0.25, 0.1, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 20, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
	}

	public void maneuverTickEffect(Player mPlayer) {
		new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 5, 0.25, 0.1, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
	}

	public void maneuverHitEffect(World world, Player mPlayer, LivingEntity le) {
		world.playSound(mPlayer.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 2.0f, 0.5f);
		new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 63, 0.25, 0.1, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
	}

	public void maneuverMarkTick(World world, Player player, LivingEntity le) {
		new PartialParticle(Particle.SMOKE_LARGE, le.getEyeLocation(), 1, 0, 0.12, 0, 0.22)
			.directionalMode(true)
			.spawnAsPlayerActive(player);
	}

	public void maneuverRefresh(World world, Player player, Location loc) {
		player.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2f, 2f);
		player.playSound(loc, Sound.ENTITY_BAT_TAKEOFF, SoundCategory.PLAYERS, 1f, 1f);

		new PartialParticle(Particle.CLOUD, loc, 20)
			.delta(0.2)
			.extra(0.25f)
			.spawnAsPlayerActive(player);
	}
}
