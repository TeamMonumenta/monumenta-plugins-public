package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellEarthshake extends Spell {

	private static final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.AIR,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.CHEST,
		Material.END_PORTAL,
		Material.END_PORTAL_FRAME
	);

	private final Plugin mPlugin;
	private final LivingEntity mLauncher;
	private final int mRadius;
	private final int mDuration;
	private final int mCooldown;
	private final boolean mLineOfSight;
	private final int mDamage;
	private final int mRange;
	private final double mKnockUpSpeed;
	private final boolean mFlyingBlocks;

	public SpellEarthshake(Plugin plugin, LivingEntity launcher, int radius, int duration, int damage, int cooldown, int range, double knockUpSpeed, boolean needLineOfSight, boolean flyingBlocks) {
		mPlugin = plugin;
		mLauncher = launcher;
		mRadius = radius;
		mDuration = duration;
		mDamage = damage;
		mCooldown = cooldown;
		mRange = range;
		mKnockUpSpeed = knockUpSpeed;
		mLineOfSight = needLineOfSight;
		mFlyingBlocks = flyingBlocks;
	}

	@Override
	public void run() {
		Player target = null;
		List<Player> players = PlayerUtils.playersInRange(mLauncher.getLocation(), mRange, false);
		if (!players.isEmpty()) {
			// Single target chooses a random player within range
			Collections.shuffle(players);
			for (Player player : players) {
				if (!mLineOfSight || LocationUtils.hasLineOfSight(mLauncher, player)) {
					target = player;
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 5f, 0.75f);
					break;
				}
			}
		}
		if (target == null) {
			return;
		}
		Location targetLocation = target.getLocation();

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mLauncher.isDead() || !mLauncher.isValid() || EntityUtils.isStunned(mLauncher) || EntityUtils.isSilenced(mLauncher)) {
					mLauncher.setAI(true);
					this.cancel();
					return;
				}
				mTicks++;
				chargeActions(targetLocation, mTicks);
				if (mTicks >= mDuration) {
					this.cancel();
					performExplosion(targetLocation);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	protected void chargeActions(Location loc, int ticks) {
		World world = loc.getWorld();

		if (ticks % 2 == 0) {
			world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 1, ((double) mRadius * 2) / 2, ((double) mRadius * 2) / 2, ((double) mRadius * 2) / 2, 0.05);
		}

		if (ticks % 20 == 0 && ticks > 0) {
			world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 3f, 0.5f);
			world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 3f, 0.5f);
			for (int i = 0; i < 360; i += 18) {
				world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(FastUtils.cos(Math.toRadians(i)) * mRadius, 0.2, FastUtils.sin(Math.toRadians(i)) * mRadius), 1, 0.1, 0.1, 0.1, 0);
			}
			world.spawnParticle(Particle.BLOCK_CRACK, loc, 80, mRadius / 2.0, 0.1, mRadius / 2.0, Bukkit.createBlockData(Material.DIRT));
			world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 8, mRadius / 2.0, 0.1, mRadius / 2.0, 0);
		}

		if (ticks <= (mDuration - 5)) {
			world.playSound(mLauncher.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.HOSTILE, 1, 0.25f + (ticks / 100f));
		}

		world.spawnParticle(Particle.BLOCK_CRACK, loc, 2, mRadius / 2.0, 0.1, mRadius / 2.0, Bukkit.createBlockData(Material.STONE));
		world.spawnParticle(Particle.LAVA, loc, 2, 0.25, 0.25, 0.25, 0.1);

		mLauncher.getWorld().spawnParticle(Particle.DRIP_LAVA, mLauncher.getLocation().clone().add(0, mLauncher.getHeight() / 2, 0), 2, 0.25, 0.45, 0.25, 1);

	}


	protected void performExplosion(Location loc) {
		World world = loc.getWorld();

		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3f, 1.35f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 3f, 0.5f);

		world.spawnParticle(Particle.CLOUD, loc, 150, 0, 0, 0, 0.5);
		world.spawnParticle(Particle.LAVA, loc, 35, mRadius / 2.0, 0.1, mRadius / 2.0, 0);
		world.spawnParticle(Particle.BLOCK_CRACK, loc, 200, mRadius / 2.0, 0.1, mRadius / 2.0, Bukkit.createBlockData(Material.DIRT));
		world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 35, mRadius / 2.0, 0.1, mRadius / 2.0, 0.1);

		for (int i = 0; i < 100; i++) {
			world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(-3 + FastUtils.RANDOM.nextDouble() * 6, 0.1, -3 + FastUtils.RANDOM.nextDouble() * 6), 0, 0, 1, 0, 0.2 + FastUtils.RANDOM.nextDouble() * 0.4);
		}

		if (mFlyingBlocks) {
			ArrayList<Block> blocks = new ArrayList<>();

			//Populate the blocks array with nearby blocks- logic here to get the topmost block with air above it
			for (int x = -mRadius; x <= mRadius; x++) {
				for (int z = -mRadius; z <= mRadius; z++) {
					Block lowerBlock = world.getBlockAt(loc.clone().add(x, -2, z));
					for (int y = -1; y <= 2; y++) {
						Block currentBlock = world.getBlockAt(loc.clone().add(x, y, z));
						if (!lowerBlock.getType().isAir() && currentBlock.getType().isAir()) {
							blocks.add(lowerBlock);
							break;
						}
						lowerBlock = currentBlock;
					}
				}
			}

			//Make the blocks go flying
			for (Block b : blocks) {
				if (b == null) {
					continue;
				}

				Material material = b.getType();
				if (!mIgnoredMats.contains(material) && !LocationUtils.containsWater(b) && !(b.getBlockData() instanceof Bed) && FastUtils.RANDOM.nextInt(4) > 1) {
					double x = (FastUtils.RANDOM.nextInt(5) - 2) / 10.0;
					double z = (FastUtils.RANDOM.nextInt(5) - 2) / 10.0;

					FallingBlock block = world.spawnFallingBlock(b.getLocation(), b.getBlockData());
					block.setVelocity(new Vector(x, 1.0, z));
					world.getBlockAt(b.getLocation()).setType(Material.AIR);
				}
			}
		}

		// Damage and knock up players
		for (Player p : PlayerUtils.playersInRange(loc, mRadius, true)) {
			world.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.0f);
			BossUtils.blockableDamage(mLauncher, p, DamageType.BLAST, mDamage);
			double knockupSpeed = mKnockUpSpeed + (p.getLocation().distance(loc) <= mRadius / 2.0 ? 0.5 : 0);
			p.setVelocity(p.getVelocity().add(new Vector(0.0, knockupSpeed, 0.0)));
		}
		//Knock up other mobs because it's fun
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRadius);
		for (LivingEntity mob : mobs) {
			if (mob.getLocation().distance(loc) <= mRadius) {
				mob.setVelocity(mob.getVelocity().add(new Vector(0.0, mKnockUpSpeed + 1.0, 0.0)));
			}
		}
	}

}
