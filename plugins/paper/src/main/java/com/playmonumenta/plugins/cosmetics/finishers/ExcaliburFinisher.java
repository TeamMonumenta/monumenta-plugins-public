package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class ExcaliburFinisher implements EliteFinisher {
	public static final String NAME = "Excalibur";

	private static final HashMap<UUID, Integer> mMobsKilled = new HashMap<>();

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		if (!(killedMob instanceof LivingEntity le)) {
			return;
		}

		mMobsKilled.putIfAbsent(p.getUniqueId(), 1);
		int mobsKilled = mMobsKilled.getOrDefault(p.getUniqueId(), 0);
		double radius = Math.min(killedMob.getBoundingBox().getWidthX(), killedMob.getBoundingBox().getWidthZ()) / Math.sqrt(2);

		new BukkitRunnable() {
			int mTicks = 0;
			final Vector mDirection = LocationUtils.getDirectionTo(p.getLocation(), killedMob.getLocation());
			final Location mStart = killedMob.getLocation().clone().add(mDirection.clone().multiply(0.92 * radius));
			final Location mEnd = killedMob.getLocation();
			final ArmorStand mExcalibur = spawnExcalibur(mStart.clone());
			final double mSlashAngle = FastUtils.RANDOM.nextDouble(Math.PI) - Math.PI / 2.0;
			LivingEntity mClonedKilledMob = (LivingEntity) killedMob;

			@Override
			public void run() {
				if (mTicks == 0) {
					if (mobsKilled >= 7) {
						mMobsKilled.put(p.getUniqueId(), 1);
					} else {
						mMobsKilled.put(p.getUniqueId(), mobsKilled + 1);
					}
				}
				playSong(p.getWorld(), p.getLocation(), mTicks, mobsKilled);
				if (mTicks == 0) {
					killedMob.remove();
					mClonedKilledMob = EliteFinishers.createClonedMob(le, p, NamedTextColor.GRAY, false, false, true);
				}
				if (mTicks <= 20) {
					mExcalibur.setRightArmPose(new EulerAngle(-Math.PI / 2.0 + 0.75 * Math.PI / 20.0 * mTicks, EntityUtils.getCounterclockwiseAngle(mExcalibur, killedMob), mSlashAngle));
				}
				if (mTicks == 0) {
					ItemStack excalibur = new ItemStack(Material.GOLDEN_SWORD);
					ItemMeta excaliburMeta = excalibur.getItemMeta();
					excaliburMeta.displayName(Component.text("Excalibur"));
					excalibur.setItemMeta(excaliburMeta);
					ItemUtils.setPlainTag(excalibur);
					mExcalibur.getEquipment().setItemInMainHand(excalibur);
				}
				// Light effect
				if (mobsKilled >= 7) {
					if (mTicks >= 28 && mTicks < 48) {
						drawXZCircle(mEnd.clone().add(0, (mTicks - 28) % 20 * 10 / 20.0, 0), mEnd.clone().add(0, ((mTicks - 28) % 20 + 1) * 10 / 20.0, 0), radius + 0.4, (radius > 3 ? 70 : 35));
					}
					if (mTicks == 40) {
						mClonedKilledMob.remove();
					}
				} else {
					// Summon some souls
					if (mTicks % 3 == 0) {
						double xOffset = FastUtils.RANDOM.nextDouble(10) - 5;
						double yOffset = FastUtils.RANDOM.nextDouble(2) - 1;
						double zOffset = FastUtils.RANDOM.nextDouble(10) - 5;
						new PartialParticle(Particle.END_ROD, mStart.clone().add(xOffset, yOffset + 1, zOffset), 1, 0, 1, 0, 0.07).spawnAsBoss();
					}
					if (mTicks == 20) {
						mClonedKilledMob.setHealth(0);
					}
				}
				if (mTicks == 40) {
					mExcalibur.remove();
				}
				if (mTicks >= 72) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void drawXZCircle(Location l1, Location l2, double r, int granularity) {
		for (int i = 0; i < granularity; i++) {
			new PPLine(Particle.END_ROD, l1.clone().add(r * FastUtils.cos(2 * i * Math.PI / (granularity * 1.0)), 0, r * FastUtils.sin(2 * i * Math.PI / (granularity * 1.0))),
				l2.clone().add(r * FastUtils.cos(2 * i * Math.PI / (granularity * 1.0)), 0, r * FastUtils.sin(2 * i * Math.PI / (granularity * 1.0)))).countPerMeter(5).groupingDistance(0.15).spawnAsBoss();
		}
	}

	public void playSong(World world, Location loc, int ticks, int variant) {
		switch (variant) {
			case 1 -> {
				switch (ticks) {
					case 0 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D8);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.A15);
					}
					case 4 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.A3);
					case 8 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D8);
					case 12 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
					case 16 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.D20);
					}
					case 20 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
					case 24 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.C18);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.E22);
					}
					case 28 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E22);
					case 32 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.F23);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.F23);
					}
					case 44 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E22);
					case 46 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D20);
					case 48 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E22);
					case 56 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.C18);
					default -> {
					}
				}
			}
			case 2 -> {
				switch (ticks) {
					case 0 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.D20);
					}
					case 12 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.C18);
					case 14 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
					case 16 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.F11);
					case 20 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
					case 24 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.F11);
					case 28 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
					case 32 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.A15);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.D20);
					}
					case 40 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.B17);
					case 44 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.D20);
					case 48 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.CS19);
					default -> {
					}
				}
			}
			case 3 -> {
				switch (ticks) {
					case 0 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D8);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.A15);
					}
					case 4 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.A3);
					case 8 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D8);
					case 12 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
					case 16 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.F11);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.D20);
					}
					case 20 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
					case 24 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.C18);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.E22);
					}
					case 28 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E22);
					case 32 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.F23);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.F23);
					}
					case 44 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E22);
					case 46 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D20);
					case 48 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E22);
					case 56 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.C18);
					default -> {
					}
				}
			}
			case 4 -> {
				switch (ticks) {
					case 0 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.D20);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.AS16);
					}
					case 12 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.F11);
					case 14 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
					case 16 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D8);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.A15);
					}
					case 20 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.CS7);
					case 24 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D8);
					case 28 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
					case 32 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D8);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.G13);
					}
					case 40 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.E10);
					case 48 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.F11);
					default -> {
					}
				}
			}
			case 5 -> {
				switch (ticks) {
					case 0 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.B17);
					}
					case 4 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.B5);
					case 8 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.B5);
					}
					case 12 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.FS12);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D8);
					}
					case 16 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.E22);
					}
					case 20 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.A15);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.FS12);
					}
					case 24 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D20);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.A15);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.FS24);
					}
					case 28 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.FS24);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D20);
					}
					case 32 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, Constants.NotePitches.D8);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.5f, Constants.NotePitches.B17);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.5f, Constants.NotePitches.G13);
					}
					case 44 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, Constants.NotePitches.FS12);
					case 46 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, Constants.NotePitches.E10);
					case 48 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, Constants.NotePitches.FS12);
					case 56 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, Constants.NotePitches.D8);
					default -> {
					}
				}
			}
			case 6 -> {
				switch (ticks) {
					case 0 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.A15);
					}
					case 12 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.FS12);
					case 14 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
					case 16 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.A15);
					case 20 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
					case 24 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.FS12);
					case 28 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
					case 32 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.B17);
					}
					case 40 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.E22);
					case 48 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.FS12);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.DS21);
					}
					default -> {
					}
				}
			}
			case 7 -> {
				switch (ticks) {
					case 0 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.E10);
					}
					case 4 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.FS12);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.FS12);
					}
					case 8 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.G13);
					}
					case 20 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.FS12);
					case 22 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.E10);
					case 24 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.D8);
					case 32 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1f, Constants.NotePitches.D20);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.5f, Constants.NotePitches.B17);
					}
					case 40 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.C18);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.A15);
					}
					case 52 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.B17);
					case 54 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.A15);
					case 56 -> {
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.G13);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, Constants.NotePitches.C18);
					}
					case 64 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.A15);
					case 72 -> world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, Constants.NotePitches.B17);
					default -> {
					}
				}
			}
			default -> {
			}
		}

	}

	public ArmorStand spawnExcalibur(Location loc) {
		ArmorStand excalibur = loc.getWorld().spawn(loc, ArmorStand.class);
		excalibur.setVisible(false);
		excalibur.setGravity(false);
		excalibur.setVelocity(new Vector());
		excalibur.setMarker(true);
		excalibur.setCollidable(false);
		excalibur.setRotation(0, 0);
		excalibur.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
		return excalibur;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_SWORD;
	}
}
