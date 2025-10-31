package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockInteractionsListener implements Listener {
	public static final String COMMAND = "blockinteractions";
	public static final String ALIAS = "bi";

	public static final String DISABLE_TAG = "DisableBlockInteractions";

	private static final EnumSet<Material> INTERACTABLES = EnumSet.of(
		Material.DECORATED_POT,
		Material.LOOM,
		Material.CRAFTING_TABLE,
		Material.STONECUTTER,
		Material.DISPENSER,
		Material.FURNACE,
		Material.BLAST_FURNACE,
		Material.SMOKER,
		Material.BARREL,
		Material.CARTOGRAPHY_TABLE,
		Material.SMITHING_TABLE,
		Material.FLETCHING_TABLE,
		Material.BREWING_STAND,
		Material.LECTERN
	);

	// Armor stands are handled using PlayerArmorStandManipulateEvent instead of the interact event, and are thus not part of this set.
	private static final EnumSet<EntityType> INTERACTABLE_ENTITIES = EnumSet.of(
		EntityType.ITEM_FRAME,
		EntityType.GLOW_ITEM_FRAME
	);

	public BlockInteractionsListener() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.blockinteractions");

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.executesPlayer((sender, args) -> {
				playerToggle(sender);
			})
			.register();
	}

	private void playerToggle(Player player) {
		if (ScoreboardUtils.toggleTag(player, DISABLE_TAG)) {
			player.sendMessage(Component.text("Interactions with blocks, item frames, and armor stands have been disabled.", NamedTextColor.GOLD, TextDecoration.BOLD));
		} else {
			player.sendMessage(Component.text("Interactions with blocks, item frames, and armor stands have been enabled.", NamedTextColor.GOLD, TextDecoration.BOLD));
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		if (block == null) {
			return;
		}
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		Player player = event.getPlayer();
		Material type = block.getType();
		if (checkAction(block, player) && INTERACTABLES.contains(type)) {
			event.setCancelled(true);
		} else if (type == Material.JUKEBOX && !player.isSneaking() && Tag.ITEMS_MUSIC_DISCS.isTagged(player.getInventory().getItemInMainHand().getType()) && commonChecks(player)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		Player player = event.getPlayer();
		if (checkEntityAction(entity, player.getInventory().getItem(event.getHand()), player) && INTERACTABLE_ENTITIES.contains(entity.getType())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerOpenSignEvent(PlayerOpenSignEvent event) {
		Player player = event.getPlayer();

		Location blockLoc = event.getSign().getLocation();
		Material realSignType = blockLoc.getBlock().getType();
		if (!Tag.ALL_SIGNS.getValues().contains(realSignType)) {
			// Likely a sign GUI, like the wallet; these don't actually place a sign in the world, but fake it per-player
			return;
		}

		if (commonChecks(player)) {
			event.setCancelled(true);
		}
	}

	// Called in PlayerListener
	public static void playerEnteredNonTeleporterBed(PlayerBedEnterEvent event) {
		if (checkAction(event.getBed(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public static void playerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
		if (checkEntityAction(event.getRightClicked(), event.getPlayerItem(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	private static boolean checkEntityAction(@Nullable Entity entity, @Nullable ItemStack playerItem, Player player) {
		return entity != null
			&& playerItem != null // null/empty hand: takes an item from an armor stand and is allowed
			&& !playerItem.getType().isAir()
			&& ItemStatUtils.getTier(playerItem) != Tier.KEYTIER // allow placing key items in item frames and on armor stands
			&& commonChecks(player);
	}

	private static boolean checkAction(Block block, Player player) {
		return block != null
			&& !player.isSneaking()
			&& !player.getInventory().getItemInMainHand().getType().isAir() // allow interactions with an empty hand as an alternative to turning this feature off
			&& commonChecks(player);
	}

	private static boolean commonChecks(Player player) {
		return player.getGameMode() == GameMode.SURVIVAL
			&& !ServerProperties.getIsTownWorld()
			&& player.getScoreboardTags().contains(DISABLE_TAG);
	}
}
