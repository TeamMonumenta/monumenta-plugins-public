package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.depths.guis.DepthsTreeGUI;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.utils.FastUtils;
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
	//Last known name of player
	public String mPlayerName;
	//A map containing all abilities the player has and their current rarity
	public Map<String, Integer> mAbilities;
	// A list of removed abilities, for data collection purposes only. Can contain duplicates
	public List<String> mRemovedAbilities;
	//Unique identifier, mapping to an active depths party object
	public long mPartyNum;

	// On initialization, the player's 3 choices for their starting tree
	public List<DepthsTree> mTreeSelection;
	// On initialization, the player's tree that will give them extra treasure (an index of mTreeSelection)
	public int mTreasureIndex;
	// Whether the player chose the bonus treasure tree or not
	public boolean mBonusTreeSelected;

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
	// Remaining number of unused rerolls
	public int mRerolls;
	// Rerolls previously gained from Opportunity
	public int mOpportunityRerolls;
	// Unique mobs killed by this player with the Solar Ray ability
	private final List<String> mSolarRayUniqueMobNames = new ArrayList<>();

	// Whether the permanent effects given by Diversity have been activated
	public boolean mDiversityActive = false;
	// Whether the prismatic given by Diversity has been given
	public boolean mDiversityGift = false;

	// Whether the player should be killed on login. Currently used when the party abandons the player.
	public boolean mZenithAbandonedByParty = false;

	// Whether the player is currently processing Abnormality
	public int mAbnormalityLevel = 0;

	// The current counter of the player's Curse of Chaos ability
	public int mCurseofChaosCount = 0;

	// Northern Star stacks
	public int mNorthernStarStacks = 0;

	// Reward skips
	public int mRewardSkips = 0;

	// Bonus treasure score from Avaricious Pendant
	public int mBonusTreasureScore = 0;

	// Comb of Selection current selection rarities
	public List<Integer> mCombOfSelectionLevels = new ArrayList<>();

	// Callicarpa's Pointed Hat stacks
	public @Nullable DepthsTree mPointedHatTree = null;
	public int mPointedHatStacks = 0;

	// Broodmother's Webbing
	public boolean mBroodmothersWebbing = false;

	// Statue of Regret previous selections
	public List<String> mRegretSelections = new ArrayList<>();

	// The last time the player logged out. If 0, then they haven't logged out yet.
	public long mLastLogoutTime = 0;

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
		mPlayerName = p.getName();
		mAbilities = new HashMap<>();
		mRemovedAbilities = new ArrayList<>();
		mEligibleTrees = new ArrayList<>();

		//Randomize order of weapon aspects
		//First 3 elements available by default, others locked behind transaction
		mWeaponOfferings = DepthsManager.getWeaponAspects();
		Collections.shuffle(mWeaponOfferings);

		mUsedChaosThisFloor = false;

		//Dummy value - either replaced on death or on win
		mFinalTreasureScore = -1;

		mEarnedRewards = new ConcurrentLinkedQueue<>();

		mDeathRoom = -1;

		mRerolls = 1;
		mOpportunityRerolls = 0;

		mContent = DepthsUtils.getDepthsContent();

		ScoreboardUtils.setScoreboardValue(p, "DDDelve1", 0);
		ScoreboardUtils.setScoreboardValue(p, "DDDelve2", 0);

		initTreeSelection();
		openTreeSelectionGUI();

		sendMessage("Initializing Depths System! Select your tree and press the button again to begin.");
	}

	public void initTreeSelection() {
		// we have to initialize the random tree choice once, so that way you can't repeatedly reroll your choices by retriggering the GUI
		if (getPlayer() == null) {
			return;
		}

		List<DepthsTree> choices = new ArrayList<>();
		int talismanScore = DepthsUtils.getDepthsContent() == DepthsContent.DARKEST_DEPTHS ? ScoreboardUtils.getScoreboardValue(getPlayer(), "DDTalisman").orElse(0) : ScoreboardUtils.getScoreboardValue(getPlayer(), "CZTalisman").orElse(0);
		if (talismanScore > 0) {
			DepthsTree talismanTree = DepthsTree.OWNABLE_TREES[talismanScore - 1];
			choices.add(talismanTree);

			// remove the talisman tree, then pick 2 more trees
			List<DepthsTree> allTrees = new ArrayList<>(List.of(DepthsTree.OWNABLE_TREES));
			allTrees.remove(talismanTree);
			Collections.shuffle(allTrees);
			for (int i = 0; i < DepthsManager.NUM_TREES_PER_RUN - 2; i++) {
				choices.add(allTrees.get(i));
			}
		} else {
			// no talisman, so just pick 3 random trees to offer
			List<DepthsTree> allTrees = new ArrayList<>(List.of(DepthsTree.OWNABLE_TREES));
			Collections.shuffle(allTrees);
			for (int i = 0; i < DepthsManager.NUM_TREES_PER_RUN - 1; i++) {
				choices.add(allTrees.get(i));
			}
		}
		Collections.shuffle(choices);
		mTreeSelection = choices;

		if (talismanScore > 0) {
			for (int i = 0; i < mTreeSelection.size(); i++) {
				// must not be our talisman tree
				if (mTreeSelection.get(i) != DepthsTree.OWNABLE_TREES[talismanScore - 1]) {
					mTreasureIndex = i;
				}
			}
		} else {
			mTreasureIndex = FastUtils.randomIntInRange(0, 2);
		}
	}

	public void openTreeSelectionGUI() {
		if (getPlayer() == null) {
			return;
		}
		new DepthsTreeGUI(getPlayer(), mTreeSelection, mTreasureIndex).open();
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

	public int getLevelInAbility(@Nullable String abilityName) {
		if (abilityName == null) {
			return 0;
		}
		return mAbilities.getOrDefault(abilityName, 0);
	}

	public boolean hasAbility(@Nullable String abilityName) {
		if (abilityName == null) {
			return false;
		}
		return mAbilities.containsKey(abilityName);
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
