package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import static org.bukkit.inventory.EquipmentSlot.*;

public class GongFinisher implements EliteFinisher {

	public static final String NAME = "Gong";
	private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(232, 186, 49), 2.0f);
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(163, 48, 7), 2.0f);
	private static final Particle.DustOptions BROWN = new Particle.DustOptions(Color.fromRGB(97, 52, 8), 2.0f);

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		loc.setY(loc.getY() + 4);
		Location eyeLocation = p.getEyeLocation();
		Vector direction = eyeLocation.getDirection().setY(0).normalize();
		Location gong = loc.clone().add(new Vector(direction.getX() * 3, 0, direction.getZ() * 3));
		Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		ArmorStand player = loc.getWorld().spawn(loc, ArmorStand.class);

		new BukkitRunnable() {
			int mTicks = 0;
			double mTiltAmount = -0.1;
			Location mStick1 = loc.clone();
			Location mStick2 = loc.clone();
			Location mStick3 = loc.clone();
			Location mStick4 = loc.clone();
			Location mStick5 = loc.clone();
			Location mStick6 = loc.clone();
			Location mHang1 = loc.clone();
			Location mHang2 = loc.clone();
			Location mHang3 = loc.clone();
			Location mHang4 = loc.clone();

			@Override
			public void run() {
				if (mTicks == 0) {
					mStick1 = gong.clone().add(right.clone().multiply(2.6));
					mStick2 = gong.clone().add(right.clone().multiply(-2.6));
					mStick3 = gong.clone().add(right.clone().multiply(2.3));
					mStick4 = gong.clone().add(right.clone().multiply(-2.3));
					mStick5 = gong.clone().add(right.clone().multiply(1.7));
					mStick6 = gong.clone().add(right.clone().multiply(-1.7));
					mStick3.setY(mStick3.getY() + 4);
					mStick4.setY(mStick4.getY() + 4);
					mStick5.setY(mStick3.getY() + 0.5);
					mStick6.setY(mStick4.getY() + 0.5);

					mHang1 = gong.clone().add(right.clone().multiply(1));
					mHang2 = gong.clone().add(right.clone().multiply(-1));
					mHang1.setY(mHang1.getY() + 4);
					mHang2.setY(mHang2.getY() + 4);
					mHang3 = mHang1.clone();
					mHang4 = mHang2.clone();
					mHang3.setY(mHang3.getY() - 0.85);
					mHang4.setY(mHang4.getY() - 0.85);
					gong.setY(gong.getY() + 2);

					player.setVisible(true);
					player.setGravity(false);
					player.setVelocity(new Vector());
					player.setMarker(true);
					player.setCollidable(false);
					player.setRotation(eyeLocation.getYaw(), 0);
					player.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
					player.setBasePlate(false);
					player.addDisabledSlots(HAND);
					player.addDisabledSlots(OFF_HAND);
					player.addDisabledSlots(FEET);
					player.addDisabledSlots(LEGS);
					player.addDisabledSlots(CHEST);
					player.addDisabledSlots(HEAD);
					player.setArms(true);
					ItemStack head = new ItemStack(Material.PLAYER_HEAD);
					SkullMeta meta = (SkullMeta) head.getItemMeta();
					if (meta != null) {
						OfflinePlayer owner = Bukkit.getOfflinePlayer(p.getName());
						meta.setOwningPlayer(owner);
						head.setItemMeta(meta);
					}
					ItemStack bell = new ItemStack(Material.BELL);
					ItemMeta bellMeta = bell.getItemMeta();
					bellMeta.displayName(Component.text("Chime of the Requiem"));
					bell.setItemMeta(bellMeta);
					ItemUtils.setPlainTag(bell);
					ItemStack chestplate = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
					ItemMeta chestplateMeta = chestplate.getItemMeta();
					chestplateMeta.displayName(Component.text("Shroud of the Lost"));
					chestplate.setItemMeta(chestplateMeta);
					ItemUtils.setPlainTag(chestplate);
					ItemStack leggings = new ItemStack(Material.IRON_LEGGINGS);
					ItemMeta leggingsMeta = leggings.getItemMeta();
					leggingsMeta.displayName(Component.text("Eternity"));
					leggings.setItemMeta(leggingsMeta);
					ItemUtils.setPlainTag(leggings);
					ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
					ItemMeta bootsMeta = boots.getItemMeta();
					bootsMeta.displayName(Component.text("Unbound Eternal Boots"));
					boots.setItemMeta(bootsMeta);
					ItemUtils.setPlainTag(boots);
					ItemStack offhand = new ItemStack(Material.GOLDEN_SWORD);
					ItemMeta offhandMeta = offhand.getItemMeta();
					offhandMeta.displayName(Component.text("Dragonbone Dagger"));
					offhand.setItemMeta(offhandMeta);
					ItemUtils.setPlainTag(offhand);
					player.getEquipment().setItemInMainHand(bell);
					player.getEquipment().setHelmet(head);
					player.getEquipment().setChestplate(chestplate);
					player.getEquipment().setLeggings(leggings);
					player.getEquipment().setBoots(boots);
					player.getEquipment().setItemInOffHand(offhand);
					loc.setY(loc.getY() - 6);

					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 2f, 1.0f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 2f, 1.0f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 2f, 1.0f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 2f, 1.25f);

				} else if (mTicks > 0 && mTicks < 21) {
					player.teleport(player.getLocation().add(direction.getX() * 0.1, 0, direction.getZ() * 0.1));

					new PPCircle(Particle.REDSTONE, gong, 1.1).ringMode(false).data(YELLOW).countPerMeter(5).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).spawnAsPlayerActive(p);
					new PPCircle(Particle.REDSTONE, gong, 1.2).data(RED).countPerMeter(3).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).spawnAsPlayerActive(p);
					new PPLine(Particle.REDSTONE, mStick2, mStick6).data(BROWN).countPerMeter(15).spawnAsPlayerActive(p);
					new PPLine(Particle.REDSTONE, mStick1, mStick5).data(BROWN).countPerMeter(15).spawnAsPlayerActive(p);
					new PPLine(Particle.REDSTONE, mStick3, mStick4).data(BROWN).countPerMeter(15).spawnAsPlayerActive(p);
					new PPLine(Particle.ENCHANTMENT_TABLE, mHang1, mHang3).countPerMeter(10).spawnAsPlayerActive(p);
					new PPLine(Particle.ENCHANTMENT_TABLE, mHang2, mHang4).countPerMeter(10).spawnAsPlayerActive(p);

					double angle = Math.sin(Math.toRadians(mTicks * 15)) * Math.PI / 4;
					EulerAngle rightLegPose = new EulerAngle(angle, 0, 0);
					EulerAngle leftLegPose = new EulerAngle(-angle, 0, 0);
					player.setRightLegPose(rightLegPose);
					player.setLeftLegPose(leftLegPose);
				} else if (mTicks >= 21 && mTicks < 50) {
					if (mTicks < 34) {
						double angle = Math.toRadians((mTicks - 20) * 15);
						EulerAngle rightArmPose = new EulerAngle(-angle, 0, 0);
						player.setRightArmPose(rightArmPose);
						new PPCircle(Particle.REDSTONE, gong, 1.1).ringMode(false).data(YELLOW).countPerMeter(5).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).spawnAsPlayerActive(p);
						new PPCircle(Particle.REDSTONE, gong, 1.2).data(RED).countPerMeter(3).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).spawnAsPlayerActive(p);

						new PPLine(Particle.REDSTONE, mStick2, mStick6).data(BROWN).countPerMeter(15).spawnAsPlayerActive(p);
						new PPLine(Particle.REDSTONE, mStick1, mStick5).data(BROWN).countPerMeter(15).spawnAsPlayerActive(p);
						new PPLine(Particle.REDSTONE, mStick3, mStick4).data(BROWN).countPerMeter(15).spawnAsPlayerActive(p);
					} else if (mTicks < 41) {
						double angle = Math.toRadians((40 - mTicks) * 15);
						EulerAngle rightArmPose = new EulerAngle(-angle, 0, 0);
						player.setRightArmPose(rightArmPose);
					}
					if (mTicks >= 34 && mTicks < 41) {
						Vector gongToPlayer = player.getLocation().toVector().subtract(gong.toVector()).normalize();
						Vector tiltedDirection = gongToPlayer.clone().add(new Vector(0, mTiltAmount, 0)).normalize();

						new PPCircle(Particle.REDSTONE, gong, 1.1)
							.ringMode(false)
							.data(YELLOW)
							.countPerMeter(5)
							.directionalMode(false)
							.rotateDelta(true)
							.axes(tiltedDirection, right.clone())
							.spawnAsPlayerActive(p);
						new PPCircle(Particle.REDSTONE, gong, 1.2)
							.data(RED)
							.countPerMeter(3)
							.directionalMode(false)
							.rotateDelta(true)
							.axes(tiltedDirection, right.clone())
							.spawnAsPlayerActive(p);
						new PPLine(Particle.REDSTONE, mStick2, mStick6).data(BROWN).countPerMeter(15).spawnAsPlayerActive(p);
						new PPLine(Particle.REDSTONE, mStick1, mStick5).data(BROWN).countPerMeter(15).spawnAsPlayerActive(p);
						new PPLine(Particle.REDSTONE, mStick3, mStick4).data(BROWN).countPerMeter(15).spawnAsPlayerActive(p);

						mTiltAmount -= 0.3;
					}
				}
				if (mTicks == 38) {
					Location hit = player.getLocation();
					hit.add(0, 2, 0);
					new PartialParticle(Particle.END_ROD, hit, 30, 1, 1, 1, .00000001).spawnAsPlayerActive(p);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
					loc.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
				}

				if (mTicks >= 49) {
					player.remove();
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.BELL;
	}
	}
