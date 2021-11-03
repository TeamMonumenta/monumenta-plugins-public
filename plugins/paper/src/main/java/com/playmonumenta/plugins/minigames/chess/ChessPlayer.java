package com.playmonumenta.plugins.minigames.chess;

import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessTeam;

import org.bukkit.entity.Player;

public class ChessPlayer {

	//Not used for this implementations
	public enum ChessPiecesTier {
		DEFAULT,
		PATREON_25,
		CHAMPION;
	}

	private static final String CHESS_PLAYER_TAG = "ChessPlayer";

	public final Player mPlayer;

	/**
	 * The chessboard where this player is playing
	 */
	public final ChessBoard mBoard;

	/**
	 * The Team of this player (Black or White)
	 */
	public final ChessTeam mTeam;

	public ChessPlayer(Player player, ChessBoard board, ChessTeam team) {
		mPlayer = player;
		mBoard = board;
		mTeam = team;
		mPlayer.addScoreboardTag(CHESS_PLAYER_TAG);
	}

	public ChessPiecesTier getPiecesTier() {
		return ChessPiecesTier.DEFAULT;
	}

	public static final ChessPiecesTier getPieceTier(Player player) {
		return ChessPiecesTier.DEFAULT;
	}

	public static final boolean isChessPlayer(Player player) {
		return player.getScoreboardTags().contains(CHESS_PLAYER_TAG);
	}

	public static final void removeChessPlayer(Player player) {
		player.removeScoreboardTag(CHESS_PLAYER_TAG);
	}

	public static final void removeChessPlayer(ChessPlayer player) {
		if (player != null && player.mPlayer != null && player.mPlayer.getScoreboardTags() != null) {
			player.mPlayer.removeScoreboardTag(CHESS_PLAYER_TAG);
		}
	}
}
