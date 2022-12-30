package com.playmonumenta.plugins.minigames.chess.events;

import com.playmonumenta.plugins.minigames.chess.ChessBoard;
import com.playmonumenta.plugins.minigames.chess.ChessPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

public class ChessEvent extends Event {

	protected final ChessBoard mBoard;
	protected final @Nullable ChessPlayer mWhitePlayer;
	protected final @Nullable ChessPlayer mBlackPlayer;

	public ChessEvent(ChessBoard board, @Nullable ChessPlayer whitePlayer, @Nullable ChessPlayer blackPlayer) {
		mBoard = board;
		mWhitePlayer = whitePlayer;
		mBlackPlayer = blackPlayer;
	}

	public ChessBoard getBoard() {
		return mBoard;
	}

	public @Nullable ChessPlayer getBlackPlayer() {
		return mBlackPlayer;
	}

	public @Nullable ChessPlayer getWhitePlayer() {
		return mWhitePlayer;
	}

	private static final HandlerList HANDLERS = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

}
