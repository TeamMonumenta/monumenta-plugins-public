package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ManaLanceCS implements CosmeticSkill {

	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 0.9f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.MANA_LANCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TRIDENT;
	}

	public void lanceHitBlock(Player player, Location loc, World world) {
		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.65f);
				new PartialParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125).spawnAsPlayerActive(player);
			}
		}.runTaskLater(Plugin.getInstance(), 2);
	}


	public void lanceParticle(Player player, Location startLoc, Location endLoc, double size) {
		Vector dir = endLoc.clone().subtract(startLoc).toVector();
		ParticleUtils.drawParticleLineSlash(startLoc.clone().add(dir.clone().multiply(0.5)).add(dir.clone().normalize().multiply(0.75)), dir, 0.0d, 0.5 * dir.length() - 0.75, 0.1, 2,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(Particle.REDSTONE, lineLoc, 1, size / 7, size / 7, size / 7, MANA_LANCE_COLOR)
				.spawnAsPlayerActive(player));
		ParticleUtils.drawParticleLineSlash(startLoc.clone().add(dir.clone().multiply(0.5)).add(dir.clone().normalize().multiply(0.75)), dir, 0.0d, 0.5 * dir.length() - 0.75, 0.2, 3,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(Particle.REDSTONE, lineLoc, 2, 0.5 * size, 0.5 * size, 0.5 * size, MANA_LANCE_COLOR)
				.spawnAsPlayerActive(player));
		ParticleUtils.drawParticleLineSlash(startLoc.clone().add(dir.clone().multiply(0.5)).add(dir.clone().normalize().multiply(1.05)), dir, 0.0d, 0.5 * dir.length() - 1.05, 0.6, 4,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(Particle.EXPLOSION_NORMAL, lineLoc, 1)
				.spawnAsPlayerActive(player));
	}

	public void lanceSound(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.8f, 2.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.4f, 0.8f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.9f, 2.0f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.9f, 2.0f);
	}

	public void lanceHit(Location loc, Player player) {

	}
}
