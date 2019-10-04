package com.playmonumenta.plugins.overrides;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class MapOverride extends BaseOverride {
	public static EnumSet<Material> BANNERS = EnumSet.of(
			Material.WHITE_BANNER,
			Material.ORANGE_BANNER,
			Material.MAGENTA_BANNER,
			Material.LIGHT_BLUE_BANNER,
			Material.YELLOW_BANNER,
			Material.LIME_BANNER,
			Material.PINK_BANNER,
			Material.GRAY_BANNER,
			Material.LIGHT_GRAY_BANNER,
			Material.CYAN_BANNER,
			Material.PURPLE_BANNER,
			Material.BLUE_BANNER,
			Material.BROWN_BANNER,
			Material.GREEN_BANNER,
			Material.RED_BANNER,
			Material.BLACK_BANNER,
			Material.WHITE_WALL_BANNER,
			Material.ORANGE_WALL_BANNER,
			Material.MAGENTA_WALL_BANNER,
			Material.LIGHT_BLUE_WALL_BANNER,
			Material.YELLOW_WALL_BANNER,
			Material.LIME_WALL_BANNER,
			Material.PINK_WALL_BANNER,
			Material.GRAY_WALL_BANNER,
			Material.LIGHT_GRAY_WALL_BANNER,
			Material.CYAN_WALL_BANNER,
			Material.PURPLE_WALL_BANNER,
			Material.BLUE_WALL_BANNER,
			Material.BROWN_WALL_BANNER,
			Material.GREEN_WALL_BANNER,
			Material.RED_WALL_BANNER,
			Material.BLACK_WALL_BANNER
		);

	private boolean canUseMap(Player player, ItemStack mapItem) {
		GameMode playerMode = player.getGameMode();
		if (playerMode.equals(GameMode.ADVENTURE) || playerMode.equals(GameMode.SPECTATOR)) {
			player.sendMessage(ChatColor.RED + "You can not place maps in town item frames");
			return false;
		} else if (playerMode.equals(GameMode.CREATIVE)) {
			return true;
		}

		if (InventoryUtils.testForItemWithLore(mapItem, "Official Map")) {
			player.sendMessage(ChatColor.RED + "You can not modify official maps");
			return false;
		}

		return true;
	}

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (BANNERS.contains(block.getType())) {
			return canUseMap(player, item);
		}

		return true;
	}

	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack itemInHand) {
		if (clickedEntity instanceof ItemFrame) {
			return canUseMap(player, itemInHand);
		}

		return true;
	}
}
