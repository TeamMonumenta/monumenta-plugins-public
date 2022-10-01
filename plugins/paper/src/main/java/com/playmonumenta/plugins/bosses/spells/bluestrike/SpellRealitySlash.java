package com.playmonumenta.plugins.bosses.spells.bluestrike;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.Samwell;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellRealitySlash extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Samwell mSamwell;
	private Location mStartLoc;

	private boolean mCooldown;
	private int mPhase;

	private static final Particle.DustOptions BLACK_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);

	private static final double DIRECT_HIT_DAMAGE = 80;
	private static final double LINGERING_HIT_DAMAGE = 40;
	private static final int LINGERING_DURATION = 10 * 20;

	private static final int DEBUFF_DURATION = 6 * 20;

	private static final String SPELL_NAME = "Reality Slash";

	// RealitySlash: Similar to Eldrask's Frost Rift, attacks in straight line and deals damage.
	// Phase 1, 2, 3, 4: 60 Damage Direct, 30 Damage Lingering. Applies 20% Slowness and Weakness debuff.
	public SpellRealitySlash(Plugin plugin, LivingEntity boss, Samwell samwell, int phase) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = mBoss.getLocation();
		mSamwell = samwell;
		mPhase = phase;
	}

	@Override
	public void run() {
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 20);
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0.5f);

		List<Player> players = PlayerUtils.playersInRange(mStartLoc, 50, true);
		List<Player> targets = new ArrayList<>();
		List<Location> locs = new ArrayList<>();
		if (players.size() >= 2) {
			int cap = (int) Math.ceil(players.size() / 3.0);
			if (cap >= 2) {
				cap = 2;
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
		mBoss.setAI(false);

		new BukkitRunnable() {
			double mT = 0;
			float mPitch = 1;
			Location mLoc = mBoss.getLocation().add(0, 0.5, 0);
			double mBossX = mLoc.getX();
			double mBossY = mLoc.getY();
			double mBossZ = mLoc.getZ();

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
						//world.spawnParticle(Particle.BARRIER, particleLine, 1, 0, 0, 0);
						Location newLoc = new Location(world, mBossX + (xloc * i), mBossY + (yloc * i), mBossZ + (zloc * i));
						if (newLoc.getBlock().getType() == Material.AIR) {
							world.spawnParticle(Particle.SQUID_INK, newLoc, 1, 0.25, 0.25, 0.25, 0);
						} else {
							world.spawnParticle(Particle.SQUID_INK, newLoc.add(0, 0.5, 0), 1, 0.25, 0.25, 0.25, 0);
						}
					}
				}
				world.playSound(mLoc, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.25f, mPitch);
				world.playSound(mLoc, Sound.ENTITY_ENDER_DRAGON_HURT, 1.25f, mPitch);
				world.spawnParticle(Particle.CLOUD, mLoc, 8, 1, 0.1, 1, 0.25);
				world.spawnParticle(Particle.SMOKE_LARGE, mLoc, 5, 1, 0.1, 1, 0.25);

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
		World world = mBoss.getWorld();

		Map<Location, Material> oldBlocks = new HashMap<>();
		Map<Location, BlockData> oldData = new HashMap<>();

		BukkitRunnable runnable = new BukkitRunnable() {
			Location mLoc = mBoss.getLocation().add(0, 0.5, 0);
			World mWorld = mLoc.getWorld();
			Vector mDir = LocationUtils.getDirectionTo(loc, mLoc).setY(0).normalize();
			BoundingBox mBox = BoundingBox.of(mLoc, 0.85, 1.2, 0.85);
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

				oldBlocks.put(bLoc.clone(), bLoc.getBlock().getType());
				oldData.put(bLoc.clone(), bLoc.getBlock().getBlockData());
				bLoc.getBlock().setType(Material.CRYING_OBSIDIAN);

				bLoc.add(0, 0.5, 0);

				locs.add(bLoc);
				if (bLoc.getBlock().getType() == Material.AIR) {
					mWorld.spawnParticle(Particle.CLOUD, bLoc, 3, 0.5, 0.5, 0.5, 0.25);
					mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, bLoc, 3, 0.5, 0.5, 0.5, 0.125);
				} else {
					Location newBLoc = bLoc.clone().add(0, 0.5, 0);
					mWorld.spawnParticle(Particle.CLOUD, newBLoc, 3, 0.5, 0.5, 0.5, 0.25);
					mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, newBLoc, 3, 0.5, 0.5, 0.5, 0.125);
				}
				mWorld.playSound(bLoc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.HOSTILE, 1, 0.85f);

				for (Player player : players) {
					if (player.getBoundingBox().overlaps(mBox)) {
						directHit(player);
					}
				}
				if (bLoc.distance(mOgLoc) >= 50) {
					this.cancel();
				}
			}

		};

		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);

		//If touching the "line" of particles, get debuffed and take damaged. Can be blocked over
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT += 5;
				for (Location loc : locs) {
					if (loc.getBlock().getType() == Material.AIR) {
						world.spawnParticle(Particle.CLOUD, loc, 1, 0.5, 0.5, 0.5, 0.075);
						world.spawnParticle(Particle.CRIT, loc, 1, 0.5, 0.5, 0.5, 0.075);
						world.spawnParticle(Particle.REDSTONE, loc, 1, 0.5, 0.5, 0.5, 0.075, BLACK_COLOR);
						world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0.5, 0.5, 0.5, 0.1);
						world.spawnParticle(Particle.DAMAGE_INDICATOR, loc, 1, 0.5, 0.5, 0.5, 0.1);
					} else {
						Location newLoc = loc.clone().add(0, 0.5, 0);
						world.spawnParticle(Particle.CLOUD, newLoc, 1, 0.5, 0.5, 0.5, 0.075);
						world.spawnParticle(Particle.CRIT, newLoc, 1, 0.5, 0.5, 0.5, 0.075);
						world.spawnParticle(Particle.REDSTONE, newLoc, 1, 0.5, 0.5, 0.5, 0.075, BLACK_COLOR);
						world.spawnParticle(Particle.EXPLOSION_NORMAL, newLoc, 1, 0.5, 0.5, 0.5, 0.1);
						world.spawnParticle(Particle.DAMAGE_INDICATOR, newLoc, 1, 0.5, 0.5, 0.5, 0.1);
					}
					BoundingBox box = BoundingBox.of(loc, 0.85, 1.2, 0.85);
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(box)) {
							lingeringHit(player);
						}
					}
				}

				if (mT >= LINGERING_DURATION || mPhase != mSamwell.mPhase || mSamwell.mDefeated) {
					this.cancel();
					for (Map.Entry<Location, Material> e : oldBlocks.entrySet()) {
						if (e.getKey().getBlock().getType() != Material.AIR) {
							e.getKey().getBlock().setType(e.getValue());
							if (oldData.containsKey(e.getKey())) {
								e.getKey().getBlock().setBlockData(oldData.get(e.getKey()));
							}
						}
					}
					locs.clear();
				}
			}

		}.runTaskTimer(mPlugin, 0, 5);
	}

	@Override
	public int cooldownTicks() {
		if (mPhase <= 3) {
			return 10 * 20;
		} else {
			return 5 * 20;
		}
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	private void directHit(Player player) {
		DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DIRECT_HIT_DAMAGE, null, false, true, SPELL_NAME);
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.SLOW, DEBUFF_DURATION, 0, false, false));
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.WEAKNESS, DEBUFF_DURATION, 0, false, false));
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.HUNGER, DEBUFF_DURATION, 0, false, false));
	}

	private void lingeringHit(Player player) {
		DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, LINGERING_HIT_DAMAGE, null, false, true, SPELL_NAME);
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.SLOW, DEBUFF_DURATION, 0, false, false));
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.WEAKNESS, DEBUFF_DURATION, 0, false, false));
		PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.HUNGER, DEBUFF_DURATION, 0, false, false));
	}
}
