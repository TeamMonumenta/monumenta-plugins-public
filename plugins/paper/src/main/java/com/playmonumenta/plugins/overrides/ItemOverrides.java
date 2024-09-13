package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import de.tr7zw.nbtapi.NBTItem;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemOverrides {
	/*
	 * Exceptions for materials that players in survival can place,
	 * or use to perform right-click interactions like
	 * tilling/stripping/making paths, even when the item has lore.
	 */
	public static final EnumSet<Material> EXCEPTION_LORED_MATERIALS = EnumSet.of(
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

		Material.ACACIA_SIGN,
		Material.ACACIA_WALL_SIGN,
		Material.BIRCH_SIGN,
		Material.BIRCH_WALL_SIGN,
		Material.CRIMSON_SIGN,
		Material.CRIMSON_WALL_SIGN,
		Material.DARK_OAK_SIGN,
		Material.DARK_OAK_WALL_SIGN,
		Material.JUNGLE_SIGN,
		Material.JUNGLE_WALL_SIGN,
		Material.OAK_SIGN,
		Material.OAK_WALL_SIGN,
		Material.SPRUCE_SIGN,
		Material.SPRUCE_WALL_SIGN,
		Material.WARPED_SIGN,
		Material.WARPED_WALL_SIGN,
		Material.MANGROVE_SIGN,
		Material.MANGROVE_WALL_SIGN,
		Material.CHERRY_SIGN,
		Material.CHERRY_WALL_SIGN
	);
	Map<Material, BaseOverride> mItems = new EnumMap<Material, BaseOverride>(Material.class);

	public ItemOverrides() {
		registerOverrides();
	}

	public void registerOverrides() {
		BaseOverride monsterEggOverride = new MonsterEggOverride();
		BaseOverride minecartOverride = new MinecartOverride();
		BaseOverride bucketOverride = new BucketOverride();
		// This is inefficient but it's only done once
		for (Material mat : Material.values()) {
			if (mat.name().contains("SPAWN_EGG")) {
				mItems.put(mat, monsterEggOverride);
			}
			if (mat.name().contains("MINECART")) {
				mItems.put(mat, minecartOverride);
			}
			if (mat.name().contains("BUCKET")) {
				mItems.put(mat, bucketOverride);
			}
		}

		BaseOverride bedOverride = new BedOverride();
		for (Material bed : Tag.BEDS.getValues()) {
			mItems.put(bed, bedOverride);
		}

		RespawnAnchorOverride respawnAnchorOverride = new RespawnAnchorOverride();
		mItems.put(Material.RESPAWN_ANCHOR, respawnAnchorOverride);

		BaseOverride boatOverride = new BoatOverride();
		for (Material boat : Tag.ITEMS_BOATS.getValues()) {
			mItems.put(boat, boatOverride);
		}

		BaseOverride kelpOverride = new KelpOverride();
		mItems.put(Material.KELP, kelpOverride);
		mItems.put(Material.SEAGRASS, kelpOverride);

		mItems.put(Material.COMPOSTER, new ComposterOverride());
		mItems.put(Material.CONDUIT, new ConduitOverride());
		mItems.put(Material.GRINDSTONE, new GrindstoneOverride());
		mItems.put(Material.FILLED_MAP, new MapOverride());
		mItems.put(Material.PUMPKIN_PIE, new PumpkinPieOverride());

		BaseOverride chestOverride = new ChestOverride();
		mItems.put(Material.CHEST, chestOverride);
		mItems.put(Material.TRAPPED_CHEST, chestOverride);

		mItems.put(Material.YELLOW_STAINED_GLASS, new YellowTesseractOverride());
		mItems.put(Material.LIME_STAINED_GLASS, new LimeTesseractOverride());
		mItems.put(Material.ICE, new FestiveTesseractOverride());

		mItems.put(InflationOverride.itemMaterial, new InflationOverride());

		BaseOverride anvilOverride = new AnvilOverride();
		mItems.put(Material.ANVIL, anvilOverride);
		mItems.put(Material.CHIPPED_ANVIL, anvilOverride);
		mItems.put(Material.DAMAGED_ANVIL, anvilOverride);

		mItems.put(Material.FISHING_ROD, new FishingRodOverride());
		mItems.put(Material.ENCHANTING_TABLE, new EnchantmentTableOverride());
		mItems.put(Material.ENDER_CHEST, new EnderChestOverride());
		mItems.put(Material.FARMLAND, new FarmlandOverride());
		mItems.put(Material.PACKED_ICE, new PackedIceOverride());
		mItems.put(Material.FIREWORK_ROCKET, new FireworkOverride());
		mItems.put(Material.HOPPER, new HopperOverride());
		mItems.put(Material.MAGMA_BLOCK, new MagmaOverride());
		mItems.put(Material.BEACON, new BeaconOverride());
		mItems.put(Material.TRIDENT, new TridentOverride());
		mItems.put(Material.BONE, new BoneOverride());

		BaseOverride fishOverride = new FishOverride();
		for (Material fish : Tag.ITEMS_FISHES.getValues()) {
			mItems.put(fish, fishOverride);
		}

		mItems.put(Material.SHEARS, new ShearsOverride());
		mItems.put(Material.FLINT_AND_STEEL, new FlintAndSteelOverride());
		mItems.put(Material.PUFFERFISH, new PufferfishOverride());
		mItems.put(Material.END_CRYSTAL, new EndCrystalOverride());
		mItems.put(Material.QUARTZ, new QuartzOverride());
		mItems.put(Material.FIRE_CHARGE, new FireChargeOverride());

		BaseOverride sculkOverride = new SculkOverride();
		mItems.put(Material.SCULK, sculkOverride);
		mItems.put(Material.SCULK_CATALYST, sculkOverride);
		mItems.put(Material.SCULK_SENSOR, sculkOverride);
		mItems.put(Material.SCULK_SHRIEKER, sculkOverride);

		BaseOverride signOverride = new SignOverride();
		for (Material sign : Tag.SIGNS.getValues()) {
			mItems.put(sign, signOverride);
		}

		BaseOverride trapdoorOverride = new TrapdoorOverride();
		for (Material trapdoor : Tag.WOODEN_TRAPDOORS.getValues()) {
			mItems.put(trapdoor, trapdoorOverride);
		}

		BaseOverride berryBushOverride = new BerryBushOverride();
		mItems.put(Material.SWEET_BERRY_BUSH, berryBushOverride);
		mItems.put(Material.CAVE_VINES, berryBushOverride);
		mItems.put(Material.CAVE_VINES_PLANT, berryBushOverride);

		BaseOverride doorOverride = new DoorOverride();
		for (Material door : Tag.WOODEN_DOORS.getValues()) {
			mItems.put(door, doorOverride);
		}

		BaseOverride unbreakableOverride = new UnbreakableOnBedrockOverride();
		mItems.put(Material.SPAWNER, unbreakableOverride);

		BaseOverride flowerOverride = new FlowerPotOverride();
		mItems.put(Material.FLOWER_POT, flowerOverride);
		for (Material flower : Tag.FLOWERS.getValues()) {
			mItems.put(flower, flowerOverride);
		}

		BaseOverride shulkerBoxOverride = new ShulkerBoxOverride();
		for (Material shulkerBox : Tag.SHULKER_BOXES.getValues()) {
			mItems.put(shulkerBox, shulkerBoxOverride);
		}

		BaseOverride noAdventurePlaceOverride = new NoAdventureModePlacementOverride();
		mItems.put(Material.LILY_PAD, noAdventurePlaceOverride);

		BaseOverride horseFoodOverride = new HorseFoodOverride();
		mItems.put(Material.SUGAR, horseFoodOverride);
		mItems.put(Material.WHEAT, horseFoodOverride);
		mItems.put(Material.APPLE, horseFoodOverride);
		mItems.put(Material.GOLDEN_CARROT, horseFoodOverride);
		mItems.put(Material.HAY_BLOCK, horseFoodOverride);
		// GOLDEN_APPLE + ENCHANTED_GOLDEN_APPLE are in GoldenAppleOverride, which manually calls this override

		BaseOverride goldenAppleOverride = new GoldenAppleOverride();
		mItems.put(Material.GOLDEN_APPLE, goldenAppleOverride);
		mItems.put(Material.ENCHANTED_GOLDEN_APPLE, goldenAppleOverride);

		BaseOverride lodestoneOverride = new LodestoneOverride();
		mItems.put(Material.LODESTONE, lodestoneOverride);

		BaseOverride boneMealOverride = new BoneMealOverride();
		mItems.put(Material.BONE_MEAL, boneMealOverride);

		BaseOverride glassBottleOverride = new GlassBottleOverride();
		mItems.put(Material.GLASS_BOTTLE, glassBottleOverride);

		//Just needs the base cake since you cant put candles on cakes that already have them
		BaseOverride candleCakeOverride = new CandleCakeOverride();
		mItems.put(Material.CAKE, candleCakeOverride);

		BaseOverride alchPotionOverride = new AlchPotionOverride();
		mItems.put(Material.SPLASH_POTION, alchPotionOverride);

		BaseOverride goatHornOverride = new GoatHornOverride();
		mItems.put(Material.GOAT_HORN, goatHornOverride);

		BaseOverride snowballOverride = new SnowballOverride();
		mItems.put(Material.SNOWBALL, snowballOverride);
	}

	public void rightClickInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, @Nullable Block block, PlayerInteractEvent event) {
		Material itemType = (item != null) ? item.getType() : Material.AIR;
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		BaseOverride itemOverride = mItems.get(itemType);
		BaseOverride blockOverride = mItems.get(blockType);

		if (item != null && itemOverride != null && event.useItemInHand() != Event.Result.DENY) {
			if (!itemOverride.rightClickItemInteraction(plugin, player, action, item, block)) {
				event.setUseItemInHand(Event.Result.DENY);
			}
		}

		if (block != null && blockOverride != null && event.useInteractedBlock() != Event.Result.DENY) {
			if (!blockOverride.rightClickBlockInteraction(plugin, player, action, item, block, event)) {
				event.setUseInteractedBlock(Event.Result.DENY);
			}
		}
	}

	public void leftClickInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item,
									 @Nullable Block block, PlayerInteractEvent event) {
		Material itemType = (item != null) ? item.getType() : Material.AIR;
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		BaseOverride itemOverride = mItems.get(itemType);
		BaseOverride blockOverride = mItems.get(blockType);

		if (item != null && itemOverride != null && event.useItemInHand() != Event.Result.DENY) {
			if (!itemOverride.leftClickItemInteraction(plugin, player, action, item, block)) {
				event.setUseItemInHand(Event.Result.DENY);
			}
		}

		if (block != null && blockOverride != null && event.useInteractedBlock() != Event.Result.DENY) {
			if (!blockOverride.leftClickBlockInteraction(plugin, player, action, item, block)) {
				event.setUseInteractedBlock(Event.Result.DENY);
			}
		}
	}

	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity,
											   ItemStack itemInHand) {
		Material itemType = (itemInHand != null) ? itemInHand.getType() : Material.AIR;
		BaseOverride override = mItems.get(itemType);

		return (override == null) || override.rightClickEntityInteraction(plugin, player, clickedEntity, itemInHand);
	}

	public boolean inventoryClickInteraction(Plugin plugin, Player player, InventoryClickEvent event) {
		ItemStack cursorItem = event.getCursor();
		if ((event.getClick() != ClickType.RIGHT && event.getClick() != ClickType.LEFT)
			|| (cursorItem != null && cursorItem.getType() != Material.AIR)) {
			return true;
		}
		ItemStack item = event.getCurrentItem();
		if (item == null) {
			return true;
		}
		BaseOverride override = mItems.get(item.getType());
		return override == null || override.inventoryClickInteraction(plugin, player, item, event);
	}

	// Generalised inventoryClickEvent for overrides that don't necessarily care if there's an item on the cursor
	public boolean inventoryClickEvent(Plugin plugin, Player player, InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (item == null) {
			return true;
		}
		BaseOverride override = mItems.get(item.getType());
		return override == null || override.inventoryClickEvent(plugin, player, item, event);
	}

	// Returns true is event swaphands is to be cancelled, this cancels player abilities too (like Shield Wall).
	public boolean swapHandsInteraction(Plugin plugin, Player player) {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item == null) {
			return true;
		}
		BaseOverride override = mItems.get(item.getType());
		return override != null && override.swapHandsInteraction(plugin, player, item);
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
				if (!ZoneUtils.isInPlot(block.getLocation())) {
					eventCancelled = true;
				}
			}
		}

		return eventCancelled;
	}

	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item,
										 BlockPlaceEvent event) {
		boolean eventCancelled = false;

		//  If it's not a certain lore item go ahead and run the normal override place interaction.
		BaseOverride override = mItems.get(item.getType());
		if (override != null) {
			eventCancelled |= !override.blockPlaceInteraction(plugin, player, item, event);
		}

		//  Don't allow placing of certain items with Lore.
		if (item.hasItemMeta()
			&& item.getItemMeta().hasLore()
			&& player.getGameMode() != GameMode.CREATIVE
			&& !EXCEPTION_LORED_MATERIALS.contains(item.getType())
			&& !ItemUtils.isNullOrAir(item)
			&& !Objects.equals(new NBTItem(item).getByte("Placeable"), (byte) 1)) {
			eventCancelled = true;
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
			eventCancelled = !override.blockBreakInteraction(plugin, player, block, event);
		}

		// Don't allow blocks to break if they're on the server's list of unbreakable blocks
		if (!eventCancelled && player.getGameMode() != GameMode.CREATIVE &&
			ServerProperties.getUnbreakableBlocks().contains(block.getType())) {
			eventCancelled = true;
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
