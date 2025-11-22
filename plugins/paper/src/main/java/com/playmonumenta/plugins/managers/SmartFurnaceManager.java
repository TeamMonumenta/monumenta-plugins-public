package com.playmonumenta.plugins.managers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.ShulkerShortcutListener;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class SmartFurnaceManager implements Listener {
	public enum SmartFurnaceType {
		BLAST_FURNACE(Material.BLAST_FURNACE, "Smart Blast Furnace", "epic:items/smart_hopper_workarounds/smart_blast_furnace"),
		FURNACE(Material.FURNACE, "Smart Furnace", "epic:items/smart_hopper_workarounds/smart_furnace"),
		SMOKER(Material.SMOKER, "Smart Smoker", "epic:items/smart_hopper_workarounds/smart_smoker"),
		;

		final Material mMat;
		final String mName;
		final String mLootTable;

		SmartFurnaceType(Material material, String name, String lootTable) {
			mMat = material;
			mName = name;
			mLootTable = lootTable;
		}

		public static @Nullable SmartFurnaceType getType(@Nullable ItemStack item) {
			if (item == null) {
				return null;
			}

			Material mat = item.getType();
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				Component displayName = meta.displayName();
				if (displayName == null) {
					return null;
				}

				String plainName = MessagingUtils.plainText(displayName);

				for (SmartFurnaceType smartFurnaceType : values()) {
					if (smartFurnaceType.mMat.equals(mat) && smartFurnaceType.mName.equals(plainName)) {
						return smartFurnaceType;
					}
				}
			}

			return null;
		}

		public static @Nullable SmartFurnaceType getType(BlockState blockState) {
			Material mat = blockState.getType();
			if (blockState instanceof Furnace furnace) {
				Component customName = furnace.customName();
				if (customName == null) {
					return null;
				}
				String plainName = MessagingUtils.plainText(customName);

				for (SmartFurnaceType smartFurnaceType : values()) {
					if (smartFurnaceType.mMat.equals(mat) && smartFurnaceType.mName.equals(plainName)) {
						return smartFurnaceType;
					}
				}
			}

			return null;
		}

		public static boolean isSmartFurnace(@Nullable ItemStack item) {
			return getType(item) != null;
		}

		public static boolean isSmartFurnace(BlockState blockState) {
			return getType(blockState) != null;
		}

		public static boolean mayBeSmartFurnace(final Block block) {
			final var mat = block.getType();
			for (final SmartFurnaceType smartFurnaceType : values()) {
				if (mat == smartFurnaceType.mMat) {
					return true;
				}
			}
			return false;
		}
	}

	private static @Nullable SmartFurnaceManager INSTANCE = null;

	private SmartFurnaceManager() {
		INSTANCE = this;
	}

	public static SmartFurnaceManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SmartFurnaceManager();
		}
		return INSTANCE;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Block block = event.getBlockPlaced();
		BlockState blockState = block.getState();
		SmartFurnaceType smartFurnaceType = SmartFurnaceType.getType(blockState);
		if (blockState instanceof Furnace furnace && smartFurnaceType != null) {
			// Give the placed smart furnace name a vanilla-ish look;
			// the darker color is more visible in a GUI than the lighter colors for item hover text
			furnace.customName(Component.text(smartFurnaceType.mName));
			blockState.update();

			// Update the items after the event; handling this mid-event might mean the furnace isn't fully ready
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> moveFurnaceItems(event.getBlock().getState()));
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void worldLoadEvent(WorldLoadEvent event) {
		for (Chunk chunk : event.getWorld().getLoadedChunks()) {
			for (BlockState blockState : chunk.getTileEntities(SmartFurnaceType::mayBeSmartFurnace, false)) {
				moveFurnaceItems(blockState);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void chunkLoadEvent(ChunkLoadEvent event) {
		for (BlockState blockState : event.getChunk().getTileEntities(SmartFurnaceType::mayBeSmartFurnace, false)) {
			moveFurnaceItems(blockState);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void furnaceBurnEvent(FurnaceBurnEvent event) {
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> moveFurnaceItems(event.getBlock().getState()));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void furnaceSmeltEvent(FurnaceSmeltEvent event) {
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> moveFurnaceItems(event.getBlock().getState()));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		Inventory inv = event.getClickedInventory();
		if (inv != null && inv.getHolder() instanceof BlockInventoryHolder blockInventoryHolder) {
			checkBlockAndNeighbors(blockInventoryHolder.getBlock().getState());
		}
	}

	private void checkBlockAndNeighbors(BlockState centerBlockState) {
		Location location = centerBlockState.getLocation();
		World world = location.getWorld();
		int minWorldHeight = world.getMinHeight();
		int maxWorldHeight = world.getMaxHeight();

		if (location.isChunkLoaded()) {
			moveFurnaceItems(centerBlockState);
		}

		for (BlockFace blockFace : BlockUtils.CARTESIAN_BLOCK_FACES) {
			Location neighborLoc = location.clone().add(blockFace.getDirection());
			int y = neighborLoc.getBlockY();
			if (minWorldHeight <= y && y < maxWorldHeight && neighborLoc.isChunkLoaded()) {
				moveFurnaceItems(neighborLoc.getBlock().getState());
			}
		}
	}

	private void moveFurnaceItems(BlockState blockState) {
		if (!SmartFurnaceType.isSmartFurnace(blockState)) {
			return;
		}

		if (!(blockState instanceof Furnace furnace)) {
			return;
		}

		Location furnaceLoc = furnace.getLocation();
		FurnaceInventory furnaceInv = furnace.getInventory();

		ItemStack smeltingStack = furnaceInv.getSmelting();
		ItemStack fuelStack = furnaceInv.getFuel();
		ItemStack resultStack = furnaceInv.getResult();

		// Move result items if possible
		Location outputLoc = furnaceLoc.clone().add(0.0, -1.0, 0.0);
		if (resultStack != null && !ItemUtils.isNullOrAir(resultStack)) {
			int resultLeft = pushOutput(outputLoc, resultStack);
			if (resultLeft <= 0) {
				furnaceInv.setResult(null);
			} else {
				resultStack.setAmount(resultLeft);
				furnaceInv.setResult(resultStack);
			}
		}

		// Move empty buckets (from using lava as fuel) and other non-fuels out of the way if possible
		if (fuelStack != null && !ItemUtils.isNullOrAir(fuelStack) && !furnaceInv.isFuel(fuelStack)) {
			int fuelLeft = pushOutput(outputLoc, fuelStack);
			if (fuelLeft <= 0) {
				fuelStack = null;
			} else {
				fuelStack.setAmount(fuelLeft);
			}
		}

		// Get more fuel from the sides if needed
		for (BlockFace blockFace : BlockUtils.CARTESIAN_BLOCK_FACES) {
			if (blockFace.getModY() != 0) {
				continue;
			}

			Location inputLoc = furnaceLoc.clone().add(blockFace.getDirection());
			fuelStack = pullInput(inputLoc, fuelStack, furnaceInv::isFuel);
		}
		furnaceInv.setFuel(fuelStack);

		// Get more ingredients
		Location inputLoc = furnaceLoc.clone().add(0.0, 1.0, 0.0);
		smeltingStack = pullInput(inputLoc, smeltingStack, furnaceInv::canSmelt);
		furnaceInv.setSmelting(smeltingStack);
	}

	// Takes items from the input location if they fit in the existingStack; returns the modified existingStack
	private static @Nullable ItemStack pullInput(Location inputLoc, @Nullable ItemStack existingStack, Predicate<ItemStack> isAcceptable) {
		if (
			existingStack != null
				&& !Material.AIR.equals(existingStack.getType())
				&& existingStack.getAmount() == existingStack.getMaxStackSize()
		) {
			// Already full
			return existingStack;
		}

		int maxWorldHeight = inputLoc.getWorld().getMaxHeight();
		if (
			inputLoc.isChunkLoaded()
				&& inputLoc.getY() < maxWorldHeight
				&& inputLoc.getBlock().getState() instanceof Barrel barrel
				// Unnamed barrels only - no potion barrels or whatever gets added in the future
				&& barrel.customName() == null
				&& !barrel.isLocked()
		) {
			Inventory barrelInv = barrel.getInventory();
			int barrelSize = barrelInv.getSize();

			for (int barrelSlotIndex = 0; barrelSlotIndex < barrelSize; barrelSlotIndex++) {
				ItemStack barrelSlot = barrelInv.getItem(barrelSlotIndex);

				if (barrelSlot == null || ItemUtils.isNullOrAir(barrelSlot)) {
					// Empty slot; continue

					continue;
				} else if (isAcceptable.test(barrelSlot)) {
					// Valid fuel/smelting item

					if (existingStack == null) {
						// No existing item set; yoink!

						existingStack = barrelSlot.clone();
						barrelInv.setItem(barrelSlotIndex, null);
					} else if (existingStack.isSimilar(barrelSlot)) {
						// Matching type; yoink!

						int existingAmount = existingStack.getAmount();
						int maxStackSize = existingStack.getMaxStackSize();
						// Handle over-sized stacks
						int maxAccepted = Integer.max(0, maxStackSize - existingAmount);

						int barrelSlotAmount = barrelSlot.getAmount();
						int accepted = Integer.min(maxAccepted, barrelSlotAmount);

						existingStack.setAmount(existingAmount + accepted);

						int barrelSlotRemaining = barrelSlotAmount - accepted;
						if (barrelSlotAmount <= 0) {
							barrelInv.setItem(barrelSlotIndex, null);
						} else {
							barrelSlot.setAmount(barrelSlotRemaining);
							barrelInv.setItem(barrelSlotIndex, barrelSlot);
						}
					}
				} else {
					BlockStateMeta blockStateMeta = validShulkerBlockStateMeta(barrelSlot);

					if (
						blockStateMeta != null
							&& blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox
					) {
						// Shulker that we might be able to add items into

						Inventory shulkerInv = shulkerBox.getInventory();
						int shulkerSize = shulkerInv.getSize();

						for (int shulkerSlotIndex = 0; shulkerSlotIndex < shulkerSize; shulkerSlotIndex++) {
							ItemStack shulkerSlot = shulkerInv.getItem(shulkerSlotIndex);

							if (
								shulkerSlot != null
									&& !ItemUtils.isNullOrAir(shulkerSlot)
									&& isAcceptable.test(shulkerSlot)
							) {
								// Valid fuel/smelting item

								if (existingStack == null) {
									// No existing item set; yoink!

									existingStack = shulkerSlot.clone();
									shulkerInv.setItem(shulkerSlotIndex, null);
								} else if (existingStack.isSimilar(shulkerSlot)) {
									// Matching type; yoink!

									int existingAmount = existingStack.getAmount();
									int maxStackSize = existingStack.getMaxStackSize();
									// Handle over-sized stacks
									int maxAccepted = Integer.max(0, maxStackSize - existingAmount);

									int shulkerSlotAmount = shulkerSlot.getAmount();
									int accepted = Integer.min(maxAccepted, shulkerSlotAmount);

									existingStack.setAmount(existingAmount + accepted);

									int shulkerSlotRemaining = shulkerSlotAmount - accepted;
									if (shulkerSlotAmount <= 0) {
										shulkerInv.setItem(shulkerSlotIndex, null);
									} else {
										shulkerSlot.setAmount(shulkerSlotRemaining);
										shulkerInv.setItem(shulkerSlotIndex, shulkerSlot);
									}
								}
							}
						}

						blockStateMeta.setBlockState(shulkerBox);
						barrelSlot.setItemMeta(blockStateMeta);
						barrelInv.setItem(barrelSlotIndex, barrelSlot);
					}
				}

				if (
					existingStack != null
						&& !Material.AIR.equals(existingStack.getType())
						&& existingStack.getAmount() == existingStack.getMaxStackSize()
				) {
					// Already full
					return existingStack;
				}
			}
		}


		return existingStack;
	}

	// Stores items at the output location if possible, then
	// returns the remaining number of items in the output stack (does not modify output stack)
	private static int pushOutput(Location outputLoc, @Nullable ItemStack outputStack) {
		if (outputStack == null || ItemUtils.isNullOrAir(outputStack)) {
			return 0;
		}

		int remaining = outputStack.getAmount();
		int maxStackSize = outputStack.getMaxStackSize();

		int minWorldHeight = outputLoc.getWorld().getMinHeight();

		if (
			outputLoc.getY() >= minWorldHeight
				&& outputLoc.isChunkLoaded()
				&& outputLoc.getBlock().getState() instanceof Barrel barrel
				// Unnamed barrels only - no potion barrels or whatever gets added in the future
				&& barrel.customName() == null
				&& !barrel.isLocked()
		) {
			Inventory barrelInv = barrel.getInventory();
			int barrelSize = barrelInv.getSize();

			for (int barrelSlotIndex = 0; barrelSlotIndex < barrelSize; barrelSlotIndex++) {
				ItemStack barrelSlot = barrelInv.getItem(barrelSlotIndex);

				if (barrelSlot == null || ItemUtils.isNullOrAir(barrelSlot)) {
					// Empty slot; attempt to add the item here

					ItemStack addedStack = outputStack.clone();
					int addedStackSize = Integer.min(remaining, maxStackSize);
					addedStack.setAmount(addedStackSize);
					barrelInv.setItem(barrelSlotIndex, addedStack);
					remaining -= addedStackSize;
				} else if (barrelSlot.isSimilar(outputStack)) {
					// Similar slot; combine if possible

					int slotAmount = barrelSlot.getAmount();
					int addedStackSize = Integer.min(remaining, Integer.max(0, maxStackSize - slotAmount));
					int newSlotAmount = slotAmount + addedStackSize;
					barrelSlot.setAmount(newSlotAmount);
					barrelInv.setItem(barrelSlotIndex, barrelSlot);
					remaining -= addedStackSize;
				} else {
					BlockStateMeta blockStateMeta = validShulkerBlockStateMeta(barrelSlot);
					if (
						blockStateMeta != null
							&& blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox
					) {
						// Shulker that we might be able to add items into

						Inventory shulkerInv = shulkerBox.getInventory();
						int shulkerSize = shulkerInv.getSize();

						for (int shulkerSlotIndex = 0; shulkerSlotIndex < shulkerSize; shulkerSlotIndex++) {
							ItemStack shulkerSlot = shulkerInv.getItem(shulkerSlotIndex);

							if (shulkerSlot == null || ItemUtils.isNullOrAir(shulkerSlot)) {
								// Empty slot; attempt to add the item here

								ItemStack addedStack = outputStack.clone();
								int addedStackSize = Integer.min(remaining, maxStackSize);
								addedStack.setAmount(addedStackSize);
								shulkerInv.setItem(shulkerSlotIndex, addedStack);
								remaining -= addedStackSize;
							} else if (outputStack.isSimilar(shulkerSlot)) {
								// Similar slot; combine if possible

								int slotAmount = shulkerSlot.getAmount();
								int addedStackSize = Integer.min(remaining, Integer.max(0, maxStackSize - slotAmount));
								int newSlotAmount = slotAmount + addedStackSize;
								shulkerSlot.setAmount(newSlotAmount);
								remaining -= addedStackSize;
							}

							if (remaining <= 0) {
								break;
							}
						}

						blockStateMeta.setBlockState(shulkerBox);
						barrelSlot.setItemMeta(blockStateMeta);
						barrelInv.setItem(barrelSlotIndex, barrelSlot);
					}
				}

				if (remaining <= 0) {
					break;
				}
			}
		}

		return remaining;
	}

	// Returns a BlockStateMeta if items can be added/removed from it, otherwise null (special shulkers return null)
	private static @Nullable BlockStateMeta validShulkerBlockStateMeta(@Nullable ItemStack item) {
		if (item == null) {
			return null;
		}

		if (item.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
			Component name = blockStateMeta.displayName();
			if (name != null && !ShulkerShortcutListener.isPurpleTesseractContainer(item)) {
				return null;
			}

			if (
				blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox
					&& !shulkerBox.hasLootTable()
					&& !shulkerBox.isLocked()
			) {
				return blockStateMeta;
			}
		}

		return null;
	}
}
