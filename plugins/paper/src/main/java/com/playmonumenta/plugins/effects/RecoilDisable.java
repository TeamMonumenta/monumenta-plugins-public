package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import java.lang.ref.WeakReference;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class RecoilDisable extends Effect {
	public static final String effectID = "RecoilDisable";
	public @Nullable BukkitTask mTask = null;

	private final double mAmount;

	public RecoilDisable(int duration, int amount) {
		super(duration, effectID);
		mAmount = amount;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		return object;
	}

	public static RecoilDisable deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		int amount = object.get("amount").getAsInt();

		return new RecoilDisable(duration, amount);
	}

	@Override
	public String toString() {
		return String.format("RecoilDisable duration:%d amount:%f", this.getDuration(), mAmount);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (mTask != null) {
			mTask.cancel();
		}
		mTask = new BukkitRunnable() {
			final WeakReference<Entity> mRefEntity = new WeakReference<>(entity);

			@Override
			public void run() {
				Entity realEntity = mRefEntity.get();
				if (realEntity == null || !realEntity.isValid()) {
					this.cancel();
					mTask = null;
					return;
				}
				if (realEntity.isOnGround() || realEntity.isInWater()) {
					setDuration(0);
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (mTask != null) {
			mTask.cancel();
			mTask = null;
		}
	}
}
