package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class FrozenSolidFinisher implements EliteFinisher {
	public static final String NAME = "Frozen Solid";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {

		new BukkitRunnable() {
			int mTicks = 0;
			HashMap<Integer, ArrayList<Location>> mIceDelays = new HashMap<>();
			ArrayList<ArmorStand> mArmorStands = new ArrayList<>();
			@Override
			public void run() {
				if (mTicks == 0) {
					// Let's let the mob freeze
					((LivingEntity) killedMob).setHealth(1);
					killedMob.setInvulnerable(true);
					killedMob.setGravity(false);
					((LivingEntity) killedMob).setCollidable(false);
					((LivingEntity) killedMob).setAI(false);
					killedMob.addScoreboardTag(AbilityUtils.IGNORE_TAG);
					killedMob.setSilent(true);
					EntityUtils.applySilence(Plugin.getInstance(), 30, (LivingEntity) killedMob);
					killedMob.setVelocity(new Vector());
					// Figure out where and when to generate ice
					BoundingBox box = killedMob.getBoundingBox();
					for (double x = box.getMinX(); x <= box.getMaxX(); x += 0.6) {
						for (double y = box.getMinY(); y <= box.getMaxY(); y += 0.6) {
							for (double z = box.getMinZ(); z <= box.getMaxZ(); z += 0.6) {
								// No need to fill the ice inside, we just need the surface
								if (!(x > box.getMinX() && x <= box.getMaxX() - 1 && y > box.getMinY() && y <= box.getMaxY() - 1 && z > box.getMinZ() && z <= box.getMaxZ() - 1)) {
									int delay = (int) (Math.random() * 20) + 1;
									mIceDelays.putIfAbsent(delay, new ArrayList<>());
									mIceDelays.get(delay).add(new Location(loc.getWorld(), x, y - 1.5, z));
								}
							}
						}
					}
				} else if (mTicks <= 20 && mIceDelays.containsKey(mTicks)) {
					// Ice is generating
					for (Location loc : mIceDelays.get(mTicks)) {
						// Get some ice blocks on armor stands; make sure these are uncollidable
						ArmorStand ice = loc.getWorld().spawn(loc, ArmorStand.class);
						ice.setVisible(false);
						ice.setGravity(false);
						ice.setVelocity(new Vector());
						ice.setMarker(true);
						ice.setCollidable(false);
						ice.getEquipment().setHelmet(new ItemStack(Material.ICE));
						loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, Constants.NotePitches.F11);
						mArmorStands.add(ice);
					}
				} else if (mTicks == 29) {
					// Remove the mob a tick before the finisher is done
					killedMob.remove();
				} else if (mTicks == 30) {
					// Break the ice with particles and play the sound
					for (ArmorStand ice : mArmorStands) {
						ice.remove();
						loc.getWorld().spawnParticle(Particle.BLOCK_DUST, ice.getLocation().add(0, 1.5, 0), 5, Bukkit.createBlockData(Material.ICE));
					}
					loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 2f, Constants.NotePitches.F11);
				}
				if (mTicks > 30) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLUE_ICE;
	}
}
