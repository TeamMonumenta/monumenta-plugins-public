package com.playmonumenta.plugins.depths.abilities.frostborn;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;

import net.md_5.bungee.api.ChatColor;

public class IceWall extends DepthsAbility {

	public static final String ABILITY_NAME = "Ice Barrier";
	public static final int[] ICE_TICKS = {8 * 20, 10 * 20, 12 * 20, 14 * 20, 16 * 20};
	public static final int[] COOLDOWN = {20 * 20, 18 * 20, 16 * 20, 14 * 20, 12 * 20};
	public static final int CAST_RANGE = 15;
	public static final int[] MAX_LENGTH = {20, 25, 30, 35, 40};
	public static final int CAST_TIME = 5 * 20;

	public boolean mIsPrimed;
	public Location mPrimedLoc;

	public IceWall(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.PRISMARINE_WALL;
		mTree = DepthsTree.FROSTBORN;
		mInfo.mCooldown = (mRarity == 0) ? 20 * 20 : COOLDOWN[mRarity - 1];
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mLinkedSpell = ClassAbility.ICE_WALL;
		mInfo.mIgnoreCooldown = true;
		mIsPrimed = false;
		mPrimedLoc = null;
	}

	@Override
	public void cast(Action action) {

		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return;
		}

		World world = mPlayer.getWorld();
		Block block = mPlayer.getTargetBlock(CAST_RANGE);
		boolean validLength = true;
		if (mPrimedLoc != null && (mPrimedLoc.distance(block.getLocation()) > MAX_LENGTH[mRarity - 1] || mPrimedLoc.distance(block.getLocation()) < 1)) {
			validLength = false;
		}

		if (block.getType() != Material.AIR && block.getType() != Material.BEDROCK && validLength) {
			DepthsUtils.spawnIceTerrain(block.getLocation(), CAST_TIME);
			world.spawnParticle(Particle.CRIT, block.getLocation(), 15, 0, 0, 0, 0.6f);
			world.spawnParticle(Particle.CRIT_MAGIC, block.getLocation(), 15, 0, 0, 0, 0.6f);
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 1.4f);
			if (!mIsPrimed || mPrimedLoc == null) {
				mIsPrimed = true;
				mPrimedLoc = block.getLocation();

				new BukkitRunnable() {

					@Override
					public void run() {
						if (mIsPrimed && mPrimedLoc != null) {
							mIsPrimed = false;
							mPrimedLoc = null;
							world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, 2.0f, 0.5f);
							//Reset cd
							mPlugin.mTimers.addCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell, 0);
						}
					}

				}.runTaskLater(mPlugin, CAST_TIME);
			} else {
				//Build the wall
				putOnCooldown();
				mIsPrimed = false;

				ArrayList<Block> blocksToIce = new ArrayList<>();

				Vector v = block.getLocation().toVector().subtract(mPrimedLoc.toVector());
				BlockIterator iterator = new BlockIterator(world, mPrimedLoc.toVector(), v, 0.0, (int) block.getLocation().distance(mPrimedLoc));
				while (iterator.hasNext()) {
					Block b = iterator.next();
					blocksToIce.add(b);
					blocksToIce.add(b.getRelative(BlockFace.UP));
					blocksToIce.add(b.getRelative(BlockFace.UP).getRelative(BlockFace.UP));
					blocksToIce.add(b.getRelative(BlockFace.DOWN));
				}

				for (Block b : blocksToIce) {
					DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.UP).getLocation(), ICE_TICKS[mRarity - 1]);
					world.spawnParticle(Particle.CRIT, b.getLocation(), 15, 0, 0, 0, 0.6f);
					world.spawnParticle(Particle.CRIT_MAGIC, b.getLocation(), 15, 0, 0, 0, 0.6f);
				}
				world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, 2.0f, 2.0f);
				mPrimedLoc = null;
			}
		}
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public String getDescription(int rarity) {
		return "Right clicking while sneaking and holding a weapon to place an ice marker up to " + CAST_RANGE + " blocks away. Placing a second marker within " + CAST_TIME / 20 + " seconds and within " + DepthsUtils.getRarityColor(rarity) + MAX_LENGTH[rarity - 1] + ChatColor.WHITE + " blocks of the first marker forms a wall of ice connecting the two points, lasting for " + DepthsUtils.getRarityColor(rarity) + ICE_TICKS[rarity - 1] / 20 + ChatColor.WHITE + " seconds. Cooldown is refunded if no second marker is placed. Cooldown: " + DepthsUtils.getRarityColor(rarity) + COOLDOWN[rarity - 1] / 20 + "s" + ChatColor.WHITE + ".";
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

