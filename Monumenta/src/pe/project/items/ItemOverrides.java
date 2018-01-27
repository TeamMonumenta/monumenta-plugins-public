package pe.project.items;

import java.util.EnumSet;
import java.util.HashMap;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;
import pe.project.utils.LocationUtils;
import pe.project.utils.LocationUtils.LocationType;

public class ItemOverrides {
	/*
	 * List of materials that are allowed to be placed by
	 * players in survival even if they have lore text
	 */
	public static EnumSet<Material> ALLOW_LORE_MATS = EnumSet.of(
		Material.CHEST,
		Material.ENDER_CHEST,
		Material.PACKED_ICE,
		Material.WOOL,

		Material.WOOD_HOE,
		Material.STONE_HOE,
		Material.GOLD_HOE,
		Material.IRON_HOE,
		Material.DIAMOND_HOE,

		Material.WOOD_SPADE,
		Material.STONE_SPADE,
		Material.GOLD_SPADE,
		Material.IRON_SPADE,
		Material.DIAMOND_SPADE,

		Material.FLINT_AND_STEEL
	);

	public ItemOverrides() {
		registerOverrides();
	}

	HashMap<Material, OverrideItem> mItems = new HashMap<Material, OverrideItem>();

	public void registerOverrides() {
		mItems.put(Material.MONSTER_EGG, new MonsterEggOverride());
		mItems.put(Material.COMPASS, new CompassOverride());
		mItems.put(Material.FISHING_ROD, new FishingRodOverride());
		mItems.put(Material.ANVIL, new AnvilOverride());
		mItems.put(Material.ENCHANTMENT_TABLE, new EnchantmentTableOverride());
		mItems.put(Material.GOLDEN_APPLE, new GoldenAppleOverride());
		mItems.put(Material.ENDER_CHEST, new EnderChestOverride());
		mItems.put(Material.SOIL, new FarmlandOverride());

		mItems.put(Material.BOAT, new BoatOverride());
		mItems.put(Material.BOAT_ACACIA, new BoatOverride());
		mItems.put(Material.BOAT_BIRCH, new BoatOverride());
		mItems.put(Material.BOAT_DARK_OAK, new BoatOverride());
		mItems.put(Material.BOAT_JUNGLE, new BoatOverride());
		mItems.put(Material.BOAT_SPRUCE, new BoatOverride());

		mItems.put(Material.BUCKET, new BucketOverride());
		mItems.put(Material.WATER_BUCKET, new BucketOverride());
		mItems.put(Material.LAVA_BUCKET, new BucketOverride());

		mItems.put(Material.PACKED_ICE, new PackedIceOverride());
		mItems.put(Material.BED_BLOCK, new BedOverride());
		mItems.put(Material.FIREWORK, new FireworkOverride());

		mItems.put(Material.MOB_SPAWNER, new MobSpawnerOverride());
		mItems.put(Material.FLOWER_POT, new FlowerPotOverride());
		mItems.put(Material.CHEST, new ChestOverride());
		mItems.put(Material.TRAPPED_CHEST, new ChestOverride());
	}

	public boolean rightClickInteraction(Plugin plugin, Player player, Action action, ItemStack item,
	                                     Block block) {
		Material itemType = (item != null) ? item.getType() : Material.AIR;
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		OverrideItem itemOverride = mItems.get(itemType);
		OverrideItem blockOverride = mItems.get(blockType);
		boolean notCancelled = true;

		if (itemOverride != null) {
			notCancelled = itemOverride.rightClickItemInteraction(plugin, player, action, item, block);
		}

		if (notCancelled && blockOverride != null) {
			notCancelled = blockOverride.rightClickBlockInteraction(plugin, player, action, item, block);
		}

		return notCancelled;
	}

	public boolean leftClickInteraction(Plugin plugin, Player player, Action action, ItemStack item,
	                                    Block block) {
		Material itemType = (item != null) ? item.getType() : Material.AIR;
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		OverrideItem itemOverride = mItems.get(itemType);
		OverrideItem blockOverride = mItems.get(blockType);
		boolean notCancelled = true;

		if (itemOverride != null) {
			notCancelled = itemOverride.leftClickItemInteraction(plugin, player, action, item, block);
		}

		if (notCancelled && blockOverride != null) {
			notCancelled = blockOverride.leftClickBlockInteraction(plugin, player, action, item, block);
		}

		return notCancelled;
	}

	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity,
	                                           ItemStack itemInHand) {
		Material itemType = (itemInHand != null) ? itemInHand.getType() : Material.AIR;
		OverrideItem override = mItems.get(itemType);

		return (override != null) ? override.rightClickEntityInteraction(plugin, player, clickedEntity, itemInHand) : true;
	}

	public boolean physicsInteraction(Plugin plugin, Player player, Action action, ItemStack item,
	                                  Block block) {
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		OverrideItem override = mItems.get(blockType);

		return (override != null) ? override.physicsInteraction(plugin, player, block) : true;
	}

	// Returns eventCancelled = true if disallowed, otherwise false
	@SuppressWarnings({"unused"})
	private boolean _safezoneDisallowsBlockChange(Plugin plugin, Player player, Block block) {
		boolean eventCancelled = false;

		// Prevent players from breaking blocks in safezones from outside of them
		if (!eventCancelled && player.getGameMode() != GameMode.CREATIVE) {
			if (LocationUtils.getLocationType(plugin, block.getLocation()) != LocationType.None &&
			    LocationUtils.getLocationType(plugin, player.getLocation()) == LocationType.None) {
				// Allow breaking if the player would be in survival mode at that spot
				Location testLocation = block.getLocation();
				testLocation.setY(10.0);
				Material testMaterial = testLocation.getBlock().getType();
				if (testMaterial != Material.SPONGE && testMaterial != Material.OBSIDIAN) {
					eventCancelled = true;
				}
			}
		}

		return eventCancelled;
	}

	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item,
	                                     BlockPlaceEvent event) {
		boolean eventCancelled = false;

		//  If it's not not a certain lore item go ahead and run the normal override place interaction.
		OverrideItem override = mItems.get(item.getType());
		if (override != null) {
			eventCancelled |= !override.blockPlaceInteraction(plugin, player, item, event);
		}

		//  Don't allow placing of certain items with Lore.
		if (item.hasItemMeta() && item.getItemMeta().hasLore() && player.getGameMode() != GameMode.CREATIVE
		    && !(ALLOW_LORE_MATS.contains(item.getType()))) {
			eventCancelled |= true;
		}

		// NOTE: This is disabled because while functionally nicer, players don't like it
		//eventCancelled |= _safezoneDisallowsBlockChange(plugin, player, event.getBlockPlaced());

		return !eventCancelled;
	}

	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block) {
		boolean eventCancelled = false;

		OverrideItem override = mItems.get(block.getType());
		if (override != null) {
			eventCancelled |= !override.blockBreakInteraction(plugin, player, block);
		}

		// Don't allow blocks to break if they're on the server's list of unbreakable blocks
		if (!eventCancelled && player.getGameMode() != GameMode.CREATIVE &&
		    plugin.mServerProperties.mUnbreakableBlocks.contains(block.getType())) {
			eventCancelled |= true;
		}

		// NOTE: This is disabled because, while functionally nicer, players don't like it
		//eventCancelled |= _safezoneDisallowsBlockChange(plugin, player, block);

		return !eventCancelled;
	}

	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		OverrideItem override = mItems.get(block.getType());
		if (override != null) {
			return override.blockExplodeInteraction(plugin, block);
		}

		return true;
	}
}
