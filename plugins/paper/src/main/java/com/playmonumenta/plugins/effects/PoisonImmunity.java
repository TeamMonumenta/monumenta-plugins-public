package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class PoisonImmunity extends SingleArgumentEffect {
	public static final String effectID = "PoisonImmunity";

	public PoisonImmunity(int duration, double amount) {
		super(duration, amount, effectID);
	}

	@Override
	public void onPotionEffectApply(LivingEntity entity, PotionEffectApplyEvent event) {
		if (event.getEffect().getType().equals(PotionEffectType.POISON) && event.getEffect().getAmplifier() < mAmount) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onPotionEffectModify(Entity entity, EntityPotionEffectEvent event) {
		if (event.getModifiedType().equals(PotionEffectType.POISON)
			&& event.getNewEffect() != null
			&& event.getNewEffect().getAmplifier() < mAmount
			&& (event.getOldEffect() == null || event.getOldEffect().getAmplifier() <= event.getNewEffect().getAmplifier())) {
			event.setCancelled(true);
		}
	}

	public static PoisonImmunity deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new PoisonImmunity(duration, amount);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text(getDisplayedName() + " " + StringUtils.toRoman((int) mAmount));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Poison Immunity";
	}

	@Override
	public String toString() {
		return String.format("PoisonImmunity duration:%d amount:%d", getDuration(), (int) mAmount);
	}
}
