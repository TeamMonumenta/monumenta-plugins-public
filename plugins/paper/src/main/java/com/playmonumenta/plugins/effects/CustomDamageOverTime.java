package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CustomDamageOverTime extends Effect {
	public static final String effectID = "CustomDamageOverTime";

	protected final double mDamage;
	protected final int mPeriod;
	protected final @Nullable Player mPlayer;
	protected final @Nullable ClassAbility mSpell;
	private int mTicks;

	public CustomDamageOverTime(int duration, double damage, int period, @Nullable Player player, @Nullable ClassAbility spell) {
		super(duration, effectID);
		mDamage = damage;
		mPeriod = period;
		mPlayer = player;
		mSpell = spell;
	}

	//Magnitude is equal to the level of wither that it is equivalent to, at low levels of wither
	//i.e. a magnitude of 2 means it is the same as wither 2 - deals 1 health per second
	@Override
	public double getMagnitude() {
		return (mDamage * 40.0) / mPeriod;
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz && entity instanceof LivingEntity le && !(entity instanceof ArmorStand)) {
			mTicks += 5; //Activates 4 times a second
			if (mTicks >= mPeriod) {
				mTicks %= mPeriod;
				DamageUtils.damage(mPlayer, le, DamageType.AILMENT, mDamage, mSpell, true, false);
				new PartialParticle(Particle.SQUID_INK, le.getEyeLocation(), 8, 0.4, 0.4, 0.4, 0.1).spawnAsEnemy();
			}
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("damage", mDamage);
		object.addProperty("period", mPeriod);
		if (mPlayer != null) {
			object.addProperty("player", mPlayer.getUniqueId().toString());
		}
		if (mSpell != null) {
			object.addProperty("spell", mSpell.name());
		}

		return object;
	}

	public static CustomDamageOverTime deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double damage = object.get("damage").getAsDouble();
		int period = object.get("period").getAsInt();

		@Nullable Player player = null;
		if (object.has("player")) {
			player = plugin.getPlayer(UUID.fromString(object.get("player").getAsString()));
		}

		@Nullable ClassAbility spell = null;
		if (object.has("spell")) {
			spell = ClassAbility.valueOf(object.get("spell").getAsString());
		}

		return new CustomDamageOverTime(duration, damage, period, player, spell);
	}

	@Override
	public String toString() {
		return String.format("CustomDoT duration:%d modifier:%s damage:%f period:%d", this.getDuration(), "CustomDamageOverTime", mDamage, mPeriod);
	}
}
