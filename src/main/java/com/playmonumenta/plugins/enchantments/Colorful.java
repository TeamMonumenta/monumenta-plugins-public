package com.playmonumenta.plugins.enchantments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * changes color every so often **only works on leather armor base item**
 */
public class Colorful implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Colorful";

	private int ticks = 0;

	List<Color> colors = new ArrayList<>(Arrays.asList(
	                                         Color.LIME,
	                                         Color.PURPLE,
	                                         Color.WHITE,
	                                         Color.RED
	                                     ));

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR);
	}

	@Override
	public int getLevelFromItem(ItemStack item) {
		if (item.getItemMeta() instanceof LeatherArmorMeta) {
			return InventoryUtils.getCustomEnchantLevel(item, getProperty());
		}
		return 0;
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		ticks++;

		int modulo = 10; //once a minute

		if (ticks % modulo == 0) {
			PlayerInventory inv = (PlayerInventory) player.getInventory();
			for (ItemStack item : inv.getArmorContents()) {
				if (item != null) {
					if (getLevelFromItem(item) > 0) { //if armor piece has colorful and right type
						LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
						int index = 0;
						try {
							index = colors.indexOf(meta.getColor()) + 1;
							if (index == colors.size()) {
								index = 0;
							}
						} catch (Exception e) {
							// Nothing to do
						}
						meta.setColor(colors.get(index));
						item.setItemMeta(meta);
					}
				}
			}
		}
	}
}
