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
	private static final List<Color> COLORS = new ArrayList<>(Arrays.asList(
			Color.LIME,
			Color.PURPLE,
			Color.WHITE,
			Color.RED
		));

	// The more people have this enchant, the faster it will change
	private int mTicks = 0;

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
		if (item != null && item.getItemMeta() != null && item.getItemMeta() instanceof LeatherArmorMeta) {
			return InventoryUtils.getCustomEnchantLevel(item, getProperty(), useEnchantLevels());
		}
		return 0;
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		mTicks++;

		int modulo = 10; //once a minute

		if (mTicks % modulo == 0) {
			PlayerInventory inv = player.getInventory();
			for (ItemStack item : inv.getArmorContents()) {
				if (item != null) {
					if (getLevelFromItem(item) > 0) { //if armor piece has colorful and right type
						LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
						int index = 0;
						if (COLORS.contains(meta.getColor())) {
							index = COLORS.indexOf(meta.getColor()) + 1;
							if (index == COLORS.size()) {
								index = 0;
							}
						}
						meta.setColor(COLORS.get(index));
						item.setItemMeta(meta);
					}
				}
			}
		}
	}
}
