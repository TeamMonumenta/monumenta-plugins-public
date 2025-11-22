package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CustomRegeneration extends Effect {
	public static final String effectID = "CustomRegeneration";

	private final double mAmount;
	private final int mInterval;
	private final @Nullable Player mSourcePlayer;
	private final Plugin mPlugin;
	private final boolean mNormalizedDisplay;

	private int mTicks;

	public CustomRegeneration(int duration, double amount, Plugin plugin) {
		this(duration, amount, null, plugin);
	}

	public CustomRegeneration(int duration, double amount, @Nullable Player sourcePlayer, Plugin plugin) {
		this(duration, amount, 20, sourcePlayer, false, plugin);
	}

	public CustomRegeneration(int duration, double amount, int interval, @Nullable Player sourcePlayer, boolean normalizedDisplay, Plugin plugin) {
		super(duration, effectID);
		mAmount = amount;
		mSourcePlayer = sourcePlayer;
		mPlugin = plugin;
		mInterval = interval;
		mTicks = Bukkit.getCurrentTick() % interval;
		mNormalizedDisplay = normalizedDisplay;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			mTicks += 5;
		}
		if (mTicks >= mInterval) {
			if (entity instanceof Player player) {
				PlayerUtils.healPlayer(mPlugin, player, mAmount, mSourcePlayer);
			}
			mTicks -= mInterval;
		}
	}

	@Override
	public double getMagnitude() {
		return mAmount / mInterval;
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);
		object.addProperty("interval", mInterval);
		object.addProperty("normalizedDisplay", mNormalizedDisplay);

		if (mSourcePlayer != null) {
			object.addProperty("sourcePlayer", mSourcePlayer.getUniqueId().toString());
		}

		return object;
	}

	public static CustomRegeneration deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		int interval = object.has("interval") ? object.get("interval").getAsInt() : 20;
		boolean simplifiedDisplay = object.has("normalizedDisplay") && object.get("normalizedDisplay").getAsBoolean();

		@Nullable Player sourcePlayer = null;
		if (object.has("sourcePlayer")) {
			sourcePlayer = plugin.getPlayer(UUID.fromString(object.get("sourcePlayer").getAsString()));
		}

		return new CustomRegeneration(duration, amount, interval, sourcePlayer, simplifiedDisplay, plugin);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text("+" + StringUtils.to2DP(mNormalizedDisplay ? mAmount / mInterval * 20 : mAmount) + getDisplayedName());
	}

	@Override
	public @Nullable String getDisplayedName() {
		String time = mNormalizedDisplay || mInterval == 20 ? "s" : StringUtils.ticksToSeconds(mInterval) + "s";
		return "/" + time + " Regeneration";
	}

	@Override
	public String toString() {
		return String.format("CustomRegeneration duration:%d amount:%f interval:%d", this.getDuration(), mAmount, mInterval);
	}

}
