package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Parasites extends Effect {
	public static final String effectID = "Parasites";
	public static final String GENERIC_NAME = "Parasites";

	private static final int DURATION = 20 * 5;

	private final double mAmount;

	public Parasites(double amount) {
		this(DURATION, amount);
	}

	public Parasites(int duration, double amount) {
		super(duration, effectID);
		mAmount = amount;
	}

	public double getAmount() {
		return mAmount;
	}

	@Override
	public boolean isBuff() {
		return false;
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		super.entityGainEffect(entity);
		if (!(entity instanceof Player player)) {
			return;
		}
		if (mAmount == 0.25) {
			player.playSound(player, Sound.BLOCK_MUD_STEP, SoundCategory.HOSTILE, 2f, 0.8f);
			player.playSound(player, Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 2f, 0.8f);
			player.playSound(player, Sound.ENTITY_TURTLE_EGG_CRACK, SoundCategory.HOSTILE, 2f, 1f);
			player.setFreezeTicks(20);
		} else if (mAmount == 0.5) {
			player.playSound(player, Sound.BLOCK_MUD_STEP, SoundCategory.HOSTILE, 2f, 0.7f);
			player.playSound(player, Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 2f, 0.7f);
			player.playSound(player, Sound.ENTITY_TURTLE_EGG_CRACK, SoundCategory.HOSTILE, 2f, 0.75f);
			player.setFreezeTicks(20);
		} else if (mAmount == 0.75 || mAmount == 1) {
			player.playSound(player, Sound.BLOCK_MUD_STEP, SoundCategory.HOSTILE, 2f, 0.6f);
			player.playSound(player, Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 2f, 0.6f);
			player.playSound(player, Sound.ENTITY_TURTLE_EGG_CRACK, SoundCategory.HOSTILE, 2f, 0.55f);
			player.setFreezeTicks(20);
			if (mAmount == 1) {
				player.playSound(player, Sound.ENTITY_TURTLE_EGG_BREAK, SoundCategory.HOSTILE, 2f, 0.5f);
				player.playSound(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 2f, 0.65f);
			}
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		super.entityTickEffect(entity, fourHertz, twoHertz, oneHertz);
		if (mAmount >= 1) {
			if (entity instanceof Player player) {
				player.setFreezeTicks(10); // causes flashing freeze effect on the edge of the screen
			}
			if (fourHertz) {
				new PartialParticle(Particle.REDSTONE, entity.getLocation().clone().add(0, 1, 0))
					.data(new Particle.DustOptions(Color.fromRGB(51, 51, 17), 0.8f))
					.count(6)
					.delta(0.4, 0.4, 0.4)
					.extra(0.02)
					.spawnAsBoss();
			}
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);
		return object;
	}

	public static Parasites deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		return new Parasites(duration, amount);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text("Parasites", mAmount >= 1 ? NamedTextColor.RED : NamedTextColor.GRAY);
	}

	@Override
	public String toString() {
		return String.format("Parasites duration:%d amount:%f", mDuration, mAmount);
	}
}
