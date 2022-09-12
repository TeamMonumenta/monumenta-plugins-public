package com.playmonumenta.plugins.cosmetics;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Cosmetic {
	public String mName;
	public CosmeticType mType;
	public boolean mEquipped;

	//Lazy solution for cosmetic skill system
	public ClassAbility mAbility;

	public Cosmetic(CosmeticType type, String name) {
		mName = name;
		mType = type;
		mEquipped = false;
		mAbility = null;
	}

	public Cosmetic(CosmeticType type, String name, boolean isEquipped) {
		mName = name;
		mType = type;
		mEquipped = isEquipped;
		mAbility = null;
	}

	public Cosmetic(CosmeticType type, String name, boolean isEquipped, ClassAbility ability) {
		this(type, name, isEquipped);
		mAbility = ability;
	}

	public String getName() {
		return mName;
	}

	public CosmeticType getType() {
		return mType;
	}

	public boolean isEquipped() {
		return mEquipped;
	}

	public ClassAbility getAbility() {
		return mAbility;
	}

	public ItemStack getDisplayItem() {
		ItemStack cosmeticItem;
		switch (mType) {
			case VANITY -> {
				String[] split = mName.split(":", 2);
				Material mat;
				try {
					mat = Material.valueOf(split[0].toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException e) {
					mat = Material.BARRIER;
				}
				cosmeticItem = new ItemStack(mat);
				ItemMeta meta = cosmeticItem.getItemMeta();
				if (split.length > 1 && !split[1].isEmpty()) {
					meta.displayName(Component.text(split[1], NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
				}
				String slot = switch (ItemUtils.getEquipmentSlot(mat)) {
					case HEAD -> "Head";
					case CHEST -> "Chest";
					case LEGS -> "Legs";
					case FEET -> "Feet";
					default -> "Offhand";
				};
				meta.lore(List.of(Component.text("Base material: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
						.append(Component.translatable(mat.getTranslationKey())),
					Component.text("Slot: " + slot, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				meta.addItemFlags(ItemFlag.values());
				cosmeticItem.setItemMeta(meta);
			}
			default -> {
				cosmeticItem = new ItemStack(mEquipped ? Material.GREEN_CONCRETE : mType.getDisplayItem(mName), 1);

				ItemMeta meta = cosmeticItem.getItemMeta();
				meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

				meta.displayName(Component.text(mName, TextColor.color(0xc0dea9)).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				List<Component> lore = new ArrayList<>();
				lore.add(Component.text("Custom " + mType.getDisplayName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
				if (mEquipped) {
					lore.add(Component.text("Currently Equipped", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
				} else {
					lore.add(Component.text("Unequipped", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
				}

				meta.lore(lore);
				cosmeticItem.setItemMeta(meta);
			}
		}
		ItemUtils.setPlainTag(cosmeticItem);
		return cosmeticItem;
	}

}
