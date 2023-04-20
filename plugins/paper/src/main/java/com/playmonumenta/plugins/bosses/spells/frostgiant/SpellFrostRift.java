package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/*
 Frost Rift: Targets ⅓  players. Afterwards, breaks the ground towards them,
 creating a large rift of ice that deals 20 damage and applies Slowness 2,
 Weakness 2, and Wither 3, for 8 seconds. This rift stays in place for 18 seconds.
 If this rift collides with a target while rippling through the terrain, they are
 knocked back and take 30 damage. This rift continues until it reaches the edge of
 the Blizzard/Hailstorm.
 */
public class SpellFrostRift extends Spell {

	private static final int DURATION = 20 * 18;
	public static final Material RIFT_BLOCK_TYPE = Material.BLACKSTONE;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mStartLoc;
	private boolean mCooldown = false;

	private final List<Block> mChangedBlocks = new ArrayList<>();

	private static final Particle.DustOptions BLACK_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);

	public SpellFrostRift(Plugin plugin, LivingEntity boss, Location loc) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = loc;
	}

	@Override
	public void run() {
		mCooldown = true;

		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 25);
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0.5f);

		List<Player> players = PlayerUtils.playersInRange(mStartLoc, FrostGiant.fighterRange, true);
		List<Player> targets = new ArrayList<>();
		List<Location> locs = new ArrayList<>();
		if (players.size() >= 2) {
			int cap = (int) Math.min(Math.ceil(players.size() / 2.0), 3);
			for (int i = 0; i < cap; i++) {
				Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
				if (!targets.contains(player)) {
					targets.add(player);
					locs.add(player.getLocation());
				} else {
					cap++;
				}
			}
		} else {
			for (Player p : players) {
				targets.add(p);
				locs.add(p.getLocation());
			}
		}
		mBoss.setAI(false);

		new BukkitRunnable() {
			double mT = 0;
			float mPitch = 1;
			final Location mLoc = mBoss.getLocation().add(0, 0.5, 0);

			@Override
			public void run() {
				mT += 2;
				mPitch += 0.025f;

				for (Location p : locs) {
					Vector line = LocationUtils.getDirectionTo(p, mLoc).setY(0);
					double xloc = line.getX();
					double yloc = line.getY();
					double zloc = line.getZ();
					for (int i = 1; i < 30; i++) {
						new PartialParticle(Particle.SQUID_INK, mLoc.clone().add(xloc * i, yloc * i, zloc * i), 1, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
					}
				}
				world.playSound(mLoc, Sound.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 0.5f, mPitch);
				new PartialParticle(Particle.CLOUD, mLoc, 8, 1, 0.1, 1, 0.25).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SMOKE_LARGE, mLoc, 5, 1, 0.1, 1, 0.25).spawnAsEntityActive(mBoss);

				//Has a max of 3 rifts
				if (mT >= 20 * 2) {
					this.cancel();

					for (Location l : locs) {
						createRift(l, players);
					}
					mBoss.setAI(true);
				}
			}
		}.runTaskTimer(mPlugin, 0, 2);

	}

	private void createRift(Location loc, List<Player> players) {
		List<Location> locs = new ArrayList<>();

		BukkitRunnable runnable = new BukkitRunnable() {
			final Location mLoc = mBoss.getLocation().add(0, 0.5, 0);
			final World mWorld = mLoc.getWorld();
			final Vector mDir = LocationUtils.getDirectionTo(loc, mLoc).setY(0).normalize();
			final BoundingBox mBox = BoundingBox.of(mLoc, 0.85, 0.35, 0.85);
			final Location mOgLoc = mLoc.clone();

			@Override
			public void run() {
				mBox.shift(mDir.clone().multiply(1.25));
				Location bLoc = mBox.getCenter().toLocation(mLoc.getWorld());

				//Allows the rift to climb up and down blocks
				if (bLoc.getBlock().getType().isSolid()) {
					bLoc.add(0, 1, 0);
					if (bLoc.getBlock().getType().isSolid()) {
						this.cancel();
						bLoc.subtract(0, 1, 0);
					}
				}

				if (!bLoc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
					bLoc.subtract(0, 1, 0);
					if (!bLoc.getBlock().getType().isSolid()) {
						bLoc.subtract(0, 1, 0);
						if (!bLoc.getBlock().getType().isSolid()) {
							this.cancel();
						}
					}
				}

				if (TemporaryBlockChangeManager.INSTANCE.changeBlock(bLoc.getBlock(), RIFT_BLOCK_TYPE, DURATION)) {
					mChangedBlocks.add(bLoc.getBlock());
				}

				bLoc.add(0, 0.5, 0);

				locs.add(bLoc);
				new PartialParticle(Particle.CLOUD, bLoc, 3, 0.5, 0.5, 0.5, 0.25).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, bLoc, 3, 0.5, 0.5, 0.5, 0.125).spawnAsEntityActive(mBoss);
				mWorld.playSound(bLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 1, 0.85f);

				for (Player player : players) {
					if (player.getBoundingBox().overlaps(mBox)) {
						DamageUtils.damage(mBoss, player, DamageType.MAGIC, 30, null, false, true, "Frost Rift");
					}
				}
				if (bLoc.distance(mOgLoc) >= 50) {
					this.cancel();
				}
			}

		};

		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);

		// If touching the line of particles, get debuffed and take damage
		mActiveTasks.add(new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT += 5;
				for (Location loc : locs) {
					new PartialParticle(Particle.CLOUD, loc, 1, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.CRIT, loc, 1, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.REDSTONE, loc, 1, 0.5, 0.5, 0.5, 0.075, BLACK_COLOR).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0.5, 0.5, 0.5, 0.1).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.DAMAGE_INDICATOR, loc, 1, 0.5, 0.5, 0.5, 0.1).spawnAsEntityActive(mBoss);
					BoundingBox box = BoundingBox.of(loc, 0.85, 1.2, 0.85);
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(box)) {
							DamageUtils.damage(mBoss, player, DamageType.MAGIC, 20, null, false, true, "Frost Rift");
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 6, 2));
							player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 6, 49));
						}
					}
				}

				if (mT >= DURATION) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 5));
	}

	@Override
	public void cancel() {
		super.cancel();

		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, RIFT_BLOCK_TYPE);
		mChangedBlocks.clear();
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	@Override
	public int cooldownTicks() {
		return 7 * 20;
	}

}
