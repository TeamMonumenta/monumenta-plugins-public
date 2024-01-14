package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class SpellBlightWall extends Spell {

	private static final int COOLDOWN = 25 * 20;
	private static final int DAMAGE = 80;
	public static final int RADIUS = 5;
	private static final double SPEED = 0.2;
	private static final float KNOCKBACKSTRENGTH = 1;

	private boolean mOnCooldown;
	private PassiveDeclaration mDeclerations;
	private Plugin mPlugin;
	private Sirius mSirius;
	private double mLength;
	private double mWidth;
	private static final int DELAY = 5 * 20;
	private static final int WAVEDELAY = 7 * 20;
	private boolean mPrimed;

	public SpellBlightWall(Plugin plugin, Sirius sirius, PassiveDeclaration decleration) {
		mPlugin = plugin;
		mSirius = sirius;
		mOnCooldown = false;
		mPrimed = false;
		mLength = mSirius.mSpawnCornerOne.getX() - mSirius.mCornerTwo.getX();
		mWidth = mSirius.mCornerOne.getZ() - mSirius.mCornerTwo.getZ();
		mDeclerations = decleration;
	}

	@Override
	public void run() {
		mPrimed = true;
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!mDeclerations.mSwapping && !mSirius.mCheeseLock && !mDeclerations.mTp) {
					cast();
					mPrimed = false;
					this.cancel();
				}
				if (mTicks >= 30 * 20) {
					mPrimed = false;
					this.cancel();
				}
				mTicks += 5;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(runnable);
	}

	private void cast() {
		int duration = (int) (mLength / SPEED) + 1;
		if (mSirius.mBlocks <= 10) {
			duration = (int) (mLength / SPEED) + WAVEDELAY + 1;
		}
		if (mSirius.mBlocks <= 5) {
			duration = (int) (mLength / SPEED) + WAVEDELAY + WAVEDELAY + 1;
		}
		int finalDuration = duration;
		new BukkitRunnable() {
			int mTicks = 0;
			ChargeUpManager mBar = new ChargeUpManager(mSirius.mBoss, DELAY,
				Component.text("Charging Blight Wave", NamedTextColor.RED), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 75);
			final int mBlocks = mSirius.mBlocks;

			@Override
			public void run() {
				mOnCooldown = true;
				Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
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
				mBar.nextTick();

				if (mTicks == DELAY) {
					mBar.reset();
					mBar.setTitle(Component.text("Unleashing Blight Wave", NamedTextColor.RED));
					mBar.setChargeTime(finalDuration);
					mBar.update();
					wave(mSirius.mCornerTwo.clone(), mSirius.mCornerTwo.clone(), SPEED);
				}
				if (mTicks == DELAY + WAVEDELAY) {
					if (mBlocks <= 10) {
						wave(mSirius.mCornerTwo.clone(), mSirius.mCornerTwo.clone(), SPEED);
					}
				}
				if (mTicks == DELAY + WAVEDELAY + WAVEDELAY) {
					if (mBlocks <= 5) {
						wave(mSirius.mCornerTwo.clone(), mSirius.mCornerTwo.clone(), SPEED);
					}
				}
				if (mTicks >= DELAY + finalDuration) {
					this.cancel();
				}
				if (mSirius.mDone) {
					this.cancel();
					mBar.remove();
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
		return !mOnCooldown && !mPrimed;
	}

	private void wave(Location loc1, Location loc2, double speed) {
		new BukkitRunnable() {
			int mTicks = 0;
			Location mLocOne = loc1.clone();
			Location mLocTwo = loc2.clone();
			int mGapPos = FastUtils.randomIntInRange(1, 7);
			double mSpeed = speed;
			List<BlockDisplay> mDisplays = new ArrayList<>();

			@Override
			public void run() {
				if (mTicks == 0) {
					mLocTwo.setZ(mSirius.mCornerOne.getZ());
					double y = mSirius.mStartLocation.getY();
					mLocOne.setY(y);
					mLocTwo.setY(y);
					for (int h = 0; h <= 30; h += 10) {
						for (int i = 0; i < mWidth; i += 10) {
							if (mGapPos * 10 != i) {
								Location loc = mLocOne.clone().add(0, h, i);
								BlockDisplay display = mSirius.mBoss.getWorld().spawn(loc, BlockDisplay.class);
								mDisplays.add(display);
								display.setBlock(Bukkit.createBlockData(Material.CYAN_STAINED_GLASS));
								display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1, 10, (float) Math.min(10, mWidth - i)), new AxisAngle4f()));
								display.setInterpolationDelay(-1);
								display.setInterpolationDuration(1);
								display.addScoreboardTag("SiriusDisplay");
								Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
									Transformation trans = display.getTransformation();
									display.setTransformation(new Transformation(new Vector3f((float) mLength, 0, 0), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
									display.setInterpolationDelay(-1);
									display.setInterpolationDuration((int) (mLength / mSpeed));
								}, 1);
							}
						}
					}
				}
				List<Player> pList = mSirius.getPlayersInArena(false);
				if (mTicks % 5 == 0) {
					for (Player p : pList) {
						p.playSound(p, Sound.ENTITY_WARDEN_AMBIENT, SoundCategory.HOSTILE, 0.6f, 1.5f);
						p.playSound(p, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, 0.4f, 1.1f);
					}
				}
				Location mLocOneClone = mLocOne.clone();
				Location mLocTwoClone = mLocTwo.clone().add(0, 30, 10 * mGapPos);
				//in case displays fail
				new PPLine(Particle.REDSTONE, mLocOne, mLocOneClone.clone().add(0, 0, 10 * mGapPos)).countPerMeter(1).data(new Particle.DustOptions(Color.fromRGB(0, 242, 242), 1.0f)).spawnAsBoss();
				new PPLine(Particle.REDSTONE, mLocOneClone.clone().add(0, 0, 10 + 10 * mGapPos), mLocTwo).countPerMeter(1).data(new Particle.DustOptions(Color.fromRGB(0, 242, 242), 1.0f)).spawnAsBoss();
				BoundingBox boxOne = BoundingBox.of(mLocOneClone, mLocOneClone.clone().add(0, 30, 10 * mGapPos));
				BoundingBox boxTwo = BoundingBox.of(mLocOneClone.clone().add(0, 0, 10 + 10 * mGapPos), mLocTwoClone.add(0, 30, 0));
				for (Player p : pList) {
					if (p.getBoundingBox().overlaps(boxOne) || p.getBoundingBox().overlaps(boxTwo)) {
						DamageUtils.damage(mSirius.mBoss, p, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false, "Blight Wave");
						MovementUtils.knockAway(p.getLocation().subtract(mSpeed, 0, 0), p, KNOCKBACKSTRENGTH, false);
						p.playSound(p, Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 0.5f, 0.4f);
						p.playSound(p, Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 0.6f, 0.4f);
						p.playSound(p, Sound.ENTITY_ALLAY_HURT, SoundCategory.HOSTILE, 0.6f, 0.6f);
						p.playSound(p, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 0.6f, 1.4f);
					}
				}
				mLocOne.add(mSpeed, 0, 0);
				mLocTwo.add(mSpeed, 0, 0);
				if (mTicks >= mLength / Math.abs(mSpeed) || mSirius.mDone) {
					this.cancel();
					for (Display display : mDisplays) {
						display.remove();
					}
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
