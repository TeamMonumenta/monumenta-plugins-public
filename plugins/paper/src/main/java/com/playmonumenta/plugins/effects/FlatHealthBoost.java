package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class FlatHealthBoost extends Effect {
	public static final String effectID = "FlatHealthBoost";
	public static final String GENERIC_NAME = "FlatHealthBoost";

	public final double mAmount;
	private final String mModifierName;


	public FlatHealthBoost(int duration, double amount, String modifierName) {
		super(duration, effectID);
		mAmount = amount;
		mModifierName = modifierName;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean isDebuff() {
		return mAmount < 0;
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof LivingEntity le) {
			EntityUtils.replaceAttribute(le, Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(mModifierName, mAmount, AttributeModifier.Operation.ADD_NUMBER));
			le.setHealth(Math.min(le.getHealth(), EntityUtils.getMaxHealth(le)));
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof LivingEntity le) {
			EntityUtils.removeAttribute(le, Attribute.GENERIC_MAX_HEALTH, mModifierName);
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		//make sure it gets removed as its a difficult mod fix otherwise
		Entity entity = event.getEntity();
		entityLoseEffect(entity);
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);
		object.addProperty("modifierName", mModifierName);

		return object;
	}

	public static FlatHealthBoost deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		String modName = object.get("modifierName").getAsString();
		return new FlatHealthBoost(duration, amount, modName);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		if (mAmount < 0) {
			return Component.text(mAmount + " " + getDisplayedName(), NamedTextColor.RED);
		} else {
			return Component.text("+" + mAmount + " " + getDisplayedName(), NamedTextColor.GREEN);
		}
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Max Health";
	}

	@Override
	public String toString() {
		return String.format("FlatHealthBoost duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
