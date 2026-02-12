package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.AbsorptionSickness;
import com.playmonumenta.plugins.effects.HealingSickness;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.itemstats.enchantments.Starvation;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Nullable;

public class PotionInjectorsOverride extends BaseOverride {
	private static final @Nullable String PI_NAME = "Potion Injector";
	private static final @Nullable String II_NAME = "Iridium Injector";
	private static final String POTION_KEY = "PotionInjectorSelectedPotion";
	private static final String COOLDOWN_SOURCE = "PotionInjectorCooldown";
	private static final int COOLDOWN = 5 * 20;

	@SuppressWarnings("SameReturnValue")
	public static boolean leftClickAction(Plugin plugin, Player player, ItemStack injector) {
		if (!player.hasPermission("monumenta.potion_injector") || ZoneUtils.hasZoneProperty(player.getLocation(), ZoneUtils.ZoneProperty.NO_POTIONS)) {
			player.sendMessage(Component.text("Potions cannot be used here!", NamedTextColor.RED));
			return false;
		}
		boolean isPI = isPotionInjector(injector);
		boolean isUpgradedPI = isIridiumInjector(injector);

		if (ScoreboardUtils.getScoreboardValue(player, "Verdant").orElse(0) <= 0) {
			player.sendMessage(Component.text("You must have completed Verdant Remnants to use this item.", NamedTextColor.RED));
			return false;
		}
		if (isUpgradedPI && ScoreboardUtils.getScoreboardValue(player, "MasqueradersRuin").orElse(0) <= 0) {
			player.sendMessage(Component.text("You must have completed Masquerader's Ruin to use this item.", NamedTextColor.RED));
			return false;
		}

		BlockStateMeta shulkerMeta = (BlockStateMeta) injector.getItemMeta();
		ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
		Inventory shulkerInventory = shulkerBox.getInventory();

		var cooldownEffect = plugin.mEffectManager.getEffects(player, COOLDOWN_SOURCE);
		if (cooldownEffect != null && !cooldownEffect.isEmpty()) {
			player.sendMessage(
				Component.text(String.format("Your %s is on cooldown for " + cooldownEffect.last().getDuration() / 20 + "s.", ItemUtils.getPlainName(injector)), NamedTextColor.RED)
			);
			return false;
		}

		if (isPI) {
			handlePI(plugin, player, injector, shulkerInventory);
		} else if (isUpgradedPI) {
			handleII(plugin, player, injector, shulkerInventory);
		}

		// reset shulker meta because of lore changes
		shulkerMeta = (BlockStateMeta) injector.getItemMeta();
		shulkerMeta.setBlockState(shulkerBox);
		injector.setItemMeta(shulkerMeta);

		player.updateInventory();

		return false;
	}

	private static boolean handlePI(Plugin plugin, Player player, ItemStack injector, Inventory shulkerInventory) {
		String potionName = getPotionName(injector);

		if (potionName.isEmpty()) {
			// try to select potion
			Location playerLoc = player.getLocation().add(0, 1, 0);
			Optional<Item> closestPotion = player.getWorld().getNearbyEntitiesByType(Item.class, playerLoc, 5).stream()
				.min(Comparator.comparing(entity -> entity.getLocation().distance(playerLoc)));
			if (closestPotion.isEmpty() || !isNotInfinitePotion(closestPotion.get().getItemStack())) {
				player.sendMessage(Component.text("You are not carrying any valid potions in your Potion Injector! Throw one on the ground to get started.", NamedTextColor.RED));
				return true;
			}

			Item item = closestPotion.get();
			ItemStack itemStack = item.getItemStack();
			setPotionName(injector, ItemUtils.getPlainName(itemStack), ItemUtils.getDisplayName(itemStack));
			Component newPotionComponent = ItemUtils.getDisplayLore(injector).get(1);

			if (shulkerInventory.addItem(itemStack).isEmpty()) {
				player.playPickupItemAnimation(item);
				item.remove();
				player.sendActionBar(newPotionComponent.append(Component.text(" has been stored into your Potion Injector!", NamedTextColor.GRAY)));
			}

			player.sendMessage(Component.text("Your Potion Injector has been Calibrated to: ", NamedTextColor.AQUA).append(newPotionComponent));
			return false;
		}
		PlayerInventory playerInventory = player.getInventory();
		for (int i = 9; i < 36; i++) {
			ItemStack itemInInventory = playerInventory.getItem(i);

			if (isNotInfinitePotion(itemInInventory) && InventoryUtils.testForItemWithName(itemInInventory, potionName, true)) {
				playerInventory.setItem(i, consumePotion(plugin, player, itemInInventory, injector));
				return false;
			}
		}
		for (int i = 0; i < shulkerInventory.getContents().length; i++) {
			ItemStack itemInPI = shulkerInventory.getItem(i);

			if (isNotInfinitePotion(itemInPI) && InventoryUtils.testForItemWithName(itemInPI, potionName, true)) {
				shulkerInventory.setItem(i, consumePotion(plugin, player, itemInPI, injector));
				return false;
			}
		}
		player.sendMessage(Component.text("You are not currently carrying any: ", NamedTextColor.RED).append(ItemUtils.getDisplayLore(injector).get(1)));
		return true;
	}

	private static boolean handleII(Plugin plugin, Player player, ItemStack injector, Inventory shulkerInventory) {
		for (int i = 0; i < shulkerInventory.getContents().length; i++) {
			ItemStack itemInII = shulkerInventory.getItem(i);

			if (isNotInfinitePotion(itemInII)) {
				shulkerInventory.setItem(i, consumePotion(plugin, player, itemInII, injector));
				return false;
			}
		}
		player.sendMessage(Component.text("You are not carrying any valid potions in your Iridium Injector!", NamedTextColor.RED));
		return true;
	}

	public static boolean swapShiftInteraction(ItemStack injector, Player player) {
		player.sendMessage(Component.text("Your Potion Injector has been cleared of calibration!", NamedTextColor.GREEN));
		setPotionName(injector, "", null);
		return true;
	}

	private static ItemStack consumePotion(Plugin plugin, Player player, ItemStack potion, ItemStack injector) {
		boolean cancel = NBT.get(potion, nbt -> {
			var effects = ItemStatUtils.getEffects(nbt);
			var healingSickness = plugin.mEffectManager.getEffects(player, HealingSickness.effectID);
			var absorptionSickness = plugin.mEffectManager.getEffects(player, AbsorptionSickness.effectID);

			boolean healing = false;
			boolean absorption = false;
			boolean other = false;

			if (effects == null || effects.isEmpty()) {
				return false;
			}
			for (ReadWriteNBT effect : effects) {
				String type = effect.getString(ItemStatUtils.EFFECT_TYPE_KEY);
				EffectType effectType = EffectType.fromType(type);
				if (healingSickness != null && !healingSickness.isEmpty() && (effectType == EffectType.INSTANT_HEALTH || effectType == EffectType.CUSTOM_HEALTH_OVER_TIME)) {
					healing = true;
				}
				if (absorptionSickness != null && !absorptionSickness.isEmpty() && effectType == EffectType.ABSORPTION) {
					absorption = true;
				}
				if (effectType != EffectType.INSTANT_HEALTH && effectType != EffectType.CUSTOM_HEALTH_OVER_TIME && effectType != EffectType.ABSORPTION) {
					other = true;
					break;
				}
			}
			if (other) {
				// Permit the injector to work as long as there are effects that are non-healing or non-absorption
				return false;
			}
			if (healing && healingSickness != null && healingSickness.last() != null) {
				player.sendMessage(Component.text("Your next potion would not heal you!", NamedTextColor.RED)
					.appendNewline()
					.append(Component.text("Wait ", NamedTextColor.GRAY))
					.append(Component.text(healingSickness.last().getDuration() / 20, NamedTextColor.WHITE))
					.append(Component.text(" more seconds before drinking another Healing potion!", NamedTextColor.GRAY))
				);
			}
			if (absorption && absorptionSickness != null && absorptionSickness.last() != null) {
				player.sendMessage(Component.text("Your next potion would not give you absorption!", NamedTextColor.RED)
					.appendNewline()
					.append(Component.text("Wait ", NamedTextColor.GRAY))
					.append(Component.text(absorptionSickness.last().getDuration() / 20, NamedTextColor.WHITE))
					.append(Component.text(" more seconds before drinking another Absorption potion!", NamedTextColor.GRAY))
				);
			}
			if (healing || absorption) {
				return true;
			} else {
				// Let the injector run if there aren't other effects but also no sicknesses
				return false;
			}
			});
		if (cancel) {
			return potion;
		}

		ItemStatUtils.applyCustomEffects(plugin, player, potion);

		ItemStack updatedPotion;

		// Test for Starvation.
		int starvation = ItemStatUtils.getEnchantmentLevel(potion, EnchantmentType.STARVATION);
		if (starvation > 0) {
			Starvation.apply(player, starvation);
		}

		// If there is more than one potion, decrement potion by one
		if (potion.getAmount() > 1) {
			potion.setAmount(potion.getAmount() - 1);
			updatedPotion = potion;
		} else {
			// Set potion to air.
			updatedPotion = new ItemStack(Material.AIR);
		}

		plugin.mEffectManager.addEffect(player, COOLDOWN_SOURCE, new ItemCooldown(COOLDOWN, injector, plugin));

		World world = player.getWorld();
		world.playSound(player, Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 1.0f);
		Bukkit.getScheduler().runTaskLater(plugin, () -> world.playSound(player, Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 1.0f), 4);
		Bukkit.getScheduler().runTaskLater(plugin, () -> world.playSound(player, Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 1.0f), 8);

		return updatedPotion;
	}

	public static void setPotionName(ItemStack itemStack, String plainName, @Nullable Component loreComponent) {
		NBT.modify(itemStack, nbt -> {
			ReadWriteNBT playerModified = ItemStatUtils.addPlayerModified(nbt);
			if (plainName.isEmpty()) {
				playerModified.removeKey(POTION_KEY);
			} else {
				playerModified.setString(POTION_KEY, plainName);
			}
		});
		List<Component> displayLore = ItemUtils.getDisplayLore(itemStack);
		displayLore.set(1, loreComponent == null ? Component.text("No Potion Selected", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false) : loreComponent);
		ItemUtils.setDisplayLore(itemStack, displayLore);
		ItemUtils.setPlainLore(itemStack);
	}

	public static String getPotionName(ItemStack itemStack) {
		return NBT.get(itemStack, nbt -> {
			return getPotionKey(ItemStatUtils.getPlayerModified(nbt));
		});
	}

	public static String getPotionKey(@Nullable ReadableNBT playerModified) {
		return playerModified != null ? playerModified.getOrDefault(POTION_KEY, "") : "";
	}

	private static boolean isNotInfinitePotion(ItemStack item) {
		return ItemUtils.isSomePotion(item) && ItemStatUtils.isConsumable(item) && !ItemStatUtils.hasEnchantment(item, EnchantmentType.INFINITY);
	}

	public static boolean isAnyInjector(ItemStack item) {
		return isPotionInjector(item) || isIridiumInjector(item);
	}

	public static boolean isPotionInjector(ItemStack item) {
		return InventoryUtils.testForItemWithName(item, PI_NAME, true);
	}

	public static boolean isIridiumInjector(ItemStack item) {
		return InventoryUtils.testForItemWithName(item, II_NAME, true);
	}
}
