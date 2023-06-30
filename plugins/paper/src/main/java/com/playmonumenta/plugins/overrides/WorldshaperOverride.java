package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.Tier;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class WorldshaperOverride {
	private static final String CAN_PLACE_SHULKER_PERM = "monumenta.canplaceshulker";
	private static final String WORLDSHAPER_PERM = "monumenta.worldshaper";
	public static final String COOLDOWN_SOURCE = "CDWorldshaperLoom";
	public static final Material COOLDOWN_ITEM = Material.LOOM;

	private static final String ITEM_NAME = "Worldshaper's Loom";

	public static boolean placeBlock(Plugin plugin, Player player, ItemStack item) {
		if (!isWorldshaperItem(item)) {
			// Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe - hopefully other shulkers with lore won't get placed
			// Check permission to enable placing shulkers, just so this can be turned off via perms if needed
			return player.hasPermission(CAN_PLACE_SHULKER_PERM);
		}
		if (!player.hasPermission(WORLDSHAPER_PERM)) {
			player.sendMessage(Component.text("You don't have permission to use this item. Please ask a moderator to fix this.", NamedTextColor.RED));
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
			player.sendMessage(Component.text("You need to have cleared Silver Knight's Tomb Savage in order to use this item!", NamedTextColor.RED));
			return false;
		}

		// A list of locations we want to place the block in the world.
		ArrayList<Location> blockPlacePattern = new ArrayList<>();
		int cooldown = 0;

		Mode mode = getMode(item);
		Predicate<Material> occludingException = null;
		if (mode == Mode.BRIDGE) {
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
		} else if (mode == Mode.WALL) {
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

			if (rotation == 0 || rotation == 2) {
				occludingException = Tag.WALLS::isTagged;
			}

			switch (rotation) {
				case 0:
					for (int x = -1; x <= 1; x++) {
						for (int y = -1; y <= 1; y++) {
							blockPlacePattern.add(origin.clone().add(x, y, 0));
						}
					}
					break;
				case 1:
					for (int y = -1; y <= 1; y++) {
						for (int xz = -1; xz <= 1; xz++) {
							blockPlacePattern.add(origin.clone().add(xz, y, xz));
						}
					}
					break;
				case 2:
					for (int y = -1; y <= 1; y++) {
						for (int z = -1; z <= 1; z++) {
							blockPlacePattern.add(origin.clone().add(0, y, z));
						}
					}
					break;
				case 3:
					for (int y = -1; y <= 1; y++) {
						for (int xz = -1; xz <= 1; xz++) {
							blockPlacePattern.add(origin.clone().add(xz, y, -xz));
						}
					}
					break;
				default:
					return false;
			}


		} else if (mode == Mode.FLOOR) {
			cooldown = 5 * 20;

			Location origin = player.getLocation();

			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					blockPlacePattern.add(origin.clone().add(x, -1, z));
				}
			}
		} else if (mode == Mode.STAIRS) {
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

			if (xIterAdd == 0 || zIterAdd == 0) {
				occludingException = Tag.STAIRS::isTagged;
			}

			for (int i = 0; i < 6; i++) {
				if (ItemUtils.noPassthrough.contains(origin.getBlock().getType())) {
					break;
				}

				blockPlacePattern.add(origin.clone());
				origin.add(xIterAdd, 1, zIterAdd);
			}
		}

		int blocksPlaced = 0;
		World world = player.getWorld();
		for (Location location : blockPlacePattern) {
			if (location.getBlock().isSolid() || ItemUtils.interactableBlocks.contains(location.getBlock().getType()) || !ZoneUtils.playerCanMineBlock(player, location) || ZoneUtils.hasZoneProperty(location, ZoneUtils.ZoneProperty.NO_QUICK_BUILDING)) {
				continue;
			}

			if (LocationUtils.blocksIntersectEntity(world, List.of(location),
				hitbox -> hitbox.getHitEntities(e -> e instanceof LivingEntity && !e.isInvulnerable() && e != player))) {
				continue;
			}

			ArrayList<BlockState> blockList = new ArrayList<>(List.of(location.getBlock().getState()));
			StructureGrowEvent event = new StructureGrowEvent(location, TreeType.TREE, true, player, blockList);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled() && !blockList.isEmpty()) {
				BlockData blockData = getBlockAndSubtract(item, occludingException);
				if (blockData != null) {
					if (mode == Mode.STAIRS && blockData instanceof Stairs stairs) {
						stairs.setFacing(BlockUtils.getCardinalBlockFace(player));
					}
					if (mode == Mode.WALL && blockData instanceof Wall wall) {
						BlockFace facing = BlockUtils.getCardinalBlockFace(player);
						for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
							// Since the walls are placed from the bottom up, all but the top row will be updated to TALL
							wall.setHeight(face, face == facing || face == facing.getOppositeFace() ? Wall.Height.NONE : Wall.Height.LOW);
						}
						wall.setUp(false);
					}
					location.getBlock().setBlockData(blockData);
					blocksPlaced++;
					new PartialParticle(Particle.SMOKE_NORMAL, location, 10, 0.15, 0.15, 0.15).spawnAsPlayerActive(player);
					world.playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1f, 0.75f);
					CoreProtectIntegration.logPlacement(player, location, blockData.getMaterial(), blockData);
				} else {
					if (blocksPlaced == 0) {
						player.sendMessage(Component.text("There are no valid blocks to place in the shulker!", NamedTextColor.RED));
					} else {
						player.sendMessage(Component.text("There were not enough valid blocks to place in the shulker!", NamedTextColor.RED));
					}
					break;
				}
			}
		}

		if (blocksPlaced > 0) {
			// Ensure only put on cooldown if blocks are placed.
			plugin.mEffectManager.addEffect(player, COOLDOWN_SOURCE, new ItemCooldown(cooldown, item, COOLDOWN_ITEM, plugin));

			// update stat track
			StatTrackManager.getInstance().incrementStatImmediately(item, player, ItemStatUtils.InfusionType.STAT_TRACK_BLOCKS, blocksPlaced);
		}
		return false;
	}

	// Gets and remove first block it finds in the shulker
	private static @Nullable BlockData getBlockAndSubtract(ItemStack item, @Nullable Predicate<Material> occludingException) {

		BlockStateMeta shulkerMeta = (BlockStateMeta) item.getItemMeta();
		ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
		Inventory shulkerInventory = shulkerBox.getInventory();

		for (int i = 0; i < 27; i++) {
			ItemStack currentItem = shulkerInventory.getItem(i);
			if (currentItem == null) {
				continue;
			}
			Material type = currentItem.getType();
			if (type.isAir() || ItemUtils.notAllowedTreeReplace.contains(type)
				|| (!type.isOccluding() && !ItemUtils.GOOD_OCCLUDERS.contains(type) && !(occludingException != null && occludingException.test(type)))
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
					if (blockData instanceof Leaves leaves) {
						leaves.setPersistent(true);
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

		boolean reverse = player.isSneaking();

		if (ScoreboardUtils.getScoreboardValue(player, "SKTH").orElse(0) <= 0) {
			// Requirements for using Worldshaper's Loom is SKT savage.
			player.sendMessage(Component.text("You need to have cleared Silver Knight's Tomb Savage in order to use this item!", NamedTextColor.RED));
			return false;
		}

		NBTItem nbt = new NBTItem(item);
		List<String> lore = ItemStatUtils.getPlainLore(nbt);

		boolean foundLine = false;
		loreLoop: for (int i = 0; i < lore.size(); ++i) {
			String line = lore.get(i);
			for (Mode mode : Mode.values()) {
				if (line.equals(mode.mPlainMessage)) {
					Mode newMode = Mode.values()[(mode.ordinal() + (reverse ? -1 : 1) + Mode.values().length) % Mode.values().length];
					ItemStatUtils.removeLore(item, i);
					ItemStatUtils.addLore(item, i, newMode.mMessage);
					player.sendMessage(newMode.mMessage);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, newMode.mPitch);
					foundLine = true;
					break loreLoop;
				}
			}
		}
		if (!foundLine) {
			Mode newMode = reverse ? Mode.FLOOR : Mode.STAIRS;
			ItemStatUtils.addLore(item, lore.size(), newMode.mMessage);
			player.sendMessage(newMode.mMessage);
			player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, newMode.mPitch);
		}
		ItemStatUtils.generateItemStats(item);
		return true;
	}

	public static boolean isWorldshaperItem(ItemStack item) {
		return item != null &&
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

		for (String line : lore) {
			for (Mode mode : Mode.values()) {
				if (line.equals(mode.mPlainMessage)) {
					return mode;
				}
			}
		}

		return Mode.BRIDGE;
	}

	enum Mode {
		BRIDGE("Bridge", NamedTextColor.GREEN, 1.0f),
		STAIRS("Stairs", NamedTextColor.LIGHT_PURPLE, 0.9f),
		WALL("Wall", NamedTextColor.AQUA, 1.1f),
		FLOOR("Floor", NamedTextColor.GOLD, 1.2f);

		private final Component mMessage;
		private final String mPlainMessage;
		private final float mPitch;

		Mode(String name, TextColor color, float pitch) {
			mMessage = Component.text("Selected Mode: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
				.append(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
			mPlainMessage = MessagingUtils.plainText(mMessage);
			mPitch = pitch;
		}
	}
}
