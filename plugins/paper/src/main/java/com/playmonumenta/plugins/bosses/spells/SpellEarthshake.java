package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class SpellEarthshake extends SpellBaseAoE {

	private static final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.AIR,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.CHEST,
		Material.END_PORTAL,
		Material.END_PORTAL_FRAME
	);

	private final int mDamage;
	private final int mRange;
	private final double mKnockUpSpeed;
	private final boolean mFlyingBlocks;

	private @Nullable Location mTargetLocation;
	private boolean mTargeted = false;
	private int mParticleCounter1 = 0;
	private int mParticleCounter2 = 0;

	public SpellEarthshake(Plugin plugin, LivingEntity launcher, int radius, int time, int damage, int cooldown, int range, double knockUpSpeed, boolean lineOfSight, boolean flyingBlocks) {
		super(plugin, launcher, radius * 2, time, cooldown, true, lineOfSight, Sound.BLOCK_ENDER_CHEST_OPEN);
		mDamage = damage;
		mRange = range;
		mKnockUpSpeed = knockUpSpeed;
		mFlyingBlocks = flyingBlocks;
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		List<Player> players = PlayerUtils.playersInRange(mLauncher.getLocation(), mRange, false);
		if (!players.isEmpty() && !mTargeted) {

			// Single target chooses a random player within range
			Collections.shuffle(players);
			for (Player player : players) {
				if (LocationUtils.hasLineOfSight(mLauncher, player) || !mLineOfSight) {
					mTargetLocation = player.getLocation().clone();
					mTargeted = true;
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 5f, 0.75f);
				} else {
					mTargetLocation = null;
				}
			}
		}
		if (mTargetLocation != null) {
			loc = mTargetLocation;
			World world = loc.getWorld();
			if (mParticleCounter1 % 2 == 0) {
				world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 1, ((double) mRadius * 2) / 2, ((double) mRadius * 2) / 2, ((double) mRadius * 2) / 2, 0.05);
			}

			world.spawnParticle(Particle.BLOCK_CRACK, loc, 2, mRadius / 2.0, 0.1, mRadius / 2.0, Bukkit.createBlockData(Material.STONE));

			if (mParticleCounter1 % 20 == 0 && mParticleCounter1 > 0) {
				world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 3f, 0.5f);
				world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 3f, 0.5f);
				for (int i = 0; i < 360; i += 18) {
					world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(FastUtils.cos(Math.toRadians(i)) * mRadius, 0.2, FastUtils.sin(Math.toRadians(i)) * mRadius), 1, 0.1, 0.1, 0.1, 0);
				}
				world.spawnParticle(Particle.BLOCK_CRACK, loc, 80, mRadius / 2.0, 0.1, mRadius / 2.0, Bukkit.createBlockData(Material.DIRT));
				world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 8, mRadius / 2.0, 0.1, mRadius / 2.0, 0);
			}
			mParticleCounter1++;
		}
	}

	@Override
	protected void chargeCircleAction(Location loc) {
		if (mTargetLocation != null) {
			loc = mTargetLocation;
			World world = loc.getWorld();

			if (mParticleCounter2 % 10 == 0) {
				world.spawnParticle(Particle.LAVA, loc, 1, 0.25, 0.25, 0.25, 0.1);
				mLauncher.getWorld().spawnParticle(Particle.DRIP_LAVA, mLauncher.getLocation().clone().add(0, mLauncher.getHeight() / 2, 0), 1, 0.25, 0.45, 0.25, 1);
			}
			mParticleCounter2++;
		}
	}

	@Override
	protected void outburstAction(Location loc) {
		if (mTargetLocation != null) {
			loc = mTargetLocation;
			World world = loc.getWorld();
			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3f, 1.35f);
			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.5f);
			world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 3f, 0.5f);

			world.spawnParticle(Particle.CLOUD, loc, 150, 0, 0, 0, 0.5);
			world.spawnParticle(Particle.LAVA, loc, 35, mRadius / 2.0, 0.1, mRadius / 2.0, 0);
			world.spawnParticle(Particle.BLOCK_CRACK, loc, 200, mRadius / 2.0, 0.1, mRadius / 2.0, Bukkit.createBlockData(Material.DIRT));
			world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 35, mRadius / 2.0, 0.1, mRadius / 2.0, 0.1);

			for (int i = 0; i < 100; i++) {
				world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(-3 + FastUtils.RANDOM.nextDouble() * 6, 0.1, -3 + FastUtils.RANDOM.nextDouble() * 6), 0, 0, 1, 0, 0.2 + FastUtils.RANDOM.nextDouble() * 0.4);
			}
		}
	}

	@Override
	protected void circleOutburstAction(Location loc) {
	}

	@Override
	protected void dealDamageAction(Location loc) {
		if (mTargetLocation != null) {
			loc = mTargetLocation;

			World world = loc.getWorld();
			if (mFlyingBlocks) {
				ArrayList<Block> blocks = new ArrayList<>();

				//Populate the blocks array with nearby blocks- logic here to get the topmost block with air above it
				for (int x = -mRadius; x <= mRadius; x++) {
					for (int z = -mRadius; z <= mRadius; z++) {
						Block lowerBlock = world.getBlockAt(loc.clone().add(x, -2, z));
						for (int y = -1; y <= 2; y++) {
							Block currentBlock = world.getBlockAt(loc.clone().add(x, y, z));
							if (!lowerBlock.getType().isAir() && currentBlock.getType().isAir()) {
								blocks.add(lowerBlock);
								break;
							}
							lowerBlock = currentBlock;
						}
					}
				}

				//Make the blocks go flying
				for (Block b : blocks) {
					if (b == null) {
						continue;
					}

					Material material = b.getType();
					if (!mIgnoredMats.contains(material) && !LocationUtils.containsWater(b) && !(b.getBlockData() instanceof Bed) && FastUtils.RANDOM.nextInt(4) > 1) {
						double x = (FastUtils.RANDOM.nextInt(5) - 2) / 10.0;
						double z = (FastUtils.RANDOM.nextInt(5) - 2) / 10.0;

						FallingBlock block = world.spawnFallingBlock(b.getLocation(), b.getBlockData());
						block.setVelocity(new Vector(x, 1.0, z));
						world.getBlockAt(b.getLocation()).setType(Material.AIR);
					}
				}
			}

			//Knock up player
			for (Player p : PlayerUtils.playersInRange(loc, mRadius * 2, true)) {
				world.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.0f);
				if (p.getLocation().distance(loc) <= mRadius) {
					BossUtils.blockableDamage(mLauncher, p, DamageType.BLAST, mDamage);
					p.setVelocity(p.getVelocity().add(new Vector(0.0, mKnockUpSpeed + 0.5, 0.0)));
				} else {
					BossUtils.blockableDamage(mLauncher, p, DamageType.BLAST, mDamage);
					p.setVelocity(p.getVelocity().add(new Vector(0.0, mKnockUpSpeed, 0.0)));
				}
			}
			//Knock up other mobs because it's fun
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRadius * 2);
			for (LivingEntity mob : mobs) {
				mob.setVelocity(mob.getVelocity().add(new Vector(0.0, mKnockUpSpeed + 1.0, 0.0)));
			}
		}

		mTargeted = false;
		mParticleCounter1 = 0;
		mParticleCounter2 = 0;
	}

}
