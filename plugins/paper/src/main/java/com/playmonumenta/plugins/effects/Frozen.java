package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Frozen extends ZeroArgumentEffect {
	public static final String effectID = "Frozen";

	public static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(127, 209, 255), 1f);
	private boolean mStartedWithAI = false;
	private boolean mStartedWithGravity = false;
	final ArrayList<ArmorStand> mArmorStands = new ArrayList<>();

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

		// I very much stole this code from the frozen solid finisher, thanks!!
		BoundingBox box = entity.getBoundingBox();
		for (double x = box.getMinX(); x <= box.getMaxX(); x += 0.6) {
			for (double y = box.getMinY(); y <= box.getMaxY(); y += 0.6) {
				for (double z = box.getMinZ(); z <= box.getMaxZ(); z += 0.6) {
					if (!(x > box.getMinX() && x <= box.getMaxX() - 1 && y > box.getMinY() && y <= box.getMaxY() - 1 && z > box.getMinZ() && z <= box.getMaxZ() - 1)) {
						ArmorStand ice = world.spawn(new Location(world, x, y - 1.5, z), ArmorStand.class);
						ice.setVisible(false);
						ice.setGravity(false);
						ice.setVelocity(new Vector());
						ice.setMarker(true);
						ice.setCollidable(false);
						ice.getEquipment().setHelmet(new ItemStack(Material.ICE));
						ice.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
						mArmorStands.add(ice);
					}
				}
			}
		}

		// Sometimes the ice can remain after a mob dies after being instantly killed, this is a failsafe.
		new BukkitRunnable() {
			@Override
			public void run() {
				if (entity.isDead() || !entity.isValid()) {
					removeStands();
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

		removeStands();
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		removeStands();
	}

	private void removeStands() {
		for (ArmorStand ice : mArmorStands) {
			ice.remove();
			new PartialParticle(Particle.BLOCK_CRACK, ice.getLocation().add(0, 1.5, 0), 5, Bukkit.createBlockData(Material.ICE)).spawnAsEnemyBuff();
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
