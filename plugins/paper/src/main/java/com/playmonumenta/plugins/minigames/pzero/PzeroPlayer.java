package com.playmonumenta.plugins.minigames.pzero;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PzeroPlayer {
	public static final int MAX_ENERGY = 30;
	public static final int BOOST_ENERGY_CONSUMPTION = 5;
	public static final int BOOST_TICKS = 3 * 20;
	public static final String PIG_LOS_NAME = "PZeroKartMKII";
	public static final double PIG_BASE_SPEED = 2.5;
	public static final double PIG_BOOSTED_SPEED = 3.5;
	public static final String BOOST_ROD_LOOT_TABLE = "epic:event/winter2023/boost_carrot_3000";
	public static final TextColor GOLD_COLOR = TextColor.fromHexString("#d4af37");
	public static final TextColor SILVER_COLOR = TextColor.fromHexString("#c0c0c0");
	public static final TextColor BRONZE_COLOR = TextColor.fromHexString("#cd7f32");
	public static final TextColor OTHER_COLOR = TextColor.fromHexString("#0f9cd6");
	// Ticks within which the hit player has to crash out to count as a kill
	public static final int KILL_CREDIT_MAX_TICKS = 2 * 20;
	public static final int KILL_ENERGY_AWARD = 8;
	public static final int MAX_ENERGY_FROM_PIT_PER_LAP = 12;
	public static final int LOW_ON_ENERGY_EFFECTS_THRESHOLD = 5;

	private final Player mPlayer;
	private final String mMapName;
	private final int mLapCount;
	private final @Nullable Pig mPig;
	private final @Nullable ItemStack mRodItem;

	private int mCurrentEnergy;
	private int mRemainingBoostTicks;
	private boolean mBeingLaunched;
	private boolean mIsInGracePeriod;
	private boolean mIsExploding;
	private boolean mIsInWinAnimation;
	private int mCurrentCheckpoint;
	private int mCurrentLap;
	private int mTimer;
	private int mLapTimer;
	private @Nullable PzeroPlayer mLastHitBy;
	private int mLastHitTicks;
	private int mPitEnergyLeftThisLap;

	public PzeroPlayer(Player player, String mapName, int lapCount) {
		mPlayer = player;
		mMapName = mapName;
		mLapCount = lapCount;
		mCurrentEnergy = MAX_ENERGY;
		mRemainingBoostTicks = 0;
		mBeingLaunched = false;
		mIsInGracePeriod = false;
		mIsExploding = false;
		mIsInWinAnimation = false;
		mCurrentCheckpoint = 0;
		mCurrentLap = 0;
		mTimer = 0;
		mLapTimer = 0;
		mLastHitBy = null;
		mLastHitTicks = 0;
		mPitEnergyLeftThisLap = MAX_ENERGY_FROM_PIT_PER_LAP;
		mPig = spawnAndMountPig();
		mRodItem = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKeyUtils.fromString(BOOST_ROD_LOOT_TABLE));
	}

	private @Nullable Pig spawnAndMountPig() {
		Entity entity = LibraryOfSoulsIntegration.summon(mPlayer.getLocation(), PIG_LOS_NAME);
		if (entity instanceof Pig pig) {
			pig.addPassenger(mPlayer);
			return pig;
		}
		return null;
	}

	public void tryBoost() {
		if (!Plugin.getInstance().mPzeroManager.getMap(mMapName).isRunning() || mCurrentEnergy < BOOST_ENERGY_CONSUMPTION || mRemainingBoostTicks > 0) {
			return;
		}

		mCurrentEnergy -= BOOST_ENERGY_CONSUMPTION;
		mRemainingBoostTicks = BOOST_TICKS;
		boostStartAesthetics();

		if (mPig != null) {
			EntityUtils.setAttributeBase(mPig, Attribute.GENERIC_MOVEMENT_SPEED, PIG_BOOSTED_SPEED);
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				mRemainingBoostTicks--;
				if (mRemainingBoostTicks <= 0) {
					mRemainingBoostTicks = 0;
					if (mPig != null) {
						EntityUtils.setAttributeBase(mPig, Attribute.GENERIC_MOVEMENT_SPEED, PIG_BASE_SPEED);
					}
					cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void boostStartAesthetics() {
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 2, 2);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 2, 0.65f);
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 25).extra(0.5).spawnAsPlayerActive(mPlayer);
	}

	private void doPorksplosion(boolean eliminated) {
		// Lose animation
		mIsExploding = true;
		removeBoostRod();
		PzeroPlayer pzPlayer = this;
		Plugin.getInstance().mPzeroManager.lose(pzPlayer, eliminated);
		if (mPig != null) {
			mPig.setAI(false);
		}
		new BukkitRunnable() {
			final int mRotationTicks = 40;
			final int mPorkchopAmount = 25;
			final float mYawIncrease = 35;

			int mTick = 0;

			@Override
			public void run() {
				if (mPig != null) {
					new PartialParticle(Particle.SMOKE_LARGE, mPig.getLocation(), 5).extra(0.1).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.CLOUD, mPig.getLocation(), 1).extra(0.1).spawnAsPlayerActive(mPlayer);
					mPig.getWorld().playSound(mPig.getLocation(), Sound.ENTITY_PIG_AMBIENT, SoundCategory.PLAYERS, 2, 1);
					mPig.setRotation(mPig.getEyeLocation().getYaw() + mYawIncrease, mPig.getEyeLocation().getPitch());
				}

				mTick++;
				if (mTick >= mRotationTicks) {
					if (mPig != null) {
						launchPork();
						new PartialParticle(Particle.EXPLOSION_LARGE, mPig.getLocation(), 5).spawnAsPlayerActive(mPlayer);
						mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 3, 0.85f);
						mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_PIG_DEATH, SoundCategory.PLAYERS, 3, 1);
						mPig.remove();
					}
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> mPlayer.setVelocity(new Vector(0, 1.5, 0)));
					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
						Plugin.getInstance().mPzeroManager.sendToSpectatorArea(pzPlayer, Plugin.getInstance().mPzeroManager.getMap(mMapName));
					}, 15);
					cancel();
				}
			}

			public void launchPork() {
				if (mPig == null) {
					return;
				}

				for (int i = 0; i < mPorkchopAmount; i++) {
					Item porkchop = EntityUtils.createUnpickableItem(Material.PORKCHOP, mPig.getLocation());
					if (porkchop != null) {
						ItemUtils.setPlainName(porkchop.getItemStack(), "P-Zero Kart Scrap");
						double xVel = FastUtils.randomDoubleInRange(-0.6, 0.6);
						double yVel = FastUtils.randomDoubleInRange(0.4, 0.8);
						double zVel = FastUtils.randomDoubleInRange(-0.6, 0.6);

						porkchop.setVelocity(new Vector(xVel, yVel, zVel));

						// Remove the item when it hits the ground
						new BukkitRunnable() {
							final Item mItem = porkchop;

							int mTick = 0;

							@Override
							public void run() {
								new PartialParticle(Particle.SPELL_INSTANT, mItem.getLocation(), 1).spawnAsPlayerActive(mPlayer);

								mTick++;
								if (mItem.isOnGround() || mTick >= 200) {
									new PartialParticle(Particle.EXPLOSION_NORMAL, mItem.getLocation(), 1).spawnAsPlayerActive(mPlayer);
									mPlayer.getWorld().playSound(mItem.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, SoundCategory.PLAYERS, 1, 1.75f);
									mItem.remove();
								}

								if (!mItem.isValid()) {
									cancel();
								}
							}
						}.runTaskTimer(Plugin.getInstance(), 0, 1);
					}
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void doPorkscension(PzeroMap map) {
		// Win animation
		mIsInWinAnimation = true;
		removeBoostRod();
		PzeroPlayer pzPlayer = this;
		mPlayer.playSound(mPlayer, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 10, 0.8f);
		mPlayer.playSound(mPlayer, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 10, 1);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.playSound(mPlayer, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, SoundCategory.PLAYERS, 10, 0.7f), 8);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.playSound(mPlayer, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.PLAYERS, 10, 0.75f), 20);
		if (mPig != null) {
			mPig.setAI(false);
		}
		new BukkitRunnable() {
			final int mRotationTicks = 40;
			final float mYawIncrease = 35;
			final double mYIncrease = 1;

			int mTick = 0;

			@Override
			public void run() {
				if (mPig != null) {
					new PartialParticle(Particle.FLAME, mPig.getLocation(), 1).spawnAsPlayerActive(mPlayer);
					mPig.getWorld().playSound(mPig.getLocation(), Sound.ENTITY_PIG_AMBIENT, SoundCategory.PLAYERS, 2, 1.5f);
					mPig.teleport(mPig.getLocation().add(new Vector(0, mYIncrease, 0)));
					mPig.setRotation(mPig.getEyeLocation().getYaw() + mYawIncrease, mPig.getEyeLocation().getPitch());
				}

				mTick++;
				if (mTick >= mRotationTicks) {
					if (mPig != null) {
						new PartialParticle(Particle.EXPLOSION_LARGE, mPig.getLocation(), 5).spawnAsPlayerActive(mPlayer);
						mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_PIG_DEATH, SoundCategory.PLAYERS, 3, 1.75f);
						mPig.remove();
					}

					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
						Plugin.getInstance().mPzeroManager.sendToSpectatorArea(pzPlayer, Plugin.getInstance().mPzeroManager.getMap(mMapName));
					}, 5);
					cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void giveBoostRod() {
		if (mRodItem == null) {
			return;
		}

		InventoryUtils.giveItem(mPlayer, mRodItem);
	}

	public void removeBoostRod() {
		if (mRodItem == null) {
			return;
		}

		mPlayer.getInventory().removeItemAnySlot(mRodItem);
	}

	public boolean moveRodFromOffhandToMainhand() {
		if (mRodItem == null) {
			return false;
		}

		if (mPlayer.getInventory().getItemInOffHand().equals(mRodItem)) {
			removeBoostRod();
			giveBoostRod();
			return true;
		}
		return false;
	}

	public void launch(double x, double y, double z, int delay, int gracePeriodTicks) {
		if (mBeingLaunched || mPig == null) {
			return;
		}
		mBeingLaunched = true;

		removeBoostRod();
		new PPCircle(Particle.CLOUD, mPig.getLocation(), 0.7).count(20).ringMode(true).directionalMode(true)
			.delta(1, 0, 0).rotateDelta(true).extra(0.2).spawnAsPlayerActive(mPlayer);
		mPlayer.playSound(mPlayer, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 2, 1.15f);
		mPlayer.playSound(mPlayer, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 2, 1.75f);
		mPig.setVelocity(new Vector(x, y, z));
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			giveBoostRod();
			mBeingLaunched = false;
				if (gracePeriodTicks > 0) {
					mIsInGracePeriod = true;
					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mIsInGracePeriod = false, gracePeriodTicks);
				}
		}, delay);
	}

	public void resetObtainableEnergyThisLap() {
		mPitEnergyLeftThisLap = MAX_ENERGY_FROM_PIT_PER_LAP;
	}

	public void restoreEnergy(int amount, boolean fromPit) {
		int actualAmount = fromPit ? Math.min(mPitEnergyLeftThisLap, amount) : amount;
		mCurrentEnergy = Math.min(MAX_ENERGY, mCurrentEnergy + actualAmount);

		if (fromPit) {
			mPitEnergyLeftThisLap -= actualAmount;
		}
	}

	public void depleteEnergy(int amount) {
		if (mIsInWinAnimation) {
			return;
		}

		mCurrentEnergy = Math.max(0, mCurrentEnergy - amount);
		if (mCurrentEnergy == 0 && !mIsExploding) {
			boolean eliminated = false;
			if (mLastHitBy != null && Bukkit.getCurrentTick() - mLastHitTicks <= KILL_CREDIT_MAX_TICKS) {
				eliminated = true;
				mLastHitBy.awardKillEnergy(mPlayer.getName());
			}
			doPorksplosion(eliminated);
		}
	}

	public void awardKillEnergy(String killedName) {
		mPlayer.playSound(mPlayer, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2, 0.5f);
		mPlayer.playSound(mPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 2, 1.4f);
		mPlayer.playSound(mPlayer, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 2, 2);

		mPlayer.sendMessage(
			Component.text("❌ K.O. ", NamedTextColor.RED, TextDecoration.BOLD)
				.append(Component.text(killedName + " - ", NamedTextColor.WHITE, TextDecoration.BOLD))
				.append(Component.text("+" + KILL_ENERGY_AWARD + " ⚡", NamedTextColor.GOLD, TextDecoration.BOLD))
		);
		restoreEnergy(KILL_ENERGY_AWARD, false);
	}

	public void showEndingInfo(PzeroPlayerPlacement placement, boolean finished, boolean eliminated) {
		Component title = Component.text(finished ? "FINISH!" : "CRASH OUT!", finished ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD);
		Component subtitleTime = Component.text(StringUtils.intToOrdinal(placement.mPlacement), getPlacementColor(placement.mPlacement), TextDecoration.BOLD)
				.append(Component.text(" - ", NamedTextColor.WHITE, TextDecoration.BOLD))
				.append(Component.text(StringUtils.intToMinuteAndSeconds(mTimer / 20) + "." + (mTimer % 20 * 50), OTHER_COLOR, TextDecoration.BOLD));

		if (mLastHitBy != null && eliminated) {
			Component subtitleEliminated = Component.text("❌ Eliminated", NamedTextColor.RED, TextDecoration.BOLD)
				.append(Component.text(" by ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
				.append(Component.text(mLastHitBy.getPlayer().getName(), NamedTextColor.WHITE, TextDecoration.BOLD));
			mPlayer.showTitle(Title.title(title, subtitleEliminated));
		} else {
			mPlayer.showTitle(Title.title(title, subtitleTime));
		}

		mPlayer.sendMessage(title);
		mPlayer.sendMessage(subtitleTime);
		if (mLastHitBy != null && eliminated) {
			mPlayer.sendMessage(
				Component.text("❌ Eliminated", NamedTextColor.RED, TextDecoration.BOLD)
					.append(Component.text(" by ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
					.append(Component.text(mLastHitBy.getPlayer().getName(), NamedTextColor.WHITE, TextDecoration.BOLD))
			);
		}
	}

	public void doPeriodicEffects() {
		if (mPig == null) {
			return;
		}

		// Boost
		if (isBoosting()) {
			new PartialParticle(Particle.END_ROD, LocationUtils.getHalfHeightLocation(mPig), 1).spawnAsPlayerActive(mPlayer);
		}

		// Being launched
		if (isBeingLaunched()) {
			new PartialParticle(Particle.CLOUD, LocationUtils.getHalfHeightLocation(mPig), 1).spawnAsPlayerActive(mPlayer);
		}

		// Low on Energy
		if (isLowOnEnergy()) {
			new PartialParticle(Particle.SMOKE_LARGE, LocationUtils.getHalfHeightLocation(mPig), 2).extra(0.075).spawnAsPlayerActive(mPlayer);
		}
	}

	public void updateLap(int newLap) {
		setCurrentLap(Math.min(mLapCount - 1, newLap));
		resetObtainableEnergyThisLap();
		// Send the lap time to the player
		mPlayer.sendMessage(
			Component.text("Lap Time: ", NamedTextColor.WHITE, TextDecoration.BOLD)
				.append(Component.text(StringUtils.intToMinuteAndSeconds(mLapTimer / 20) + "." + (mLapTimer % 20 * 50), OTHER_COLOR, TextDecoration.BOLD))
		);
		mPlayer.playSound(mPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 2, 2);
		mLapTimer = 0;
	}

	public static TextColor getPlacementColor(int placement) {
		switch (placement) {
			case 1 -> {
				return GOLD_COLOR;
			}
			case 2 -> {
				return SILVER_COLOR;
			}
			case 3 -> {
				return BRONZE_COLOR;
			}
			default -> {
				return OTHER_COLOR;
			}
		}
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public UUID getUniqueId() {
		return mPlayer.getUniqueId();
	}

	public String getMapName() {
		return mMapName;
	}

	public int getLapCount() {
		return mLapCount;
	}

	public int getCurrentEnergy() {
		return mCurrentEnergy;
	}

	public int getCurrentCheckpoint() {
		return mCurrentCheckpoint;
	}

	public int getCurrentLap() {
		return mCurrentLap;
	}

	public int getTimer() {
		return mTimer;
	}

	public boolean isLowOnEnergy() {
		return mCurrentEnergy <= LOW_ON_ENERGY_EFFECTS_THRESHOLD;
	}

	public boolean isBoosting() {
		return mRemainingBoostTicks > 0;
	}

	public boolean isBeingLaunched() {
		return mBeingLaunched;
	}

	public boolean isInGracePeriod() {
		return mIsInGracePeriod;
	}

	public boolean isInWinAnimation() {
		return mIsInWinAnimation;
	}

	public void setCurrentCheckpoint(int currentCheckpoint) {
		mCurrentCheckpoint = currentCheckpoint;
	}

	public void setCurrentLap(int currentLap) {
		mCurrentLap = currentLap;
	}

	public void setLastHitBy(PzeroPlayer pzPlayer) {
		mLastHitBy = pzPlayer;
		mLastHitTicks = Bukkit.getCurrentTick();
	}

	public void incrementTimer() {
		mTimer++;
		mLapTimer++;
	}

	public @Nullable Pig getPig() {
		return mPig;
	}
}
