package com.playmonumenta.plugins.explosions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enums.PickupFilterResult;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.listeners.ShulkerShortcutListener.isCarrierOfExplosions;

public class ExplosionManager implements Listener {
	private static final int ITEM_ENTITY_THRESHOLD = (int) Math.ceil(27.0 / PickupFilterResult.values().length);

	private int mLastTick = Integer.MIN_VALUE;
	private @Nullable BukkitRunnable mCleanupRunnable = null;
	private final ExplosionSourceMap mExplosionSources = new ExplosionSourceMap();
	private final Map<Location, Map<PickupFilterResult, Set<Item>>> mExplosionItems = new HashMap<>();

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		registerExplodedBlocks(event.getBlock().getLocation(), event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		registerExplodedBlocks(event.getLocation(), event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void itemSpawnEvent(ItemSpawnEvent event) {
		Item item = event.getEntity();
		ItemStack itemStack = item.getItemStack();
		if (ItemUtils.isShulkerBox(itemStack.getType())) {
			return;
		}

		Location blockLocation = item.getLocation().toBlockLocation();
		Location explosionSource = mExplosionSources.getSource(blockLocation);
		if (explosionSource == null) {
			return;
		}

		PickupFilterResult filterResult = PickupFilterResult.getFilterResult(itemStack);
		mExplosionItems
			.computeIfAbsent(explosionSource, k -> new HashMap<>())
			.computeIfAbsent(filterResult, k -> new HashSet<>())
			.add(item);
	}

	private void clear() {
		mExplosionSources.clear();
		mExplosionItems.clear();
		if (mCleanupRunnable != null) {
			mCleanupRunnable.cancel();
			mCleanupRunnable = null;
		}
	}

	private void registerExplodedBlocks(Location explosionSource, List<Block> blocks) {
		final int registerTick = Bukkit.getCurrentTick();
		if (mLastTick != registerTick) {
			mLastTick = registerTick;
			spawnCarriers();
			clear();
			mCleanupRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (mLastTick == registerTick) {
						spawnCarriers();
						clear();
					}
				}
			};
			mCleanupRunnable.runTask(Plugin.getInstance());
		}
		for (Block block : blocks) {
			mExplosionSources.put(explosionSource, block);
		}
	}

	private void spawnCarriers() {
		for (Map.Entry<Location, Map<PickupFilterResult, Set<Item>>> explosionEntry : mExplosionItems.entrySet()) {
			Location explosionSource = explosionEntry.getKey();

			double searchRadius = mExplosionSources.getRadius(explosionSource) + 1.0;
			List<Item> existingCarriers = new ArrayList<>();
			for (Item itemEntity : explosionSource.getNearbyEntitiesByType(Item.class, searchRadius)) {
				if (isCarrierOfExplosions(itemEntity.getItemStack())) {
					existingCarriers.add(itemEntity);
				}
			}

			for (Map.Entry<PickupFilterResult, Set<Item>> filterResultEntry : explosionEntry.getValue().entrySet()) {
				PickupFilterResult filterResult = filterResultEntry.getKey();

				spawnCarriers(explosionSource, filterResult, existingCarriers, filterResultEntry.getValue());
			}
		}
	}

	private void spawnCarriers(
		Location explosionSource,
		PickupFilterResult filterResult,
		List<Item> existingCarriers,
		Set<Item> explosionItemEntities
	) {
		explosionItemEntities.removeIf(itemEntity -> !itemEntity.isValid());
		if (explosionItemEntities.isEmpty()) {
			return;
		}

		List<Location> spawnLocations = new ArrayList<>(explosionItemEntities.stream().map(Item::getLocation).toList());

		Map<ItemStack, List<Item>> itemsByType = new HashMap<>();
		for (Item itemEntity : explosionItemEntities) {
			ItemStack originalStack = itemEntity.getItemStack();
			ItemStack asOne = originalStack.asOne();
			itemsByType
				.computeIfAbsent(asOne, k -> new ArrayList<>())
				.add(itemEntity);
		}

		// Attempt to store items in existing carriers if possible
		for (Map.Entry<ItemStack, List<Item>> itemTypeEntry : itemsByType.entrySet()) {
			Iterator<Item> carrierIterator = existingCarriers.iterator();
			Item existingCarrier = carrierIterator.hasNext() ? carrierIterator.next() : null;
			nextItemEntity:
			for (Item itemEntity : itemTypeEntry.getValue()) {
				ItemStack originalStack = itemEntity.getItemStack();
				while (existingCarrier != null) {
					ItemStack remainingStack = insertIntoCarrier(existingCarrier, originalStack);
					if (remainingStack == null) {
						// No items remaining on this entity; delete it
						explosionItemEntities.remove(itemEntity);
						itemEntity.remove();
						continue nextItemEntity;
					}

					// Not all items removed from this entity, try another carrier if available
					itemEntity.setItemStack(remainingStack);
					existingCarrier = carrierIterator.hasNext() ? carrierIterator.next() : null;
				}

				// Out of carriers that work with this item type; try the next type
			}
		}

		// Do threshold check on remaining items
		if (explosionItemEntities.size() < ITEM_ENTITY_THRESHOLD) {
			return;
		}

		Map<ItemStack, Integer> itemCounts = new HashMap<>();
		for (Item itemEntity : explosionItemEntities) {
			ItemStack originalStack = itemEntity.getItemStack();
			itemEntity.remove();
			ItemStack asOne = originalStack.asOne();
			int totalCount = itemCounts.getOrDefault(asOne, 0) + originalStack.getAmount();

			itemCounts.put(asOne, totalCount);
		}

		World world = explosionSource.getWorld();

		// Create new carriers for any that remain
		List<ItemStack> contents = new ArrayList<>();
		for (Map.Entry<ItemStack, Integer> itemCountEntry : itemCounts.entrySet()) {
			ItemStack asOne = itemCountEntry.getKey();
			int remainingItems = itemCountEntry.getValue();
			int maxStack = asOne.getMaxStackSize();

			while (remainingItems > 0) {
				int stackSize = Math.min(maxStack, remainingItems);
				remainingItems -= stackSize;
				ItemStack newStack = asOne.asQuantity(stackSize);

				contents.add(newStack);
				if (contents.size() == 27) {
					spawnCarrier(world, getRandomLocation(spawnLocations), filterResult, contents);
					contents = new ArrayList<>();
				}
			}
		}
		if (!contents.isEmpty()) {
			spawnCarrier(world, getRandomLocation(spawnLocations), filterResult, contents);
		}
	}

	private Location getRandomLocation(List<Location> spawnLocations) {
		int numEntries = spawnLocations.size();
		if (numEntries == 1) {
			return spawnLocations.get(0);
		}
		return spawnLocations.remove(FastUtils.RANDOM.nextInt(numEntries));
	}

	private void spawnCarrier(World world, Location location, PickupFilterResult filterResult, List<ItemStack> contents) {
		ItemStack carrierItem = createCarrierOfExplosions(filterResult, contents);
		world.spawn(location, Item.class, carrierEntity -> carrierEntity.setItemStack(carrierItem));
	}

	private ItemStack createCarrierOfExplosions(PickupFilterResult filterResult, List<ItemStack> contents) {
		Material shulkerMat;
		TextColor nameColor;
		switch (filterResult) {
			case TIERED -> {
				shulkerMat = Material.YELLOW_SHULKER_BOX;
				nameColor = TextColor.color(DyeColor.YELLOW.getColor().asRGB());
			}
			case LORE_AND_INTERESTING -> {
				shulkerMat = Material.ORANGE_SHULKER_BOX;
				nameColor = TextColor.color(DyeColor.ORANGE.getColor().asRGB());
			}
			case LORE -> {
				shulkerMat = Material.RED_SHULKER_BOX;
				nameColor = TextColor.color(DyeColor.RED.getColor().asRGB());
			}
			case INTERESTING -> {
				shulkerMat = Material.LIGHT_GRAY_SHULKER_BOX;
				nameColor = TextColor.color(DyeColor.LIGHT_GRAY.getColor().asRGB());
			}
			default -> {
				shulkerMat = Material.GRAY_SHULKER_BOX;
				nameColor = TextColor.color(DyeColor.GRAY.getColor().asRGB());
			}
		}

		ItemStack carrier = new ItemStack(shulkerMat);
		BlockStateMeta meta = (BlockStateMeta) carrier.getItemMeta();
		meta.displayName(Component.text("Carrier of Explosions", nameColor, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));

		ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
		Inventory inventory = shulkerBox.getInventory();
		int maxItemCount = 1;
		for (ItemStack itemStack : contents) {
			inventory.addItem(itemStack);
			maxItemCount = Math.max(maxItemCount, itemStack.getAmount());
		}
		meta.setBlockState(shulkerBox);

		carrier.setItemMeta(meta);
		ItemUtils.setPlainTag(carrier);
		PickupFilterResult.setFilterResult(carrier, filterResult);
		PickupFilterResult.setPickupCount(carrier, maxItemCount);
		return carrier;
	}

	// Returns the leftover item stack, if any
	private @Nullable ItemStack insertIntoCarrier(Item carrierItemEntity, ItemStack toInsert) {
		ItemStack carrier = carrierItemEntity.getItemStack();
		if (!(carrier.getItemMeta() instanceof BlockStateMeta blockStateMeta)) {
			return toInsert;
		}

		if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
			return toInsert;
		}

		Inventory inv = shulkerBox.getInventory();
		Iterator<ItemStack> remainingIter = inv.addItem(toInsert).values().iterator();
		ItemStack leftOver = remainingIter.hasNext() ? remainingIter.next() : null;

		blockStateMeta.setBlockState(shulkerBox);
		carrier.setItemMeta(blockStateMeta);
		carrierItemEntity.setItemStack(carrier);
		return leftOver;
	}
}
