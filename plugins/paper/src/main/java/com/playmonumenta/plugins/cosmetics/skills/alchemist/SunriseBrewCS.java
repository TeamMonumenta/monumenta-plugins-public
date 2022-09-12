package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SunriseBrewCS extends BezoarCS {
	// Change bezoar item into sun drop style. Depth set: dawnbringer

	public static final String NAME = "Sunrise Brew";

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.BEZOAR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.HONEYCOMB_BLOCK;
	}

	@Override
	public ItemStack bezoarItem() {
		ItemStack itemBezoar = new ItemStack(Material.HONEYCOMB_BLOCK);
		ItemUtils.setPlainName(itemBezoar, "Sundrop");
		ItemMeta sundropMeta = itemBezoar.getItemMeta();
		sundropMeta.displayName(Component.text("Sundrop", NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false));
		itemBezoar.setItemMeta(sundropMeta);
		return itemBezoar;
	}
}
