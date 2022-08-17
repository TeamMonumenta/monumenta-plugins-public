package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class TrapdoorOverride extends UnbreakableOnBedrockOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		if (player == null) {
			return true;
		}
		Location loc = block.getLocation();
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.BIG_DOOR_DOWN_CW)
				|| ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.BIG_DOOR_DOWN_CCW)) {
			toggleBigDoor(player, block);
			return false;
		}
		return player.getGameMode() == GameMode.CREATIVE || !ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_TRAPDOOR_CLICKS);
	}

	// Toggle a large door between its states; in creative, only previews the change.
	public void toggleBigDoor(Player player, Block block) {
		Location loc = block.getLocation();
		boolean downIsClockwise = ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.BIG_DOOR_DOWN_CW);

		// Get info about the clicked block
		TrapDoor clickedData = (TrapDoor) block.getBlockData();
		Half clickedHalf = clickedData.getHalf();
		BlockFace clickedFacing = clickedData.getFacing();
		boolean clickedAlongX = BlockFace.NORTH.equals(clickedFacing) || BlockFace.SOUTH.equals(clickedFacing);
		boolean rotateClockwise = downIsClockwise ^ Half.TOP.equals(clickedHalf);
		boolean clickedCenterPositive = false;
		if (rotateClockwise) {
			clickedCenterPositive = BlockFace.NORTH.equals(clickedFacing) || BlockFace.EAST.equals(clickedFacing);
		} else {
			clickedCenterPositive = BlockFace.SOUTH.equals(clickedFacing) || BlockFace.WEST.equals(clickedFacing);
		}

		// Get the full door's structure
		int centerX = (int)loc.getX();
		int centerZ = (int)loc.getZ();
		Set<Block> currentDoorBlocks = new HashSet<>();
		Set<Block> visitedDoorBlocks = new HashSet<>();
		Set<Block> possibleDoorBlocks = new HashSet<>();
		possibleDoorBlocks.add(block);
		while (!possibleDoorBlocks.isEmpty()) {
			Iterator<Block> it = possibleDoorBlocks.iterator();
			Block possibleDoorBlock = it.next();
			it.remove();
			visitedDoorBlocks.add(possibleDoorBlock);

			BlockData blockData = possibleDoorBlock.getBlockData();
			if (!(blockData instanceof TrapDoor)) {
				continue;
			}
			TrapDoor trapDoorData = (TrapDoor) blockData;
			if (!clickedHalf.equals(trapDoorData.getHalf())) {
				continue;
			}
			if (!clickedFacing.equals(trapDoorData.getFacing())) {
				continue;
			}

			Location doorBlockLoc = possibleDoorBlock.getLocation();
			if (downIsClockwise) {
				if (!ZoneUtils.hasZoneProperty(doorBlockLoc, ZoneUtils.ZoneProperty.BIG_DOOR_DOWN_CW)) {
					continue;
				}
			} else {
				if (!ZoneUtils.hasZoneProperty(doorBlockLoc, ZoneUtils.ZoneProperty.BIG_DOOR_DOWN_CCW)) {
					continue;
				}
			}

			currentDoorBlocks.add(possibleDoorBlock);

			int x = (int)doorBlockLoc.getX();
			int y = (int)doorBlockLoc.getY();
			int z = (int)doorBlockLoc.getZ();
			Block newPossibleDoorBlock;
			if (clickedAlongX) {
				if (clickedCenterPositive) {
					if (centerX < x) {
						centerX = x;
					}
				} else {
					if (centerX > x) {
						centerX = x;
					}
				}

				newPossibleDoorBlock = loc.getWorld().getBlockAt(x - 1, y, z);
				if (!visitedDoorBlocks.contains(newPossibleDoorBlock)) {
					possibleDoorBlocks.add(newPossibleDoorBlock);
				}

				newPossibleDoorBlock = loc.getWorld().getBlockAt(x + 1, y, z);
				if (!visitedDoorBlocks.contains(newPossibleDoorBlock)) {
					possibleDoorBlocks.add(newPossibleDoorBlock);
				}
			} else {
				if (clickedCenterPositive) {
					if (centerZ < z) {
						centerZ = z;
					}
				} else {
					if (centerZ > z) {
						centerZ = z;
					}
				}

				newPossibleDoorBlock = loc.getWorld().getBlockAt(x, y, z - 1);
				if (!visitedDoorBlocks.contains(newPossibleDoorBlock)) {
					possibleDoorBlocks.add(newPossibleDoorBlock);
				}

				newPossibleDoorBlock = loc.getWorld().getBlockAt(x, y, z + 1);
				if (!visitedDoorBlocks.contains(newPossibleDoorBlock)) {
					possibleDoorBlocks.add(newPossibleDoorBlock);
				}
			}

			newPossibleDoorBlock = loc.getWorld().getBlockAt(x, y - 1, z);
			if (!visitedDoorBlocks.contains(newPossibleDoorBlock)) {
				possibleDoorBlocks.add(newPossibleDoorBlock);
			}

			newPossibleDoorBlock = loc.getWorld().getBlockAt(x, y + 1, z);
			if (!visitedDoorBlocks.contains(newPossibleDoorBlock)) {
				possibleDoorBlocks.add(newPossibleDoorBlock);
			}
		}

		// Check the door is able to toggle state without destroying blocks
		for (Block doorBlock : currentDoorBlocks) {
			Location doorBlockLoc = doorBlock.getLocation();
			int newY = ((int)doorBlockLoc.getY());
			int newX = ((int)doorBlockLoc.getZ() - centerZ) * (rotateClockwise ? -1 : 1) + centerX;
			int newZ = ((int)doorBlockLoc.getX() - centerX) * (rotateClockwise ? 1 : -1) + centerZ;
			Block destBlock = loc.getWorld().getBlockAt(newX, newY, newZ);
			if (player.getGameMode() == GameMode.CREATIVE) {
				markPos(destBlock.getLocation());
			}
			if (newX == centerX && newZ == centerZ) {
				// Ignore blocks that replace themselves at the center
				continue;
			}
			Material destMat = destBlock.getType();
			if (!(destMat.equals(Material.AIR) || destMat.equals(Material.WATER))) {
				player.sendActionBar(Component.text("The door is blocked"));
				return;
			}
		}

		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}

		// Move the door
		for (Block srcBlock : currentDoorBlocks) {
			Location srcBlockLoc = srcBlock.getLocation();
			int newY = ((int)srcBlockLoc.getY());
			int newX = ((int)srcBlockLoc.getZ() - centerZ) * (rotateClockwise ? -1 : 1) + centerX;
			int newZ = ((int)srcBlockLoc.getX() - centerX) * (rotateClockwise ? 1 : -1) + centerZ;
			TrapDoor srcState = (TrapDoor)srcBlock.getBlockData();

			if (Half.TOP.equals(clickedHalf)) {
				srcState.setHalf(Half.BOTTOM);
			} else {
				srcState.setHalf(Half.TOP);
			}
			if (rotateClockwise) {
				switch (clickedFacing) {
				case NORTH:
					srcState.setFacing(BlockFace.WEST);
					break;
				case EAST:
					srcState.setFacing(BlockFace.NORTH);
					break;
				case SOUTH:
					srcState.setFacing(BlockFace.EAST);
					break;
				default:
					srcState.setFacing(BlockFace.SOUTH);
				}
			} else {
				switch (clickedFacing) {
				case NORTH:
					srcState.setFacing(BlockFace.EAST);
					break;
				case EAST:
					srcState.setFacing(BlockFace.SOUTH);
					break;
				case SOUTH:
					srcState.setFacing(BlockFace.WEST);
					break;
				default:
					srcState.setFacing(BlockFace.NORTH);
				}
			}

			// Center just needs updating, which is simpler
			if (newX == centerX && newZ == centerZ) {
				srcBlock.setBlockData(srcState);
				continue;
			}

			Material srcMat = srcBlock.getType();
			boolean srcWaterlogged = srcState.isWaterlogged();
			Block destBlock = loc.getWorld().getBlockAt(newX, newY, newZ);
			Material destMat = destBlock.getType();
			boolean destWaterlogged = destMat.equals(Material.WATER);

			destBlock.setType(srcMat);
			srcState.setWaterlogged(destWaterlogged);
			destBlock.setBlockData(srcState);

			if (srcWaterlogged) {
				srcBlock.setType(Material.WATER);
			} else {
				srcBlock.setType(Material.AIR);
			}
		}
	}

	public void markPos(Location loc) {
		Location tempLoc = new Location(loc.getWorld(), loc.getX() + 0.5, loc.getY() + 0.5, loc.getZ() + 0.5);
		loc.getWorld().spawnParticle(Particle.REDSTONE, tempLoc, 1, new Particle.DustOptions(Color.fromRGB(255, 0, 0), 4));
	}
}
