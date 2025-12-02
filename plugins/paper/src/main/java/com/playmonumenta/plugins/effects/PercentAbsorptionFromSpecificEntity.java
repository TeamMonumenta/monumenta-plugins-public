package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.EntityGainAbsorptionEvent;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class PercentAbsorptionFromSpecificEntity extends SingleArgumentEffect {
	public static final String effectID = "PercentAbsorptionFromSpecificEntity";
	public static final String GENERIC_NAME = "PercentAbsorptionFromSpecificEntity";
	private final UUID mSourceId;
	private final String mSourceName;

	public PercentAbsorptionFromSpecificEntity(int duration, double amount, LivingEntity source) {
		this(duration, amount, source.getUniqueId(), source.getName());
	}

	public PercentAbsorptionFromSpecificEntity(int duration, double amount, UUID sourceId, String sourceName) {
		super(duration, amount, effectID);
		mSourceId = sourceId;
		mSourceName = sourceName;
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
	public void entityGainAbsorptionEvent(EntityGainAbsorptionEvent event) {
		@Nullable UUID source = event.getSource();
		if (source == null || !source.equals(mSourceId)) {
			return;
		}

		event.setAmount(event.getAmount() * (1 + mAmount));
		event.setMaxAmount(event.getMaxAmount() * (1 + mAmount));
	}

	public double getValue() {
		return mAmount;
	}

	public static PercentAbsorptionFromSpecificEntity deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		UUID sourceId = UUID.fromString(object.get("sourceId").getAsString());
		String sourceName = object.get("sourceName").getAsString();

		return new PercentAbsorptionFromSpecificEntity(duration, amount, sourceId, sourceName);
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("sourceId", mSourceId.toString());
		object.addProperty("sourceName", mSourceName);

		return object;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount).append(Component.text(" " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Absorption (%s)".formatted(mSourceName);
	}

	@Override
	public String toString() {
		return String.format("PercentAbsorptionFromSpecificEntity duration:%d amount:%f sourceId:%s", this.getDuration(), mAmount, mSourceId.toString());
	}
}
