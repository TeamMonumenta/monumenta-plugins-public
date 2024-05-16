package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ChainedFinisher implements EliteFinisher {
	public static final String NAME = "Chained";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		if (!(killedMob instanceof LivingEntity le)) {
			return;
		}

		loc.setYaw(0);
		loc.setPitch(0);
		BoundingBox tempBox = killedMob.getBoundingBox();
		loc.add(0, tempBox.getHeight() + 1, 0);
		Location highLoc = loc.clone();
		Location loc1 = loc.clone();
		Location loc2 = loc.clone();
		Location loc3 = loc.clone();
		Location ver1 = loc.clone();
		Location ver2 = loc.clone();
		Location ver3 = loc.clone();
		Location arc1 = loc.clone();
		Location arc2 = loc.clone();
		Location arc3 = loc.clone();
		ItemStack sword = DisplayEntityUtils.generateRPItem(Material.DIAMOND_SWORD, "Athena");
		ArmorStand sword1 = spawnSword(loc, sword);
		ArmorStand sword2 = spawnSword(loc, sword);
		ArmorStand sword3 = spawnSword(loc, sword);
		Location rise = loc.clone().add(0, -(killedMob.getHeight() / 2), 0);
		Location rise2 = loc.clone().add(0, (-killedMob.getHeight() / 2) + 0.6, 0);

		new BukkitRunnable() {
			int mTicks = 0;
			double mBot;

			@Nullable LivingEntity mClonedKilledMob;

			@Override
			public void run() {
				if (mTicks == 0) {
					// summon
					killedMob.remove();
					mClonedKilledMob = EliteFinishers.createClonedMob(le, p);
					ScoreboardUtils.addEntityToTeam(mClonedKilledMob, "chainedfinisher", NamedTextColor.DARK_AQUA)
						.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

					BoundingBox box = mClonedKilledMob.getBoundingBox();
					loc1.add((box.getWidthX() / 2) + 3 + 0.35, 1, 0.6);
					loc2.add(-(box.getWidthX() / 2) - 2.25 + 0.35, 1, 0.6);
					loc3.add(1, 1, (box.getWidthZ() / 2) + 3);
					ver1.add((loc1.getX() - loc.getX()) / 2, box.getHeight() * 0.75 + 3.5, (loc1.getZ() - loc.getZ()) / 2);
					ver2.add((loc2.getX() - loc.getX()) / 2, box.getHeight() * 0.75 + 3.5, (loc2.getZ() - loc.getZ()) / 2);
					ver3.add((loc3.getX() - loc.getX()) / 2, box.getHeight() * 0.75 + 3.5, (loc3.getZ() - loc.getZ()) / 2);
					mBot = box.getMinY();
					// spawn sword at loc
					sword1.setRightArmPose(new EulerAngle(-(Math.PI / 2.0), 0, 0));
					sword2.setRightArmPose(new EulerAngle(-(Math.PI / 2.0), 0, 0));
					sword3.setRightArmPose(new EulerAngle(-(Math.PI / 2.0), 3 * Math.PI / 2, 0));

					loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1f, 2f);
					loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.5f, 0.5f);
					loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.2f, 0.5f);
					loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.2f, 1f);

					highLoc.add(0, 4, 0);
					new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);

				} else if (mTicks > 1 && mTicks < 12) {
					// get the distance between current x and x that it needs to reach, divide by 10, then add that amount
					arc1.add((ver1.getX() - loc.getX()) / 10, 0, 0);
					arc2.add((ver2.getX() - loc.getX()) / 10, 0, 0);
					arc3.add(0, 0, (ver3.getZ() - loc.getZ()) / 10);

					// find the y value on parabola based on new x and set it to that
					arc1.setY(parabolicPath(ver1, loc1, arc1.getX()));
					arc2.setY(parabolicPath(ver2, loc2, arc2.getX()));
					arc3.setY(parabolicPath2(ver3, loc3, arc3.getZ()));
					// make the thing actually go to those points
					new PartialParticle(Particle.CLOUD, arc1, 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
					new PartialParticle(Particle.CLOUD, arc2, 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
					new PartialParticle(Particle.CLOUD, arc3, 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
					sword1.teleport(arc1);
					sword2.teleport(arc2);
					sword3.teleport(arc3);
					// does loop on sword face
					sword1.setRightArmPose(new EulerAngle(-(Math.PI / 2.0) + (Math.PI / 20) * (2 * (mTicks - 1)), 0, 0));
					sword2.setRightArmPose(new EulerAngle(-(Math.PI / 2.0) + (Math.PI / 20) * (2 * (mTicks - 1)), 0, 0));
					sword3.setRightArmPose(new EulerAngle(-(Math.PI / 2.0) + (Math.PI / 20) * (2 * (mTicks - 1)), 3 * Math.PI/2, 0));

					rise.add(0, 0.2, 0);
					if (mClonedKilledMob != null) {
						mClonedKilledMob.teleport(rise.clone().subtract(0, mClonedKilledMob.getHeight(), 0));
					}
					new PPLine(Particle.END_ROD, loc, highLoc).countPerMeter(1).spawnAsPlayerActive(p);

				} else if (mTicks >= 12 && mTicks < 18) {
					// get the distance between current x and x that it needs to reach, divide by 6, then add that amount
					arc1.add((loc1.getX() - ver1.getX()) / 6, 0, 0);
					arc2.add((loc2.getX() - ver2.getX()) / 6, 0, 0);
					arc3.add(0, 0, (loc3.getZ() - ver3.getZ()) / 6);
					// find the y value on parabola based on new x and set it to that
					arc1.setY(parabolicPath(ver1, loc1, arc1.getX()));
					arc2.setY(parabolicPath(ver2, loc2, arc2.getX()));
					arc3.setY(parabolicPath2(ver3, loc3, arc3.getZ()));
					// make the thing actually go to those points
					new PartialParticle(Particle.SMOKE_NORMAL, arc1, 3, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
					new PartialParticle(Particle.SMOKE_NORMAL, arc2, 3, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
					new PartialParticle(Particle.SMOKE_NORMAL, arc3, 3, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
					sword1.teleport(arc1);
					sword2.teleport(arc2);
					sword3.teleport(arc3);

					rise.add(0, 0.2, 0);
					if (mClonedKilledMob != null) {
						mClonedKilledMob.teleport(rise.clone().subtract(0, mClonedKilledMob.getHeight(), 0));
					}
					loc.getWorld().playSound(loc, Sound.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 0.5f, 1);
				} else if (mTicks >= 18 && mTicks < 21) {
					if (mBot < arc1.getY()) {
						double multiplier = 1.5;
						arc1.add(0, -((loc1.getY() - mBot) * (multiplier / 2)), 0);
						arc2.add(0, -((loc1.getY() - mBot) * (multiplier / 2)), 0);
						arc3.add(0, -((loc1.getY() - mBot) * (multiplier / 2)), 0);
						// !!!!!!!!!!!!!!! fire particles on the way down
						new PartialParticle(Particle.FLAME, arc1, 6, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.FLAME, arc2, 6, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.FLAME, arc3, 6, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.SMOKE_LARGE, arc1, 6, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.SMOKE_LARGE, arc2, 6, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.SMOKE_LARGE, arc3, 6, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
						sword1.teleport(arc1);
						sword2.teleport(arc2);
						sword3.teleport(arc3);

						rise.add(0, 0.15, 0);
						if (mClonedKilledMob != null) {
							mClonedKilledMob.teleport(rise.clone().subtract(0, mClonedKilledMob.getHeight(), 0));
						}
						loc.getWorld().playSound(loc, Sound.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 0.5f, 1);

					}
				} else if (mTicks == 21) {
					arc1.set(arc1.getX(), mBot - 0.17, arc1.getZ());
					arc2.set(arc2.getX(), mBot - 0.17, arc2.getZ());
					arc3.set(arc3.getX(), mBot - 0.17, arc3.getZ());
					sword1.teleport(arc1);
					sword2.teleport(arc2);
					sword3.teleport(arc3);


					new PartialParticle(Particle.EXPLOSION_LARGE, arc1, 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
					new PartialParticle(Particle.EXPLOSION_LARGE, arc2, 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
					new PartialParticle(Particle.EXPLOSION_LARGE, arc3, 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(p);
					loc.add(0, -1.5, 0);
					// revert the changed
					arc1.set(arc1.getX() - 0.3, mBot - 0.17 + 0.75, arc1.getZ() - 0.6);
					arc2.set(arc2.getX() - 0.3, mBot - 0.17 + 0.75, arc2.getZ() - 0.6);
					arc3.set(arc3.getX() - 1, mBot - 0.17 + 0.75, arc3.getZ());

					if (mClonedKilledMob != null) {
						mClonedKilledMob.teleport(rise2.clone().subtract(0, mClonedKilledMob.getHeight(), 0));
					}

					loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1, 1);
					loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.8f, 0.5f);
					loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.3f, 0.85f);
					loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_DEATH, SoundCategory.PLAYERS, 0.3f, 0.6f);
					loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.6f, 0.8f);


				} else if (mTicks > 21 && mTicks < 36) {
					new PartialParticle(Particle.CRIT_MAGIC, loc, 6, 1, 1, 1, .00000001).spawnAsPlayerActive(p);
					new PPLine(Particle.ENCHANTMENT_TABLE, loc, arc1).countPerMeter(10).spawnAsPlayerActive(p);
					new PPLine(Particle.ENCHANTMENT_TABLE, loc, arc2).countPerMeter(10).spawnAsPlayerActive(p);
					new PPLine(Particle.ENCHANTMENT_TABLE, loc, arc3).countPerMeter(10).spawnAsPlayerActive(p);
				} else if (mTicks == 65) {
					if (mClonedKilledMob != null) {
						mClonedKilledMob.remove();
						sword1.remove();
						sword2.remove();
						sword3.remove();
					}
				}
				if (mTicks > 66) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public static double parabolicPath(Location vertex, Location point, double xValue) {
		double h = vertex.getX();
		double k = vertex.getY();
		double x = point.getX();
		double y = point.getY();
		double a = (y - k) / Math.pow(x - h, 2);
		double yValue;
		yValue = a * Math.pow(xValue - h, 2) + k;
		return yValue;
	}

	public static double parabolicPath2(Location vertex, Location point, double xValue) {
		double h = vertex.getZ();
		double k = vertex.getY();
		double z = point.getZ();
		double y = point.getY();
		double a = (y - k) / Math.pow(z - h, 2);
		double yValue;
		yValue = a * Math.pow(xValue - h, 2) + k;
		return yValue;
	}

	public ArmorStand spawnSword(Location loc, ItemStack mainhand) {
		ArmorStand sword = loc.getWorld().spawn(loc, ArmorStand.class);
		sword.setVisible(false);
		sword.setGravity(false);
		sword.setVelocity(new Vector());
		sword.setMarker(true);
		sword.setCollidable(false);
		sword.setRotation(0, 0);
		sword.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
		sword.getEquipment().setItemInMainHand(mainhand);
		return sword;
	}


	@Override
	public Material getDisplayItem() {
		return Material.CHAIN;
	}
}
