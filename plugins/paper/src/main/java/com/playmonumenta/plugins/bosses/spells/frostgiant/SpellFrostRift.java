package com.playmonumenta.plugins.bosses.spells.frostgiant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 Frost Rift: Targets ⅓  players. Afterwards, breaks the ground towards them,
 creating a large rift of ice that deals 20 damage and applies Slowness 2,
 Weakness 2, and Wither 3, for 8 seconds. This rift stays in place for 18 seconds.
 If this rift collides with a target while rippling through the terrain, they are
 knocked back and take 30 damage. This rift continues until it reaches the edge of
 the Blizzard/Hailstorm.
 */
public class SpellFrostRift extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mStartLoc;
	private boolean mCooldown = false;

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

		List<Player> players = PlayerUtils.playersInRange(mStartLoc, FrostGiant.fighterRange);
		List<Player> targets = new ArrayList<Player>();
		List<Location> locs = new ArrayList<Location>();
		if (players.size() >= 2) {
			int cap = (int) Math.ceil(players.size() / 2);
			if (cap >= 3) {
				cap = 3;
			}
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

		new BukkitRunnable() {
			int mT = 0;
			float mPitch = 1;
			@Override
			public void run() {
				mT += 2;
				Location loc = mBoss.getLocation();
				mPitch += 0.025;

				for (Player p : targets) {
					p.playSound(loc, Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2f, mPitch + 0.5f);
				}
				world.playSound(loc, Sound.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 0.5f, mPitch);
				world.spawnParticle(Particle.CLOUD, loc, 8, 1, 0.1, 1, 0.25);
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 5, 1, 0.1, 1, 0.25);

				//Has a max of 3 rifts
				if (mT >= 20 * 2.5) {
					this.cancel();

					for (Location l : locs) {
						createRift(l, players);
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 2);

	}

	private void createRift(Location loc, List<Player> players) {
		List<Location> locs = new ArrayList<Location>();
		World world = mBoss.getWorld();

		Map<Location, Material> oldBlocks = new HashMap<>();
		Map<Location, BlockData> oldData = new HashMap<>();

		new BukkitRunnable() {
			Location mLoc = mBoss.getLocation().add(0, 0.5, 0);
			World mWorld = mLoc.getWorld();
			Vector mDir = LocationUtils.getDirectionTo(loc, mLoc).setY(0).normalize();
			BoundingBox mBox = BoundingBox.of(mLoc, 0.85, 0.35, 0.85);
			Location mOgLoc = mLoc.clone();
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

				//Do not replace frosted ice back down, set it to cracked stone bricks
				if (bLoc.getBlock().getType() == Material.FROSTED_ICE) {
					oldBlocks.put(bLoc.clone(), Material.CRACKED_STONE_BRICKS);
				} else {
					oldBlocks.put(bLoc.clone(), bLoc.getBlock().getType());
					oldData.put(bLoc.clone(), bLoc.getBlock().getBlockData());
				}
				bLoc.getBlock().setType(Material.BLACKSTONE);

				bLoc.add(0, 0.5, 0);

				locs.add(bLoc);
				mWorld.spawnParticle(Particle.CLOUD, bLoc, 3, 0.5, 0.5, 0.5, 0.25);
				mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, bLoc, 3, 0.5, 0.5, 0.5, 0.125);
				mWorld.playSound(bLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 1, 0.85f);

				for (Player player : players) {
					if (player.getBoundingBox().overlaps(mBox)) {
						BossUtils.bossDamage(mBoss, player, 30);
					}
				}
				if (bLoc.distance(mOgLoc) >= 50) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		//If touching the "line" of particles, get debuffed and take damaged. Can be blocked over
		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT += 5;
				for (Location loc : locs) {
					world.spawnParticle(Particle.CLOUD, loc, 1, 0.5, 0.5, 0.5, 0.075);
					world.spawnParticle(Particle.CRIT, loc, 1, 0.5, 0.5, 0.5, 0.075);
					world.spawnParticle(Particle.REDSTONE, loc, 1, 0.5, 0.5, 0.5, 0.075, BLACK_COLOR);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0.5, 0.5, 0.5, 0.1);
					world.spawnParticle(Particle.DAMAGE_INDICATOR, loc, 1, 0.5, 0.5, 0.5, 0.1);
					BoundingBox box = BoundingBox.of(loc, 0.85, 1.5, 0.85);
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(box)) {
							BossUtils.bossDamage(mBoss, player, 20, null);
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 6, 2));
							player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 6, 49));
						}
					}
				}

				if (mT >= 20 * 18) {
					this.cancel();
					for (Map.Entry<Location, Material> e : oldBlocks.entrySet()) {
						e.getKey().getBlock().setType(e.getValue());
						if (oldData.containsKey(e.getKey())) {
							e.getKey().getBlock().setBlockData(oldData.get(e.getKey()));
						}
					}
					locs.clear();
				}
			}

		};

		runnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(runnable);
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	@Override
	public int duration() {
		return 20 * 7;
	}

}
