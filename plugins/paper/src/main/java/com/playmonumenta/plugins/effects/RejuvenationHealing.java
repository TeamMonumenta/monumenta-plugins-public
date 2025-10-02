package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class RejuvenationHealing extends Effect {
	public static final String effectID = "RejuvenationHealing";

	private final double mHealPercent;
	private final int mInterval;
	private final double mThreshold;
	private final Player mSourcePlayer;
	private final Plugin mPlugin;
	private final double mTriage;

	private int mTicks;

	// Copied from CustomRegeneration
	public RejuvenationHealing(int duration, double healPercent, double threshold, int interval, Player sourcePlayer, Plugin plugin) {
		super(duration, effectID);
		mHealPercent = healPercent;
		mSourcePlayer = sourcePlayer;
		mPlugin = plugin;
		mThreshold = threshold;
		mInterval = interval;
		mTicks = Bukkit.getCurrentTick() % mInterval;
		mTriage = mPlugin.mItemStatManager.getEnchantmentLevel(mSourcePlayer, EnchantmentType.TRIAGE) * 0.05;
		deleteOnLogout(true);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			mTicks += 5;
		}
		if (mTicks < mInterval) {
			return;
		}
		if (entity instanceof Player player) {
			double maxHealth = EntityUtils.getMaxHealth(player);
			double hp = player.getHealth() / maxHealth;
			if (hp <= mThreshold) {
				double healAmount = maxHealth * mHealPercent;
				PlayerUtils.healPlayer(mPlugin, player, healAmount, mSourcePlayer);
				int numHearts = (int) (mHealPercent * 20);
				new PartialParticle(Particle.HEART, player.getLocation().add(0, 2, 0), numHearts, 0.07, 0.07, 0.07, 0.001).spawnAsPlayerBuff(mSourcePlayer);
			}
			mTicks -= mInterval;
		}
	}

	@Override
	public double getMagnitude() {
		return mHealPercent * (1 + mThreshold) * (1 + (mTriage * 0.05)); // goofy calculation
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mHealPercent);
		object.addProperty("interval", mInterval);
		object.addProperty("threshold", mThreshold);
		object.addProperty("sourcePlayer", mSourcePlayer.getUniqueId().toString());

		return object;
	}

	public static @Nullable RejuvenationHealing deserialize(JsonObject object, Plugin plugin) {
		return null;
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return null;
		// return Component.text("+" +  StringUtils.multiplierToPercentageWithSign(getMagnitude()) + " " + getDisplayedName());
	}

	@Override
	public @Nullable String getDisplayedName() {
		return null;
		// return "Rejuvenation From " + mSourcePlayer.getName();
	}

	@Override
	public String toString() {
		return String.format("RejuvenationHealing duration:%d amount:%f interval:%d", this.getDuration(), mHealPercent, mInterval);
	}
}
