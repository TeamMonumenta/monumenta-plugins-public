package com.playmonumenta.plugins.overrides;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.GraveUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

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
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
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

		if (!player.getGameMode().equals(GameMode.SPECTATOR) && !command_chest(block)) {
			return false;
		}

		if (player != null && player.getGameMode() != GameMode.SPECTATOR) {
			ChestUtils.chestScalingLuck(plugin, player, block);
		}

		if (player == null) {
			return true;
		} else if (player.getGameMode() != GameMode.SPECTATOR) {
			check_nerf_chest(block, player);
			return true;
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
	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block) {
		if (!command_chest(block)) {
			return false;
		} else if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (!breakable(block)) {
			MessagingUtils.sendActionBarMessage(plugin, player, "This block can not be broken!");
			return false;
		} else if (GraveUtils.isGrave(block)) {
			if (ChestUtils.isEmpty(block)) {
				// Remove the custom name from the grave before breaking it.
				// Players should not be able to obtain graves.
				Chest chest = (Chest)block.getState();
				chest.setCustomName(null);
				chest.update();
				return true;
			} else {
				MessagingUtils.sendActionBarMessage(plugin, player, "You cannot break graves with items inside");
				return false;
			}
		}

		check_nerf_chest(block, player);

		return true;
	}

	@Override
	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		if (!command_chest(block)) {
			return false;
		} else if (!breakable(block)) {
			return false;
		} else if (GraveUtils.isGrave(block)) {
			if (ChestUtils.isEmpty(block)) {
				// Remove the custom name from the grave before breaking it.
				// Players should not be able to obtain graves.
				Chest chest = (Chest)block.getState();
				chest.setCustomName(null);
				chest.update();
				return true;
			} else {
				return false;
			}
		}

		for (Player player : PlayerUtils.playersInRange(block.getLocation(), 30)) {
			check_nerf_chest(block, player);
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

	/*
	 * TODO: Make this more general and add it to server properties
	 *
	 * R2 anti-rush loot table capping mechanism
	 *
	 * If Lime = 0 cap overworld loot at t2. If Lime =1+ cap overworld loot at t3, if any other dungeon is also complete remove the cap.
	 */
	private static void check_nerf_chest(Block block, Player player) {
		if (block == null || player == null) {
			return;
		}

		BlockState state = block.getState();
		if (state instanceof Chest) {
			Chest chest = (Chest)state;

			LootTable table = chest.getLootTable();
			if (table != null) {
				final NamespacedKey origNamedKey = table.getKey();
				final String namespace = origNamedKey.getNamespace();
				final String key = origNamedKey.getKey();
				final String keyPrefix;
				final String keySuffix;
				final int origLevel;
				try {
					/*
					 * Current tables affected by this:
					 * "monumenta:loot2/overworld/level_3_vbeach"
					 * "monumenta:loot2/overworld/level_4_vbeach"
					 * "monumenta:loot2/overworld/level_5_vbeach"
					 * "monumenta:loot2/overworld/level_2_vbeach"
					 * "monumenta:loot2/overworld/level_2_tfrost"
					 * "monumenta:loot2/overworld/level_3_tfrost"
					 * "monumenta:loot2/overworld/level_4_tfrost"
					 * "monumenta:loot2/overworld/level_5_tfrost"
					 * "monumenta:loot2/overworld/level_2_dcanyon"
					 * "monumenta:loot2/overworld/level_3_dcanyon"
					 * "monumenta:loot2/overworld/level_4_dcanyon"
					 * "monumenta:loot2/overworld/level_5_dcanyon"
					*/
					if (key.startsWith("loot2/overworld/level_")) {
						origLevel = CommandUtils.parseIntFromString(null, key.substring("loot2/overworld/level_".length()).substring(0, 1));
						keyPrefix = "loot2/overworld/level_";
					} else if (key.startsWith("r2/world/tiered_chests/level_")) {
						origLevel = CommandUtils.parseIntFromString(null, key.substring("r2/world/tiered_chests/level_".length()).substring(0, 1));
						keyPrefix = "r2/world/tiered_chests/level_";
					} else {
						// Nothing to do - not an adjustable table
						return;
					}
					keySuffix = key.substring(keyPrefix.length() + 1);
				} catch (Exception e) {
					// Nothing to do - can't parse
					return;
				}

				if (ScoreboardUtils.getScoreboardValue(player, "Pink") > 0
				    || ScoreboardUtils.getScoreboardValue(player, "Gray") > 0
				    || ScoreboardUtils.getScoreboardValue(player, "Cyan") > 0) {
					// Nothing to do - player has met the prereqs
					return;
				}

				final int level;
				if (ScoreboardUtils.getScoreboardValue(player, "Lime") == 0) {
					level = Math.min(origLevel, 2);
				} else {
					level = Math.min(origLevel, 3);
				}

				if (level != origLevel) {
					// Level was capped!
					player.sendMessage(ChatColor.RED + "Loot in this chest will improve when you are higher level");

					final NamespacedKey newNamedKey = new NamespacedKey(namespace, keyPrefix + Integer.toString(level) + keySuffix);
					if (player.getGameMode().equals(GameMode.CREATIVE) && player.isOp()) {
						player.sendMessage(ChatColor.GOLD + "Original: " + origNamedKey.toString());
						player.sendMessage(ChatColor.GOLD + "Adjusted: " + newNamedKey.toString());
						player.sendMessage(ChatColor.GOLD + "This info is only shown to creative mode operators");
					}

					LootTable newTable = Bukkit.getLootTable(newNamedKey);
					chest.setLootTable(newTable);
					chest.update();
				}
			}
		}
	}
}
