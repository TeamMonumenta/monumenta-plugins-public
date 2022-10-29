package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelveLootTableGroup;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.TOVUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;
import org.bukkit.util.Vector;

public class ChestOverride extends BaseOverride {
	// Convenience list of offsets to get adjacent blocks
	private static final List<Vector> ADJACENT_OFFSETS = Arrays.asList(
	                                                         new Vector(1, 0, 0),
	                                                         new Vector(-1, 0, 0),
	                                                         new Vector(0, -1, 0),
	                                                         new Vector(0, 0, 1),
	                                                         new Vector(0, 0, -1)
	                                                     );
	// Convenience list of offsets to get adjacent blocks
	private static final EnumSet<Material> GRAVITY_BLOCKS = EnumSet.of(
			Material.SAND,
			Material.RED_SAND,
			Material.GRAVEL,
			Material.WHITE_CONCRETE_POWDER,
			Material.ORANGE_CONCRETE_POWDER,
			Material.MAGENTA_CONCRETE_POWDER,
			Material.LIGHT_BLUE_CONCRETE_POWDER,
			Material.YELLOW_CONCRETE_POWDER,
			Material.LIME_CONCRETE_POWDER,
			Material.PINK_CONCRETE_POWDER,
			Material.GRAY_CONCRETE_POWDER,
			Material.LIGHT_GRAY_CONCRETE_POWDER,
			Material.CYAN_CONCRETE_POWDER,
			Material.PURPLE_CONCRETE_POWDER,
			Material.BLUE_CONCRETE_POWDER,
			Material.BROWN_CONCRETE_POWDER,
			Material.GREEN_CONCRETE_POWDER,
			Material.RED_CONCRETE_POWDER,
			Material.BLACK_CONCRETE_POWDER,
			Material.WATER,
			Material.LAVA,
			Material.ANVIL
	);

	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		if (player != null && !player.getGameMode().equals(GameMode.SPECTATOR)) {
			// Iterate over adjacent blocks to trigger physics
			for (Vector vec : ADJACENT_OFFSETS) {
				Location tmpLoc = block.getLocation().add(vec);
				Block blk = tmpLoc.getBlock();
				Material type = blk.getType();
				Location underLoc = tmpLoc.clone().subtract(0, 1, 0);
				Material underType = underLoc.getBlock().getType();
				if (GRAVITY_BLOCKS.contains(type) && (underType.equals(Material.AIR) || underType.equals(Material.CAVE_AIR))) {
					if (underType.equals(Material.CAVE_AIR)) {
						underLoc.getBlock().setType(Material.AIR);
						underLoc.getBlock().setType(Material.CAVE_AIR);
					} else {
						underLoc.getBlock().setType(Material.CAVE_AIR);
						underLoc.getBlock().setType(Material.AIR);
					}
				}
			}
		}

		if (player != null && !player.getGameMode().equals(GameMode.SPECTATOR) && !event.isCancelled() && !commandChest(block)) {
			return false;
		}

		if (player == null) {
			return true;
		} else if (player.getGameMode() != GameMode.SPECTATOR) {
			DelveLootTableGroup.setDelveLootTable(player, block);
			if (TOVUtils.isUnopenedTovLootCache(block)) {
				// This returns directly - TOV caches do not get loot scaling,
				// and it's also important for the loot table to generate the vanilla way for the functions to trigger.
				return TOVUtils.setTOVLootTable(plugin, player, block);
			} else if (TOVUtils.isOpenedTovLootCache(block)) {
				// same as above, except nothing to do here as the loot table is already set (and possibly already rolled as well)
				return true;
			}
			// This will be allowed, should just generate the loot directly before the player actually finishes opening
			ChestUtils.generateContainerLootWithScaling(player, block);
			return true;
		}

		/* Only spectating players get to here */
		BlockState state = block.getState();
		if (state instanceof Chest chest) {
			LootTable table = chest.getLootTable();
			if (table != null) {
				player.sendMessage(ChatColor.GOLD + "This chest has loot table: " + table.getKey());
				return false;
			}
		}

		return true;
	}

	/* Chests placed on barriers can not be broken */
	@Override
	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block, BlockBreakEvent event) {
		if (!event.isCancelled() && !commandChest(block)) {
			return false;
		} else if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (!breakable(block)) {
			MessagingUtils.sendActionBarMessage(player, "This block can not be broken!");
			return false;
		}

		DelveLootTableGroup.setDelveLootTable(player, block);
		ChestUtils.generateContainerLootWithScaling(player, block);
		return TOVUtils.canBreak(plugin, player, block, event);
	}

	@Override
	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		if (!commandChest(block)) {
			return false;
		} else if (!breakable(block)) {
			return false;
		}

		List<Player> players = PlayerUtils.playersInRange(block.getLocation(), 30, true, true);

		// Runs replacement with any nearby player
		if (!players.isEmpty()) {
			Player player = players.get(0);
			DelveLootTableGroup.setDelveLootTable(player, block);
			ChestUtils.generateContainerLootWithScaling(player, block);
		}

		return true;
	}

	protected static boolean breakable(Block block) {
		if (ServerProperties.getIsTownWorld()) {
			return true;
		}
		Block blockUnder = block.getRelative(BlockFace.DOWN);
		Material type = blockUnder.getType();
		if (type == Material.BARRIER) {
			return false;
		} else if (type == Material.BEDROCK && block.getState() instanceof Chest chest && chest.hasLootTable()) {
			return false;
		}
		return true;
	}

	// If this returns false, the caller should also return false and stop processing the chest
	private boolean commandChest(Block block) {
		if (block.getState() instanceof Chest chest) {
			String name = chest.getCustomName();
			if (name != null && (name.equalsIgnoreCase("trap") || name.equalsIgnoreCase("function"))) {
				// This is a function chest - run it!
				// Run the first command block found in the 10 blocks under this block
				Location loc = block.getLocation();
				for (int y = 0; y < 10; y++) {
					loc = loc.subtract(0, 1, 0);
					Block testBlock = loc.getBlock();

					if (testBlock.getType().equals(Material.COMMAND_BLOCK)
						    && testBlock.getState() instanceof CommandBlock commandBlock) {

						// Run the command positioned at the chest block
						NmsUtils.getVersionAdapter().executeCommandAsBlock(block, commandBlock.getCommand());
						break;
					}
				}

				if (name.equalsIgnoreCase("trap")) {
					// This was a trapped chest - clear its name and still let the player open it
					chest.setCustomName(null);
					chest.update();
					return true;
				} else {
					// This was a function chest - don't let the player open it
					return false;
				}
			}
		}
		return true;
	}
}
