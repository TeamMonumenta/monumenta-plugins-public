package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.plugins.custominventories.PlayerDisplayCustomInventory;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.Plugin;

public class RecipeBookGUIOpener extends PacketAdapter {
	public static final String DISABLE_TAG = "RecipeBookGUIDisable";

	private final Plugin mPlugin;

	public RecipeBookGUIOpener(Plugin plugin) {
		super(plugin, PacketType.Play.Client.RECIPE_SETTINGS);
		mPlugin = plugin;
	}

	@Override
	public void onPacketReceiving(PacketEvent event) {
		Player player = event.getPlayer();
		InventoryType inventoryType = player.getOpenInventory().getType();
		if (InventoryType.CRAFTING.equals(inventoryType) && !ScoreboardUtils.checkTag(player, DISABLE_TAG)) {
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				new PlayerDisplayCustomInventory(player, player).openInventory(player, mPlugin);
			});
		}
	}

}
