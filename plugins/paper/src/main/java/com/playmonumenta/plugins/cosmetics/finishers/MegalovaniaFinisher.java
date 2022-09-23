package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class MegalovaniaFinisher implements EliteFinisher {

	public static final String NAME = "Megalovania";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		loc.add(0, 1.5, 0);
		Location loc2 = loc.clone().add(-1, 0, 0);
		Location loc3 = loc.clone().add(1, 0, 0);
		//Sansundertale
		BukkitRunnable sansRunnable = new BukkitRunnable() {
			int mTicks = 0;
			double mXOffset = (Math.random() * 11) - 5;
			double mZOffset = (Math.random() * 11) - 5;
			double mYOffset = (Math.random() - 0.5);
			Location mSansLoc = loc.clone().add(mXOffset, mYOffset, mZOffset);
			ArmorStand mSans;

			@Override
			public void run() {
				if (mTicks == 0) {
					mSans = spawnSans(mSansLoc);
				}
				if (mTicks % 9 == 0 && mTicks <= 36) {
					// Sans head track
					// So the Euler angles take a counterclockwise angle
					mSans.setHeadPose(new EulerAngle(0, EntityUtils.getCounterclockwiseAngle(mSans, p), 0));
					// But rotating a vector takes a clockwise angle
					// Why, Bukkit, why?
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, mSansLoc.clone().add(new Vector(-0.15, 1.75, 0.32).rotateAroundY(-EntityUtils.getCounterclockwiseAngle(mSans, p))), 1, 0, 0, 0, 0.0);
				} else if (mTicks == 46) {
					mSans.remove();
				} else if (mTicks >= 46) {
					this.cancel();
				}
				mTicks++;
			}
		};
		BukkitRunnable megalovaniaRunnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, Constants.NotePitches.D8);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 3:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, Constants.NotePitches.D8);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 6:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, Constants.NotePitches.D20);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 12:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, Constants.NotePitches.A15);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 21:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, Constants.NotePitches.GS14);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 27:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, Constants.NotePitches.G13);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 33:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, Constants.NotePitches.F11);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 39:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, Constants.NotePitches.D8);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 42:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, Constants.NotePitches.F11);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 45:
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, Constants.NotePitches.G13);
						world.spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1);
						world.spawnParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1);
						break;
					case 46:
						this.cancel();
						break;
					default:
						break;
				}
				if (mTicks >= 46) {
					this.cancel();
				}
				mTicks++;
			}
		};
		if (Math.random() < 0.01) {
			sansRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
		megalovaniaRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.SKELETON_SKULL;
	}

	public ArmorStand spawnSans(Location loc) {
		ArmorStand sans = loc.getWorld().spawn(loc, ArmorStand.class);
		sans.setVisible(false);
		sans.setGravity(false);
		sans.setVelocity(new Vector());
		sans.setMarker(true);
		sans.setCollidable(false);
		sans.getEquipment().setHelmet(new ItemStack(Material.SKELETON_SKULL));
		sans.setRotation(0, 0);
		return sans;
	}
}
