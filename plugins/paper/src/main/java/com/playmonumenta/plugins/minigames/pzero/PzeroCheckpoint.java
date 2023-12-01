package com.playmonumenta.plugins.minigames.pzero;

import com.playmonumenta.plugins.utils.Hitbox;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class PzeroCheckpoint {
	private final int mId;
	private final Vector mCornerCoordinates;
	private final Vector mDeltas;

	public PzeroCheckpoint(int id, Vector cornerCoordinates, Vector deltas) {
		mId = id;
		mCornerCoordinates = cornerCoordinates;
		mDeltas = deltas;
	}

	public int getId() {
		return mId;
	}

	public boolean isPlayerInside(PzeroPlayer pzPlayer) {
		return isPlayerInside(pzPlayer.getPlayer());
	}

	public boolean isPlayerInside(Player player) {
		return new Hitbox.AABBHitbox(player.getWorld(), BoundingBox.of(mCornerCoordinates, mCornerCoordinates.clone().add(mDeltas)))
			.getHitPlayers(true).contains(player);
	}

	public double distanceSquaredFromCenter(PzeroPlayer pzPlayer) {
		return distanceSquaredFromCenter(pzPlayer.getPlayer());
	}

	public double distanceSquaredFromCenter(Player player) {
		Location centerLoc = mCornerCoordinates.clone().add(mDeltas.clone().multiply(0.5)).toLocation(player.getWorld());
		return player.getLocation().distanceSquared(centerLoc);
	}
}
