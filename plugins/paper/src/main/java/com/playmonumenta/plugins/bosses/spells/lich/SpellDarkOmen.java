package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellDarkOmen extends Spell {

	/*
	 * Lich shoots 4 phantom blades towards the 4 cardinal direction, travel 24 blocks
	 * after the blade expires or reaches the edge of the arena, it summons invisible, invulnerable, 1 damage vexes with particle indication
	 * vexes are active for 20 seconds
	 *
	 * if players are attacked by the blade, does 50% damage, then apply curse
	 * if players bounding box overlaps vex, apply curse, kill vex
	 *
	 * CURSE: the player receive double damage from all sources and -50% healing for 1 minute.
	 */

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenter;
	private int mTell = 20 * 2;
	private double mRange;
	private double mMaxRange = 24;
	private double mArenaRange = 42;
	private double mVelocity = 20;
	private static final Particle.DustOptions BLADE_COLOR1 = new Particle.DustOptions(Color.fromRGB(199, 0, 255), 1.0f);
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(185, 0, 0), 1.0f);
	private ChargeUpManager mChargeUp;
	private List<Player> mDamaged = new ArrayList<Player>();
	private PartialParticle mPSoul;
	private PartialParticle mPSpark;
	private PartialParticle mPSmoke;
	private PartialParticle mPRed;
	private PartialParticle mPBlade;

	public SpellDarkOmen(Plugin plugin, LivingEntity boss, Location loc, int r) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = r;
		mChargeUp = new ChargeUpManager(mBoss, mTell, ChatColor.YELLOW + "Charging Dark Omen...", BarColor.YELLOW, BarStyle.SOLID, 50);
		mPSoul = new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0);
		mPSpark = new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 100, 0.1, 0.1, 0.1, 0.3);
		mPSmoke = new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 100, 0.1, 0.1, 0.1, 0.2);
		mPRed = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0, RED);
		mPBlade = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0, BLADE_COLOR1);
	}

	@Override
	public void run() {
		for (Player p : Lich.playersInRange(mCenter, mRange, true)) {
			p.sendMessage(ChatColor.LIGHT_PURPLE + "THIS FLAME ETERNAL SHALL BE A PORTENT OF YOUR DEMISE!");
		}
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.0f);

		BukkitRunnable runA = new BukkitRunnable() {
			double mT = 0.0;
			List<Vector> mBasevec = new ArrayList<Vector>();
			@Override
			public void run() {
				mChargeUp.nextTick();
				if (mT == 0) {
					mBasevec.add(new Vector(0, 0, 1));
					mBasevec.add(new Vector(-4, 0, -2).normalize());
					mBasevec.add(new Vector(4, 0, -2).normalize());
					launchBlade(mBasevec, world, true);
				}
				mT++;
				//4 points swirl into center
				if (mT < mTell) {
					Vector dir = new Vector(4, 0, 0);
					float pitch = (float) (0.5 + mT / 20);
					world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, 5.0f, pitch);
					for (int i = 0; i < 4; i++) {
						Vector quad = VectorUtils.rotateYAxis(dir.clone(), 90 * i);
						Vector qlength = quad.multiply(1 - mT / mTell);
						Vector qdir = VectorUtils.rotateYAxis(qlength.clone(), 90 / (mT / mTell));
						Location l = mBoss.getLocation().add(qdir).add(0, 0.5, 0);
						mPSoul.location(l).spawnAsBoss();
					}
				}

				//blade function
				if (mT >= mTell || Lich.phase3over()) {
					//clear all entries before launching blade
					mDamaged.clear();

					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 10.0f, 0.75f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_VEX_CHARGE, 10.0f, 1.0f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_VEX_CHARGE, 10.0f, 0.75f);

					launchBlade(mBasevec, world, false);
					this.cancel();
					mChargeUp.reset();
				}
			}

		};
		runA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runA);
	}

	public void launchBlade(List<Vector> basevec, World world, boolean warning) {
		//loop for each direction, starting +Z, clockwise
		for (int i = 0; i <= 3; i++) {
			List<Vector> vec = new ArrayList<Vector>();
			vec.addAll(basevec);

			//rotate vectors
			for (int j = 0; j < vec.size(); j++) {
				Vector v = vec.get(j);
				Vector v2 = VectorUtils.rotateYAxis(v, i * 90);
				vec.set(j, v2);
			}

			//spawn loc shift up 1 block
			Vector dir = vec.get(0);
			Location startLoc = mBoss.getLocation().add(0, 1, 0).add(dir);
			//launch blade tip
			BukkitRunnable runB = new BukkitRunnable() {
				int mT = 0;
				@Override
				public void run() {
					mT++;
					Location anchor = startLoc.clone();
					//iterate twice for higher accuracy
					for (int x = 0; x < 2; x++) {
						anchor = startLoc.clone().add(dir.clone().multiply(mVelocity / 20 * (mT + 0.5 * x)));
						if (!warning && (anchor.distance(mCenter) > mArenaRange || anchor.distance(startLoc) > mMaxRange)) {
							vex(anchor);
							mPSpark.location(anchor).spawnAsBoss();
							mPSmoke.location(anchor).spawnAsBoss();
							world.playSound(anchor, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 3.0f, 1.5f);
							world.playSound(anchor, Sound.ENTITY_VEX_AMBIENT, 5.0f, 0.75f);
							this.cancel();
						} else if (anchor.distance(mCenter) > mArenaRange || anchor.distance(startLoc) > mMaxRange || Lich.phase3over()) {
							this.cancel();
						} else {
							//construct blade
							createBlade(anchor, vec, warning);
						}
					}
				}

			};
			runB.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runB);
		}
	}

	public void createBlade(Location startLoc, List<Vector> vec, boolean warning) {
		World world = mBoss.getWorld();
		List<Location> locAll = new ArrayList<Location>();
		//construct blade
		for (int j = 1; j <= 2; j++) {
			Vector v = vec.get(j);
			//for 1 side, 5 locations
			for (int k = 0; k < 5; k++) {
				Location l = startLoc.clone();
				l.add(v.clone().multiply(0.25 * k));
				if (!locAll.contains(l)) {
					locAll.add(l);
				}
			}
		}
		//spawn particle + check loc
		List<Player> players = Lich.playersInRange(mCenter, mRange, true);
		List<Player> damage = new ArrayList<Player>();
		for (Location l : locAll) {
			if (warning) {
				mPRed.location(l).spawnAsBoss();
			} else {
				mPBlade.location(l).spawnAsBoss();
				mPSoul.location(l).spawnAsBoss();
				BoundingBox box = BoundingBox.of(l, 0.3, 0.3, 0.3);
				for (Player p : players) {
					if (p.getBoundingBox().overlaps(box) && !damage.contains(p)) {
						damage.add(p);
					}
				}
			}
		}
		//do damage + curse
		if (!warning) {
			for (Player p : damage) {
				if (!mDamaged.contains(p)) {
					mDamaged.add(p);
					world.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.5f);
					world.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 2.0f);
					BossUtils.bossDamagePercent(mBoss, p, 0.75, null, "Dark Omen");
					Lich.cursePlayer(mPlugin, p, 120);
				}
			}
		}
	}

	public void vex(Location anchor) {
		List<Player> players = Lich.playersInRange(mCenter, mRange, true);
		double count = Math.min(8, players.size() / 8.0 + 0.5);
		for (int i = 0; i < count; i++) {
			LibraryOfSoulsIntegration.summon(anchor, "LivingCurse");
		}
	}

	@Override
	public int cooldownTicks() {
		return 20 * 9;
	}

}
