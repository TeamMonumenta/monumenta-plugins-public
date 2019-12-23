package com.playmonumenta.plugins.bosses.spells;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellChangeFloor extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenterLoc;
	private int mRange;
	private int mRadius;
	private Material mMaterial;
	private int mFloorDuration;

	private Random mRandom = new Random();

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
	            Material.AIR,
	            Material.COMMAND_BLOCK,
	            Material.CHAIN_COMMAND_BLOCK,
	            Material.REPEATING_COMMAND_BLOCK,
				Material.BARRIER,
	            Material.BEDROCK,
	            Material.OBSIDIAN,
	            Material.CHEST,
	            Material.SPAWNER
	        );


	public SpellChangeFloor(Plugin plugin, LivingEntity launcher, Location centerLoc, int range, int radius, Material material, int floorduration) {
		mPlugin = plugin;
		mBoss = launcher;
		mCenterLoc = centerLoc;
		mRange = range;
		mRadius = radius;
		mMaterial = material;
		mIgnoredMats.add(material);
		mFloorDuration = floorduration;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mCenterLoc, mRange);
		if (!players.isEmpty()) {
			launch(players.get(mRandom.nextInt(players.size())));
		}
	}

	@Override
	public int duration() {
		return 160; // 8 seconds
	}

	public void launch(Player target) {
		/*
		 * First phase - play sound effect
		 * Second phase - convert top layer of ground under player to mMaterial, particles
		 * Third phase - cleanup converted blocks
		 */
		final int PHASE1_TICKS = 60;
		final int PHASE2_TICKS = mFloorDuration;

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			List<BlockState> restoreBlocks = new LinkedList<BlockState>();
			Random mRandom = new Random();

			@Override
			public void run() {
				if (mTicks > 0 && mTicks < PHASE2_TICKS) {
					// Particles over the changed blocks
					for (BlockState state : restoreBlocks) {
						Location loc = state.getLocation().add(0.5f, 1f, 0.5f);
						loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 1, 0.3, 0.3, 0.3, 0);
					}
				}

				if (mTicks == 0) {
					target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 4f);
					mBoss.getLocation().getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 5f);
					mBoss.getLocation().getWorld().spawnParticle(Particle.LAVA, mBoss.getLocation(), 1, 0.8, 0.8, 0.8, 0);
					mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 3), true);

					// Get a list of blocks that should be changed
					for (int dx = -mRadius; dx < mRadius; dx++) {
						for (int dy = -mRadius; dy < mRadius; dy++) {
							for (int dz = -mRadius; dz < mRadius; dz++) {
								BlockState state = target.getLocation().add(dx, dy, dz).getBlock().getState();
								if (!mIgnoredMats.contains(state.getType()) &&
								    !state.getType().isInteractable() &&
									mRandom.nextInt(16) > 6) {
									restoreBlocks.add(state);
								}
							}
						}
					}
				} else if (mTicks == PHASE1_TICKS) {
					// Set the blocks to the specified material
					for (BlockState state : restoreBlocks) {
						state.getLocation().getBlock().setType(mMaterial);
					}

					/*
					 * This is weird! But on purpose. The change floor spell can only be
					 * cancelled before it changes the floor blocks. Once it changes them,
					 * it must run until it changes the blocks back to what they should be
					 */
					mActiveRunnables.remove(this);
				} else if (mTicks == PHASE2_TICKS) {
					// Restore the block states saved earlier
					for (BlockState state : restoreBlocks) {
						state.update(true);
					}
				} else if (mTicks > PHASE2_TICKS) {
					this.cancel();
					// Don't need to remove runnable here, already done
				}
				mTicks++;
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}
}
