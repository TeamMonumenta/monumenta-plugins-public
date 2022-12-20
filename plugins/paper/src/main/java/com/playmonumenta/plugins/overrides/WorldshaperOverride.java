package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.Tier;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class WorldshaperOverride {
	private static final String CAN_PLACE_SHULKER_PERM = "monumenta.canplaceshulker";
	private static final String WORLDSHAPER_PERM = "monumenta.worldshaper";
	public static final String COOLDOWN_SOURCE = "CDWorldshaperLoom";
	public static final Material COOLDOWN_ITEM = Material.LOOM;

	private static final Component BRIDGE_MODE = Component.text("Selected Mode: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		.append(Component.text("Bridge", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
	private static final Component WALL_MODE = Component.text("Selected Mode: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		.append(Component.text("Wall", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
	private static final Component FLOOR_MODE = Component.text("Selected Mode: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		.append(Component.text("Floor", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
	private static final Component STAIRS_MODE = Component.text("Selected Mode: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		.append(Component.text("Stairs", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
	private static final String PLAIN_BRIDGE_MODE = MessagingUtils.plainText(BRIDGE_MODE);
	private static final String PLAIN_WALL_MODE = MessagingUtils.plainText(WALL_MODE);
	private static final String PLAIN_FLOOR_MODE = MessagingUtils.plainText(FLOOR_MODE);
	private static final String PLAIN_STAIRS_MODE = MessagingUtils.plainText(STAIRS_MODE);
	private static final String ITEM_NAME = "Worldshaper's Loom";
	// private static final String DELVE_SKIN_NAME = "Doorway from Eternity"; - Insert DELVE SKIN here

	public static boolean placeBlock(Plugin plugin, Player player, ItemStack item) {
		if (!isWorldshaperItem(item)) {
			// Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe - hopefully other shulkers with lore won't get placed
			// Check permission to enable placing shulkers, just so this can be turned off via perms if needed
			return player.hasPermission(CAN_PLACE_SHULKER_PERM);
		}
		if (!player.hasPermission(WORLDSHAPER_PERM)) {
			player.sendMessage(ChatColor.RED + "You don't have permission to use this item. Please ask a moderator to fix this.");
			return false;
		}
		if (plugin.mEffectManager.hasEffect(player, COOLDOWN_SOURCE)) {
			player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(item) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
			return false;
		}

		if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			return false;
		}

		if (ScoreboardUtils.getScoreboardValue(player, "SKTH").orElse(0) <= 0) {
			// Requirements for using Worldshaper's Loom is SKT savage.
			player.sendMessage(ChatColor.RED + "You need to have cleared Silver Knight's Tomb Savage in order to use this item!");
			return false;
		}

		// A list of locations we want to place the block in the world.
		ArrayList<Location> blockPlacePattern = new ArrayList<>();
		int cooldown = 0;

		if (getMode(item) == Mode.BRIDGE) {
			cooldown = 2 * 20;

			Location origin;

			origin = player.getLocation().clone().add(0, -1, 0);

			if (!origin.getBlock().isSolid()) {
				player.sendMessage(Component.text("Bridge Mode can only be used while on a solid block!", TextColor.fromHexString("#D02E28")));
				return false;
			}

			// 337.5 to 360 or 0 to 22.5 is positive Z
			// 22.5 to 67.5 is negative X and positive Z
			// 67.5 to 112.5 is negative X... and so on.
			float playerYaw = player.getLocation().getYaw();

			// For whatever reason it gives a negative value... yaww.
			if (playerYaw < 0) {
				playerYaw += 360;
			}

			// So, 22.5 to 157.5 for negative X. 202.5 to 337.5 for positive X.
			int xIterAdd = (playerYaw >= 22.5 && playerYaw <= 157.5) ? -1 : ((playerYaw >= 202.5 && playerYaw <= 337.5) ? 1 : 0);

			// For Z, 67.5 to 112.5 or 247.5 to 292.5 for 0, 112.5 to 247.5 for -1, default 1.
			int zIterAdd = ((playerYaw >= 67.5 && playerYaw <= 112.5) || (playerYaw >= 247.5 && playerYaw <= 292.5)) ? 0 : ((playerYaw >= 112.5 && playerYaw <= 247.5) ? -1 : 1);

			// If there is a complaint about how readable that code was, the response is "yes".

			origin.add(xIterAdd, 0, zIterAdd);

			for (int i = 0; i < 6; i++) {
				if (ItemUtils.noPassthrough.contains(origin.getBlock().getType())) {
					break;
				}

				blockPlacePattern.add(origin.clone());
				origin.add(xIterAdd, 0, zIterAdd);
			}
		} else if (getMode(item) == Mode.WALL) {
			cooldown = 5 * 20;

			Location origin = player.getTargetBlock(3).getLocation();
			origin.setY(player.getLocation().getY() + 1);

			float playerYaw = player.getLocation().getYaw();

			// For whatever reason it gives a negative value... yaww.
			if (playerYaw < 0) {
				playerYaw += 360;
			}

			// Rotation modes:
			// 0 - Along X-axis
			// 1 - Along a diagonal
			// 2 - Along Z-axis
			// 3 - Along the other diagonal
			int rotation = (int) ((playerYaw + 22.5) / 45) % 4;

			switch (rotation) {
				case 0:
					blockPlacePattern.add(origin.clone().add(-1, -1, 0));
					blockPlacePattern.add(origin.clone().add(0, -1, 0));
					blockPlacePattern.add(origin.clone().add(1, -1, 0));
					blockPlacePattern.add(origin.clone().add(-1, 0, 0));
					blockPlacePattern.add(origin.clone().add(0, 0, 0));
					blockPlacePattern.add(origin.clone().add(1, 0, 0));
					blockPlacePattern.add(origin.clone().add(-1, 1, 0));
					blockPlacePattern.add(origin.clone().add(0, 1, 0));
					blockPlacePattern.add(origin.clone().add(1, 1, 0));
					break;
				case 1:
					blockPlacePattern.add(origin.clone().add(-1, -1, -1));
					blockPlacePattern.add(origin.clone().add(0, -1, 0));
					blockPlacePattern.add(origin.clone().add(1, -1, 1));
					blockPlacePattern.add(origin.clone().add(-1, 0, -1));
					blockPlacePattern.add(origin.clone().add(0, 0, 0));
					blockPlacePattern.add(origin.clone().add(1, 0, 1));
					blockPlacePattern.add(origin.clone().add(-1, 1, -1));
					blockPlacePattern.add(origin.clone().add(0, 1, 0));
					blockPlacePattern.add(origin.clone().add(1, 1, 1));
					break;
				case 2:
					blockPlacePattern.add(origin.clone().add(0, -1, -1));
					blockPlacePattern.add(origin.clone().add(0, -1, 0));
					blockPlacePattern.add(origin.clone().add(0, -1, 1));
					blockPlacePattern.add(origin.clone().add(0, 0, -1));
					blockPlacePattern.add(origin.clone().add(0, 0, 0));
					blockPlacePattern.add(origin.clone().add(0, 0, 1));
					blockPlacePattern.add(origin.clone().add(0, 1, -1));
					blockPlacePattern.add(origin.clone().add(0, 1, 0));
					blockPlacePattern.add(origin.clone().add(0, 1, 1));
					break;
				case 3:
					blockPlacePattern.add(origin.clone().add(-1, -1, 1));
					blockPlacePattern.add(origin.clone().add(0, -1, 0));
					blockPlacePattern.add(origin.clone().add(1, -1, -1));
					blockPlacePattern.add(origin.clone().add(-1, 0, 1));
					blockPlacePattern.add(origin.clone().add(0, 0, 0));
					blockPlacePattern.add(origin.clone().add(1, 0, -1));
					blockPlacePattern.add(origin.clone().add(-1, 1, 1));
					blockPlacePattern.add(origin.clone().add(0, 1, 0));
					blockPlacePattern.add(origin.clone().add(1, 1, -1));
					break;
				default:
					return false;
			}


		} else if (getMode(item) == Mode.FLOOR) {
			cooldown = 5 * 20;

			Location origin = player.getLocation();

			blockPlacePattern.add(origin.clone().add(-1, -1, -1));
			blockPlacePattern.add(origin.clone().add(0, -1, -1));
			blockPlacePattern.add(origin.clone().add(1, -1, -1));
			blockPlacePattern.add(origin.clone().add(-1, -1, 0));
			blockPlacePattern.add(origin.clone().add(0, -1, 0));
			blockPlacePattern.add(origin.clone().add(1, -1, 0));
			blockPlacePattern.add(origin.clone().add(-1, -1, 1));
			blockPlacePattern.add(origin.clone().add(0, -1, 1));
			blockPlacePattern.add(origin.clone().add(1, -1, 1));
		} else if (getMode(item) == Mode.STAIRS) {
			cooldown = 4 * 20;

			Location origin;

			origin = player.getLocation().clone().add(0, -1, 0);

			if (!origin.getBlock().isSolid()) {
				player.sendMessage(Component.text("Stairs Mode can only be used while on a solid block!", TextColor.fromHexString("#D02E28")));
				return false;
			}

			// 337.5 to 360 or 0 to 22.5 is positive Z
			// 22.5 to 67.5 is negative X and positive Z
			// 67.5 to 112.5 is negative X... and so on.
			float playerYaw = player.getLocation().getYaw();

			// For whatever reason it gives a negative value... yaww.
			if (playerYaw < 0) {
				playerYaw += 360;
			}

			// So, 22.5 to 157.5 for negative X. 202.5 to 337.5 for positive X.
			int xIterAdd = (playerYaw >= 22.5 && playerYaw <= 157.5) ? -1 : ((playerYaw >= 202.5 && playerYaw <= 337.5) ? 1 : 0);

			// For Z, 67.5 to 112.5 or 247.5 to 292.5 for 0, 112.5 to 247.5 for -1, default 1.
			int zIterAdd = ((playerYaw >= 67.5 && playerYaw <= 112.5) || (playerYaw >= 247.5 && playerYaw <= 292.5)) ? 0 : ((playerYaw >= 112.5 && playerYaw <= 247.5) ? -1 : 1);

			// If there is a complaint about how readable that code was, the response is "yes".
			origin.add(xIterAdd, 1, zIterAdd);

			for (int i = 0; i < 6; i++) {
				if (ItemUtils.noPassthrough.contains(origin.getBlock().getType())) {
					break;
				}

				blockPlacePattern.add(origin.clone());
				origin.add(xIterAdd, 1, zIterAdd);
			}
		}

		boolean blockPlaced = false;
		for (Location location : blockPlacePattern) {
			if (location.getBlock().isSolid() || ItemUtils.interactableBlocks.contains(location.getBlock().getType()) || !ZoneUtils.playerCanMineBlock(player, location) || ZoneUtils.hasZoneProperty(location, ZoneUtils.ZoneProperty.NO_QUICK_BUILDING)) {
				continue;
			}

			ArrayList<BlockState> blockList = new ArrayList<>(List.of(location.getBlock().getState()));
			StructureGrowEvent event = new StructureGrowEvent(location, TreeType.TREE, true, player, blockList);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled() && !blockList.isEmpty()) {
				BlockData blockData = getBlockAndSubtract(item);
				if (blockData != null) {
					location.getBlock().setBlockData(blockData);
					blockPlaced = true;
					new PartialParticle(Particle.SMOKE_NORMAL, location, 10, 0.15, 0.15, 0.15).spawnAsPlayerActive(player);
					player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 1f, 0.75f);
					CoreProtectIntegration.logPlacement(player, location, blockData.getMaterial(), blockData);

				} else {
					player.sendMessage(ChatColor.RED + "There were not enough valid blocks to place in the shulker!");
					break;
				}
			}
		}

		if (blockPlaced) {
			// Ensure only put on cooldown if blocks are placed.
			plugin.mEffectManager.addEffect(player, COOLDOWN_SOURCE, new ItemCooldown(cooldown, item, COOLDOWN_ITEM, plugin));
		}
		return false;
	}

	// Gets and remove first block it finds in the shulker
	private static BlockData getBlockAndSubtract(ItemStack item) {

		BlockStateMeta shulkerMeta = (BlockStateMeta) item.getItemMeta();
		ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
		Inventory shulkerInventory = shulkerBox.getInventory();

		for (int i = 0; i < 27; i++) {
			ItemStack currentItem = shulkerInventory.getItem(i);
			if (currentItem == null
				|| currentItem.getType().isAir()
				|| ItemUtils.notAllowedTreeReplace.contains(currentItem.getType())
				|| (!currentItem.getType().isOccluding() && !ItemUtils.GOOD_OCCLUDERS.contains(currentItem.getType()))
				|| currentItem.getItemMeta().hasLore()) {
				// Air breaks it, skip over it. Also the banned items break it, skip over those.
				continue;
			}

			ItemMeta meta = currentItem.getItemMeta();
			// No known way to preserve BlockStateMeta - so check that it's either null or simple BlockDataMeta
			if (currentItem.getType().isBlock() && (meta == null || meta instanceof BlockDataMeta)) {
				BlockData blockData;

				// Use block data from meta if the meta has some already
				if (meta instanceof BlockDataMeta blockMeta && blockMeta.hasBlockData()) {
					blockData = blockMeta.getBlockData(currentItem.getType());
				} else {
					blockData = currentItem.getType().createBlockData();
					if (blockData instanceof Leaves) {
						((Leaves) blockData).setPersistent(true);
					}
				}

				// Update the Shulker's inventory
				shulkerInventory.setItem(i, currentItem.subtract());
				shulkerMeta.setBlockState(shulkerBox);
				item.setItemMeta(shulkerMeta);

				return blockData;
			}
		}

		return null;
	}

	public static boolean changeMode(ItemStack item, Player player) {
		if (!isWorldshaperItem(item)) {
			//Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe
			return false;
		}

		boolean reverse = false;
		if (player.isSneaking()) {
			reverse = true;
		}

		if (ScoreboardUtils.getScoreboardValue(player, "SKTH").orElse(0) <= 0) {
			// Requirements for using Worldshaper's Loom is SKT savage.
			player.sendMessage(ChatColor.RED + "You need to have cleared Silver Knight's Tomb Savage in order to use this item!");
			return false;
		}

		NBTItem nbt = new NBTItem(item);
		List<String> lore = ItemStatUtils.getPlainLore(nbt);

		boolean foundLine = false;
		if (InventoryUtils.testForItemWithName(item, ITEM_NAME, true)) {
			for (int i = 0; i < lore.size(); ++i) {
				String line = lore.get(i);
				if (!reverse) {
					if (line.equals(PLAIN_BRIDGE_MODE) && !foundLine) {
						ItemStatUtils.removeLore(item, i);
						ItemStatUtils.addLore(item, i, STAIRS_MODE);
						player.sendMessage(STAIRS_MODE);
						player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
						foundLine = true;
						break;
					} else if (line.equals(PLAIN_STAIRS_MODE) && !foundLine) {
						ItemStatUtils.removeLore(item, i);
						ItemStatUtils.addLore(item, i, WALL_MODE);
						player.sendMessage(WALL_MODE);
						player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, (float) 0.9);
						foundLine = true;
						break;
					} else if (line.equals(PLAIN_WALL_MODE) && !foundLine) {
						ItemStatUtils.removeLore(item, i);
						ItemStatUtils.addLore(item, i, FLOOR_MODE);
						player.sendMessage(FLOOR_MODE);
						player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, (float) 1.1);
						foundLine = true;
						break;
					} else if (line.equals(PLAIN_FLOOR_MODE) && !foundLine) {
						ItemStatUtils.removeLore(item, i);
						ItemStatUtils.addLore(item, i, BRIDGE_MODE);
						player.sendMessage(BRIDGE_MODE);
						player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, (float) 1.2);
						foundLine = true;
						break;
					}
				} else {
					if (line.equals(PLAIN_BRIDGE_MODE) && !foundLine) {
						ItemStatUtils.removeLore(item, i);
						ItemStatUtils.addLore(item, i, FLOOR_MODE);
						player.sendMessage(FLOOR_MODE);
						player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
						foundLine = true;
						break;
					} else if (line.equals(PLAIN_STAIRS_MODE) && !foundLine) {
						ItemStatUtils.removeLore(item, i);
						ItemStatUtils.addLore(item, i, BRIDGE_MODE);
						player.sendMessage(BRIDGE_MODE);
						player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, (float) 0.9);
						foundLine = true;
						break;
					} else if (line.equals(PLAIN_WALL_MODE) && !foundLine) {
						ItemStatUtils.removeLore(item, i);
						ItemStatUtils.addLore(item, i, STAIRS_MODE);
						player.sendMessage(STAIRS_MODE);
						player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, (float) 1.1);
						foundLine = true;
						break;
					} else if (line.equals(PLAIN_FLOOR_MODE) && !foundLine) {
						ItemStatUtils.removeLore(item, i);
						ItemStatUtils.addLore(item, i, WALL_MODE);
						player.sendMessage(WALL_MODE);
						player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, (float) 1.2);
						foundLine = true;
						break;
					}
				}
			}
			if (!foundLine) {
				if (!reverse) {
					ItemStatUtils.addLore(item, lore.size(), STAIRS_MODE);
					player.sendMessage(STAIRS_MODE);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
				} else {
					ItemStatUtils.addLore(item, lore.size(), FLOOR_MODE);
					player.sendMessage(FLOOR_MODE);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
				}
			}
		}
		ItemStatUtils.generateItemStats(item);
		return true;
	}

	public static boolean isWorldshaperItem(ItemStack item) {
		return item != null &&
			       item.getType() != null &&
			       // (InventoryUtils.testForItemWithName(item, ITEM_NAME) || InventoryUtils.testForItemWithName(item, DELVE_SKIN_NAME)) - Commented out for now, re-add this for delve skin
			       InventoryUtils.testForItemWithName(item, ITEM_NAME, true) &&
			       ItemStatUtils.getTier(item).equals(Tier.EPIC) &&
			       ItemUtils.isShulkerBox(item.getType());
	}

	public static Mode getMode(ItemStack item) {
		if (!isWorldshaperItem(item)) {
			return Mode.BRIDGE;
		}

		NBTItem nbt = new NBTItem(item);
		List<String> lore = ItemStatUtils.getPlainLore(nbt);

		for (int i = 0; i < lore.size(); ++i) {
			String line = lore.get(i);
			if (line.equals(PLAIN_BRIDGE_MODE)) {
				return Mode.BRIDGE;
			} else if (line.equals(PLAIN_WALL_MODE)) {
				return Mode.WALL;
			} else if (line.equals(PLAIN_FLOOR_MODE)) {
				return Mode.FLOOR;
			} else if (line.equals(PLAIN_STAIRS_MODE)) {
				return Mode.STAIRS;
			}
		}

		return Mode.BRIDGE;
	}

	enum Mode {
		BRIDGE,
		WALL,
		FLOOR,
		STAIRS
	}
}
