package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.itemstats.enchantments.Undroppable;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class DroppedItemReplacementListener implements Listener {
	private static @Nullable Map<Material, Map<String, ItemStack>> mReplacementItems = null;

	public static Map<Material, Map<String, ItemStack>> getReplacementItems() {
		Map<Material, Map<String, ItemStack>> result = mReplacementItems;
		if (result != null) {
			return result;
		} else {
			result = new HashMap<>();
		}

		Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
		List<ItemStack> items = ServerProperties.getDroppedItemReplacements().stream()
			.flatMap(key -> InventoryUtils.getItemsFromLootTable(spawn, key).stream())
			.filter(Objects::nonNull)
			.toList();
		for (ItemStack item : items) {
			ItemMeta meta = item.getItemMeta();
			if (meta == null) {
				continue;
			}
			Component displayName = meta.displayName();
			if (displayName == null) {
				continue;
			}
			String plainName = MessagingUtils.plainText(displayName);
			if (plainName.isEmpty()) {
				continue;
			}

			Material material = item.getType();
			result.computeIfAbsent(material, k -> new HashMap<>())
				.put(plainName, item.asOne());
		}

		mReplacementItems = result;
		return result;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void serverResourcesReloadedEvent(ServerResourcesReloadedEvent event) {
		// Force items to reload on the next item drop
		mReplacementItems = null;
	}

	public @Nullable ItemStack getReplacement(@Nullable ItemStack originalItem) {
		if (originalItem == null) {
			return null;
		}

		Map<Material, Map<String, ItemStack>> replacementItems = getReplacementItems();

		Map<String, ItemStack> materialReplacements = replacementItems.get(originalItem.getType());
		if (materialReplacements == null) {
			return null;
		}

		ItemMeta meta = originalItem.getItemMeta();
		if (meta == null) {
			return null;
		}
		Component displayName = meta.displayName();
		if (displayName == null) {
			return null;
		}
		String plainName = MessagingUtils.plainText(displayName);
		if (plainName.isEmpty()) {
			return null;
		}

		ItemStack template = materialReplacements.get(plainName);
		if (template == null) {
			return null;
		}

		return template.asQuantity(originalItem.getAmount());
	}

	// Replace dropped items with the version that has lore text and such
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockDropItemEvent(BlockDropItemEvent event) {
		Iterator<Item> it = event.getItems().iterator();
		while (it.hasNext()) {
			Item itemEntity = it.next();
			ItemStack originalItem = itemEntity.getItemStack();

			if (Undroppable.isUndroppable(originalItem)) {
				it.remove();
				continue;
			}

			ItemStack replacementItem = getReplacement(originalItem);
			if (replacementItem == null) {
				continue;
			}
			if (Undroppable.isUndroppable(replacementItem)) {
				it.remove();
				continue;
			}
			itemEntity.setItemStack(replacementItem);
		}
	}
}
