package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PrecisionStrikeDamage extends Effect {
	public static final String effectID = "PrecisionStrikeDamage";
	private final int mStacks;
	private final double mAmount;
	private final double mDistanceSquared;

	public PrecisionStrikeDamage(int duration, int stacks, double amount, double distanceSquared) {
		super(duration, effectID);
		mStacks = stacks;
		mAmount = amount;
		mDistanceSquared = distanceSquared;
		displaysTime(false);
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageEvent.DamageType.TRUE && event.getType() != DamageEvent.DamageType.OTHER && event.getAbility() != ClassAbility.PRECISION_STRIKE
			&& entity.getLocation().distanceSquared(enemy.getLocation()) >= mDistanceSquared && entity instanceof Player player) {

			World world = player.getWorld();
			Location loc = player.getLocation();

			if (getDuration() > 1) {
				world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.3f, 2f);
				world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1f, 1.75f);
				world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 1f, 1.75f);
				world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 1f);
				world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1f, 1f);
			}

			// Remove the effect, but continue to activate during this tick for AOEs and such
			setDuration(1);
			DamageUtils.damage(entity, enemy, DamageEvent.DamageType.PROJECTILE_SKILL, mAmount * mStacks, ClassAbility.PRECISION_STRIKE, true, false);

			new PartialParticle(Particle.EXPLOSION_LARGE, LocationUtils.getHalfHeightLocation(enemy), 1).spawnAsPlayerActive(player);

			double[] yaws = {0, -50, 50, -100, 100};
			double[] pitches = {-60, -52, -52, -44, -44};
			for (int i = 0; i < mStacks; i++) {
				Vector dir = VectorUtils.rotateTargetDirection(loc.getDirection(), yaws[i], pitches[i]);
				launchArc(dir, player.getEyeLocation(), player, enemy);
			}

		}
	}

	private void launchArc(Vector dir, Location loc, Player player, LivingEntity target) {
		new BukkitRunnable() {
			final Location mL = loc.clone();
			int mT = 0;
			double mArcCurve = 0.33;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = LocationUtils.getHalfHeightLocation(target);
				if (!to.getWorld().equals(mL.getWorld())) {
					cancel();
					return;
				}

				for (int i = 0; i < 6; i++) {
					mArcCurve += 0.1;
					mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));

					if (mD.length() > 0.7) {
						mD.normalize().multiply(0.7);
					}

					mL.add(mD);

					new PartialParticle(Particle.REDSTONE, mL, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(175, 175, 175), 2.0f))
						.spawnAsPlayerActive(player);
					new PartialParticle(Particle.CRIT, mL, 2, 0, 0, 0, 0.2).spawnAsPlayerActive(player);

					if (mT > 5 && mL.distance(to) < 1.1) {
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public int getStacks() {
		return mStacks;
	}

	@Override
	public double getMagnitude() {
		return mAmount * mStacks;
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("stacks", mStacks);
		object.addProperty("amount", mAmount);
		object.addProperty("distanceSquared", mDistanceSquared);

		return object;
	}

	public static PrecisionStrikeDamage deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		int stacks = object.get("stacks").getAsInt();
		double amount = object.get("amount").getAsDouble();
		double distanceSquared = object.get("distanceSquared").getAsDouble();

		return new PrecisionStrikeDamage(duration, stacks, amount, distanceSquared);
	}

	@Override
	public String toString() {
		return String.format("PrecisionStrikeDamage duration:%d stacks:%d amount:%f distanceSquared:%f", getDuration(), mStacks, mAmount, mDistanceSquared);
	}
}
