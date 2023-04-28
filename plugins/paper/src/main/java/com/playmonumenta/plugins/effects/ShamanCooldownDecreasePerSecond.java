package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ShamanCooldownDecreasePerSecond extends Effect {
	public static final String effectID = "ShamanCDRPerSecond";

	private final double mPercent;
	private final int mMaxDecrease;
	private final @Nullable Player mSourcePlayer;
	private final Plugin mPlugin;

	public ShamanCooldownDecreasePerSecond(int duration, double amount, int maxDecrease, Plugin plugin) {
		this(duration, amount, maxDecrease, null, plugin);
	}

	public ShamanCooldownDecreasePerSecond(int duration, double percent, int maxDecrease, @Nullable Player sourcePlayer, Plugin plugin) {
		super(duration, effectID);
		mPercent = percent;
		mMaxDecrease = maxDecrease;
		mSourcePlayer = sourcePlayer;
		mPlugin = plugin;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			if (entity instanceof Player player) {
				for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(player).getAbilities()) {
					ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
					if (linkedSpell == null || linkedSpell == ClassAbility.WHIRLWIND_TOTEM) {
						continue;
					}
					int totalCD = abil.getModifiedCooldown();
					int reducedCD = Math.min((int) (totalCD * mPercent), mMaxDecrease);
					mPlugin.mTimers.updateCooldown(player, linkedSpell, reducedCD);
				}
			}
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("percent", mPercent);
		object.addProperty("maxdecrease", mMaxDecrease);

		if (mSourcePlayer != null) {
			object.addProperty("sourcePlayer", mSourcePlayer.getUniqueId().toString());
		}

		return object;
	}

	public static ShamanCooldownDecreasePerSecond deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("percent").getAsDouble();
		int maxDecrease = object.get("maxdecrease").getAsInt();

		@Nullable Player sourcePlayer = null;
		if (object.has("sourcePlayer")) {
			sourcePlayer = plugin.getPlayer(UUID.fromString(object.get("sourcePlayer").getAsString()));
		}

		return new ShamanCooldownDecreasePerSecond(duration, amount, maxDecrease, sourcePlayer, plugin);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mPercent) + " Cooldown Reduction Per Second";
	}

	@Override
	public String toString() {
		return String.format("ShamanCDRPerSecond duration:%d amount:%f", this.getDuration(), mPercent);
	}

}
