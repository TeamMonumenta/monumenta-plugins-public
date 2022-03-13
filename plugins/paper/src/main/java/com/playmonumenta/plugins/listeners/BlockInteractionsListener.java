package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.EnumSet;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class BlockInteractionsListener implements Listener {
	public static final String COMMAND = "blockinteractions";
	public static final String ALIAS = "bi";

	private static final String DISABLE_TAG = "DisableBlockInteractions";

	private static final EnumSet<Material> INTERACTABLES = EnumSet.of(
		Material.WHITE_BED,
		Material.ORANGE_BED,
		Material.MAGENTA_BED,
		Material.LIGHT_BLUE_BED,
		Material.YELLOW_BED,
		Material.LIME_BED,
		Material.PINK_BED,
		Material.GRAY_BED,
		Material.LIGHT_GRAY_BED,
		Material.CYAN_BED,
		Material.PURPLE_BED,
		Material.BLUE_BED,
		Material.BROWN_BED,
		Material.GREEN_BED,
		Material.RED_BED,
		Material.BLACK_BED,
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
		Material.BREWING_STAND
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
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Interactions with blocks have been disabled.");
		} else {
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Interactions with blocks have been enabled.");
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		if (block != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && !player.isSneaking() && !ServerProperties.getIsTownWorld() && !player.getInventory().getItemInMainHand().getType().isAir() && player.getScoreboardTags().contains(DISABLE_TAG) && INTERACTABLES.contains(block.getType())) {
			event.setCancelled(true);
		}
	}
}
