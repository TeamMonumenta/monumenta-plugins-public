package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BruteForceCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BRUTE_FORCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STONE_AXE;
	}

	public void bruteOnDamage(Player player, World world, Location loc, double radius, int combo) {
		world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.2f, 1.6f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.2f, 0.5f);

		new PartialParticle(Particle.EXPLOSION_LARGE, loc)
			.minimumCount(1)
			.extra(0)
			.spawnAsPlayerActive(player);

		new PPCircle(Particle.EXPLOSION_NORMAL, loc.clone().subtract(0, 0.5, 0), 0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.1, 0, 0)
			.count((int) (10 * radius / 2))
			.extra(radius)
			.spawnAsPlayerActive(player);

		new BukkitRunnable() {
			double mRadius = 0.5;

			@Override
			public void run() {
				new PPCircle(Particle.BLOCK_CRACK, loc.clone().subtract(0, 0.5, 0), mRadius)
					.rotateDelta(true)
					.directionalMode(true)
					.innerRadiusFactor(0.5)
					.data(getBlockData(loc))
					.count(12)
					.delta(10, 1, 0)
					.extra(10)
					.spawnAsPlayerActive(player);
				mRadius++;
				if (mRadius > radius) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		new PPCircle(Particle.WHITE_SMOKE, loc.clone().subtract(0, 0.5, 0), 0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.065, 0, 0)
			.count((int) (25 * radius / 2))
			.extra(radius)
			.spawnAsPlayerActive(player);
	}

	private static BlockData getBlockData(Location loc) {
		Block block = loc.toBlockLocation().subtract(0, 1, 0).getBlock();
		return block.isSolid() ? block.getBlockData() : Material.WHITE_CONCRETE_POWDER.createBlockData();
	}
}
