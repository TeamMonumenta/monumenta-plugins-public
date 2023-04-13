package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockInteractionsListener implements Listener {
	public static final String COMMAND = "blockinteractions";
	public static final String ALIAS = "bi";

	private static final String DISABLE_TAG = "DisableBlockInteractions";

	private static final EnumSet<Material> INTERACTABLES = EnumSet.of(
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
			player.sendMessage(Component.text("Interactions with blocks have been disabled.", NamedTextColor.GOLD, TextDecoration.BOLD));
		} else {
			player.sendMessage(Component.text("Interactions with blocks have been enabled.", NamedTextColor.GOLD, TextDecoration.BOLD));
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		if (checkAction(block, player) && event.getAction() == Action.RIGHT_CLICK_BLOCK && INTERACTABLES.contains(block.getType())) {
			event.setCancelled(true);
		}
	}

	// Called in PlayerListener
	public static void playerEnteredNonTeleporterBed(PlayerBedEnterEvent event) {
		if (checkAction(event.getBed(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	private static boolean checkAction(@Nullable Block block, Player player) {
		return block != null && !player.isSneaking() && player.getGameMode() == GameMode.SURVIVAL && !ServerProperties.getIsTownWorld() && !player.getInventory().getItemInMainHand().getType().isAir() && player.getScoreboardTags().contains(DISABLE_TAG);
	}
}
