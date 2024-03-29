package com.playmonumenta.plugins.minigames.chess.events;

import com.playmonumenta.plugins.minigames.chess.ChessBoard;
import com.playmonumenta.plugins.minigames.chess.ChessPlayer;
import org.jetbrains.annotations.Nullable;

public class MovePieceChessEvent extends ChessEvent {

	private final int mOldPos;
	private final int mNewPos;

	public MovePieceChessEvent(ChessBoard board, @Nullable ChessPlayer white, @Nullable ChessPlayer black, int oldPos, int newPos) {
		super(board, white, black);
		mOldPos = oldPos;
		mNewPos = newPos;
	}

	public int getOldPos() {
		return mOldPos;
	}

	public int getNewPos() {
		return mNewPos;
	}

}
