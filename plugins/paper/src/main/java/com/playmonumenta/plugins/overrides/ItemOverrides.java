package com.playmonumenta.plugins.overrides;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

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

	public static EnumMap<Material, String> ALLOW_PRECISE_LORE_MATS = new EnumMap<>(Material.class);

	static {
		ALLOW_PRECISE_LORE_MATS.put(Material.MAGMA_BLOCK, "Turns into lava when");
		ALLOW_PRECISE_LORE_MATS.put(Material.PACKED_ICE, "Turns into water when");
	}

	public ItemOverrides() {
		registerOverrides();
	}

	Map<Material, BaseOverride> mItems = new EnumMap<Material, BaseOverride>(Material.class);

	public void registerOverrides() {
		BaseOverride monsterEggOverride = new MonsterEggOverride();
		mItems.put(Material.BAT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.BEE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.BLAZE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.CAT_SPAWN_EGG, monsterEggOverride);
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
		mItems.put(Material.FOX_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.GHAST_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.GUARDIAN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.HOGLIN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.HORSE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.HUSK_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.LLAMA_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.MAGMA_CUBE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.MOOSHROOM_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.MULE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.OCELOT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PANDA_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PARROT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PHANTOM_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PIG_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PIGLIN_BRUTE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PIGLIN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PILLAGER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.POLAR_BEAR_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PUFFERFISH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.RABBIT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.RAVAGER_SPAWN_EGG, monsterEggOverride);
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
		mItems.put(Material.STRIDER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.TRADER_LLAMA_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.TROPICAL_FISH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.TURTLE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.VEX_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.VILLAGER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.VINDICATOR_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WANDERING_TRADER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WITCH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WITHER_SKELETON_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WOLF_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOGLIN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIE_HORSE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIE_VILLAGER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIFIED_PIGLIN_SPAWN_EGG, monsterEggOverride);

		BaseOverride bedOverride = new BedOverride();
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

		BaseOverride boatOverride = new BoatOverride();
		mItems.put(Material.OAK_BOAT, boatOverride);
		mItems.put(Material.ACACIA_BOAT, boatOverride);
		mItems.put(Material.BIRCH_BOAT, boatOverride);
		mItems.put(Material.DARK_OAK_BOAT, boatOverride);
		mItems.put(Material.JUNGLE_BOAT, boatOverride);
		mItems.put(Material.SPRUCE_BOAT, boatOverride);

		BaseOverride minecartOverride = new MinecartOverride();
		mItems.put(Material.MINECART, minecartOverride);
		mItems.put(Material.CHEST_MINECART, minecartOverride);
		mItems.put(Material.COMMAND_BLOCK_MINECART, minecartOverride);
		mItems.put(Material.FURNACE_MINECART, minecartOverride);
		mItems.put(Material.HOPPER_MINECART, minecartOverride);
		mItems.put(Material.TNT_MINECART, minecartOverride);

		mItems.put(Material.COMPOSTER, new ComposterOverride());
		mItems.put(Material.CONDUIT, new ConduitOverride());
		mItems.put(Material.GRINDSTONE, new GrindstoneOverride());
		mItems.put(Material.FILLED_MAP, new MapOverride());
		mItems.put(Material.PUMPKIN_PIE, new PumpkinPieOverride());

		BaseOverride bucketOverride = new BucketOverride();
		mItems.put(Material.BUCKET, bucketOverride);
		mItems.put(Material.WATER_BUCKET, bucketOverride);
		mItems.put(Material.LAVA_BUCKET, bucketOverride);
		mItems.put(Material.COD_BUCKET, bucketOverride);
		mItems.put(Material.MILK_BUCKET, bucketOverride);
		mItems.put(Material.PUFFERFISH_BUCKET, bucketOverride);
		mItems.put(Material.SALMON_BUCKET, bucketOverride);
		mItems.put(Material.TROPICAL_FISH_BUCKET, bucketOverride);

		BaseOverride chestOverride = new ChestOverride();
		mItems.put(Material.CHEST, chestOverride);
		mItems.put(Material.TRAPPED_CHEST, chestOverride);

		mItems.put(Material.YELLOW_STAINED_GLASS, new YellowTesseractOverride());
		mItems.put(Material.WHITE_STAINED_GLASS, new FestiveTesseractOverride());

		mItems.put(Material.FISHING_ROD, new FishingRodOverride());
		mItems.put(Material.ANVIL, new AnvilOverride());
		mItems.put(Material.CHIPPED_ANVIL, new AnvilOverride());
		mItems.put(Material.DAMAGED_ANVIL, new AnvilOverride());
		mItems.put(Material.ENCHANTING_TABLE, new EnchantmentTableOverride());
		mItems.put(Material.GOLDEN_APPLE, new GoldenAppleOverride());
		mItems.put(Material.ENDER_CHEST, new EnderChestOverride());
		mItems.put(Material.FARMLAND, new FarmlandOverride());
		mItems.put(Material.PACKED_ICE, new PackedIceOverride());
		mItems.put(Material.FIREWORK_ROCKET, new FireworkOverride());
		mItems.put(Material.HOPPER, new HopperOverride());
		mItems.put(Material.MAGMA_BLOCK, new MagmaOverride());
		mItems.put(Material.BEACON, new BeaconOverride());
		mItems.put(Material.TRIDENT, new TridentOverride());
		mItems.put(Material.BONE, new BoneOverride());
		mItems.put(Material.SHEARS, new ShearsOverride());
		mItems.put(Material.FLINT_AND_STEEL, new FlintAndSteelOverride());
		mItems.put(Material.PUFFERFISH, new PufferfishOverride());
		mItems.put(Material.CROSSBOW, new CrossbowOverride());

		BaseOverride signOverride = new SignOverride();
		mItems.put(Material.ACACIA_SIGN, signOverride);
		mItems.put(Material.ACACIA_WALL_SIGN, signOverride);
		mItems.put(Material.BIRCH_SIGN, signOverride);
		mItems.put(Material.BIRCH_WALL_SIGN, signOverride);
		mItems.put(Material.DARK_OAK_SIGN, signOverride);
		mItems.put(Material.DARK_OAK_WALL_SIGN, signOverride);
		mItems.put(Material.JUNGLE_SIGN, signOverride);
		mItems.put(Material.JUNGLE_WALL_SIGN, signOverride);
		mItems.put(Material.OAK_SIGN, signOverride);
		mItems.put(Material.OAK_WALL_SIGN, signOverride);
		mItems.put(Material.SPRUCE_SIGN, signOverride);
		mItems.put(Material.SPRUCE_WALL_SIGN, signOverride);
		mItems.put(Material.WARPED_SIGN, signOverride);
		mItems.put(Material.WARPED_WALL_SIGN, signOverride);
		mItems.put(Material.CRIMSON_SIGN, signOverride);
		mItems.put(Material.CRIMSON_WALL_SIGN, signOverride);


		BaseOverride unbreakableOverride = new UnbreakableOnBedrockOverride();
		mItems.put(Material.SPAWNER, unbreakableOverride);
		mItems.put(Material.SLIME_BLOCK, unbreakableOverride);

		BaseOverride flowerOverride = new FlowerPotOverride();
		mItems.put(Material.FLOWER_POT, flowerOverride);
		mItems.put(Material.POTTED_DANDELION, flowerOverride);
		mItems.put(Material.POTTED_POPPY, flowerOverride);
		mItems.put(Material.POTTED_BLUE_ORCHID, flowerOverride);
		mItems.put(Material.POTTED_ALLIUM, flowerOverride);
		mItems.put(Material.POTTED_AZURE_BLUET, flowerOverride);
		mItems.put(Material.POTTED_RED_TULIP, flowerOverride);
		mItems.put(Material.POTTED_ORANGE_TULIP, flowerOverride);
		mItems.put(Material.POTTED_WHITE_TULIP, flowerOverride);
		mItems.put(Material.POTTED_PINK_TULIP, flowerOverride);
		mItems.put(Material.POTTED_OXEYE_DAISY, flowerOverride);
		mItems.put(Material.POTTED_OAK_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_SPRUCE_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_BIRCH_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_JUNGLE_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_ACACIA_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_DARK_OAK_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_RED_MUSHROOM, flowerOverride);
		mItems.put(Material.POTTED_BROWN_MUSHROOM, flowerOverride);
		mItems.put(Material.POTTED_FERN, flowerOverride);
		mItems.put(Material.POTTED_DEAD_BUSH, flowerOverride);
		mItems.put(Material.POTTED_CACTUS, flowerOverride);

		BaseOverride firmamentOverride = new FirmamentOverride();
		mItems.put(Material.SHULKER_BOX, firmamentOverride);
		mItems.put(Material.WHITE_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.ORANGE_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.MAGENTA_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.LIGHT_BLUE_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.YELLOW_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.LIME_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.PINK_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.GRAY_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.LIGHT_GRAY_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.CYAN_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.PURPLE_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.BLUE_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.BROWN_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.GREEN_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.RED_SHULKER_BOX, firmamentOverride);
		mItems.put(Material.BLACK_SHULKER_BOX, firmamentOverride);
	}

	public boolean rightClickInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block, PlayerInteractEvent event) {
		Material itemType = (item != null) ? item.getType() : Material.AIR;
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		BaseOverride itemOverride = mItems.get(itemType);
		BaseOverride blockOverride = mItems.get(blockType);
		boolean notCancelled = true;

		if (itemOverride != null) {
			notCancelled = itemOverride.rightClickItemInteraction(plugin, player, action, item, block);
		}

		if (notCancelled && blockOverride != null) {
			notCancelled = blockOverride.rightClickBlockInteraction(plugin, player, action, item, block, event);
		}

		return notCancelled;
	}

	public boolean leftClickInteraction(Plugin plugin, Player player, Action action, ItemStack item,
	                                    Block block) {
		Material itemType = (item != null) ? item.getType() : Material.AIR;
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		BaseOverride itemOverride = mItems.get(itemType);
		BaseOverride blockOverride = mItems.get(blockType);
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
		BaseOverride override = mItems.get(itemType);

		return (override == null) || override.rightClickEntityInteraction(plugin, player, clickedEntity, itemInHand);
	}

	// Returns eventCancelled = true if disallowed, otherwise false
	@SuppressWarnings({"unused"})
	private boolean safezoneDisallowsBlockChange(Plugin plugin, Player player, Block block) {
		boolean eventCancelled = false;

		// Prevent players from breaking blocks in safezones from outside of them
		if (!eventCancelled && player.getGameMode() != GameMode.CREATIVE) {
			if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.ADVENTURE_MODE) &&
			    !ZoneUtils.hasZoneProperty(player.getLocation(), ZoneProperty.ADVENTURE_MODE)) {
				// Allow breaking if the player would be in survival mode at that spot
				if (!ZoneUtils.inPlot(block.getLocation(), ServerProperties.getIsTownWorld())) {
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
		BaseOverride override = mItems.get(item.getType());
		if (override != null) {
			eventCancelled |= !override.blockPlaceInteraction(plugin, player, item, event);
		}

		//  Don't allow placing of certain items with Lore.
		if (item.hasItemMeta()
		    && item.getItemMeta().hasLore()
		    && player.getGameMode() != GameMode.CREATIVE
		    && !(ALLOW_LORE_MATS.contains(item.getType())
		         || (ALLOW_PRECISE_LORE_MATS.containsKey(item.getType())
			         && InventoryUtils.testForItemWithLore(item, ALLOW_PRECISE_LORE_MATS.get(item.getType()))))) {
			eventCancelled |= true;
		}

		// Don't allow placing blocks on top of barriers for plots
		if (event.getBlockPlaced().getLocation().getBlockY() > 0 && !player.getGameMode().equals(GameMode.CREATIVE)) {
			Material belowMat = event.getBlockPlaced().getRelative(BlockFace.DOWN).getType();
			if (belowMat.equals(Material.BARRIER)) {
				eventCancelled = true;
			}

			// Don't allow players to place rail on bedrock because of a dumb mojang bug
			Material blockPlacedMat = event.getBlockPlaced().getType();
			if (belowMat.equals(Material.BEDROCK) &&
				(blockPlacedMat.equals(Material.RAIL) ||
				 blockPlacedMat.equals(Material.POWERED_RAIL) ||
				 blockPlacedMat.equals(Material.DETECTOR_RAIL))) {
				eventCancelled = true;
			}
		}

		// NOTE: This is disabled because while functionally nicer, players don't like it
		//eventCancelled |= safezoneDisallowsBlockChange(plugin, player, event.getBlockPlaced());

		return !eventCancelled;
	}

	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block, BlockBreakEvent event) {
		boolean eventCancelled = false;

		BaseOverride override = mItems.get(block.getType());
		if (override != null) {
			eventCancelled |= !override.blockBreakInteraction(plugin, player, block, event);
		}

		// Don't allow blocks to break if they're on the server's list of unbreakable blocks
		if (!eventCancelled && player.getGameMode() != GameMode.CREATIVE &&
		    ServerProperties.getUnbreakableBlocks().contains(block.getType())) {
			eventCancelled |= true;
		}

		// NOTE: This is disabled because, while functionally nicer, players don't like it
		//eventCancelled |= safezoneDisallowsBlockChange(plugin, player, block);

		return !eventCancelled;
	}

	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		BaseOverride override = mItems.get(block.getType());
		if (override != null) {
			return override.blockExplodeInteraction(plugin, block);
		}

		return true;
	}

	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		BaseOverride override = mItems.get(dispensed.getType());
		if (override != null) {
			return override.blockDispenseInteraction(plugin, block, dispensed);
		}

		return true;
	}

	public boolean blockChangeInteraction(Plugin plugin, Block block) {
		BaseOverride override = mItems.get(block.getType());
		if (override != null) {
			return override.blockChangeInteraction(plugin, block);
		}
		return true;
	}

	public boolean playerItemConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		BaseOverride override = mItems.get(event.getItem().getType());
		if (override != null) {
			return override.playerItemConsume(plugin, player, event);
		}
		return true;
	}

	public boolean playerRiptide(Plugin plugin, Player player, PlayerRiptideEvent event) {
		BaseOverride override = mItems.get(event.getItem().getType());
		if (override != null) {
			return override.playerRiptide(plugin, player, event);
		}
		return true;
	}
}
