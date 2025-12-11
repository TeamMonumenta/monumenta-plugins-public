package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class Frozen extends ZeroArgumentEffect {
	public static final String effectID = "Frozen";

	public static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(127, 209, 255), 1f);
	private boolean mStartedWithAI = false;
	private boolean mStartedWithGravity = false;
	private @Nullable BlockDisplay mDisplay;

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

		BoundingBox box = entity.getBoundingBox();
		Location lowerCorner = new Location(world, box.getMinX(), box.getMinY(), box.getMinZ());
		Vector3f size = new Vector3f((float) entity.getWidth(), (float) entity.getHeight(), (float) entity.getWidth());
		BlockDisplay display = world.spawn(lowerCorner, BlockDisplay.class);
		Transformation transformation = new Transformation(
			new Vector3f(),
			new AxisAngle4f(),
			size,
			new AxisAngle4f()
		);
		display.setTransformation(transformation);
		display.setBlock(Bukkit.createBlockData(Material.ICE));
		display.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
		mDisplay = display;

		// Sometimes the ice can remain after a mob dies after being instantly killed, this is a failsafe.
		new BukkitRunnable() {
			@Override
			public void run() {
				if (entity.isDead() || !entity.isValid()) {
					removeDisplay();
				}
			}
		}.runTask(Plugin.getInstance());
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

		removeDisplay();
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		removeDisplay();
	}

	private void removeDisplay() {
		if (mDisplay != null) {
			mDisplay.remove();
		}
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
