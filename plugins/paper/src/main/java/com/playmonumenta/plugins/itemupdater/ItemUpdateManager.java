package com.playmonumenta.plugins.itemupdater;

import java.time.Instant;
import java.util.List;

import org.bukkit.block.BlockState;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.ArmoredHorseInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LlamaInventory;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;

import net.kyori.adventure.text.Component;

public class ItemUpdateManager implements Listener {
	// Updates items if needed as they load.
	private static @Nullable Plugin mPlugin;
	private static long mUpdateTimestamp;

	public ItemUpdateManager(Plugin plugin) {
		mPlugin = plugin;
		// TODO Change this to the weekly update timestamp, not the plugin load timestamp.
		mUpdateTimestamp = Instant.now().toEpochMilli();
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void join(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		long lastSaved = player.getLastLogin();
		if (lastSaved > mUpdateTimestamp) {
			return;
		}

		mPlugin.getLogger().fine("Updating items on player " + player.getName() + "; last update was " + Long.toString(lastSaved));

		// This is separate from the player inventory.
		@Nullable ItemStack[] enderItems = player.getEnderChest().getContents();
		for (int i = 0; i < enderItems.length; i++) {
			updateNested(enderItems[i]);
		}

		// Entities specified are saved to the player file.
		updateNested(player.getVehicle());
		updateNested(player.getShoulderEntityLeft());
		updateNested(player.getShoulderEntityRight());

		// Update the rest of the player's stuff as an entity.
		updateNested((Entity) player);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
		Item entity = event.getItem();
		ItemStack item = entity.getItemStack();
		updateNested(item);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		Item entity = event.getItemDrop();
		ItemStack preGrabbedItem = entity.getItemStack().clone();
		if (entity.isValid()) {
			updateNested(entity.getItemStack());
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				ItemStack[] items = player.getInventory().getContents();
				for (int i = 0; i < items.length; i++) {
					ItemStack item = items[i];
					if (preGrabbedItem.isSimilar(item)) {
						updateNested(item);
					}
				}
			}
		}.runTask(mPlugin);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityPickupItemEvent(EntityPickupItemEvent event) {
		Item entity = event.getItem();
		ItemStack item = entity.getItemStack();
		updateNested(item);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void itemSpawnEvent(ItemSpawnEvent event) {
		Item itemEntity = event.getEntity();
		ItemStack item = itemEntity.getItemStack();
		updateNested(item);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void inventoryOpenEvent(InventoryOpenEvent event) {
		Inventory inv = event.getInventory();
		@Nullable ItemStack[] items = inv.getContents();
		for (int i = 0; i < items.length; i++) {
			updateNested(items[i]);
		}
	}

	public static void updateNested(@Nullable ItemStack item) {
		if (item == null || !item.hasItemMeta()) {
			return;
		}

		if (item.hasItemMeta()) {
			ItemMeta itemMeta = item.getItemMeta();
			if (itemMeta.hasLore()) {
				for (Component loreLine : itemMeta.lore()) {
					if (ItemUtils.toPlainTagText(loreLine).contains("This is a placeholder item.")) {
						return;
					}
				}
			}
		}

		ItemUtils.setPlainTag(item);

		/* Updating containers nested in items disabled for now to improve performance.
		if (item.hasItemMeta()) {
			ItemMeta itemMeta = item.getItemMeta();
			if (itemMeta instanceof BlockStateMeta) {
				BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;
				if (blockStateMeta.hasBlockState()) {
					BlockState blockState = blockStateMeta.getBlockState();
					updateNested(blockState);
				}
			}

			// TODO Spawn eggs don't expose their contained entity NBT - now what?
		}
		*/
	}

	public static void updateNested(BlockState blockState) {
		if (blockState == null) {
			return;
		}

		// TODO CreatureSpawner (Mob Spawner) does not expose its entities. Now what?

		if (blockState instanceof Jukebox) {
			updateNested(((Jukebox) blockState).getRecord());
		}

		if (blockState instanceof InventoryHolder) {
			updateNested((InventoryHolder) blockState);
		}
	}

	public static void updateNested(Entity entity) {
		if (entity == null) {
			return;
		}

		if (entity instanceof AbstractArrow) {
			// Includes tridents and all arrow types.
			updateNested(((AbstractArrow) entity).getItemStack());
		}

		if (entity instanceof AbstractHorse) {
			AbstractHorseInventory absHorseInv = ((AbstractHorse) entity).getInventory();
			updateNested(absHorseInv.getSaddle());

			if (absHorseInv instanceof ArmoredHorseInventory) {
				updateNested(((ArmoredHorseInventory) absHorseInv).getArmor());
			}

			if (absHorseInv instanceof LlamaInventory) {
				updateNested(((LlamaInventory) absHorseInv).getDecor());
			}
		}

		if (entity instanceof EnderSignal) {
			updateNested(((EnderSignal) entity).getItem());
		}

		// TODO Falling blocks can have a NBT tags, but there's no method for it. Now what?

		if (entity instanceof Item) {
			updateNested(((Item) entity).getItemStack());
		}

		if (entity instanceof ItemFrame) {
			updateNested(((ItemFrame) entity).getItem());
		}

		if (entity instanceof Merchant) {
			Merchant merchant = (Merchant) entity;
			for (int i = 0; i < merchant.getRecipeCount(); i++) {
				MerchantRecipe trade = merchant.getRecipe(i);
				List<ItemStack> ingredients = trade.getIngredients();
				for (int j = 0; j < ingredients.size(); j++) {
					ItemStack ingredient = ingredients.get(j);
					updateNested(ingredient);
				}
			}
		}

		// TODO SpawnerMinecart does not give access to its mobs. Now what?

		if (entity instanceof ThrowableProjectile) {
			updateNested(((ThrowableProjectile) entity).getItem());
		}

		if (entity instanceof InventoryHolder) {
			updateNested((InventoryHolder) entity);
		}

		for (Entity passenger : entity.getPassengers()) {
			if (passenger instanceof Player) {
				continue;
			}

			updateNested(passenger);
		}
	}

	public static void updateNested(InventoryHolder inventoryHolder) {
		if (inventoryHolder == null) {
			return;
		}

		@Nullable ItemStack[] items = inventoryHolder.getInventory().getContents();
		for (int i = 0; i < items.length; i++) {
			updateNested(items[i]);
		}
	}
}
