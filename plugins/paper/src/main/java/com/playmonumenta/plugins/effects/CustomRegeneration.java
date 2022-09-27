package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class CustomRegeneration extends Effect {
	public static final String effectID = "CustomRegeneration";

	private final double mAmount;
	private final @Nullable Player mSourcePlayer;
	private final Plugin mPlugin;

	public CustomRegeneration(int duration, double amount, Plugin plugin) {
		this(duration, amount, null, plugin);
	}

	public CustomRegeneration(int duration, double amount, @Nullable Player sourcePlayer, Plugin plugin) {
		super(duration, effectID);
		mAmount = amount;
		mSourcePlayer = sourcePlayer;
		mPlugin = plugin;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			if (entity instanceof Player player) {
				PlayerUtils.healPlayer(mPlugin, player, mAmount, mSourcePlayer);
			}
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		if (mSourcePlayer != null) {
			object.addProperty("sourcePlayer", mSourcePlayer.getUniqueId().toString());
		}

		return object;
	}

	public static CustomRegeneration deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		@Nullable Player sourcePlayer = null;
		if (object.has("sourcePlayer")) {
			sourcePlayer = plugin.getPlayer(UUID.fromString(object.get("sourcePlayer").getAsString()));
		}

		return new CustomRegeneration(duration, amount, sourcePlayer, plugin);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return "+" + StringUtils.to2DP(mAmount) + " Regeneration Per Second";
	}

	@Override
	public String toString() {
		return String.format("CustomRegeneration duration:%d amount:%f", this.getDuration(), mAmount);
	}

}
