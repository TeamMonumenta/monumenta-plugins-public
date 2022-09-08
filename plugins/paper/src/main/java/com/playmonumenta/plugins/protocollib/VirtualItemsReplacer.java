package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutSetSlotHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutWindowItemsHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.VirtualFirmament;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class VirtualItemsReplacer extends PacketAdapter {

	public static final String IS_VIRTUAL_ITEM_NBT_KEY = "IsVirtualItem";

	private final Plugin mPlugin;

	public VirtualItemsReplacer(Plugin plugin) {
		super(plugin, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT);
		mPlugin = plugin;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			// Creative mode directly sends items to the server, so would break virtual items.
			// Spectators don't see items anyway so skip them.
			return;
		}
		PacketContainer packet = event.getPacket();
		if (packet.getType().equals(PacketType.Play.Server.WINDOW_ITEMS)) {
			PacketPlayOutWindowItemsHandle handle = PacketPlayOutWindowItemsHandle.createHandle(packet.getHandle());
			if (handle.getWindowId() != 0) { //  0 = player inventory
				return;
			}
			List<ItemStack> items = handle.getItems();
			for (int i = 0; i < items.size(); i++) {
				ItemStack item = items.get(i);
				processItem(item, i, player);
			}
		} else { // PacketType.Play.Server.SET_SLOT
			PacketPlayOutSetSlotHandle handle = PacketPlayOutSetSlotHandle.createHandle(packet.getHandle());
			if (handle.getWindowId() != 0) { //  0 = player inventory
				return;
			}
			processItem(handle.getItem(), handle.getSlot(), player);
		}
	}

	private void processItem(ItemStack itemStack, int slot, Player player) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return;
		}
		if (36 <= slot && slot < 46 // hotbar or offhand
			    && ItemUtils.isShulkerBox(itemStack.getType())
			    && VirtualFirmament.isEnabled(player)) {
			String plainName = ItemUtils.getPlainNameIfExists(itemStack);
			if ("Firmament".equals(plainName)) {
				itemStack.setType(Material.PRISMARINE);
				itemStack.setAmount(64);
				markVirtual(itemStack);
				return;
			} else if ("Doorway from Eternity".equals(plainName)) {
				itemStack.setType(Material.BLACKSTONE);
				itemStack.setAmount(64);
				markVirtual(itemStack);
				return;
			}
		}
		if ((5 <= slot && slot <= 8) || slot == 45) { // armor or offhand
			VanityManager.VanityData vanityData = mPlugin.mVanityManager.getData(player);
			if (vanityData != null && vanityData.mSelfVanityEnabled) {
				EquipmentSlot equipmentSlot = switch (slot) {
					case 5 -> EquipmentSlot.HEAD;
					case 6 -> EquipmentSlot.CHEST;
					case 7 -> EquipmentSlot.LEGS;
					case 8 -> EquipmentSlot.FEET;
					default -> EquipmentSlot.OFF_HAND;
				};
				VanityManager.applyVanity(itemStack, vanityData, equipmentSlot, true);
			}
		}
	}

	public static boolean isVirtualItem(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return false;
		}
		NBTCompound monumenta = new NBTItem(itemStack).getCompound(ItemStatUtils.MONUMENTA_KEY);
		return monumenta != null && Boolean.TRUE.equals(monumenta.getBoolean(IS_VIRTUAL_ITEM_NBT_KEY));
	}

	public static void markVirtual(ItemStack item) {
		new NBTItem(item, true).addCompound(ItemStatUtils.MONUMENTA_KEY).setBoolean(IS_VIRTUAL_ITEM_NBT_KEY, true);
	}

}
