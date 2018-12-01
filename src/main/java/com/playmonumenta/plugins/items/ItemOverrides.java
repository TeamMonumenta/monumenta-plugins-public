package com.playmonumenta.plugins.items;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.LocationUtils.LocationType;

import java.util.EnumSet;
import java.util.HashMap;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;

public class ItemOverrides {
	/*
	 * List of materials that are allowed to be placed by
	 * players in survival even if they have lore text
	 */
	public static EnumSet<Material> ALLOW_LORE_MATS = EnumSet.of(
			Material.ANVIL,
			Material.CHEST,
			Material.FLINT_AND_STEEL,
			Material.ENDER_CHEST,
			Material.PACKED_ICE,
			Material.MAGMA_BLOCK,

			Material.WHITE_BANNER,
			Material.ORANGE_BANNER,
			Material.MAGENTA_BANNER,
			Material.LIGHT_BLUE_BANNER,
			Material.YELLOW_BANNER,
			Material.LIME_BANNER,
			Material.PINK_BANNER,
			Material.GRAY_BANNER,
			Material.LIGHT_GRAY_BANNER,
			Material.CYAN_BANNER,
			Material.PURPLE_BANNER,
			Material.BLUE_BANNER,
			Material.BROWN_BANNER,
			Material.GREEN_BANNER,
			Material.RED_BANNER,
			Material.BLACK_BANNER,

			Material.WHITE_WOOL,
			Material.ORANGE_WOOL,
			Material.MAGENTA_WOOL,
			Material.LIGHT_BLUE_WOOL,
			Material.YELLOW_WOOL,
			Material.LIME_WOOL,
			Material.PINK_WOOL,
			Material.GRAY_WOOL,
			Material.LIGHT_GRAY_WOOL,
			Material.CYAN_WOOL,
			Material.PURPLE_WOOL,
			Material.BLUE_WOOL,
			Material.BROWN_WOOL,
			Material.GREEN_WOOL,
			Material.RED_WOOL,
			Material.BLACK_WOOL,

			Material.WOODEN_HOE,
			Material.STONE_HOE,
			Material.GOLDEN_HOE,
			Material.IRON_HOE,
			Material.DIAMOND_HOE,

			Material.WOODEN_SHOVEL,
			Material.STONE_SHOVEL,
			Material.GOLDEN_SHOVEL,
			Material.IRON_SHOVEL,
			Material.DIAMOND_SHOVEL
		);

	public ItemOverrides() {
		registerOverrides();
	}

	HashMap<Material, OverrideItem> mItems = new HashMap<Material, OverrideItem>();

	public void registerOverrides() {
		OverrideItem monsterEggOverride = new MonsterEggOverride();
		mItems.put(Material.BAT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.BLAZE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.CAVE_SPIDER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.CHICKEN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.COD_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.COW_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.CREEPER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.DOLPHIN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.DONKEY_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.DROWNED_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ELDER_GUARDIAN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ENDERMAN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ENDERMITE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.EVOKER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.GHAST_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.GUARDIAN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.HORSE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.HUSK_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.LLAMA_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.MAGMA_CUBE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.MOOSHROOM_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.MULE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.OCELOT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PARROT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PHANTOM_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PIG_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.POLAR_BEAR_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PUFFERFISH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.RABBIT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SALMON_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SHEEP_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SHULKER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SILVERFISH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SKELETON_HORSE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SKELETON_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SLIME_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SPIDER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SQUID_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.STRAY_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.TROPICAL_FISH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.TURTLE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.VEX_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.VILLAGER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.VINDICATOR_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WITCH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WITHER_SKELETON_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WOLF_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIE_HORSE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIE_PIGMAN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIE_VILLAGER_SPAWN_EGG, monsterEggOverride);

		OverrideItem bedOverride = new BedOverride();
		mItems.put(Material.WHITE_BED, bedOverride);
		mItems.put(Material.ORANGE_BED, bedOverride);
		mItems.put(Material.MAGENTA_BED, bedOverride);
		mItems.put(Material.LIGHT_BLUE_BED, bedOverride);
		mItems.put(Material.YELLOW_BED, bedOverride);
		mItems.put(Material.LIME_BED, bedOverride);
		mItems.put(Material.PINK_BED, bedOverride);
		mItems.put(Material.GRAY_BED, bedOverride);
		mItems.put(Material.LIGHT_GRAY_BED, bedOverride);
		mItems.put(Material.CYAN_BED, bedOverride);
		mItems.put(Material.PURPLE_BED, bedOverride);
		mItems.put(Material.BLUE_BED, bedOverride);
		mItems.put(Material.BROWN_BED, bedOverride);
		mItems.put(Material.GREEN_BED, bedOverride);
		mItems.put(Material.RED_BED, bedOverride);
		mItems.put(Material.BLACK_BED, bedOverride);

		OverrideItem boatOverride = new BoatOverride();
		mItems.put(Material.OAK_BOAT, boatOverride);
		mItems.put(Material.ACACIA_BOAT, boatOverride);
		mItems.put(Material.BIRCH_BOAT, boatOverride);
		mItems.put(Material.DARK_OAK_BOAT, boatOverride);
		mItems.put(Material.JUNGLE_BOAT, boatOverride);
		mItems.put(Material.SPRUCE_BOAT, boatOverride);

		OverrideItem minecartOverride = new MinecartOverride();
		mItems.put(Material.MINECART, minecartOverride);
		mItems.put(Material.CHEST_MINECART, minecartOverride);
		mItems.put(Material.FURNACE_MINECART, minecartOverride);
		mItems.put(Material.COMMAND_BLOCK_MINECART, minecartOverride);

		OverrideItem conduitOverride = new ConduitOverride();
		mItems.put(Material.CONDUIT, conduitOverride);

		OverrideItem bucketOverride = new BucketOverride();
		mItems.put(Material.BUCKET, bucketOverride);
		mItems.put(Material.WATER_BUCKET, bucketOverride);
		mItems.put(Material.LAVA_BUCKET, bucketOverride);

		OverrideItem chestOverride = new ChestOverride();
		mItems.put(Material.CHEST, chestOverride);
		mItems.put(Material.TRAPPED_CHEST, chestOverride);

		mItems.put(Material.FISHING_ROD, new FishingRodOverride());
		mItems.put(Material.ANVIL, new AnvilOverride());
		mItems.put(Material.ENCHANTING_TABLE, new EnchantmentTableOverride());
		mItems.put(Material.GOLDEN_APPLE, new GoldenAppleOverride());
		mItems.put(Material.ENDER_CHEST, new EnderChestOverride());
		mItems.put(Material.FARMLAND, new FarmlandOverride());
		mItems.put(Material.PACKED_ICE, new PackedIceOverride());
		mItems.put(Material.FIREWORK_ROCKET, new FireworkOverride());
		mItems.put(Material.SPAWNER, new MobSpawnerOverride());
		mItems.put(Material.FLOWER_POT, new FlowerPotOverride());
		mItems.put(Material.HOPPER, new HopperOverride());
		mItems.put(Material.MAGMA_BLOCK, new MagmaOverride());
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

	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		OverrideItem override = mItems.get(dispensed.getType());
		if (override != null) {
			return override.blockDispenseInteraction(plugin, block, dispensed);
		}

		return true;
	}
}
