package com.playmonumenta.plugins.infinitytower;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiBuyMob;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobInfo;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class TowerGame {

	private static final int COOLDOWN_TURNS = 60;
	//in sec

	public int mPlayerLevel = 1;
	//player level is cap of how many mob the player can have and what item show in the gui

	public int mRoll = 0;
	//mRoll is used so even if the player open the gui multiple times its always keep the same item

	protected int mCurrentFloor;

	public TowerPlayer mPlayer;
	protected TowerTeam mFloorTeam;

	protected TowerFloor mFloor;
	//keep track of the starting point of the floor.

	public final List<LivingEntity> mPlayerMobs;
	public final List<LivingEntity> mFloorMobs;

	private BukkitRunnable mCountDown;
	private BukkitRunnable mAesthetics;

	private boolean mIsGameEnded = false;
	private boolean mIsTurnEnded = false;

	protected final int ID;
	private final TowerGame INSTANCE;

	public TowerGame(int idGame, Player player) {
		ID = idGame;
		INSTANCE = this;
		mPlayer = new TowerPlayer(player);
		mPlayerMobs = new ArrayList<>();
		mFloorMobs = new ArrayList<>();
		mCurrentFloor = 0;

		mFloor = TowerFileUtils.getTowerFloor(this, mCurrentFloor);

		if (mFloor == null) {
			//no file?
			//make a log and stop the game.
			TowerFileUtils.warning("mFloor == null, no file loaded? stopping the game.");

			forceStopGame();
			return;
		}

		mFloorTeam = TowerFileUtils.getFloorTeam(mCurrentFloor);

		if (mFloorTeam == null) {
			//no file?
			//make a log and stop the game.
			TowerFileUtils.warning("mFloorTeam == null, no file loaded? stopping the game.");

			forceStopGame();
			return;
		}

		ItemStack stack = InventoryUtils.getItemFromLootTable(mPlayer.mPlayer, TowerConstants.BOOK_LOOT_TABLE_KEY);
		if (stack == null) {
			TowerFileUtils.warning("Falied to load loottable");
			forceStopGame();
			return;
		}
		Item item = mPlayer.mPlayer.getWorld().dropItemNaturally(new Location(mPlayer.mPlayer.getWorld(), mFloor.mVector.getX() + (((float) mFloor.mXSize) / 4), mFloor.mVector.getY() + 5, mFloor.mVector.getZ() + ((float) mFloor.mZSize) / 2), stack);
		item.setCanMobPickup(false);
		item.setGlowing(true);
	}

	public void setFloor(TowerFloor floor) {
		clearMobs();
		mFloor = floor;
		mPlayer.mPlayer.teleport(new Location(mPlayer.mPlayer.getWorld(), mFloor.mVector.getX(), mFloor.mVector.getY(), mFloor.mVector.getZ()));
	}

	public boolean isGameEnded() {
		//return wherever the game is ended or not
		return mIsGameEnded;
	}

	public boolean isTurnEnded() {
		return mIsTurnEnded;
	}

	public boolean didPlayerWin() {
		for (LivingEntity mob : new ArrayList<>(mFloorMobs)) {
			if (!mob.isDead()) {
				return false;
			}
		}
		return true;
	}

	public boolean didPlayerLose() {
		for (LivingEntity mob : new ArrayList<>(mPlayerMobs)) {
			if (!mob.isDead()) {
				return false;
			}
		}
		return true;
	}

	public void playerLoseTurn() {
		mIsGameEnded = true;
		//set scoreboard for result
		TowerGameUtils.sendMessage(mPlayer.mPlayer, "You lose at round: " + (mCurrentFloor + 1));

		TowerGameUtils.giveLoot(this);
		clearPlayer(mPlayer.mPlayer);
		forceStopGame();
	}

	public void playerWinTurn() {

		//move to the "next" floor
		mCurrentFloor++;
		mRoll++;
		//refresh the roll for the shop mobs gui

		TowerGameUtils.sendMessage(mPlayer.mPlayer, "You are now at round: " + (mCurrentFloor + 1));
		TowerGameUtils.addGold(mPlayer.mPlayer, TowerConstants.getGoldWin(mCurrentFloor));
		if (mCurrentFloor > mFloor.mMax) {
			//we need to change the floor
			mFloor = TowerFileUtils.getTowerFloor(this, mCurrentFloor);
		}

		if (mFloor == null) {
			//no floor usable ->stop the player
			stop();
			return;
		}

		mFloorTeam = TowerFileUtils.getFloorTeam(mCurrentFloor);
		if (mFloorTeam == null) {
			//we reach the end of the game GG
			stop();
			return;
		}

		//the player choose to continue the game
		TowerGameUtils.sendMessage(mPlayer.mPlayer, "Going to start next round!");
		mPlayer.mPlayer.removeScoreboardTag(TowerConstants.TAG_IN_BATTLE);
		mPlayer.mPlayer.addScoreboardTag(TowerConstants.TAG_BETWEEN_BATTLE);
		mPlayer.mPlayer.addScoreboardTag(TowerConstants.TAG_BOOK);
		//now the player is in the right floor
		//start the 60s count down

		launchCooldown();

	}

	private void endTurnAesthetic(List<LivingEntity> mobs, boolean playerWin) {
		for (LivingEntity mob : new ArrayList<>(mobs)) {
			mob.setInvulnerable(true);
		}
		mAesthetics = new BukkitRunnable() {
			int mTimer = 0;
			List<LivingEntity> mMobAbilities = new ArrayList<>(mobs);

			final List<LivingEntity> mMobs = new ArrayList<>(mobs);
			final boolean mPlayerWin = playerWin;
			final BossManager mBossManager = BossManager.getInstance();

			@Override
			public void run() {

				if (mMobAbilities != null && !mMobAbilities.isEmpty()) {
					for (LivingEntity mob : new ArrayList<>(mMobAbilities)) {
						try {
							if (mob.isValid() && !mob.isDead()) {
								mBossManager.unload(mob, false);
								mMobAbilities.remove(mob);
							} else {
								mMobAbilities.remove(mob);
							}
						} catch (Exception e) {
							//sometimes we have a ConcurrentModificationException
							TowerFileUtils.warning("Exception while unloading a Boss. reason: " + e.getMessage());
						}
					}

					if (mMobAbilities.isEmpty()) {
						mMobAbilities = null;
					}
				}

				for (LivingEntity mob : mMobs) {
					if (mob.isOnGround() && FastUtils.RANDOM.nextDouble() > 0.2) {
						mob.setVelocity(new Vector(0, FastUtils.RANDOM.nextDouble(), 0));
					}
				}

				if (mTimer >= 20 * 5) {
					cancel();
					return;
				}

				mTimer++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				mIsTurnEnded = false;
				clearMobs();
				if (!mIsGameEnded) {
					if (mPlayerWin) {
						playerWinTurn();
					} else {
						playerLoseTurn();
					}
				}


			}

		};

		mAesthetics.runTaskTimer(TowerManager.mPlugin, 0, 1);

	}

	public void startTurn() {
		mCountDown.cancel();

		if (mIsGameEnded) {
			return;
		}

		mPlayer.mPlayer.removeScoreboardTag(TowerConstants.TAG_BETWEEN_BATTLE);
		mPlayer.mPlayer.removeScoreboardTag(TowerConstants.TAG_BOOK);
		mPlayer.mPlayer.addScoreboardTag(TowerConstants.TAG_IN_BATTLE);

		//we may want to use 2 runnable (one for each group of mobs) so it would be faster?
		mPlayer.mTeam.summonAll(this, mPlayerMobs, true);
		mFloorTeam.summonAll(this, mFloorMobs, false);

		if (mPlayerMobs.isEmpty()) {
			endTurnAesthetic(mFloorMobs, false);
		}
	}


	public void buyMobs() {
		new TowerGuiBuyMob(mPlayer.mPlayer, this).openInventory(mPlayer.mPlayer, TowerManager.mPlugin);
	}

	public boolean canAdd(TowerMobInfo info) {
		int size = info.mMobStats.mWeight;
		int currentSize = mPlayer.mTeam.mCurrentSize;
		int maxSize = TowerConstants.STARTING_TEAM_SIZE + (mPlayerLevel > 1 ? (mPlayerLevel - 1) * TowerConstants.LEVEL_UP_TEAM_SIZE_INCREASE : 0);
		if (currentSize + size > maxSize) {
			return false;
		}

		int limit = info.mMobStats.mLimit;
		for (TowerMob mob : mPlayer.mTeam.mMobs) {
			if (mob.isSameBaseMob(info)) {
				limit--;
			}

			if (limit <= 0) {
				return false;
			}
		}

		return true;
	}

	public void addNewMob(TowerMobInfo info) {
		double dx = (mPlayer.mPlayer.getLocation().getX() - mFloor.mVector.getX());
		double dy = (mPlayer.mPlayer.getLocation().getY() - mFloor.mVector.getY()) + 0.1;
		double dz = (mPlayer.mPlayer.getLocation().getZ() - mFloor.mVector.getZ());
		dz = Math.max(0.5, Math.min(dz, mFloor.mZSize - 0.5));
		dx = Math.max(0.5, Math.min(dx, mFloor.mXSize - 0.5));
		dy = Math.max(0.1, dy);
		mPlayer.mTeam.addMob(new TowerMob(info, dx, dy, dz));
	}

	public void removeMob(TowerMob mob) {
		mPlayer.mTeam.remove(mob);
	}

	public void start() {
		Bukkit.dispatchCommand(mPlayer.mPlayer, "function mechanisms/music/music_stop_reset");
		mPlayer.mPlayer.teleport(new Location(mPlayer.mPlayer.getWorld(), mFloor.mVector.getX() + 0.5, mFloor.mVector.getY() + 1, mFloor.mVector.getZ() + 0.5));
		launchCooldown();
	}

	public void stop() {
		if (!mIsGameEnded) {
			mIsGameEnded = true;
			mIsTurnEnded = true;
			mAesthetics.cancel();
			mCountDown.cancel();
			clearMobs();
		}
		if (mCurrentFloor > TowerConstants.DESIGNED_FLOORS - 1 && mFloor != null) {
			if (mFloorTeam == null) {
				TowerFileUtils.convertPlayerTeamLocation(this);
				TowerFileUtils.savePlayerTeam(mPlayer.mTeam, mCurrentFloor);
			} else if (mCurrentFloor > TowerConstants.DESIGNED_FLOORS + 1) {
				TowerFileUtils.convertPlayerTeamLocation(this);
				TowerFileUtils.savePlayerTeam(mPlayer.mTeam, mCurrentFloor - 1);
			}
			TowerGameUtils.sendMessage(mPlayer.mPlayer, "Congratulations! your team will now defend " + (mCurrentFloor + 1) + "th round");
		}

		clearPlayer(mPlayer.mPlayer);
		TowerGameUtils.giveLoot(this);
		forceStopGame();

	}

	public void forceStopGame() {
		mIsGameEnded = true;
		if (mCountDown != null) {
			mCountDown.cancel();
		}
		if (mAesthetics != null) {
			mAesthetics.cancel();
		}

		clearMobs();
		TowerGuiBuyMob.unloadGame(this);
		mPlayer.mPlayer.removeScoreboardTag(TowerConstants.TAG_IN_BATTLE);
		mPlayer.mPlayer.removeScoreboardTag(TowerConstants.TAG_BETWEEN_BATTLE);
		mPlayer.mPlayer.removeScoreboardTag(TowerConstants.TAG_BOOK);
		mPlayer.mPlayer.removeScoreboardTag(TowerConstants.PLAYER_TAG);
		UUID uuid = mPlayer.mPlayer.getUniqueId();
		TowerManager.GAMES.remove(uuid);
	}

	public void towerMobsDied(LivingEntity mob) {
		mPlayerMobs.remove(mob);
		mFloorMobs.remove(mob);
		//check who win

		if (mIsGameEnded) {
			return;
		}

		if (mIsTurnEnded) {
			return;
		}

		if (didPlayerLose()) {
			mIsTurnEnded = true;
			endTurnAesthetic(mFloorMobs, false);
			return;
		}

		if (didPlayerWin()) {
			mIsTurnEnded = true;
			endTurnAesthetic(mPlayerMobs, true);
		}


	}

	//---------functions used by the abilities------

	public List<LivingEntity> getPlayerMobs() {
		return new ArrayList<>(mPlayerMobs);
	}

	public List<LivingEntity> getFloorMobs() {
		return new ArrayList<>(mFloorMobs);
	}

	public Location getRandomLocation() {
		int x = FastUtils.RANDOM.nextInt(mFloor.mXSize);
		int z = FastUtils.RANDOM.nextInt(mFloor.mZSize);
		return new Location(mPlayer.mPlayer.getWorld(), mFloor.mVector.getX() + x, mFloor.mVector.getY(), mFloor.mVector.getZ() + z);
	}

	public Boolean isInArena(Location location) {
		return location.getX() <= (mFloor.mVector.getX() + mFloor.mXSize) && location.getZ() <= (mFloor.mVector.getZ() + mFloor.mZSize) && location.getY() >= (mFloor.mVector.getY() - 2) &&
			location.getX() >= mFloor.mVector.getX() && location.getZ() >= mFloor.mVector.getZ();
	}

	//--------------private functions------

	private void clearMobs() {
		for (LivingEntity mob : getPlayerMobs()) {
			mob.remove();
		}

		mPlayerMobs.clear();

		for (LivingEntity mob : getFloorMobs()) {
			mob.remove();
		}

		mFloorMobs.clear();
	}


	private void launchCooldown() {
		mCountDown = new BukkitRunnable() {
			int mTimer = 0;

			final List<LivingEntity> mMobs = new ArrayList<>();

			@Override
			public void run() {
				if (mTimer == 0) {
					for (TowerMob mob : mFloorTeam.mMobs) {
						mob.spawnPuppet(INSTANCE, mMobs);
					}
				}

				if (mPlayer != null) {
					World world = mPlayer.mPlayer.getWorld();
					for (TowerMob playerMob : mPlayer.mTeam.mMobs) {
						world.spawnParticle(Particle.REDSTONE, playerMob.getSpawnLocation(INSTANCE), 80, 0, 0.5, 0, 1, new DustOptions(Color.ORANGE, 0.5f));
					}
				}


				if (mTimer % 20 == 0) {
					if (mPlayer != null) {
						//send message to player
						mPlayer.mPlayer.sendActionBar("The next round will start in " + (COOLDOWN_TURNS - (mTimer / 20)) + "s");
					}
				}

				if (mTimer >= COOLDOWN_TURNS * 20) {
					cancel();
					startTurn();
				}

				if (isCancelled()) {
					return;
				}


				mTimer += 10;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				for (LivingEntity mob : mMobs) {
					mob.remove();
				}
				mTimer = 0;
			}
		};
		mCountDown.runTaskTimer(TowerManager.mPlugin, 10, 10);
	}

	public static void clearPlayer(Player player) {
		player.removeScoreboardTag(TowerConstants.TAG_IN_BATTLE);
		player.removeScoreboardTag(TowerConstants.TAG_BETWEEN_BATTLE);
		player.removeScoreboardTag(TowerConstants.PLAYER_TAG);
		player.removeScoreboardTag(TowerConstants.TAG_BOOK);

	}



}
