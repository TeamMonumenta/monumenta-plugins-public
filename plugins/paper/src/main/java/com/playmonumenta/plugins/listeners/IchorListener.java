package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class IchorListener implements Listener {
	public static final String ITEM_NAME = "Starblood Ichor";
	private static final String ITEM_PATH = "epic:r3/items/epics/starblood_ichor";
	private static final Material ITEM_MATERIAL = Material.DRAGON_BREATH;
	public static final List<InfusionType> ICHOR_INFUSIONS = List.of(
		InfusionType.ICHOR_DAWNBRINGER,
		InfusionType.ICHOR_EARTHBOUND,
		InfusionType.ICHOR_FLAMECALLER,
		InfusionType.ICHOR_FROSTBORN,
		InfusionType.ICHOR_PRISMATIC,
		InfusionType.ICHOR_SHADOWDANCER,
		InfusionType.ICHOR_STEELSAGE,
		InfusionType.ICHOR_WINDWALKER
	);

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		ItemStack item = event.getCurrentItem();
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}
		if (!(event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SWAP_OFFHAND)) {
			return;
		}
		if (!isInfiniteConsumable(item)) {
			return;
		}

		if (event.getClick() == ClickType.SWAP_OFFHAND) {
			InfusionType ichorInfusion = getIchorInfusion(item);
			if (ichorInfusion == null) {
				return;
			}

			ItemStack ichorRefund = InventoryUtils.getItemFromLootTable(player.getLocation(), NamespacedKeyUtils.fromString(ITEM_PATH));
			if (ichorRefund == null) {
				return;
			}

			if (!InventoryUtils.canFitInInventory(ichorRefund, player.getInventory())) {
				player.sendMessage(Component.text("There's not enough room in your inventory!", NamedTextColor.RED));
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
				event.setCancelled(true);
				return;
			}

			ItemStatUtils.removeInfusion(item, ichorInfusion);
			ItemStatUtils.addInfusion(ichorRefund, ichorInfusion, 1, player.getUniqueId());
			InventoryUtils.giveItem(player, ichorRefund);
			player.playSound(player.getLocation(), Sound.BLOCK_LADDER_PLACE, SoundCategory.PLAYERS, 1.0f, 2.0f);
			event.setCancelled(true);
		} else if (event.getClick() == ClickType.RIGHT) {
			ItemStack cursor = event.getCursor();
			if (!isIchor(cursor)) {
				return;
			}
			if (hasNonIchorInfusion(cursor)) {
				player.sendMessage(Component.text("Your " + ITEM_NAME + " is infused, so it can't be added!", NamedTextColor.RED));
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
				event.setCancelled(true);
				return;
			}
			if (cursor.getAmount() > 1 || item.getAmount() > 1) {
				player.sendMessage(Component.text("You cannot add multiple " + ITEM_NAME + "s at once!", NamedTextColor.RED));
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
				event.setCancelled(true);
				return;
			}
			if (getIchorInfusion(item) != null) {
				player.sendMessage(Component.text("Your " + ItemUtils.getPlainNameIfExists(item) + " already has an imbuement, so another can't be added!", NamedTextColor.RED));
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
				event.setCancelled(true);
				return;
			}
			InfusionType ichorInfusion = getIchorInfusion(cursor);
			if (ichorInfusion == null) {
				player.sendMessage(Component.text("Your " + ITEM_NAME + " is not imbued!", NamedTextColor.RED));
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.2f);
				event.setCancelled(true);
				return;
			}

			player.setItemOnCursor(null);
			ItemStatUtils.addInfusion(item, ichorInfusion, 1, player.getUniqueId());
			player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 1.0f, 1.6f);
			event.setCancelled(true);
		}
	}

	public static boolean isIchor(ItemStack item) {
		if (item == null || item.getType() != ITEM_MATERIAL) {
			return false;
		}
		return ItemUtils.getPlainNameIfExists(item).equals(ITEM_NAME);
	}

	private boolean hasNonIchorInfusion(ItemStack item) {
		for (InfusionType infusionType : InfusionType.values()) {
			if (ItemStatUtils.hasInfusion(item, infusionType) && !ICHOR_INFUSIONS.contains(infusionType)) {
				return true;
			}
		}
		return false;
	}

	private boolean isInfiniteConsumable(ItemStack item) {
		return (item.getType().isEdible() || item.getType() == Material.POTION) && ItemStatUtils.hasEnchantment(item, EnchantmentType.INFINITY);
	}

	public static @Nullable InfusionType getIchorInfusion(ItemStack item) {
		for (InfusionType infusionType : ICHOR_INFUSIONS) {
			if (ItemStatUtils.hasInfusion(item, infusionType)) {
				return infusionType;
			}
		}
		return null;
	}

	public static DepthsTree getCorrespondingDepthsTree(InfusionType ichorInfusion) {
		switch (ichorInfusion) {
			case ICHOR_DAWNBRINGER -> {
				return DepthsTree.DAWNBRINGER;
			}
			case ICHOR_EARTHBOUND -> {
				return DepthsTree.EARTHBOUND;
			}
			case ICHOR_FLAMECALLER -> {
				return DepthsTree.FLAMECALLER;
			}
			case ICHOR_FROSTBORN -> {
				return DepthsTree.FROSTBORN;
			}
			case ICHOR_PRISMATIC -> {
				return DepthsTree.PRISMATIC;
			}
			case ICHOR_SHADOWDANCER -> {
				return DepthsTree.SHADOWDANCER;
			}
			case ICHOR_STEELSAGE -> {
				return DepthsTree.STEELSAGE;
			}
			default -> {
				return DepthsTree.WINDWALKER;
			}
		}
	}
}
