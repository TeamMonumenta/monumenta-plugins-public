package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class DragonsBreathFinisher implements EliteFinisher {
	public static final String NAME = "Dragon's Breath";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {

		final Location mLoc = killedMob.getLocation();
		Location mDragonLoc = mLoc.clone().add(0, 3, 0);

		World world = p.getWorld();
		world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 0.21f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 1f, 1.f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1.f, 0.5f);
		world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.f, 0.5f);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1.5, 0), 64, .05, .5, .05).spawnAsPlayerActive(p);

		ArmorStand mDragon = spawnDragonHead(mDragonLoc);
		mDragon.setHeadPose(new EulerAngle(Math.PI / 2, EntityUtils.getCounterclockwiseAngle(mDragon, p), 0));

		new BukkitRunnable() {
			double mRadius = 0;
			int mTicks = 0;

			@Override
			public void run() {
				mRadius += 0.25;
				if (mTicks < 10) {
					new PPCircle(Particle.SMALL_FLAME, mLoc, mRadius).ringMode(true).count(36).delta(0.15).spawnAsPlayerActive(p);
					new PPCircle(Particle.SOUL_FIRE_FLAME, mLoc, mRadius).ringMode(true).count(36).delta(0.15).spawnAsPlayerActive(p);
				}
				if (mTicks >= 50) {
					mDragon.remove();
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 5, 1);
	}


	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_HEAD;
	}

	public ArmorStand spawnDragonHead(Location loc) {
		ArmorStand dragon = loc.getWorld().spawn(loc, ArmorStand.class);
		dragon.setGravity(false);
		dragon.setVelocity(new Vector());
		dragon.setMarker(true);
		dragon.setCollidable(false);
		dragon.setVisible(false);
		dragon.getEquipment().setHelmet(new ItemStack(Material.DRAGON_HEAD));
		dragon.setRotation(270, 0);
		return dragon;
	}
}



