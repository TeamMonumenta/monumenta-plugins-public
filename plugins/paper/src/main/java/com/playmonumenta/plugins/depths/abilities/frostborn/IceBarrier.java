package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class IceBarrier extends DepthsAbility {

	public static final String ABILITY_NAME = "Ice Barrier";
	public static final int[] ICE_TICKS = {8 * 20, 10 * 20, 12 * 20, 14 * 20, 16 * 20, 20 * 20};
	public static final int[] COOLDOWN = {20 * 20, 18 * 20, 16 * 20, 14 * 20, 12 * 20, 8 * 20};
	public static final int CAST_RANGE = 15;
	public static final int[] MAX_LENGTH = {20, 25, 30, 35, 40, 50};
	public static final int CAST_TIME = 5 * 20;

	public static final DepthsAbilityInfo<IceBarrier> INFO =
		new DepthsAbilityInfo<>(IceBarrier.class, ABILITY_NAME, IceBarrier::new, DepthsTree.FROSTBORN, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.ICE_BARRIER)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IceBarrier::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.PRISMARINE_WALL))
			.descriptions(IceBarrier::getDescription);

	public boolean mIsPrimed;
	public @Nullable Location mPrimedLoc;

	public IceBarrier(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mIsPrimed = false;
		mPrimedLoc = null;
	}

	public void cast() {

		if (isOnCooldown()) {
			return;
		}

		World world = mPlayer.getWorld();
		Block block = mPlayer.getTargetBlock(CAST_RANGE);
		if (block == null) {
			return;
		}

		boolean validLength = mPrimedLoc == null || (!(mPrimedLoc.distance(block.getLocation()) > MAX_LENGTH[mRarity - 1]) && !(mPrimedLoc.distance(block.getLocation()) < 1));

		if (block.getType() != Material.AIR && block.getType() != Material.BEDROCK && validLength) {
			DepthsUtils.spawnIceTerrain(block.getLocation(), CAST_TIME, mPlayer);
			new PartialParticle(Particle.CRIT, block.getLocation(), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT_MAGIC, block.getLocation(), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1, 1.4f);
			if (!mIsPrimed || mPrimedLoc == null) {
				mIsPrimed = true;
				mPrimedLoc = block.getLocation();
				ClientModHandler.updateAbility(mPlayer, this);

				new BukkitRunnable() {

					@Override
					public void run() {
						if (mIsPrimed && mPrimedLoc != null) {
							mIsPrimed = false;
							mPrimedLoc = null;
							world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.BLOCKS, 2.0f, 0.5f);
							//Reset cd
							mPlugin.mTimers.addCooldown(mPlayer, ClassAbility.ICE_BARRIER, 0);
						}
					}

				}.runTaskLater(mPlugin, CAST_TIME);
			} else {
				//Build the wall
				mIsPrimed = false;
				putOnCooldown();

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
					DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.UP).getLocation(), ICE_TICKS[mRarity - 1], mPlayer, Boolean.TRUE);
					new PartialParticle(Particle.CRIT, b.getLocation(), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.CRIT_MAGIC, b.getLocation(), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
				}
				world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.BLOCKS, 2.0f, 2.0f);
				mPrimedLoc = null;
			}
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Right clicking while sneaking and holding a weapon to place an ice marker up to " + CAST_RANGE + " blocks away. Placing a second marker within " + CAST_TIME / 20 + " seconds and within ")
			.append(Component.text(MAX_LENGTH[rarity - 1], color))
			.append(Component.text(" blocks of the first marker forms a wall of ice connecting the two points, lasting for "))
			.append(Component.text(ICE_TICKS[rarity - 1] / 20, color))
			.append(Component.text(" seconds. Mobs that break the barrier are stunned for 2s. Cooldown is refunded if no second marker is placed. Cooldown: "))
			.append(Component.text(COOLDOWN[rarity - 1] / 20 + "s", color))
			.append(Component.text("."));
	}


	@Override
	public @Nullable String getMode() {
		return mIsPrimed ? "primed" : null;
	}
}

