package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class IronTinctureEnhancementAbsorptionInfused extends Effect {
	public static final String GENERIC_NAME = "IronTinctureEnhancementAbsorptionInfused";
	public static final String effectID = "IronTinctureEnhancementAbsorptionInfused";
	private static final Particle.DustOptions DUST_OPTIONS = new Particle.DustOptions(Color.YELLOW, 1f);

	private final double mAmount;
	private final double mMaxAmount;
	private final int mAbsorptionDuration;
	private final UUID mAlchemistId;
	private int mRotation = 0;

	public IronTinctureEnhancementAbsorptionInfused(int duration, double amount, double maxAmount, int absorptionDuration, UUID alchemistId) {
		super(duration, effectID);
		mAmount = amount;
		mMaxAmount = maxAmount;
		mAbsorptionDuration = absorptionDuration;
		mAlchemistId = alchemistId;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean isBuff() {
		return false;
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		@Nullable Player killer = event.getEntity().getKiller();
		if (killer == null) {
			return;
		}

		AbsorptionUtils.addAbsorption(killer, mAlchemistId, mAmount, mMaxAmount, mAbsorptionDuration);
		killer.getWorld().playSound(killer.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1f, 2f);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		double oldAngle = Math.toRadians(mRotation);
		mRotation += 40;
		double newAngle = Math.toRadians(mRotation);
		Location baseLoc = entity.getLocation().clone().add(0, 0.2, 0);
		Location orbital1Loc1 = baseLoc.clone().add(FastUtils.cos(oldAngle) * entity.getWidth() * 0.75, 0, FastUtils.sin(oldAngle) * entity.getWidth() * 0.75);
		Location orbital1Loc2 = baseLoc.clone().add(FastUtils.cos(newAngle) * entity.getWidth() * 0.75, 0, FastUtils.sin(newAngle) * entity.getWidth() * 0.75);
		Location orbital2Loc1 = baseLoc.clone().subtract(FastUtils.cos(oldAngle) * entity.getWidth() * 0.75, 0, FastUtils.sin(oldAngle) * entity.getWidth() * 0.75);
		Location orbital2Loc2 = baseLoc.clone().subtract(FastUtils.cos(newAngle) * entity.getWidth() * 0.75, 0, FastUtils.sin(newAngle) * entity.getWidth() * 0.75);
		new PPLine(Particle.REDSTONE, orbital1Loc1, orbital1Loc2)
			.countPerMeter(4)
			.data(DUST_OPTIONS)
			.spawnAsEnemyBuff();
		new PPLine(Particle.REDSTONE, orbital2Loc1, orbital2Loc2)
			.countPerMeter(4)
			.data(DUST_OPTIONS)
			.spawnAsEnemyBuff();
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);
		object.addProperty("maxAmount", mMaxAmount);
		object.addProperty("absorptionDuration", mAbsorptionDuration);
		object.addProperty("alchemistId", mAlchemistId.toString());

		return object;
	}

	public static IronTinctureEnhancementAbsorptionInfused deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		double maxAmount = object.get("maxAmount").getAsDouble();
		int absorptionDuration = object.get("absorptionDuration").getAsInt();
		UUID alchemistId = UUID.fromString(object.get("alchemistId").getAsString());

		return new IronTinctureEnhancementAbsorptionInfused(duration, amount, maxAmount, absorptionDuration, alchemistId);
	}

	@Override
	public String toString() {
		return String.format("IronTinctureEnhancementAbsorptionInfused duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
