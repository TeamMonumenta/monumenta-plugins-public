package com.playmonumenta.plugins.minigames.chess.events;

import com.playmonumenta.plugins.minigames.chess.ChessBoard;
import com.playmonumenta.plugins.minigames.chess.ChessPlayer;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EndGameChessEvent extends ChessEvent {

	private float mEndGameScore = -1;
	//0 if white win, 1 if black win, 0.5 draw

	public EndGameChessEvent(ChessBoard board, ChessPlayer white, ChessPlayer black) {
		super(board, white, black);
	}

	public void setEndGameScore(float score) {
		mEndGameScore = score;
	}

	public float getEndGameScore() {
		return mEndGameScore;
	}

	public @Nullable ChessPlayer getWinner() {
		if (mEndGameScore == 0) {
			return mWhitePlayer;
		}

		if (mEndGameScore == 1) {
			return mBlackPlayer;
		}

		return null;
	}

	public @Nullable ChessPlayer getLoser() {
		if (mEndGameScore == 1) {
			return mWhitePlayer;
		}

		if (mEndGameScore == 0) {
			return mBlackPlayer;
		}

		return null;
	}
}
