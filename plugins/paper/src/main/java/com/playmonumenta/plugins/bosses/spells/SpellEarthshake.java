package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.EarthshakeBoss;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
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
		Material.END_PORTAL_FRAME,
		Material.LIGHT
	);

	private final Plugin mPlugin;
	private final LivingEntity mLauncher;
	private final EarthshakeBoss.Parameters mParameters;

	public SpellEarthshake(Plugin plugin, LivingEntity launcher, EarthshakeBoss.Parameters parameters) {
		mPlugin = plugin;
		mLauncher = launcher;
		mParameters = parameters;
	}

	@Override
	public void run() {
		for (LivingEntity target : mParameters.TARGETS.getTargetsList(mLauncher)) {
			mParameters.SOUND_WARNING.play(target.getLocation());
			performEarthshake(target);
		}
	}

	@Override
	public int cooldownTicks() {
		return mParameters.COOLDOWN;
	}

	protected void performEarthshake(LivingEntity target) {
		if (target == null) {
			return;
		}

		new BukkitRunnable() {
			int mTicks = 0;
			final Location mTargetLocation = target.getLocation().clone();

			@Override
			public void run() {
				if (mLauncher.isDead() || !mLauncher.isValid() || EntityUtils.isStunned(mLauncher) || EntityUtils.isSilenced(mLauncher)) {
					mLauncher.setAI(true);
					this.cancel();
					return;
				}
				mTicks++;
				chargeActions(mTargetLocation, mTicks);
				if (mTicks >= mParameters.FUSE_TIME) {
					this.cancel();
					performExplosion(mTargetLocation);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	protected void chargeActions(Location loc, int ticks) {

		if (ticks % 2 == 0) {
			mParameters.PARTICLES_CHARGE_TWO_TICKS.spawn(mLauncher, loc);
		}

		if (ticks % 20 == 0 && ticks > 0) {
			mParameters.SOUND_CHARGE_TARGET.play(loc);
			for (int i = 0; i < 360; i += 18) {
				mParameters.PARTICLES_CHARGE_TWENTY_TICKS_BORDER.spawn(mLauncher, loc.clone().add(FastUtils.cos(Math.toRadians(i)) * mParameters.RADIUS, 0.2, FastUtils.sin(Math.toRadians(i)) * mParameters.RADIUS));
			}
			mParameters.PARTICLES_CHARGE_TWENTY_TICKS.spawn(mLauncher, loc);
		}

		if (ticks <= (mParameters.FUSE_TIME - 5)) {
			mParameters.SOUND_CHARGE_BOSS.play(mLauncher.getLocation());
		}

		mParameters.PARTICLES_CHARGE.spawn(mLauncher, loc);

		mParameters.PARTICLES_CHARGE_BOSS.spawn(mLauncher, mLauncher.getLocation().clone().add(0, mLauncher.getHeight() / 2, 0));

	}


	protected void performExplosion(Location loc) {
		World world = loc.getWorld();

		mParameters.SOUND_EXPLOSION.play(loc);
		mParameters.PARTICLES_EXPLOSION.spawn(mLauncher, loc);

		for (int i = 0; i < 100; i++) {
			mParameters.PARTICLES_EXPLOSION_DIRECTIONAL.spawn(mLauncher, loc.clone().add(FastUtils.randomDoubleInRange(-1, 1) * mParameters.RADIUS * 0.75, 0.1, FastUtils.randomDoubleInRange(-1, 1) * mParameters.RADIUS * 0.75),
				0, 1, 0, 0.2 + FastUtils.RANDOM.nextDouble() * 0.4);
		}

		if (mParameters.FLY_BLOCKS || mParameters.REPLACE_BLOCKS != Material.AIR) {
			ArrayList<Block> blocks = new ArrayList<>();

			//Populate the blocks array with nearby blocks- logic here to get the topmost block with air above it
			for (int x = -mParameters.RADIUS; x <= mParameters.RADIUS; x++) {
				int zDelta = (int) Math.round(Math.sqrt(mParameters.RADIUS * mParameters.RADIUS - x * x)); // choose only block in a circle
				for (int z = -zDelta; z <= zDelta; z++) {
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
				if (!mIgnoredMats.contains(material) && !BlockUtils.containsWater(b) && !(b.getBlockData() instanceof Bed) && FastUtils.RANDOM.nextDouble() < mParameters.FLY_BLOCKS_CHANCE) {
					if (mParameters.FLY_BLOCKS) {
						double x = (FastUtils.RANDOM.nextInt(5) - 2) / 10.0;
						double z = (FastUtils.RANDOM.nextInt(5) - 2) / 10.0;
						FallingBlock block = world.spawnFallingBlock(b.getLocation(), b.getBlockData());
						block.setVelocity(new Vector(x, 1.0, z));
					}
					world.getBlockAt(b.getLocation()).setType(mParameters.REPLACE_BLOCKS);
				}
			}
		}

		// Damage and knock up players
		for (Player p : PlayerUtils.playersInRange(loc, mParameters.RADIUS, true)) {
			mParameters.SOUND_EXPLOSION_PLAYER.play(p.getLocation());
			BossUtils.blockableDamage(mLauncher, p, DamageType.BLAST, mParameters.DAMAGE);
			double knockupSpeed = mParameters.KNOCK_UP_SPEED + (p.getLocation().distance(loc) <= mParameters.RADIUS / 2.0 ? 0.5 : 0);
			p.setVelocity(p.getVelocity().add(new Vector(0.0, knockupSpeed, 0.0)));
		}
		//Knock up other mobs because it's fun
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mParameters.RADIUS);
		for (LivingEntity mob : mobs) {
			if (mob.getLocation().distance(loc) <= mParameters.RADIUS) {
				mob.setVelocity(mob.getVelocity().add(new Vector(0.0, mParameters.KNOCK_UP_SPEED + 1.0, 0.0)));
			}
		}
	}

}
