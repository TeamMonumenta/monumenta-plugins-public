package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BezoarCS implements CosmeticSkill {

	public static final ImmutableMap<String, BezoarCS> SKIN_LIST = ImmutableMap.<String, BezoarCS>builder()
		.put(SunriseBrewCS.NAME, new SunriseBrewCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.BEZOAR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.LIME_CONCRETE;
	}

	public ItemStack bezoarItem() {
		ItemStack itemBezoar = new ItemStack(Material.LIME_CONCRETE);
		ItemMeta bezoarMeta = itemBezoar.getItemMeta();
		bezoarMeta.displayName(Component.text("Bezoar", NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false));
		itemBezoar.setItemMeta(bezoarMeta);
		ItemUtils.setPlainName(itemBezoar, "Bezoar");
		return itemBezoar;
	}
}
