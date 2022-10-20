package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.NmsUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class RiptideDisable extends ZeroArgumentEffect {
	public static final String effectID = "RiptideDisable";

	private BukkitTask mTask;

	public RiptideDisable(int duration) {
		super(duration, effectID);
	}

	public static RiptideDisable deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new RiptideDisable(duration);
	}

	@Override
	public String toString() {
		return String.format("RiptideDisable duration:%d", this.getDuration());
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			if (mTask != null) {
				mTask.cancel();
			}
			mTask = new BukkitRunnable() {
				@Override
				public void run() {
					if (!player.isValid()
						    || player.getHealth() <= 0
						    || getDuration() <= 0) {
						cancel();
					}
					if (!player.isRiptiding()
						    && (player.isInWater()
							        || player.isOnGround()
							        || NmsUtils.getVersionAdapter().hasCollision(player.getWorld(), player.getBoundingBox().shift(0, -0.1, 0)))) {
						setDuration(0);
						cancel();
					}
				}
			}.runTaskTimer(Plugin.getInstance(), 1, 1);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (mTask != null) {
			mTask.cancel();
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof Player player) {
			Plugin.getInstance().mPotionManager.addPotion(player, PotionManager.PotionID.ITEM, new PotionEffect(PotionEffectType.SLOW_FALLING, 21, 0, false, true, false));
		}
	}

}
