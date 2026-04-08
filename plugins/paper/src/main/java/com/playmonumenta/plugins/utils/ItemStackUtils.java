package com.playmonumenta.plugins.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemStackUtils {

	public static ItemStack withTypePreserveName(ItemStack itemStack, Material material) {
		ItemStack newItemStack = itemStack.withType(material);
		newItemStack.editMeta(itemMeta ->
			itemMeta.displayName(itemStack.getItemMeta().displayName())
		);
		return newItemStack;
	}

}
