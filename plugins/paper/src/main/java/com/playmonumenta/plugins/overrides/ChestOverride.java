package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.TOVUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

		if (player != null && !player.getGameMode().equals(GameMode.SPECTATOR) && !event.isCancelled() && !command_chest(block)) {
			return false;
		}

		if (player == null) {
			return true;
		} else if (player.getGameMode() != GameMode.SPECTATOR) {
			DelvesUtils.setDelveLootTable(player, block);
			boolean retval = TOVUtils.setTOVLootTable(plugin, player, block);
			if (retval == true) {
				// This will be allowed, should just generate the loot directly before the player actually finishes opening
				ChestUtils.generateContainerLootWithScaling(player, block);
			}
		}

		/* Only spectating players get to here */
		BlockState state = block.getState();
		if (state instanceof Chest) {
			Chest chest = (Chest)state;
			LootTable table = chest.getLootTable();
			if (table != null) {
				player.sendMessage(ChatColor.GOLD + "This chest has loot table: " + table.getKey().toString());
				return false;
			}
		}

		return true;
	}

	/* Chests placed on barriers can not be broken */
	@Override
	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block, BlockBreakEvent event) {
		if (!event.isCancelled() && !command_chest(block)) {
			return false;
		} else if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (!breakable(block)) {
			MessagingUtils.sendActionBarMessage(player, "This block can not be broken!");
			return false;
		}

		DelvesUtils.setDelveLootTable(player, block);
		ChestUtils.generateContainerLootWithScaling(player, block);
		return TOVUtils.canBreak(plugin, player, block, event);
	}

	@Override
	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		if (!command_chest(block)) {
			return false;
		} else if (!breakable(block)) {
			return false;
		}

		List<Player> players = PlayerUtils.playersInRange(block.getLocation(), 30, true);

		//Runs replacement with closest player
		if (!players.isEmpty()) {
			Player player = players.get(0);
			DelvesUtils.setDelveLootTable(player, block);
			ChestUtils.generateContainerLootWithScaling(player, block);
		}

		return true;
	}

	protected static boolean breakable(Block block) {
		Block blockUnder = block.getLocation().add(0, -1, 0).getBlock();
		if (blockUnder != null && blockUnder.getType() == Material.BARRIER) {
			return false;
		}
		return true;
	}

	// If this returns false, the caller should also return false and stop processing the chest
	private boolean command_chest(Block block) {
		BlockState state = block.getState();
		if (state != null && state instanceof Chest) {
			Chest chest = (Chest)state;
			String name = chest.getCustomName();
			if (name != null && (name.toLowerCase().equals("trap") || name.toLowerCase().equals("function"))) {
				// This is a function chest - run it!
				// Run the first command block found in the 10 blocks under this block
				Location loc = block.getLocation();
				for (int y = 0; y < 10; y++) {
					loc = loc.subtract(0, 1, 0);
					Block testBlock = loc.getBlock();
					BlockState testState = testBlock.getState();

					if (testBlock.getType().equals(Material.COMMAND_BLOCK)
					    && testState instanceof CommandBlock) {

						// Run the command positioned at the chest block
						String command = "execute positioned " +
						                 Integer.toString((int)chest.getLocation().getX()) + " " +
						                 Integer.toString((int)chest.getLocation().getY()) + " " +
						                 Integer.toString((int)chest.getLocation().getZ()) + " run " +
						                 ((CommandBlock)testState).getCommand();
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
						break;
					}
				}

				if (name.toLowerCase().equals("trap")) {
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
