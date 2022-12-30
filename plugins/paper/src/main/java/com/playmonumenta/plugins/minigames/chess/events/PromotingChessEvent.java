package com.playmonumenta.plugins.minigames.chess.events;

import com.playmonumenta.plugins.minigames.chess.ChessBoard;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPiece;
import com.playmonumenta.plugins.minigames.chess.ChessPlayer;
import org.jetbrains.annotations.Nullable;

public class PromotingChessEvent extends ChessEvent {

	private final @Nullable ChessPlayer mPromotingPlayer;
	private final ChessPiece mChessPiece;

	public PromotingChessEvent(ChessBoard board, @Nullable ChessPlayer promotingPlayer, ChessPiece piece) {
		super(board, null, null);
		mPromotingPlayer = promotingPlayer;
		mChessPiece = piece;
	}

	public @Nullable ChessPlayer getPlayer() {
		return mPromotingPlayer;
	}

	public ChessPiece getPiece() {
		return mChessPiece;
	}

}
