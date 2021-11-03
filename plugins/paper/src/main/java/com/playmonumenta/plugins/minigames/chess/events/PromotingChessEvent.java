package com.playmonumenta.plugins.minigames.chess.events;

import com.playmonumenta.plugins.minigames.chess.ChessBoard;
import com.playmonumenta.plugins.minigames.chess.ChessPlayer;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPiece;

public class PromotingChessEvent extends ChessEvent {

	private final ChessPlayer mPromotingPlayer;
	private final ChessPiece mChessPiece;

	public PromotingChessEvent(ChessBoard board, ChessPlayer promotingPlayer, ChessPiece piece) {
		super(board, null, null);
		mPromotingPlayer = promotingPlayer;
		mChessPiece = piece;
	}

	public ChessPlayer getPlayer() {
		return mPromotingPlayer;
	}

	public ChessPiece getPiece() {
		return mChessPiece;
	}

}
