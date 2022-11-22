package com.playmonumenta.plugins.listeners;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.TrainingDummyBoss;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.overrides.FirmamentOverride;
import com.playmonumenta.plugins.overrides.WorldshaperOverride;
import com.playmonumenta.plugins.overrides.YellowTesseractOverride;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class ShulkerEquipmentListener implements Listener {
	private static final String LOCK_STRING = "AdminEquipmentTool";
	private static final String CHARM_STRING = "CharmLockBox";
	private static final String PORTAL_EPIC_STRING = "PortalEpicBox";

	private static final ImmutableMap<Integer, Integer> SWAP_SLOTS = ImmutableMap.<Integer, Integer>builder()
		.put(0, 0)
		.put(1, 1)
		.put(2, 2)
		.put(3, 3)
		.put(4, 4)
		.put(5, 5)
		.put(6, 6)
		.put(7, 7)
		.put(8, 8)
		.put(36, 9)
		.put(37, 10)
		.put(38, 11)
		.put(39, 12)
		.put(40, 13)
		.build();

	private static final ImmutableMap<Integer, Integer> CHARM_SLOTS = ImmutableMap.<Integer, Integer>builder()
		.put(0, 18)
		.put(1, 19)
		.put(2, 20)
		.put(3, 21)
		.put(4, 22)
		.put(5, 23)
		.put(6, 24)
		.build();


	private final Plugin mPlugin;
	private final Map<UUID, BukkitRunnable> mLockBoxCooldowns = new HashMap<>();
	private final Map<UUID, BukkitRunnable> mCharmBoxCooldowns = new HashMap<>();
	private final Map<UUID, BukkitRunnable> mOmniBoxCooldowns = new HashMap<>();

	public ShulkerEquipmentListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (
			// Must be a right click
			event.getClick() == null ||
				!event.getClick().equals(ClickType.RIGHT) ||
				// Must be placing a single block
				event.getAction() == null ||
				!event.getAction().equals(InventoryAction.PICKUP_HALF) ||
				// Must be a player interacting with their main inventory
				event.getWhoClicked() == null ||
				!(event.getWhoClicked() instanceof Player player) ||
				event.getClickedInventory() == null ||
				// If it's a player inventory, must be in main inventory
				// https://minecraft.gamepedia.com/Player.dat_format#Inventory_slot_numbers
				(event.getClickedInventory() instanceof PlayerInventory && (event.getSlot() < 9 || event.getSlot() > 35)) ||
				// Must be a player inventory, ender chest, or regular chest
				!(event.getClickedInventory() instanceof PlayerInventory ||
					event.getClickedInventory().getType().equals(InventoryType.ENDER_CHEST) ||
					event.getClickedInventory().getType().equals(InventoryType.CHEST)) ||
				// Must be a click on a shulker box with an empty hand
				(event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) ||
				event.getCurrentItem() == null ||
				!ItemUtils.isShulkerBox(event.getCurrentItem().getType())
		) {
			// Nope!
			return;
		}

		if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_PORTABLE_STORAGE)) {
			player.sendMessage(ChatColor.RED + "You can't use this here");
			player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.PLAYERS, 1.0f, 0.6f);
			return;
		}

		if (player.getLocation().getY() < player.getWorld().getMinHeight()) {
			player.sendMessage(ChatColor.RED + "You can't use this in the void");
			player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.PLAYERS, 1.0f, 0.6f);
			return;
		}

		if (EntityUtils.touchesLava(player)) {
			player.sendMessage(ChatColor.RED + "You can't use this in lava");
			player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.PLAYERS, 1.0f, 0.6f);
			return;
		}

		PlayerInventory pInv = player.getInventory();
		ItemStack sboxItem = event.getCurrentItem();

		if (sboxItem != null && ItemUtils.isShulkerBox(sboxItem.getType()) && sboxItem.hasItemMeta()) {
			if (sboxItem.getItemMeta() instanceof BlockStateMeta sMeta && sMeta.getBlockState() instanceof ShulkerBox sbox) {
				if (sbox.isLocked() && sbox.getLock().equals(LOCK_STRING)) {

					//if on cooldown don't swap
					if (checkLockboxSwapCooldown(player)) {
						player.sendMessage(ChatColor.RED + "Lockbox still on cooldown!");
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						event.setCancelled(true);
						return;
					}

					swapEquipment(player, pInv, sbox);

					//check if swapped in radius of boss
					Location loc = player.getLocation();
					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 24)) {
						if (mob.getScoreboardTags().contains("Boss") && !mob.getScoreboardTags().contains(TrainingDummyBoss.identityTag)) {
							player.sendMessage(ChatColor.RED + "Close to boss - Lockbox on 15s cooldown!");
							setLockboxSwapCooldown(player);
						}
					}

					sMeta.setBlockState(sbox);
					sboxItem.setItemMeta(sMeta);

					swapVanity(player, sboxItem);

					player.updateInventory();
					event.setCancelled(true);
					Map<UUID, ItemStatManager.PlayerItemStats> itemStatsMap = mPlugin.mItemStatManager.getPlayerItemStatsMappings();
					if (itemStatsMap.containsKey(player.getUniqueId())) {
						itemStatsMap.get(player.getUniqueId()).updateStats(player, true, player.getMaxHealth(), true);
					}
					InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, null);
				} else if (sbox.isLocked() && sbox.getLock().equals(CHARM_STRING)) {
					//if on cooldown don't swap
					if (checkCharmSwapCooldown(player)) {
						player.sendMessage(ChatColor.RED + "Charm Box still on cooldown!");
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						event.setCancelled(true);
						return;
					}

					swapCharms(player, sbox);

					//check if swapped in radius of boss
					Location loc = player.getLocation();
					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 24)) {
						if (mob.getScoreboardTags().contains("Boss") && !mob.getScoreboardTags().contains(TrainingDummyBoss.identityTag)) {
							player.sendMessage(ChatColor.RED + "Close to boss - Charm Box on 15s cooldown!");
							setCharmSwapCooldown(player);
						}
					}

					sMeta.setBlockState(sbox);
					sboxItem.setItemMeta(sMeta);

					if (CharmManager.getInstance().mPlayerCharms.get(player.getUniqueId()) != null) {
						CharmManager.getInstance().updateCharms(player, CharmManager.getInstance().mPlayerCharms.get(player.getUniqueId()));
					}
					event.setCancelled(true);
				} else if (sbox.isLocked() && sbox.getLock().equals(PORTAL_EPIC_STRING)) {
					//if on cooldown don't swap
					if (checkOmniSwapCooldown(player)) {
						player.sendMessage(ChatColor.RED + "Omni Box still on cooldown!");
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						event.setCancelled(true);
						return;
					}

					// Swap Skills, Equipment, Charms
					swapEquipment(player, pInv, sbox);
					swapCharms(player, sbox);

					sMeta.setBlockState(sbox);
					sboxItem.setItemMeta(sMeta);

					// Shares with Yellow Tesseract Cooldown: Because I am lazy to setup a new cooldown timer thing.
					boolean safeZone = ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.RESIST_5);
					int yellowCooldown = ScoreboardUtils.getScoreboardValue(player, "YellowCooldown").orElse(0);
					// If the player is in a safezone and the Tesseract is on CD, remove CD and continue.
					if (safeZone && yellowCooldown > 0) {
						ScoreboardUtils.setScoreboardValue(player, "YellowCooldown", 0);
						yellowCooldown = 0;
					}

					// If the CD hasn't hit 0, tell the player and exit.
					if (yellowCooldown != 0) {
						player.sendMessage(Component.text("Your items are swapped, but your class skills remain unchanged.", NamedTextColor.AQUA).append(Component.text(ChatColor.AQUA + " (Skill CD: " + ChatColor.YELLOW + "" + yellowCooldown + "" + ChatColor.AQUA + " mins)")));
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					} else {
						swapSkills(player, sboxItem);

						if (!ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.RESIST_5)) {
							ScoreboardUtils.setScoreboardValue(player, "YellowCooldown", 3);
						}
					}

					swapVanity(player, sboxItem);

					player.updateInventory();
					if (CharmManager.getInstance().mPlayerCharms.get(player.getUniqueId()) != null) {
						CharmManager.getInstance().updateCharms(player, CharmManager.getInstance().mPlayerCharms.get(player.getUniqueId()));
					}
					event.setCancelled(true);

					//check if swapped in radius of boss
					Location loc = player.getLocation();
					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 24)) {
						if (mob.getScoreboardTags().contains("Boss") && !mob.getScoreboardTags().contains(TrainingDummyBoss.identityTag)) {
							player.sendMessage(ChatColor.RED + "Close to boss - Omni Box on 15s cooldown!");
							setOmniBoxCooldowns(player);
						}
					}

					Map<UUID, ItemStatManager.PlayerItemStats> itemStatsMap = mPlugin.mItemStatManager.getPlayerItemStatsMappings();
					if (itemStatsMap.containsKey(player.getUniqueId())) {
						itemStatsMap.get(player.getUniqueId()).updateStats(player, true, player.getMaxHealth(), true);
					}
					InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, null);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		if (isEquipmentBox(event.getItem()) || isCharmBox(event.getItem())) {
			event.setCancelled(true);
		}
	}

	public static boolean isEquipmentBox(@Nullable ItemStack sboxItem) {
		if (sboxItem != null && ItemUtils.isShulkerBox(sboxItem.getType()) && sboxItem.hasItemMeta()) {
			if (sboxItem.getItemMeta() instanceof BlockStateMeta sMeta) {
				if (sMeta.getBlockState() instanceof ShulkerBox sbox) {
					if (sbox.isLocked() && (sbox.getLock().equals(LOCK_STRING) || sbox.getLock().equals(PORTAL_EPIC_STRING))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isCharmBox(@Nullable ItemStack sboxItem) {
		if (sboxItem != null && ItemUtils.isShulkerBox(sboxItem.getType()) && sboxItem.hasItemMeta()) {
			if (sboxItem.getItemMeta() instanceof BlockStateMeta sMeta) {
				if (sMeta.getBlockState() instanceof ShulkerBox sbox) {
					if (sbox.isLocked() && (sbox.getLock().equals(CHARM_STRING) || sbox.getLock().equals(PORTAL_EPIC_STRING))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static @Nullable Integer getShulkerSlot(int playerInventorySlot) {
		return SWAP_SLOTS.get(playerInventorySlot);
	}

	@SuppressWarnings("PMD.EmptyIfStmt")
	private void swapEquipment(Player player, PlayerInventory pInv, ShulkerBox sbox) {
		/* Prevent swapping/nesting shulkers */
		for (Map.Entry<Integer, Integer> slot : SWAP_SLOTS.entrySet()) {
			ItemStack item = pInv.getItem(slot.getKey());
			if (item != null && ItemUtils.isShulkerBox(item.getType()) && !FirmamentOverride.isFirmamentItem(item) && !WorldshaperOverride.isWorldshaperItem(item) && !isPotionInjectorItem(item)) {
				player.sendMessage(ChatColor.RED + "You can not store shulker boxes");
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.1f);
				return;
			}
		}

		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Equipment Swapped");
		player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.1f);
		Inventory sInv = sbox.getInventory();

		for (Map.Entry<Integer, Integer> slot : SWAP_SLOTS.entrySet()) {
			ItemStack item = pInv.getItem(slot.getKey());
			if (slot.getKey() >= 36 && slot.getKey() <= 39 && item != null && ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_BINDING) != 0) {
				//Does not swap if armor equipped has curse of binding on it
			} else if (item != null && ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_EPHEMERALITY) != 0) {
				//Doesn't swap with curse of ephemerality either
			} else if (item != null && ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.LOCKED) != 0) {
				//Doesn't swap with Locked either
			} else {
				swapItem(pInv, sInv, slot.getKey(), slot.getValue());
			}
		}

		// Reset player's attack cooldown so it cannot be exploited by swapping from high to low cooldown items of the same type
		PlayerUtils.resetAttackCooldown(player);
	}

	private void swapItem(Inventory from, Inventory to, int fromSlot, int toSlot) {
		ItemStack fromItem = from.getItem(fromSlot);
		ItemStack toItem = to.getItem(toSlot);
		ItemStatUtils.generateItemStats(fromItem);
		ItemStatUtils.generateItemStats(toItem);
		from.setItem(fromSlot, toItem);
		to.setItem(toSlot, fromItem);
	}

	private void swapVanity(Player player, ItemStack shulkerBox) {
		VanityManager.VanityData vanityData = mPlugin.mVanityManager.getData(player);
		if (!vanityData.mLockboxSwapEnabled) {
			return;
		}
		NBTItem nbt = new NBTItem(shulkerBox, true);
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot == EquipmentSlot.HAND) {
				continue;
			}
			NBTCompound vanityItems = nbt.addCompound(ItemStatUtils.MONUMENTA_KEY).addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY).addCompound(ItemStatUtils.VANITY_ITEMS_KEY);
			String slotKey = slot.name().toLowerCase(Locale.ROOT);
			ItemStack newVanity = vanityItems.hasKey(slotKey) ? vanityItems.getItemStack(slotKey) : null;
			if (newVanity != null && (newVanity.getType() == Material.AIR || !VanityManager.isValidVanityItem(player, newVanity, slot))) {
				newVanity = null;
			} else if (newVanity != null && !ItemStatUtils.isClean(newVanity)) {
				ItemUtils.setPlainTag(newVanity);
				ItemStatUtils.generateItemStats(newVanity);
				ItemStatUtils.markClean(newVanity);
			}
			ItemStack oldVanity = vanityData.getEquipped(slot);
			if (oldVanity == null || oldVanity.getType() == Material.AIR) {
				vanityItems.removeKey(slotKey);
			} else {
				vanityItems.setItemStack(slotKey, oldVanity);
			}
			vanityData.equip(slot, newVanity);
		}
	}

	private void swapCharms(Player player, ShulkerBox shulkerBox) {
		List<ItemStack> charmInventory = CharmManager.getInstance().mPlayerCharms.get(player.getUniqueId());
		List<ItemStack> tempInventory = new ArrayList<>();
		if (charmInventory != null) {
			for (ItemStack itemStack : charmInventory) {
				ItemStack charmCopy = new ItemStack(itemStack);
				tempInventory.add(charmCopy);
			}

			charmInventory.clear();
		}
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Charms Swapped");
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 2.0f);

		for (Map.Entry<Integer, Integer> slot : CHARM_SLOTS.entrySet()) {
			int charmSlot = slot.getKey();
			int sBoxSlot = slot.getValue();

			if (shulkerBox.getInventory().getItem(sBoxSlot) != null) {
				ItemStack shulkerBoxCharm = shulkerBox.getInventory().getItem(sBoxSlot);
				if (CharmManager.getInstance().validateCharm(player, shulkerBoxCharm)) {
					// If can equip charm, equip.
					CharmManager.getInstance().addCharm(player, shulkerBoxCharm);
				} else {
					player.sendMessage(ChatColor.RED + "Failed to equip charm " + ItemUtils.getPlainName(shulkerBoxCharm) + ", it has been re-added to your inventory.");
					InventoryUtils.giveItem(player, shulkerBoxCharm);
				}
			}

			if (charmSlot < tempInventory.size()) {
				shulkerBox.getInventory().setItem(sBoxSlot, tempInventory.get(charmSlot));
			} else {
				shulkerBox.getInventory().clear(sBoxSlot);
			}
		}
	}

	private void swapSkills(Player player, ItemStack item) {
		ItemMeta itemMeta = YellowTesseractOverride.generateAbilityLore(player, item);
		YellowTesseractOverride.loadAbilityFromLore(player, item);

		// Set new lore
		item.setItemMeta(itemMeta);

		ItemStatUtils.generateItemStats(item);
		ItemUtils.setPlainTag(item);
	}

	//Set cooldown after swapping in RADIUS 24 blocks of boss
	private void setLockboxSwapCooldown(Player player) {
		BukkitRunnable runnable = mLockBoxCooldowns.remove(player.getUniqueId());
		if (runnable != null) {
			runnable.cancel();
		}
		runnable = new BukkitRunnable() {
			@Override
			public void run() {
				//Don't need to put anything here - method checks if BukkitRunnable is active
				mLockBoxCooldowns.remove(player.getUniqueId());
			}
		};
		runnable.runTaskLater(mPlugin, 20 * 15); //15s cooldown
		mLockBoxCooldowns.put(player.getUniqueId(), runnable);
	}

	private void setCharmSwapCooldown(Player player) {
		BukkitRunnable runnable = mCharmBoxCooldowns.remove(player.getUniqueId());
		if (runnable != null) {
			runnable.cancel();
		}
		runnable = new BukkitRunnable() {
			@Override
			public void run() {
				//Don't need to put anything here - method checks if BukkitRunnable is active
				mCharmBoxCooldowns.remove(player.getUniqueId());
			}
		};
		runnable.runTaskLater(mPlugin, 20 * 15); //15s cooldown
		mCharmBoxCooldowns.put(player.getUniqueId(), runnable);
	}

	private void setOmniBoxCooldowns(Player player) {
		BukkitRunnable runnable = mOmniBoxCooldowns.remove(player.getUniqueId());
		if (runnable != null) {
			runnable.cancel();
		}
		runnable = new BukkitRunnable() {
			@Override
			public void run() {
				//Don't need to put anything here - method checks if BukkitRunnable is active
				mOmniBoxCooldowns.remove(player.getUniqueId());
			}
		};
		runnable.runTaskLater(mPlugin, 20 * 15); //15s cooldown
		mOmniBoxCooldowns.put(player.getUniqueId(), runnable);
	}

	//Returns true if cooldown is up right now
	//False if no cooldowns and the lockbox is activatable now
	private boolean checkLockboxSwapCooldown(Player player) {
		return mLockBoxCooldowns.containsKey(player.getUniqueId()) && !mLockBoxCooldowns.get(player.getUniqueId()).isCancelled();
	}

	private boolean checkCharmSwapCooldown(Player player) {
		return mCharmBoxCooldowns.containsKey(player.getUniqueId()) && !mCharmBoxCooldowns.get(player.getUniqueId()).isCancelled();
	}

	private boolean checkOmniSwapCooldown(Player player) {
		return mOmniBoxCooldowns.containsKey(player.getUniqueId()) && !mOmniBoxCooldowns.get(player.getUniqueId()).isCancelled();
	}

	public static boolean isPotionInjectorItem(ItemStack item) {
		return item != null &&
			       ItemUtils.isShulkerBox(item.getType()) &&
			       item.hasItemMeta() &&
			       item.getItemMeta().hasLore() &&
			(InventoryUtils.testForItemWithName(item, "Potion Injector") || InventoryUtils.testForItemWithName(item, "Iridium Injector"));
	}
}
