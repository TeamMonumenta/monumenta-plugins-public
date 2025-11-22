package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Nameable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class RenameOnPlaceListener implements Listener {
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Block block = event.getBlockPlaced();
		BlockState blockState = block.getState();

		if (blockState instanceof Nameable nameable) {
			Component customName = nameable.customName();
			if (customName == null) {
				return;
			}

			String plainName = MessagingUtils.plainText(customName);

			if (ServerProperties.getFormatingFreeBlockNames().contains(plainName)) {
				nameable.customName(Component.text(plainName));
				blockState.update();
				event.getPlayer().sendActionBar(Component.text("Placed a " + plainName));
			}
		}
	}
}
