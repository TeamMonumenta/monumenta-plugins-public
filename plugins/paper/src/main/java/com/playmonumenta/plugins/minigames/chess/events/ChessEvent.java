package com.playmonumenta.plugins.minigames.chess.events;

import com.playmonumenta.plugins.minigames.chess.ChessBoard;
import com.playmonumenta.plugins.minigames.chess.ChessPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ChessEvent extends Event {

	protected final ChessBoard mBoard;
	protected final ChessPlayer mWhitePlayer;
	protected final ChessPlayer mBlackPlayer;

	public ChessEvent(ChessBoard board, ChessPlayer whitePlayer, ChessPlayer blackPlayer) {
		mBoard = board;
		mWhitePlayer = whitePlayer;
		mBlackPlayer = blackPlayer;
	}

	public ChessBoard getBoard() {
		return mBoard;
	}

	public ChessPlayer getBlackPlayer() {
		return mBlackPlayer;
	}

	public ChessPlayer getWhitePlayer() {
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
