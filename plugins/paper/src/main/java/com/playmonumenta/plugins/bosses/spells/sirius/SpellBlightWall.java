package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

public class SpellBlightWall extends Spell {

	private static final int COOLDOWN = 20 * 20;
	private static final int DAMAGE = 100;
	public static final int RADIUS = 5;
	private static final double SPEED = 0.25;
	private static final float KNOCKBACKSTRENGTH = 1;

	private boolean mOnCooldown;
	private Plugin mPlugin;
	private Sirius mSirius;
	private static final int mDamageHeight = 5;
	private double mLength;
	private static final int DELAY = 2 * 20;
	private static final int WAVEDELAY = 3 * 20;

	public SpellBlightWall(Plugin plugin, Sirius sirius) {
		mPlugin = plugin;
		mSirius = sirius;
		mOnCooldown = false;
		mLength = mSirius.mSpawnCornerOne.getX() - mSirius.mCornerTwo.getX();
	}

	@Override
	public void run() {
		int duration = (int) (mLength / SPEED);
		if (mSirius.mBlocks <= 5) {
			duration = (int) (mLength / SPEED) + WAVEDELAY;
		}
		int finalDuration = duration;
		new BukkitRunnable() {
			int mTicks = 0;
			ChargeUpManager mBar = new ChargeUpManager(mSirius.mBoss, DELAY,
				Component.text("Charging Blight Wave", NamedTextColor.RED), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 75);
			final int mBlocks = mSirius.mBlocks;

			@Override
			public void run() {
				if (mTicks == 0) {
					World world = mSirius.mBoss.getWorld();
					for (Player p : mSirius.getPlayersInArena(false)) {
						world.playSound(p, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 0.7f, 2f);
						world.playSound(p, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.4f, 1.2f);
						world.playSound(p, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.6f, 0.6f);
						world.playSound(p, Sound.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 0.3f, 0.8f);
						world.playSound(p, Sound.ENTITY_ENDERMAN_DEATH, SoundCategory.HOSTILE, 0.4f, 1.8f);
					}
				}
				if (mTicks < DELAY) {
					mBar.nextTick();
				} else {
					mBar.nextTick();
				}
				if (mTicks == DELAY) {
					mBar.setProgress(0);
					mBar.setTitle(Component.text("Unleashing Blight Wave", NamedTextColor.RED));
					mBar.setChargeTime(finalDuration);
					mBar.update();
					wave(mSirius.mCornerTwo.clone(), mSirius.mCornerTwo.clone(), SPEED);
					if (mBlocks <= 10) {
						Location loc = mSirius.mSpawnCornerOne.clone();
						loc.setZ(mSirius.mCornerTwo.getZ());
						wave(loc, loc, -SPEED);
					}
				}
				if (mTicks == DELAY + WAVEDELAY) {
					if (mBlocks <= 5) {
						wave(mSirius.mCornerTwo.clone(), mSirius.mCornerTwo.clone(), SPEED);
						Location loc = mSirius.mSpawnCornerOne.clone();
						loc.setZ(mSirius.mCornerTwo.getZ());
						wave(loc, loc, -SPEED);
					}
				}
				if (mTicks >= DELAY + finalDuration) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	private void wave(Location loc1, Location loc2, double speed) {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			Location mLocOne = loc1.clone();
			Location mLocTwo = loc2.clone();
			double mSpeed = speed;

			@Override
			public void run() {
				if (mTicks == 0) {
					mLocTwo.setZ(mSirius.mCornerOne.getZ());
					double y = mSirius.mStartLocation.getY();
					mLocOne.setY(y);
					mLocTwo.setY(y);
				}
				List<Player> pList = mSirius.getPlayersInArena(false);
				if (mTicks % 5 == 0) {
					for (Player p : pList) {
						p.playSound(p, Sound.ENTITY_WARDEN_AMBIENT, SoundCategory.HOSTILE, 0.6f, 1.5f);
						p.playSound(p, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, 0.4f, 1.1f);
					}
				}
				for (int i = 0; i < 15; i += mDamageHeight + mDamageHeight) {
					Location mLocOneClone = mLocOne.clone().add(0, i, 0);
					Location mLocTwoClone = mLocTwo.clone().add(0, i, 0);

					if (mTicks % 5 == 0) {
						for (double j = 0; j <= mDamageHeight; j += mDamageHeight / 2.0) {
							new PPLine(Particle.REDSTONE, mLocOneClone.clone().add(0, j, 0), mLocTwoClone.clone().add(0, j, 0))
								.countPerMeter(2)
								.data(new Particle.DustOptions(Color.fromRGB(0, 242, 242), 1.65f))
								.spawnAsBoss();
						}
					}
					BoundingBox box = BoundingBox.of(mLocOneClone, mLocTwoClone.add(0, mDamageHeight, 0));
					for (Player p : pList) {
						if (p.getBoundingBox().overlaps(box)) {
							DamageUtils.damage(mSirius.mBoss, p, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false, "Blight Wave");
							MovementUtils.knockAway(p.getLocation().subtract(mSpeed, 0, 0), p, KNOCKBACKSTRENGTH, true);
							p.playSound(p, Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 0.5f, 0.4f);
							p.playSound(p, Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 0.6f, 0.4f);
							p.playSound(p, Sound.ENTITY_ALLAY_HURT, SoundCategory.HOSTILE, 0.6f, 0.6f);
							p.playSound(p, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 0.6f, 1.4f);
						}
					}
				}
				mLocOne.add(mSpeed, 0, 0);
				mLocTwo.add(mSpeed, 0, 0);
				if (mTicks >= mLength / Math.abs(mSpeed) || mSirius.mDone) {
					this.cancel();
				}
				mTicks++;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}
}
