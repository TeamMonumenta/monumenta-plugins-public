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
	private final Player mPlayer;
	private final Interaction mSeat;
	private int mWarmupDelay = 20;
	private int mAnimationPhase = 0;
	private final HashMap<UUID, TravelUiTarget> mAnchorToTarget = new HashMap<>();
	private @Nullable TravelUiTarget mClosestTarget = null;

	public TravelUi(Player player) {
		mPlayer = player;

		Location loc = player.getLocation();
		World world = loc.getWorld();
		mSeat = world.spawn(loc, Interaction.class, interaction -> {
			interaction.customName(Component.text("TravelAnchorUiSeat"));
			interaction.addScoreboardTag("TravelAnchorUiSeat");
			interaction.addScoreboardTag(Tags.REMOVE_ON_UNLOAD);
			interaction.setInteractionWidth(0.0f);
			interaction.setInteractionHeight(0.0f);
			interaction.addPassenger(mPlayer);
		});

		Location eyeLoc = mPlayer.getEyeLocation();

		for (EntityTravelAnchor anchor : TravelAnchorManager.getInstance().anchorsInWorld(world).getAnchors()) {
			TravelUiTarget uiTarget = new TravelUiTarget(eyeLoc, anchor, mPlayer);
			mAnchorToTarget.put(anchor.getEntityId(), uiTarget);
		}
	}

	@Override
	public void run() {
		mAnimationPhase -= 5;
		if (mAnimationPhase < 0) {
			mAnimationPhase += 360;
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
			}
		}
		for (UUID entityId : toEntitiesUnlink) {
			mAnchorToTarget.remove(entityId);
		}

		// Process new anchors
		Location eyeLoc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();
		for (EntityTravelAnchor anchor : TravelAnchorManager.getInstance().anchorsInWorld(world).getAnchors()) {
			if (mAnchorToTarget.containsKey(anchor.getEntityId())) {
				continue;
			}
			TravelUiTarget uiTarget = new TravelUiTarget(eyeLoc, anchor, mPlayer);
			mAnchorToTarget.put(anchor.getEntityId(), uiTarget);
		}

		// Find the closest target to the player's look angle
		Vector lookDir = mPlayer.getLocation().getDirection();
		double closestLookDiff = 5.0;
		TravelUiTarget closestTarget = null;
		for (TravelUiTarget target : mAnchorToTarget.values()) {
			double lookDiff = lookDir.distanceSquared(target.getDir());
			if (lookDiff >= closestLookDiff) {
				continue;
			}
			closestLookDiff = lookDiff;
			closestTarget = target;
		}
		if (!Objects.equals(mClosestTarget, closestTarget)) {
			if (mClosestTarget != null) {
				mClosestTarget.highlight(false, 0);
			}
			mClosestTarget = closestTarget;
		}
		if (mClosestTarget != null) {
			mClosestTarget.highlight(true, mAnimationPhase);
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
}
