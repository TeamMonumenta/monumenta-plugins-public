package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellBlightBomb extends Spell {

	private static final int COOLDOWN = 10 * 20;
	private boolean mOnCooldown;
	private final Sirius mSirius;
	private final Plugin mPlugin;
	private final PassiveStarBlightConversion mConverter;
	private static final double BOMBSPERPLAYER = 0.25;
	private static final int BOMBFLIGHTDURATION = 2 * 20; // edit the equation if you change this
	private static final int BOMBDURATION = 5 * 20;
	private static final int RADIUS = 5;

	public SpellBlightBomb(Sirius sirius, Plugin plugin, PassiveStarBlightConversion converter) {
		mSirius = sirius;
		mOnCooldown = false;
		mPlugin = plugin;
		mConverter = converter;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
		int mBombCount = (int) (mSirius.getPlayers().size() * BOMBSPERPLAYER + 1);
		if (mSirius.mBlocks <= 10) {
			mBombCount *= 2;
		}
		World world = mSirius.mBoss.getWorld();
		Location loc = mSirius.mBoss.getLocation();
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.HOSTILE, 0.5f, 0.4f);
		world.playSound(loc, Sound.ENTITY_WITCH_THROW, SoundCategory.HOSTILE, 2f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 2f, 0.1f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 0.6f, 0.6f);


		for (int i = 0; i < mBombCount; i++) {
			Location mLoc = mSirius.getValidLoc();
			if (!mLoc.equals(mSirius.mBoss.getLocation())) {
				bomb(mLoc);
			}
		}
	}


	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	private void bomb(Location target) {
		//Get real start location
		Team mGold = ScoreboardUtils.getExistingTeamOrCreate("gold");
		BlockDisplay grenade = mSirius.mBoss.getWorld().spawn(mSirius.mBoss.getLocation().clone().subtract(0.5, 2.5, 0.5), BlockDisplay.class);
		grenade.setBlock(Bukkit.createBlockData(Material.SCULK));
		grenade.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf()));
		grenade.setInterpolationDuration(2);
		Location endPoint = mSirius.mBoss.getLocation().clone();
		endPoint.setY(grenade.getLocation().getY());


		BukkitRunnable run = new BukkitRunnable() {
			int mTicks = 0;
			final float mX = (float) ((target.getX() - grenade.getLocation().getX()) * (1.0 / BOMBFLIGHTDURATION));
			final float mZ = (float) ((target.getZ() - grenade.getLocation().getZ()) * (1.0 / BOMBFLIGHTDURATION));
			@Nullable
			LivingEntity mBomb = null;

			@Override
			public void run() {
				if (isCancelled()) {
					return;
				}
				if (mSirius.mBoss.isDead()) {
					if (mBomb != null) {
						mBomb.remove();
					}
					this.cancel();
					return;
				}
				if (mTicks <= BOMBFLIGHTDURATION) {
					double currentHeight;
					if (mTicks == BOMBFLIGHTDURATION) {
						currentHeight = LocationUtils.fallToGround(grenade.getLocation().add(mX * mTicks, 0, mZ * mTicks), mSirius.mBoss.getLocation().getY() - 10).getY() - grenade.getLocation().getY();
					} else {
						currentHeight = (-0.008 * ((mTicks - 11.5) * (mTicks - 11.5)) + 6.5);
					}
					grenade.setInterpolationDelay(-1);
					grenade.setTransformation(new Transformation(new Vector3f(mX * mTicks - 0.5f, (float) currentHeight, mZ * mTicks - 0.5f), new Quaternionf(), new Vector3f(1), new Quaternionf()));
					grenade.setInterpolationDelay(-1);
					if (mTicks != 0) {
						int tick = mTicks - 1;
						Location mTempGrenadeLoc = grenade.getLocation().add(mX * tick, (-0.008 * ((tick - 11.5) * (tick - 11.5)) + 6.5), mZ * tick);
						Vector moveDistance = new Vector(mX, currentHeight - (-0.008 * ((tick - 11.5) * (tick - 11.5)) + 6.5), mZ).multiply(0.5);
						for (int i = 0; i < 2; i++) {
							mTempGrenadeLoc.add(moveDistance);
							Block block = mTempGrenadeLoc.getBlock();
							if (block.isSolid()) {
								mConverter.convertColumn(block.getX(), block.getZ());
							}
						}
					}
				}
				if (mTicks == 42) {
					grenade.remove();
					mBomb = (LivingEntity) LibraryOfSoulsIntegration.summon(target, "BlightBomb");
					if (mBomb != null) {
						mBomb.addScoreboardTag(Sirius.MOB_TAG);
						mGold.addEntity(mBomb);
						mBomb.setGlowing(true);
						mBomb.customName(Component.text(mBomb.getName(), NamedTextColor.WHITE));
						World world = mBomb.getWorld();
						Location loc = mBomb.getLocation();
						world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 0.4f, 1.2f);
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.4f, 1.4f);
						world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 2f);
					}
				}

				//explode
				if (mTicks >= BOMBDURATION + BOMBFLIGHTDURATION) {
					if (mBomb != null && !mBomb.isDead()) {
						World world = mBomb.getWorld();
						Location loc = mBomb.getLocation();
						if (mSirius.mBlocks <= 5) {
							mConverter.convertSphere(RADIUS, mBomb.getLocation());
						} else {
							mConverter.convertSphere(RADIUS + 2, mBomb.getLocation());
						}
						mBomb.remove();
						world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.3f, 0.6f);
						world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.6f, 0.6f);
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.4f, 0.7f);
						world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 0.4f, 0.6f);
						world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 0.9f);
						world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 0.6f, 1f);


					}
					this.cancel();
					return;
				}


				mTicks += 1;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				grenade.remove();
				super.cancel();
				mActiveRunnables.remove(this);
			}
		};
		run.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(run);

	}

}
