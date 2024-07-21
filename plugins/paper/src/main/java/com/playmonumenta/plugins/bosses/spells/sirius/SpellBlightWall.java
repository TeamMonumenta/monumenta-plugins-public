package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.listeners.StasisListener;
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
	private final PassiveDeclaration mDeclerations;
	private final Plugin mPlugin;
	private final Sirius mSirius;
	private final double mLength;
	private final double mWidth;
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
		mDeclerations.mTpBlocked = true;
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
			final ChargeUpManager mBar = new ChargeUpManager(mSirius.mBoss, DELAY,
				Component.text("Charging Blight Wave", NamedTextColor.RED), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 75);
			final int mBlocks = mSirius.mBlocks;

			@Override
			public void run() {
				mOnCooldown = true;
				Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
				if (mTicks == 0) {
					World world = mSirius.mBoss.getWorld();
					for (Player p : mSirius.getPlayers()) {
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
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> mDeclerations.mTpBlocked = false, 5 * 20);
					this.cancel();
				}
				if (mSirius.mDone) {
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> mDeclerations.mTpBlocked = false, 5 * 20);
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
			final Location mLocOne = loc1.clone();
			final Location mLocTwo = loc2.clone();
			final int mGapPos = FastUtils.randomIntInRange(2, 6);
			final double mSpeed = speed;
			final List<BlockDisplay> mDisplays = new ArrayList<>();

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
								Transformation trans = new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1, 10, (float) Math.min(10, mWidth - i)), new AxisAngle4f());
								display.setTransformation(trans);
								display.setInterpolationDelay(-1);
								display.setInterpolationDuration(1);
								display.addScoreboardTag("SiriusDisplay");
							} else {
								//Gap
								Location loc = mLocOne.clone().add(0, h, i);
								BlockDisplay close = mSirius.mBoss.getWorld().spawn(loc, BlockDisplay.class);
								BlockDisplay far = mSirius.mBoss.getWorld().spawn(loc.clone().add(0, 0, 10f), BlockDisplay.class);
								mDisplays.add(close);
								mDisplays.add(far);
								Transformation trans = new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1, 10, 0.1f), new AxisAngle4f());
								close.setTransformation(trans);
								close.setInterpolationDelay(-1);
								close.setInterpolationDuration(1);
								close.addScoreboardTag("SiriusDisplay");
								close.setGlowing(true);
								close.setGlowColorOverride(Color.fromRGB(255, 255, 255));
								close.setBlock(Bukkit.createBlockData(Material.CYAN_STAINED_GLASS));
								far.setTransformation(trans);
								far.setInterpolationDelay(-1);
								far.setInterpolationDuration(1);
								far.addScoreboardTag("SiriusDisplay");
								far.setGlowing(true);
								far.setGlowColorOverride(Color.fromRGB(255, 255, 255));
								far.setBlock(Bukkit.createBlockData(Material.CYAN_STAINED_GLASS));
							}
						}
					}
				}
				var pList = new ArrayList<>(mSirius.getPlayers());
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
				BoundingBox boxOne = BoundingBox.of(mLocOneClone, mLocOneClone.clone().add(0.25, 30, 10 * mGapPos));
				BoundingBox boxTwo = BoundingBox.of(mLocOneClone.clone().add(0, 0, 10 + 10 * mGapPos), mLocTwoClone.add(0.25, 30, 0));
				for (Player p : pList) {
					if (!StasisListener.isInStasis(p) && (p.getBoundingBox().overlaps(boxOne) || p.getBoundingBox().overlaps(boxTwo))) {
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
				for (Display display : mDisplays) {
					Transformation trans = display.getTransformation();
					display.setTransformation(trans);
					display.setInterpolationDelay(-1);
					display.setInterpolationDuration(1);
					display.teleport(display.getLocation().add(mSpeed, 0, 0));
				}
				if (mTicks >= mLength / Math.abs(mSpeed) || mSirius.mDone || mSirius.mBoss.isDead()) {
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
