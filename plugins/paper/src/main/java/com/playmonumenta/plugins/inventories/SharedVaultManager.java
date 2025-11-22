package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.guis.WalletGui;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.luckperms.GuildPlotUtils;
import com.playmonumenta.plugins.listeners.ShulkerShortcutListener;
import com.playmonumenta.plugins.mail.recipient.PlayerRecipient;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EnchantingTable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.Nullable;

public class SharedVaultManager implements Listener {
	public static final String SHARED_VAULT_NAME = "Shared Vault";
	public static final Permission PERMISSION_SHARED_VAULT = new Permission("monumenta.shared_vault");

	private static final Map<String, Map<Long, Set<WalletBlock>>> mLoadedSharedVaults = new HashMap<>();

	public static boolean isSharedVault(@Nullable Block block) {
		if (block == null) {
			return false;
		}
		return isSharedVault(block.getState());
	}

	public static boolean isSharedVault(@Nullable BlockState blockState) {
		if (!(blockState instanceof EnchantingTable enchantingTable)) {
			return false;
		}
		Component customName = enchantingTable.customName();
		if (customName == null) {
			return false;
		}
		return SHARED_VAULT_NAME.equals(MessagingUtils.plainText(customName));
	}

	public static @Nullable WalletBlock getOrRegisterWallet(BlockState blockState) {
		if (!isSharedVault(blockState)) {
			return null;
		}
		Location loc = blockState.getLocation();
		String worldName = loc.getWorld().getName();
		long chunkKey = loc.getChunk().getChunkKey();
		Set<WalletBlock> vaultsInChunk = mLoadedSharedVaults
			.computeIfAbsent(worldName, k -> new HashMap<>())
			.computeIfAbsent(chunkKey, k -> new HashSet<>());

		for (WalletBlock walletBlock : vaultsInChunk) {
			if (loc.equals(walletBlock.getLocation())) {
				return walletBlock;
			}
		}

		WalletBlock walletBlock = WalletBlock.deserialize(blockState);
		vaultsInChunk.add(walletBlock);
		return walletBlock;
	}

	public static void unregisterWallet(BlockState blockState) {
		if (!isSharedVault(blockState)) {
			return;
		}
		Location loc = blockState.getLocation();
		String worldName = loc.getWorld().getName();
		Map<Long, Set<WalletBlock>> worldVaults = mLoadedSharedVaults.get(worldName);
		if (worldVaults == null) {
			return;
		}

		long chunkKey = loc.getChunk().getChunkKey();
		Set<WalletBlock> chunkVaults = worldVaults.get(chunkKey);
		if (chunkVaults == null) {
			return;
		}

		Iterator<WalletBlock> it = chunkVaults.iterator();
		while (it.hasNext()) {
			WalletBlock vault = it.next();
			if (!loc.equals(vault.getLocation())) {
				continue;
			}

			it.remove();
			if (!chunkVaults.isEmpty()) {
				return;
			}
			worldVaults.remove(chunkKey);

			if (worldVaults.isEmpty()) {
				mLoadedSharedVaults.remove(worldName);
			}
			return;
		}
	}

	public static void unregisterChunk(Chunk chunk) {
		String worldName = chunk.getWorld().getName();
		Map<Long, Set<WalletBlock>> worldVaults = mLoadedSharedVaults.get(worldName);
		if (worldVaults == null) {
			return;
		}

		long chunkKey = chunk.getChunkKey();
		worldVaults.remove(chunkKey);

		if (worldVaults.isEmpty()) {
			mLoadedSharedVaults.remove(worldName);
		}
	}

	public static void unregisterWorld(World world) {
		mLoadedSharedVaults.remove(world.getName());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null) {
			return;
		}
		WalletBlock vault = getOrRegisterWallet(clickedBlock.getState());
		if (vault == null) {
			return;
		}
		Player player = event.getPlayer();
		PlayerInventory playerInventory = player.getInventory();

		if (Action.RIGHT_CLICK_BLOCK.equals(event.getAction())) {
			if (player.isSneaking()) {
				return;
			}
			if (!player.hasPermission(PERMISSION_SHARED_VAULT)) {
				player.sendMessage(Component.text("You do not have permission to use a " + SHARED_VAULT_NAME + ".", NamedTextColor.RED));
				event.setCancelled(true);
				return;
			}
			if (vault.canNotAccess(player)) {
				player.sendMessage(Component.text("You may not access this " + SHARED_VAULT_NAME + ".", NamedTextColor.RED));
				event.setCancelled(true);
				return;
			}
			event.setCancelled(true);
			new WalletGui(
				player,
				vault,
				WalletManager.MAX_SETTINGS,
				Component.text(SHARED_VAULT_NAME, NamedTextColor.DARK_GREEN, TextDecoration.BOLD),
				false
			).open();
			event.setCancelled(true);
		} else if (Action.LEFT_CLICK_BLOCK.equals(event.getAction())) {
			if (ShulkerShortcutListener.isPurpleTesseract(playerInventory.getItemInMainHand())) {
				player.sendMessage(Component.text("You cannot use a purple tesseract on a " + SHARED_VAULT_NAME, NamedTextColor.RED));
				event.setCancelled(true);
				return;
			}
			if (!player.hasPermission(PERMISSION_SHARED_VAULT)) {
				player.sendMessage(Component.text("You do not have permission to use a " + SHARED_VAULT_NAME + ".", NamedTextColor.RED));
				event.setCancelled(true);
				return;
			}
			if (vault.canNotAccess(player)) {
				player.sendMessage(Component.text("You may not access this " + SHARED_VAULT_NAME + ".", NamedTextColor.RED));
				event.setCancelled(true);
				return;
			}
			if (player.isSneaking()) {
				int deposited = 0;
				Map<String, Integer> depositedItems = new TreeMap<>();
				ItemStack[] inventoryItems = player.getInventory().getStorageContents();
				for (int i = 9; i < inventoryItems.length; i++) {
					ItemStack item = inventoryItems[i];
					if (WalletManager.canPutIntoWallet(item, WalletManager.MAX_SETTINGS)) {
						deposited += item.getAmount();
						depositedItems.merge(ItemUtils.getPlainName(item), item.getAmount(), Integer::sum);
						vault.add(player, item);
					}
				}
				if (deposited > 0) {
					String depositedHoverString = depositedItems.entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
						.collect(Collectors.joining("\n"));
					Location loc = clickedBlock.getLocation();
					loc.getWorld().playSound(loc, Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
					player.sendMessage(Component.text(deposited + " item" + (deposited == 1 ? "" : "s") + " deposited into the " + SHARED_VAULT_NAME, NamedTextColor.GOLD)
						.hoverEvent(HoverEvent.showText(Component.text(depositedHoverString, NamedTextColor.GRAY))));
				} else {
					player.sendMessage(Component.text("Nothing to deposit.", NamedTextColor.GOLD));
				}
				event.setCancelled(true);
			}
		}
	}

	// block handling

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		event.blockList().removeIf(SharedVaultManager::isSharedVault);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		event.blockList().removeIf(SharedVaultManager::isSharedVault);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		BlockState blockState = event.getBlock().getState();
		WalletBlock vault = getOrRegisterWallet(blockState);
		if (vault != null) {
			if (vault.canNotAccess(player)) {
				player.sendMessage(Component.text("You may not access this " + SHARED_VAULT_NAME + ".", NamedTextColor.RED));
				event.setCancelled(true);
				return;
			}
			if (!vault.mItems.isEmpty()) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(Component.text("You cannot break a filled " + SHARED_VAULT_NAME + "! Empty it first.", NamedTextColor.RED));
			} else {
				unregisterWallet(blockState);
				File walletBlockFile = WalletBlock.getFile(blockState);
				try {
					FileUtils.deletePathAndEmptyParentFolders(walletBlockFile);
				} catch (Exception ex) {
					String errorMessage = "Failed to delete wallet data at " + walletBlockFile.getPath() + ": " + ex;
					MMLog.warning(errorMessage, ex);
					MonumentaNetworkRelayIntegration.sendAdminMessage(errorMessage);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlockPlaced();
		if (!isSharedVault(block)) {
			return;
		}

		if (!ServerProperties.getSharedVaultEnabled()) {
			player.sendMessage(Component.text("You may not place this " + SHARED_VAULT_NAME + " here.", NamedTextColor.RED));
			event.setCancelled(true);
			return;
		}

		if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneUtils.ZoneProperty.SHOPS_POSSIBLE)) {
			player.sendMessage(Component.text("Using a " + SHARED_VAULT_NAME + " in the player market is disabled. We have logs, but they're not accessible in-game, so we don't recommend this.", NamedTextColor.RED));
			event.setCancelled(true);
			return;
		}

		if (
			GuildPlotUtils.guildPlotChangeVaultOwnerBlocked(player)
				|| GuildPlotUtils.guildPlotUseVaultBlocked(player)
		) {
			player.sendMessage(Component.text("You may not place this " + SHARED_VAULT_NAME + " here.", NamedTextColor.RED));
			event.setCancelled(true);
			return;
		}

		WalletBlock vault = getOrRegisterWallet(block.getState());
		if (vault != null) {
			vault.setOwner(new PlayerRecipient(player.getUniqueId()));
		}
	}

	// load/save/unload handling

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkLoadEvent(ChunkLoadEvent event) {
		Chunk chunk = event.getChunk();
		for (BlockState blockState : chunk.getTileEntities(b -> b.getType() == Material.ENCHANTING_TABLE, false)) {
			if (isSharedVault(blockState)) {
				getOrRegisterWallet(blockState);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void worldUnloadEvent(WorldUnloadEvent event) {
		unregisterWorld(event.getWorld());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		unregisterChunk(event.getChunk());
	}
}
