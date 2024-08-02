package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class Frozen extends ZeroArgumentEffect {
	public static final String effectID = "Frozen";

	public static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(127, 209, 255), 1f);
	private boolean mStartedWithAI = false;
	private boolean mStartedWithGravity = false;

	public Frozen(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof LivingEntity mob) {
			mStartedWithAI = mob.hasAI();
			mStartedWithGravity = mob.hasGravity();
			mob.setAI(false);
			mob.setGravity(false);
		}
		entity.setVelocity(new Vector(0, 0, 0));

		World world = entity.getWorld();
		Location loc = LocationUtils.getEntityCenter(entity);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 50, 0.5, 0.5, 0.5, 0.2).data(Material.ICE.createBlockData()).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, loc, 50, 0.3, 0.3, 0.3, 0.1, COLOR).spawnAsEnemyBuff();
		new PartialParticle(Particle.ELECTRIC_SPARK, loc, 50, 0.5, 0.5, 0.5, 0).spawnAsEnemyBuff();
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1f, 1.15f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 0.65f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 1.1f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1f, 0.85f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1f, 0.85f);
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof LivingEntity mob && mStartedWithAI) {
			mob.setAI(true);
		}
		if (mStartedWithGravity) {
			entity.setGravity(true);
		}
		entity.setVelocity(new Vector(0, 0, 0));

		World world = entity.getWorld();
		Location loc = LocationUtils.getEntityCenter(entity);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.6f, 0.85f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.3f, 0.55f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.3f, 0.8f);
		new PartialParticle(Particle.BLOCK_CRACK, LocationUtils.getEntityCenter(entity), 75).delta(0.5).data(Material.ICE.createBlockData()).spawnAsEnemyBuff();
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		entity.setVelocity(new Vector(0, 0, 0));

		new PartialParticle(Particle.BLOCK_CRACK, LocationUtils.getEntityCenter(entity), 15).delta(0.3).data(Material.ICE.createBlockData()).spawnAsEnemyBuff();
	}

	public static Frozen deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new Frozen(duration);
	}

	@Override
	public String toString() {
		return String.format("Frozen duration: %s", mDuration);
	}
}