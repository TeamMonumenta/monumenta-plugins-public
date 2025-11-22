package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.EffectTypeApplyFromPotionEvent;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class UamielPetrification extends ZeroArgumentEffect {
	public static final String effectID = "UamielPetrification";

	public UamielPetrification(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityApplyEffectTypeFromPotion(Entity entity, EffectTypeApplyFromPotionEvent event) {
		ItemStack itemStack = event.getItem();
		if (itemStack.getType() == Material.POTION && !ItemStatUtils.hasEnchantment(itemStack, EnchantmentType.INFINITY)) {
			setDuration(0);
		}
	}

	@Override
	public String getDisplayedName() {
		return "Petrifying...";
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		String display = getDisplayedName();
		if (display == null) {
			return null;
		}
		return Component.text(display, Uamiel.TEXT_COLOR);
	}

	public static UamielPetrification deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new UamielPetrification(duration);
	}
}
