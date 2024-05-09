package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class DepthsPlayer {

	/*
	 * !!!! WARNING !!!!
	 * This class is automatically serialized
	 * IF YOU ADD ANYTHING HERE THAT REFERENCES A COMPLEX OBJECT THE SERVER WILL CRASH WHILE SAVING
	 * (especially Player or World)
	 */

	//Unique identifier for the player
	public UUID mPlayerId;
	//A map containing all abilities the player has and their current rarity
	public Map<String, Integer> mAbilities;
	//Unique identifier, mapping to an active depths party object
	public long mPartyNum;
	//The depths ability trees the player is eligible to select from this run
	public List<DepthsTree> mEligibleTrees;
	//Weapon offering options for the player
	public transient List<DepthsAbilityInfo<? extends WeaponAspectDepthsAbility>> mWeaponOfferings;
	//Whether or not they have selected a weapon aspect
	public boolean mHasWeaponAspect;
	//Whether or not they have used chaos room this floor already
	public boolean mUsedChaosThisFloor;
	//Whether or not they have used chaos room this floor already
	public boolean mUsedWheelThisFloor;
	//Whether or not they have deleted an ability on this floor
	public boolean mUsedAbilityDeletion;
	//Whether or not they have mutated an ability on this floor
	public boolean mUsedAbilityMutation;
	//Whether or not they have used generosity on this floor
	public boolean mUsedGenerosity;
	//Remaining active ability only wand aspect uses
	public int mWandAspectCharges = 0;
	//Individual treasure score. Copied from the party's treasure score when they die.
	public int mFinalTreasureScore;
	//Reward queue implementation to let the player catch up on reward chests they have missed later
	public Queue<DepthsRewardType> mEarnedRewards;
	//The room on which the player died. -1 if the player has not died
	public int mDeathRoom;
	// Number of times this player has died as relevant for graves (gets reduced with bosses beaten, so is not an accurate total count)
	public int mNumDeaths;
	// Unique mobs killed by this player with the Solar Ray ability
	private final List<String> mSolarRayUniqueMobNames = new ArrayList<>();

	// Whether the permanent effects given by Diversity have been activated
	public boolean mDiversityActive = false;
	// Whether the prismatic given by Diversity has been given
	public boolean mDiversityGift = false;

	// Abilities that the player can receive from Generosity
	public final List<DepthsAbilityItem> mGenerosityGifts = new ArrayList<>();

	public boolean mDead = false; // If true, awaiting respawn
	public int mGraveTicks = 0;
	public double mReviveTicks = 0;
	boolean mCurrentlyReviving = false;
	public transient @Nullable BukkitRunnable mGraveRunnable;

	//The location to teleport offline players to; null if no teleport is required on login - DO NOT REFERENCE WORLD
	private String mOfflineTeleportWorld = "";
	private @Nullable Vector mOfflineTeleportLoc = null;
	private @Nullable Float mOfflineTeleportYaw = null;
	private @Nullable Float mOfflineTeleportPitch = null;
	//The Depths content this player is in
	private final DepthsContent mContent;

	/*
	 * !!!! WARNING !!!!
	 * This class is automatically serialized
	 * IF YOU ADD ANYTHING HERE THAT REFERENCES A COMPLEX OBJECT THE SERVER WILL CRASH WHILE SAVING
	 * (especially Player or World)
	 */

	public DepthsPlayer(Player p) {
		mPlayerId = p.getUniqueId();
		mAbilities = new HashMap<>();
		mEligibleTrees = initTrees(p);

		//Randomize order of weapon aspects
		//First 3 elements available by default, others locked behind transaction
		mWeaponOfferings = DepthsManager.getWeaponAspects();
		Collections.shuffle(mWeaponOfferings);

		mUsedChaosThisFloor = false;

		//Dummy value - either replaced on death or on win
		mFinalTreasureScore = -1;

		mEarnedRewards = new ConcurrentLinkedQueue<>();

		mDeathRoom = -1;

		mContent = DepthsUtils.getDepthsContent();

		ScoreboardUtils.setScoreboardValue(p, "DDDelve1", 0);
		ScoreboardUtils.setScoreboardValue(p, "DDDelve2", 0);

		sendMessage("Initializing Depths System! Press button again to begin.");

		// Remove Heavenly Boon extensions when beginning a run
		HeavenlyBoon.cancelExtensions(p);
	}

	public List<DepthsTree> initTrees(Player player) {
		int talismanScore = DepthsUtils.getDepthsContent() == DepthsContent.DARKEST_DEPTHS ? ScoreboardUtils.getScoreboardValue(player, "DDTalisman").orElse(0) : ScoreboardUtils.getScoreboardValue(player, "CZTalisman").orElse(0);
		if (0 < talismanScore && talismanScore <= DepthsTree.OWNABLE_TREES.length) {
			double chance = 1;
			if ((player.getScoreboardTags().contains(DepthsManager.ENDLESS_MODE_STRING) && !player.getScoreboardTags().contains(DepthsManager.RIGGED_STRING))
				    || ((ScoreboardUtils.getScoreboardValue(player, "CurrentAscension").orElse(0) > 0) && !player.getScoreboardTags().contains(DepthsManager.RIGGED_STRING))) {
				chance = 0.75;
			}

			double rand = Math.random();
			if (rand < 4.0/7.0) {
				//Guarantee the tree, but same chance that they would normally get it
				return initTreesWithGuarantee(talismanScore);
			} else if (rand < chance) {
				//The Talisman granted them the tree when it wouldn't normally
				sendMessage("Due to your Talisman, you have become a " + DepthsTree.OWNABLE_TREES[talismanScore - 1].getDisplayName() + " when you would not have otherwise!");
				return initTreesWithGuarantee(talismanScore);
			} else {
				//They failed both the usual random chance of the tree and the Talisman chance
				List<DepthsTree> randomTrees = new ArrayList<>();

				DepthsTree talismanTree = DepthsTree.OWNABLE_TREES[talismanScore - 1];

				List<DepthsTree> allTrees = new ArrayList<>();
				//Get all possible trees and shuffle them
				Collections.addAll(allTrees, DepthsTree.OWNABLE_TREES);
				//This is the 1/4 chance in Endless with the Talisman to NOT get the requested tree
				allTrees.remove(talismanTree);
				Collections.shuffle(allTrees);

				for (int i = 0; i < DepthsManager.NUM_TREES_PER_RUN; i++) {
					randomTrees.add(allTrees.get(i));
				}

				return randomTrees;
			}
		}
		return initRandomTrees();
	}

	/**
	 * Gets random trees from the system that the player can exclusively select from
	 * @return tree list
	 */
	public List<DepthsTree> initRandomTrees() {
		List<DepthsTree> randomTrees = new ArrayList<>();
		List<DepthsTree> allTrees = new ArrayList<>();
		//Get all possible trees and shuffle them
		Collections.addAll(allTrees, DepthsTree.OWNABLE_TREES);
		Collections.shuffle(allTrees);

		//Select the first x values as valid tree offerings this run
		for (int i = 0; i < DepthsManager.NUM_TREES_PER_RUN; i++) {
			randomTrees.add(allTrees.get(i));
		}

		return randomTrees;
	}

	public List<DepthsTree> initTreesWithGuarantee(int talismanScore) {
		List<DepthsTree> randomTrees = new ArrayList<>();

		DepthsTree talismanTree = DepthsTree.OWNABLE_TREES[talismanScore - 1];
		randomTrees.add(talismanTree);

		List<DepthsTree> allTrees = new ArrayList<>();
		//Get all possible trees and shuffle them
		Collections.addAll(allTrees, DepthsTree.OWNABLE_TREES);
		allTrees.remove(talismanTree);
		Collections.shuffle(allTrees);

		//Select one less tree since we already have one chosen
		for (int i = 0; i < DepthsManager.NUM_TREES_PER_RUN - 1; i++) {
			randomTrees.add(allTrees.get(i));
		}

		return randomTrees;
	}

	public void setDeathRoom(int room) {
		mDeathRoom = room;
	}

	public int getDeathRoom() {
		return mDeathRoom;
	}

	public boolean hasDied() {
		return mDeathRoom >= 0;
	}

	public void offlineTeleport(Location location) {
		if (hasDied()) {
			return;
		}
		location = location.clone();
		// DO NOT STORE A REFERENCE TO A WORLD
		mOfflineTeleportWorld = location.getWorld().getName();
		location.setWorld(null);
		mOfflineTeleportLoc = location.toVector();
		mOfflineTeleportYaw = location.getYaw();
		mOfflineTeleportPitch = location.getPitch();
	}

	public boolean doOfflineTeleport() {
		if (mOfflineTeleportLoc == null) {
			return false;
		}

		Player player = Bukkit.getPlayer(mPlayerId);
		if (player == null) {
			return false;
		}

		World world = Bukkit.getWorld(mOfflineTeleportWorld);
		if (world == null) {
			world = player.getWorld();
		}

		if (mOfflineTeleportYaw == null) {
			mOfflineTeleportYaw = 0.0f;
		}
		if (mOfflineTeleportPitch == null) {
			mOfflineTeleportPitch = 0.0f;
		}

		Location location = new Location(world,
			mOfflineTeleportLoc.getX(),
			mOfflineTeleportLoc.getY(),
			mOfflineTeleportLoc.getZ(),
			mOfflineTeleportYaw,
			mOfflineTeleportPitch);
		location.setWorld(world);
		player.teleport(location);
		mOfflineTeleportWorld = "";
		mOfflineTeleportLoc = null;
		mOfflineTeleportYaw = null;
		mOfflineTeleportPitch = null;
		return true;
	}

	public List<String> getSolarRayUniqueMobNames() {
		return mSolarRayUniqueMobNames;
	}

	public @Nullable Player getPlayer() {
		return Bukkit.getPlayer(mPlayerId);
	}

	public void sendMessage(String message) {
		sendMessage(Component.text(message));
	}

	public void sendMessage(Component message) {
		Player p = Bukkit.getPlayer(mPlayerId);
		if (p != null && p.isOnline()) {
			DepthsUtils.sendFormattedMessage(p, mContent, message);
		}
	}

	public DepthsContent getContent() {
		return mContent;
	}
}
