package com.playmonumenta.plugins.managers.travelanchor;

import com.playmonumenta.plugins.Constants.Tags;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class TravelUi extends BukkitRunnable {
	public static final String TRAVEL_ANCHOR_UI_SEAT_TAG = "TravelAnchorUiSeat";

	protected final Player mPlayer;
	private final Interaction mSeat;
	private int mWarmupDelay = 20;
	protected int mRainbowAnimationPhase = 0;
	protected int mGroupIndex = 0;
	protected final EntityTravelAnchor mStartingAnchor;
	private final HashMap<UUID, TravelUiTarget> mAnchorToTarget = new HashMap<>();
	protected @Nullable TravelUiTarget mClosestTarget = null;

	public TravelUi(Player player, EntityTravelAnchor travelAnchor) {
		mPlayer = player;
		mStartingAnchor = travelAnchor;

		Location loc = player.getLocation();
		loc.add(0.0, 0.6, 0.0); // For some reason this can teleport players down by 0.6 blocks
		World world = loc.getWorld();
		mSeat = world.spawn(loc, Interaction.class, interaction -> {
			interaction.customName(Component.text("TravelAnchorUiSeat"));
			interaction.addScoreboardTag(TRAVEL_ANCHOR_UI_SEAT_TAG);
			interaction.addScoreboardTag(Tags.REMOVE_ON_UNLOAD);
			interaction.setInteractionWidth(0.0f);
			interaction.setInteractionHeight(0.0f);
			interaction.addPassenger(mPlayer);
		});

		for (EntityTravelAnchor anchor : TravelAnchorManager.getInstance().anchorsInWorld(world).getAnchors()) {
			if (shouldHideAnchor(anchor)) {
				continue;
			}

			TravelUiTarget uiTarget = new TravelUiTarget(this, anchor);
			mAnchorToTarget.put(anchor.getEntityId(), uiTarget);
		}
	}

	@Override
	public void run() {
		mRainbowAnimationPhase -= 5;
		if (mRainbowAnimationPhase < 0) {
			mRainbowAnimationPhase += 360;
			mGroupIndex++;
		}

		// Abort early if the player is offline or this has been tampered with
		if (
			!mPlayer.isOnline()
				|| mPlayer.isDead()
				|| mSeat.isDead()
				|| !mSeat.getChunk().isEntitiesLoaded()
		) {
			cancel();
			return;
		}

		// Handle players accidentally dismounting the seat at startup (in case of shift+click shortcut)
		if (mWarmupDelay > 0) {
			mWarmupDelay--;
			if (!mSeat.getPassengers().contains(mPlayer)) {
				mSeat.addPassenger(mPlayer);
			}
		}

		// Remove anchors that have ceased to be
		Set<UUID> toEntitiesUnlink = new HashSet<>();
		for (TravelUiTarget target : mAnchorToTarget.values()) {
			if (target.shouldRemove()) {
				toEntitiesUnlink.addAll(target.getEntityIds());
				target.remove();
				continue;
			}

			EntityTravelAnchor anchor = target.getAnchor();
			if (shouldHideAnchor(anchor)) {
				// No longer in a common group
				toEntitiesUnlink.addAll(target.getEntityIds());
				target.remove();
			}
		}
		for (UUID entityId : toEntitiesUnlink) {
			mAnchorToTarget.remove(entityId);
		}

		// Process new anchors
		World world = mPlayer.getWorld();
		for (EntityTravelAnchor anchor : TravelAnchorManager.getInstance().anchorsInWorld(world).getAnchors()) {
			if (mAnchorToTarget.containsKey(anchor.getEntityId())) {
				continue;
			}

			if (shouldHideAnchor(anchor)) {
				continue;
			}

			TravelUiTarget uiTarget = new TravelUiTarget(this, anchor);
			mAnchorToTarget.put(anchor.getEntityId(), uiTarget);
		}

		// Find the closest target to the player's look angle
		Vector lookDir = mPlayer.getLocation().getDirection();
		double closestLookDiff = 5.0;
		TravelUiTarget closestTarget = null;
		for (TravelUiTarget target : mAnchorToTarget.values()) {
			target.animate();

			double lookDiff = lookDir.distanceSquared(target.getDir());
			if (lookDiff >= closestLookDiff) {
				continue;
			}
			closestLookDiff = lookDiff;
			closestTarget = target;
		}
		if (!Objects.equals(mClosestTarget, closestTarget)) {
			mClosestTarget = closestTarget;
		}

		// Players leaving their seat at this point indicates a teleport attempt - go to the closest target
		if (mWarmupDelay <= 0 && !mSeat.getPassengers().contains(mPlayer)) {
			if (mClosestTarget != null) {
				Location lastLoc = mClosestTarget.getAnchor().lastLocation();
				if (lastLoc != null && world.equals(lastLoc.getWorld())) {
					lastLoc = lastLoc.clone().setDirection(lookDir);
					Location playerPrevLoc = mPlayer.getLocation();
					mPlayer.teleport(lastLoc);
					world.playSound(playerPrevLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					world.playSound(lastLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
			}
			cancel();
		}
	}

	@Override
	public synchronized void cancel() throws IllegalStateException {
		super.cancel();

		if (mSeat.getPassengers().contains(mPlayer)) {
			mSeat.removePassenger(mPlayer);
		}
		mSeat.remove();

		for (TravelUiTarget target : mAnchorToTarget.values()) {
			target.remove();
		}
		mAnchorToTarget.clear();
		mClosestTarget = null;
	}

	private boolean shouldHideAnchor(EntityTravelAnchor anchor) {
		return mStartingAnchor.cannotAccess(anchor);
	}
}
