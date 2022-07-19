package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;

/*
 * NOTE:
 * This only works if the boss arena is completely flat!
 * change kaul's block break to this later (mBoss, mShrineMarker.getY(), 1, 3, 1, true, true)
 */
public class SpellBossBlockBreak extends Spell {
	private final Entity mBoss;
	private final double mDepth;
	private int mINC = 0;
	private final int mXBreak;
	private final int mYBreak;
	private final int mZBreak;
	private final boolean mBreakArena;
	private final boolean mCreateStair;
	private List<Material> mNoBreak;

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
			Material.AIR,
			Material.COMMAND_BLOCK,
			Material.CHAIN_COMMAND_BLOCK,
			Material.REPEATING_COMMAND_BLOCK,
			Material.BEDROCK,
			Material.BARRIER,
			Material.SPAWNER,
			Material.WATER,
			Material.LAVA,
			Material.END_PORTAL
		);

	/*
	 * for world bosses only
	 * manually input arena height to prevent cheese pillars, use <Spawn Location>.getY()
	 */

	public SpellBossBlockBreak(Entity launcher, double height, int xBreak, int yBreak, int zBreak, boolean canBreakArena, boolean createStair) {
		this(launcher, height, xBreak, yBreak, zBreak, canBreakArena, createStair, Material.AIR);
	}

	public SpellBossBlockBreak(Entity launcher, double height, int xBreak, int yBreak, int zBreak, boolean canBreakArena, boolean createStair, Material... noBreak) {
		mBoss = launcher;
		mDepth = height;
		mXBreak = xBreak;
		mYBreak = yBreak;
		mZBreak = zBreak;
		mBreakArena = canBreakArena;
		mCreateStair = createStair;
		mNoBreak = Arrays.asList(noBreak);
	}

	@Override
	public void run() {
		LivingEntity target = null;
		if (mBoss instanceof Mob m) {
			target = m.getTarget();
		}
		if (!(target instanceof Player)) {
			return;
		}

		Location l = mBoss.getLocation();

		if (ZoneUtils.hasZoneProperty(l, ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED)) {
			return;
		}

		int shift = 0;

		// if boss can create stairs while under ground to tunnel to target above ground
		if (target.getLocation().getY() > mBoss.getLocation().getY() + 0.9 && mCreateStair) {
			shift = 1;
		}
		// cheese pillar break
		if (target.getLocation().getY() > mDepth + 0.5 && target.getLocation().getY() >= mBoss.getLocation().getY() + 0.5) {
			mINC++;
			if (target.getLocation().getY() >= mBoss.getLocation().getY() + 1 && target.getLocation().getY() >= mBoss.getLocation().getY() + 0.5
					&& mBoss.getLocation().distance(target.getLocation()) < 6 && mINC > 5) {
				l = target.getLocation();
				l.setY(target.getLocation().getY() - 2);
				shift = 0;
				mINC = 0;
			}
		} else {
			mINC = 0;
		}

		Location testloc = new Location(l.getWorld(), 0, 0, 0);

		/* Get a list of all blocks that impede the boss's movement */
		List<Block> badBlockList = new ArrayList<Block>();
		for (int x = -mXBreak; x <= mXBreak; x++) {
			testloc.setX(l.getX() + x);
			for (int z = -mZBreak; z <= mZBreak; z++) {
				testloc.setZ(l.getZ() + z);
				for (int y = shift; y <= mYBreak + shift; y++) {
					testloc.setY(l.getY() + y + 0.2);
					if (!mBreakArena && testloc.getY() < mDepth) {
						continue;
					}
					Block block = testloc.getBlock();
					if (!mIgnoredMats.contains(block.getType()) && !mNoBreak.contains(block.getType())) {
						badBlockList.add(block);
					}
				}
			}
		}

		/* Destroy all blocks */
		if (badBlockList.size() > 0) {

			/* Call an event with these exploding blocks to give plugins a chance to modify it */
			EntityExplodeEvent event = new EntityExplodeEvent(mBoss, l, badBlockList, 0f);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}

			/* Remove any remaining blocks, which might have been modified by the event */
			for (Block block : badBlockList) {
				if (block.getState() instanceof Container) {
					block.breakNaturally();
				} else {
					block.setType(Material.AIR);
				}
			}
			l.getWorld().playSound(l, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 0.9f);
			l.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, l, 6, 1, 1, 1, 0.03);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
