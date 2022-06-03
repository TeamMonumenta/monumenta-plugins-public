package com.playmonumenta.plugins.protocollib;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

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
				for (int i = 0; i < items.size(); i++) {
					ItemStack item = items.get(i);
					processItem(item, i, player);
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
			for (ItemStack itemStack : packet.getItemModifier().getValues()) {
				processItem(itemStack, slot, player);
			}
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
				ItemStack vanityItem = vanityData.getEquipped(equipmentSlot);
				if (vanityItem != null && vanityItem.getType() != Material.AIR) {
					if (equipmentSlot == EquipmentSlot.OFF_HAND
						    && (itemStack.getMaxItemUseDuration() > 0 || vanityItem.getMaxItemUseDuration() > 0)
						    && itemStack.getType() != vanityItem.getType()) {
						// don't allow changing item type of useable items (e.g. food, shields) to prevent not being slowed down while using them or just messing with their use in general
						return;
					}
					boolean invisible = VanityManager.isInvisibleVanityItem(vanityItem);
					if (invisible && equipmentSlot != EquipmentSlot.OFF_HAND) { // invisible armor: only add tag for RP and add lore line
						NBTItem nbt = new NBTItem(itemStack, true);
						nbt.addCompound(ItemStatUtils.MONUMENTA_KEY).setBoolean(VanityManager.INVISIBLE_NBT_KEY, true);
						ItemMeta meta = itemStack.getItemMeta();
						if (meta != null) {
							List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
							lore.add(Component.text("Invisibility vanity skin applied", NamedTextColor.GRAY));
							meta.lore(lore);
							itemStack.setItemMeta(meta);
						}
						return;
					}
					ItemMeta vanityMeta = vanityItem.getItemMeta();
					if (vanityMeta == null) {
						return;
					}
					ItemMeta originalMeta = itemStack.getItemMeta();

					// copy over durability, adjusted for potentially changed max durability
					if (vanityMeta instanceof Damageable vanityDamage
						    && vanityItem.getType().getMaxDurability() > 0
						    && originalMeta instanceof Damageable originalDamage
						    && itemStack.getType().getMaxDurability() > 0
						    && !originalMeta.isUnbreakable()) {
						vanityMeta.setUnbreakable(false);
						vanityDamage.setDamage((int) Math.round(1.0 * vanityItem.getType().getMaxDurability() * originalDamage.getDamage() / itemStack.getType().getMaxDurability()));
					}

					// copy display name and lore, but not plain ones
					if (originalMeta != null) {
						vanityMeta.displayName(originalMeta.displayName());
					}
					List<Component> lore = originalMeta == null || originalMeta.lore() == null ? new ArrayList<>() : new ArrayList<>(originalMeta.lore());
					lore.add(Component.text(invisible ? "Invisibility vanity skin applied" : "Vanity skin applied", NamedTextColor.GRAY));
					// add durability lore line if vanity item is unbreakable
					if (originalMeta instanceof Damageable originalDamage
						    && itemStack.getType().getMaxDurability() > 0
						    && !originalMeta.isUnbreakable()
						    && (!(vanityMeta instanceof Damageable) || vanityItem.getType().getMaxDurability() <= 0)) {
						lore.add(Component.translatable("item.durability", NamedTextColor.WHITE,
							Component.text(itemStack.getType().getMaxDurability() - originalDamage.getDamage()),
							Component.text(itemStack.getType().getMaxDurability())
						).decoration(TextDecoration.ITALIC, false));
					}
					vanityMeta.lore(lore);

					// copy attributes
					for (EquipmentSlot s : EquipmentSlot.values()) {
						vanityMeta.removeAttributeModifier(s);
					}
					if (originalMeta != null) {
						for (Map.Entry<Attribute, AttributeModifier> entry : originalMeta.getAttributeModifiers(equipmentSlot).entries()) {
							vanityMeta.addAttributeModifier(entry.getKey(), entry.getValue());
						}
					}

					// copy enchantments
					vanityMeta.getEnchants().keySet().forEach(vanityMeta::removeEnchant);
					if (originalMeta != null) {
						originalMeta.getEnchants().forEach((ench, level) -> vanityMeta.addEnchant(ench, level, true));
					}

					// merge flags
					if (originalMeta != null) {
						vanityMeta.addItemFlags(originalMeta.getItemFlags().toArray(ItemFlag[]::new));
					}

					itemStack.setType(vanityItem.getType());
					itemStack.setItemMeta(vanityMeta);
					markVirtual(itemStack);

					if (invisible) {
						NBTItem nbt = new NBTItem(itemStack, true);
						nbt.addCompound(ItemStatUtils.MONUMENTA_KEY).setBoolean(VanityManager.INVISIBLE_NBT_KEY, true);
					}
				}
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

	private void markVirtual(ItemStack item) {
		new NBTItem(item, true).addCompound(ItemStatUtils.MONUMENTA_KEY).setBoolean(IS_VIRTUAL_ITEM_NBT_KEY, true);
	}

}
