package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SteelWingHawk;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellImpactfulSwoop extends Spell {
	private static final int COOLDOWN = 6 * 20;
	private static final int ROTATE_DURATION = 10;
	private static final int SWOOP_DURATION = 30;
	private static final int STUN_DURATION = 2 * 20;
	private static final double RADIUS = 4;
	private static final double DAMAGE = 95;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final SteelWingHawk mHawk;
	private final PassivePhantomControl mPhantomControl;

	public SpellImpactfulSwoop(Plugin plugin, LivingEntity boss, SteelWingHawk hawk, PassivePhantomControl phantomControl) {
		mPlugin = plugin;
		mBoss = boss;
		mHawk = hawk;
		mPhantomControl = phantomControl;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mHawk.mSpawnLoc, SteelWingHawk.OUTER_RADIUS, true);
		if (players.isEmpty()) {
			return;
		}
		Player target = FastUtils.getRandomElement(players);
		Location loc = LocationUtils.fallToGround(target.getLocation().add(FastUtils.randomDoubleInRange(-1.5, 1.5), 0, FastUtils.randomDoubleInRange(-1.5, 1.5)), mHawk.mSpawnLoc.getY() - 10);
		mPhantomControl.setNextPoint(loc, mBoss.getLocation().distance(loc) / SWOOP_DURATION, ROTATE_DURATION, SWOOP_DURATION + ROTATE_DURATION, true);

		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, SoundCategory.HOSTILE, 2.0f, 1.2f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GOAT_SCREAMING_PREPARE_RAM, SoundCategory.HOSTILE, 2.0f, 2.0f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_CAT_HISS, SoundCategory.HOSTILE, 2.0f, 0.8f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!mBoss.isValid()) {
					this.cancel();
					return;
				}

				world.playSound(mBoss.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 2.0f, 1.5f - 0.75f * ((float) mTicks) / SWOOP_DURATION);

				if (mTicks >= SWOOP_DURATION) {
					impact(loc, world);
					this.cancel();
				}

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 8, 2);
	}

	private void impact(Location loc, World world) {
		List<Player> players = PlayerUtils.playersInRange(loc, RADIUS, true);
		if (players.isEmpty()) {
			mPhantomControl.freeze(STUN_DURATION);
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					double angle = Math.toRadians(mTicks * 20);
					Location l = mBoss.getLocation();
					l.add(FastUtils.cos(angle) * 0.75, mBoss.getHeight(), FastUtils.sin(angle) * 0.75);
					new PartialParticle(Particle.REDSTONE, l, 5, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(100, 220, 255), 1.0f)).spawnAsEnemyBuff();
					if (mTicks >= STUN_DURATION) {
						this.cancel();
					}
					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		} else {
			mPhantomControl.setRandomPoint(2);
			for (Player player : players) {
				DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, DAMAGE, null, true, false, "Impactful Swoop");
				MovementUtils.knockAway(loc, player, 0.4f, 0.9f, true);
			}
		}

		world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 2.0f, 0.4f);
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1.5f, 0.7f);
		world.playSound(loc, Sound.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.HOSTILE, 1.5f, 0.6f);
		PPCircle circle = new PPCircle(Particle.BLOCK_CRACK, loc, RADIUS).data(Material.OAK_LEAVES.createBlockData());
		circle.countPerMeter(2.5).spawnAsBoss();
		circle.countPerMeter(3).ringMode(false).randomizeAngle(true).spawnAsBoss();
		new PPCircle(Particle.CRIT, loc, RADIUS).countPerMeter(3.5).delta(0.05).spawnAsBoss();
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return mBoss.getLocation().getY() > mHawk.mSpawnLoc.getY() + 18;
	}
}
