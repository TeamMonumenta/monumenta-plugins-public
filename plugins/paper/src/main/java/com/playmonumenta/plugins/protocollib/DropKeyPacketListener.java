package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayInBlockDigHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * Listens for the drop key being pressed and cancels it, then triggers any abilities using that key as trigger.
 */
public class DropKeyPacketListener extends PacketAdapter {
	public DropKeyPacketListener(Plugin plugin) {
		super(plugin, PacketType.Play.Client.BLOCK_DIG);
	}

	@Override
	public void onPacketReceiving(PacketEvent event) {

		PacketPlayInBlockDigHandle packet = PacketPlayInBlockDigHandle.createHandle(event.getPacket().getHandle());

		PacketPlayInBlockDigHandle.EnumPlayerDigTypeHandle digType = packet.getDigType();
		if (PacketPlayInBlockDigHandle.EnumPlayerDigTypeHandle.DROP_ITEM.equals(digType)
			    || PacketPlayInBlockDigHandle.EnumPlayerDigTypeHandle.DROP_ALL_ITEMS.equals(digType)) {
			Player player = event.getPlayer();
			if (player.getGameMode() == GameMode.CREATIVE) {
				// Allow dropping in creative mode. Accessing the player's game mode should be reasonably thread-safe.
				return;
			}
			event.setCancelled(true);
			// Drop key causes a left click that needs to be ignored
			Plugin.getInstance().mAbilityManager.preDropKey(player);
			Bukkit.getScheduler().runTask(plugin, () -> {
				Plugin.getInstance().mAbilityManager.checkTrigger(player, AbilityTrigger.Key.DROP);
				player.updateInventory();
			});
		}

	}
}
