package com.playmonumenta.plugins.depths.abilities.frostborn;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;

import net.md_5.bungee.api.ChatColor;

public class IceWall extends DepthsAbility {

	public static final String ABILITY_NAME = "Ice Barrier";
	public static final int[] ICE_TICKS = {8 * 20, 10 * 20, 12 * 20, 14 * 20, 16 * 20};
	public static final int[] COOLDOWN = {20 * 20, 18 * 20, 16 * 20, 14 * 20, 12 * 20};
	public static final int SLOW_DURATION = 3 * 20; // time for lingering after wall goes away
	public static final double SLOW_AMPLIFIER = 0.15;

	public static String tree;

	public IceWall(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.PRISMARINE_WALL;
		mTree = DepthsTree.FROSTBORN;
		mInfo.mCooldown = (mRarity == 0) ? 20 * 20 : COOLDOWN[mRarity - 1];
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mLinkedSpell = ClassAbility.ICE_WALL;
	}

	@Override
	public void cast(Action action) {

		World world = mPlayer.getWorld();
		Block block = mPlayer.getTargetBlock(5);
		BlockFace direction = mPlayer.getFacing();
		ArrayList<Block> blocksToIce = new ArrayList<>();
		blocksToIce.add(block);
		blocksToIce.add(block.getRelative(BlockFace.UP));
		blocksToIce.add(block.getRelative(BlockFace.DOWN));
		Location center = block.getLocation().add(0.5, 0, 0.5);
		BoundingBox box = null;

		if (direction == BlockFace.NORTH || direction == BlockFace.SOUTH) {
			blocksToIce.add(block.getRelative(BlockFace.EAST));
			blocksToIce.add(block.getRelative(BlockFace.WEST));
			blocksToIce.add(block.getRelative(BlockFace.EAST).getRelative(BlockFace.UP));
			blocksToIce.add(block.getRelative(BlockFace.EAST).getRelative(BlockFace.DOWN));
			blocksToIce.add(block.getRelative(BlockFace.WEST).getRelative(BlockFace.UP));
			blocksToIce.add(block.getRelative(BlockFace.WEST).getRelative(BlockFace.DOWN));
			blocksToIce.add(block.getRelative(BlockFace.EAST).getRelative(BlockFace.EAST).getRelative(BlockFace.UP));
			blocksToIce.add(block.getRelative(BlockFace.EAST).getRelative(BlockFace.EAST).getRelative(BlockFace.DOWN));
			blocksToIce.add(block.getRelative(BlockFace.WEST).getRelative(BlockFace.WEST).getRelative(BlockFace.UP));
			blocksToIce.add(block.getRelative(BlockFace.WEST).getRelative(BlockFace.WEST).getRelative(BlockFace.DOWN));
			blocksToIce.add(block.getRelative(BlockFace.EAST).getRelative(BlockFace.EAST));
			blocksToIce.add(block.getRelative(BlockFace.WEST).getRelative(BlockFace.WEST));

			box = BoundingBox.of(center, 3.5, 2.5, 1.5);
		}

		if (direction == BlockFace.EAST || direction == BlockFace.WEST) {
			blocksToIce.add(block.getRelative(BlockFace.NORTH));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.UP));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.DOWN));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.UP));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.DOWN));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH).getRelative(BlockFace.UP));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH).getRelative(BlockFace.DOWN));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.SOUTH).getRelative(BlockFace.UP));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.SOUTH).getRelative(BlockFace.DOWN));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.SOUTH));

			box = BoundingBox.of(center, 1.5, 2.5, 3.5);
		}

		for (Block b : blocksToIce) {
			DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.UP).getLocation(), ICE_TICKS[mRarity - 1]);
			world.spawnParticle(Particle.CRIT, b.getLocation(), 15, 0, 0, 0, 0.6f);
			world.spawnParticle(Particle.CRIT_MAGIC, b.getLocation(), 15, 0, 0, 0, 0.6f);
		}
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 1.4f);

		BoundingBox slowBox = box;
		if (slowBox != null) {
			new BukkitRunnable() {
				int mApplyTicks = 0;
				@Override
				public void run() {
					if (mApplyTicks >= ICE_TICKS[mRarity - 1]) {
						this.cancel();
					}
					for (Entity mob : EntityUtils.getNearbyMobs(center, 5)) {
						if (slowBox.overlaps(mob.getBoundingBox()) && mob instanceof LivingEntity) {
							EntityUtils.applySlow(mPlugin, SLOW_DURATION, SLOW_AMPLIFIER, (LivingEntity)mob);
							new BukkitRunnable() {
								int mEffectTicks = 0;
								@Override
								public void run() {
									if (mEffectTicks >= SLOW_DURATION) {
										this.cancel();
									}
									mob.getWorld().spawnParticle(Particle.SNOW_SHOVEL, mob.getLocation(), 5, .5, .2, .5, 0.65);
									mEffectTicks += 5;
								}
							}.runTaskTimer(mPlugin, 0, 5);
						}
					}
					mApplyTicks += 5;
				}
			}.runTaskTimer(mPlugin, 0, 5);
		}

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Right clicking while sneaking and holding a weapon to form a 5 block wide and 3 block tall wall of ice where you are looking within 5 blocks, lasting for " + DepthsUtils.getRarityColor(rarity) + ICE_TICKS[rarity - 1] / 20 + ChatColor.WHITE + " seconds. Mobs near the wall are slowed by " + DepthsUtils.roundPercent(SLOW_AMPLIFIER) + "% for " + SLOW_DURATION / 20 + " seconds. Cooldown: " + DepthsUtils.getRarityColor(rarity) + COOLDOWN[rarity - 1] / 20 + "s" + ChatColor.WHITE + ".";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FROSTBORN;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_RIGHT_CLICK;
	}

}

