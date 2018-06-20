package pe.bossfights.spells;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import pe.bossfights.utils.Utils;

public class SpellChangeFloor implements Spell
{
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mRange;
	private int mRadius;
	private Material mMaterial;

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
	                                                   Material.AIR,
	                                                   Material.COMMAND,
	                                                   Material.COMMAND_CHAIN,
	                                                   Material.COMMAND_REPEATING,
	                                                   Material.BEDROCK,
	                                                   Material.OBSIDIAN,
	                                                   Material.CHEST,
	                                                   Material.MOB_SPAWNER
	                                               );


	public SpellChangeFloor(Plugin plugin, Entity launcher, int range, int radius, Material material)
	{
		mPlugin = plugin;
		mLauncher = launcher;
		mRange = range;
		mRadius = radius;
		mMaterial = material;
	}

	@Override
	public void run()
	{
		for (Player player : Utils.playersInRange(mLauncher.getLocation(), mRange))
			launch(player);
	}

	public void launch(Player target)
	{
		/*
		 * First phase - play sound effect
		 * Second phase - convert top layer of ground under player to mMaterial, particles
		 * Third phase - cleanup converted blocks
		 */
		final int PHASE1_TICKS = 10;
		final int PHASE2_TICKS = 1000;

		new BukkitRunnable()
		{
			int mTicks = 0;
			List<BlockState> restoreBlocks = new LinkedList<BlockState>();

			@Override
			public void run()
			{
				if (mTicks == 0)
				{
					// TODO: Play some sound here, maybe particles around the mob?
				}
				else if (mTicks == PHASE1_TICKS)
				{
					// TODO: Play a sound

					// Get a list of blocks that should be changed
					for (int dx = -mRadius; dx < mRadius; dx++)
					{
						for (int dy = -mRadius; dy < mRadius; dy++)
						{
							for (int dz = -mRadius; dz < mRadius; dz++)
							{
								BlockState state = target.getLocation().add(dx, dy, dz).getBlock().getState();
								if (!mIgnoredMats.contains(state.getType()))
									restoreBlocks.add(state);
							}
						}
					}

					// Set the blocks to the specified material
					for (BlockState state : restoreBlocks)
						state.getLocation().getBlock().setType(mMaterial);
				}
				else if (mTicks > PHASE1_TICKS && mTicks < PHASE2_TICKS)
				{
					// TODO: Play particles over changed blocks
				}
				else if (mTicks == PHASE2_TICKS)
				{
					// Restore the block states saved earlier
					for (BlockState state : restoreBlocks)
						state.update();
				}
				else if ((mTicks < PHASE1_TICKS && mLauncher.isDead()) || mTicks > PHASE2_TICKS)
					this.cancel();
				mTicks++;
			}
		} .runTaskTimer(mPlugin, 0, 1);
	}
}
