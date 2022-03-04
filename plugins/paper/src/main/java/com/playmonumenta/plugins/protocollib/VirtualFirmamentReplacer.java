package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.VirtualFirmament;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VirtualFirmamentReplacer extends PacketAdapter {

	public VirtualFirmamentReplacer(Plugin plugin) {
		super(plugin, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		if (!VirtualFirmament.isEnabled(player) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			// Creative mode directly sends items to the server, so would break Firmaments.
			// Spectators don't see items anyway so skip them.
			return;
		}
		PacketContainer packet = event.getPacket();
		if (packet.getType().equals(PacketType.Play.Server.WINDOW_ITEMS)) {
			// doc: https://wiki.vg/Protocol#Window_Items
			if (packet.getIntegers().read(0) != 0) {
				// first int (should be a byte?) is the window ID, with ID 0 being the player inventory
				return;
			}
			for (List<ItemStack> items : packet.getItemListModifier().getValues()) {
				for (ItemStack item : items.subList(36, 46)) { // hotbar and offhand
					processItem(item);
				}
			}
		} else { // PacketType.Play.Server.SET_SLOT
			// doc: https://wiki.vg/Protocol#Set_Slot
			if (packet.getIntegers().read(0) != 0) {
				// first int (should be a byte?) is the window ID, with ID 0 being the player inventory
				return;
			}
			// second integer (should be first short?) is the slot ID
			int slot = packet.getIntegers().read(1);
			if (36 <= slot && slot < 46) {
				for (ItemStack itemStack : packet.getItemModifier().getValues()) {
					processItem(itemStack);
				}
			}
		}
	}

	public static boolean isVirtualFirmament(ItemStack itemStack) {
		return (itemStack.getType() == Material.PRISMARINE && "Firmament".equals(ItemUtils.getPlainNameIfExists(itemStack)))
			       || (itemStack.getType() == Material.BLACKSTONE && "Doorway from Eternity".equals(ItemUtils.getPlainNameIfExists(itemStack)));
	}

	private void processItem(ItemStack itemStack) {
		if (ItemUtils.isShulkerBox(itemStack.getType())) {
			String plainName = ItemUtils.getPlainNameIfExists(itemStack);
			if ("Firmament".equals(plainName)) {
				itemStack.setType(Material.PRISMARINE);
				itemStack.setAmount(64);
			} else if ("Doorway from Eternity".equals(plainName)) {
				itemStack.setType(Material.BLACKSTONE);
				itemStack.setAmount(64);
			}
		}
	}

}
