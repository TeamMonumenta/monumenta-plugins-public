package com.playmonumenta.plugins.minigames.chess;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.minigames.chess.ChessManager.ChessBoardType;
import com.playmonumenta.plugins.minigames.chess.events.ChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.EndGameChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.MovePieceChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.PromotingChessEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ChessBoard {

	public enum BoardState {
		STARTED,
		WAIT,
		BLACK_TURN,
		WHITE_TURN,
		WAIT_TURN,
		ENDED,
		FORCE_END;
	}

	public enum ChessTeam {
		WHITE,
		BLACK;
	}

	public enum ChessPieceType {
		KING,
		QUEEN,
		ROOKS,
		BISHOPS,
		KNIGHTS,
		PAWNS;
	}

	private static final String PAWNS_EN_PASSANT_TAG = "EnPassantPossible";
	//private static final String KING_UNDER_ATTACK_TAG = "KingUnderAttack";
	//private static final String BLOCK_KING_UNDER_ATTACK_TAG = "AttackingEnemyKing";

	private static final int[][] ROOKS_MOVES = {{-8, -16, -24, -32, -40, -48, -56},
	                                            {8, 16, 24, 32, 40, 48, 56}};
	private static final int[][] ROOKS_MOVES_RIGHT = {{1, 2, 3, 4, 5, 6, 7}};
	private static final int[][] ROOKS_MOVES_LEFT = {{-1, -2, -3, -4, -5, -6, -7}};

	private static final int[][] BISHOPS_MOVES_RIGHT = {{9, 18, 27, 36, 45, 54, 63},
	                                                    {-7, -14, -21, -28, -35, -42, -49}};
	private static final int[][] BISHOPS_MOVES_LEFT = {{-9, -18, -27, -36, -45, -54, -63},
	                                                   {7, 14, 21, 28, 35, 42, 49}};

	private static final int[] KNIGHTS_MOVES_RIGHT = {-6, -15, 10, 17};
	private static final int[] KNIGHTS_MOVES_LEFT = {6, 15, -10, -17};

	private static final int[] KING_MOVES = {-8, 8};
	private static final int[] KING_MOVES_RIGHT = {1, 9, -7};
	private static final int[] KING_MOVES_LEFT = {-1, -9, 7};

	private static final int[][] QUEEN_MOVES = ROOKS_MOVES;
	private static final int[][] QUEEN_MOVES_RIGHT = {{9, 18, 27, 36, 45, 54, 63},
	                                                  {-7, -14, -21, -28, -35, -42, -49},
	                                                  {1, 2, 3, 4, 5, 6, 7}};
	private static final int[][] QUEEN_MOVES_LEFT = {{-9, -18, -27, -36, -45, -54, -63},
	                                                 {7, 14, 21, 28, 35, 42, 49},
	                                                 {-1, -2, -3, -4, -5, -6, -7}};

	public static class ChessPiece {
		private ChessTeam mTeam;
		private ChessPieceType mType;
		private int mBoardLoc;
		private Set<Integer> mPossibleMoves = new HashSet<>();
		private Set<String> mPieceTags = new HashSet<>();

		public ChessPiece(ChessPieceType type, ChessTeam team, int boardLocation) {
			mType = type;
			mTeam = team;
			mBoardLoc = boardLocation;
		}

		public ChessTeam getPieceTeam() {
			return mTeam;
		}

		public ChessPieceType getPieceType() {
			return mType;
		}

		public int getPieceLocation() {
			return mBoardLoc;
		}

		public void setPossibleMoves(Collection<Integer> moves) {
			mPossibleMoves.clear();
			mPossibleMoves.addAll(moves);
		}

		public Set<Integer> getPossibleMoves() {
			return mPossibleMoves;
		}

		@Override
		public String toString() {
			return "Chess Piece type: " + mType.name().toLowerCase() + " Team: " + mTeam.name().toLowerCase() + " Loc: " + mBoardLoc;
		}
	}

	private final ChessBoard INSTANCE;
	private final String mName;
	private final ChessBoardType mType;

	private final ChessPiece[] mBoard = new ChessPiece[64];


	//list of all the piece on the board
	private final List<ChessPiece> mWhitePieces = new ArrayList<>();
	private final List<ChessPiece> mBlackPieces = new ArrayList<>();

	//contains all the cell that can be eatable by a team.
	public final Set<Integer> mBlackCellsEatable = new HashSet<>();
	public final Set<Integer> mWhiteCellsEatable = new HashSet<>();

	// list of command to run at the end of the game if the player win and it was a quest version
	//private List<String> mCommandsToRun = new ArrayList<>(); //used for quest

	private @Nullable ChessPlayer mWhitePlayer = null;
	private @Nullable ChessPlayer mBlackPlayer = null;
	private BoardState mBoardState;

	//Castle (arrocco)
	private boolean mWhiteCastlingLong = true;
	private boolean mWhiteCastlingShort = true;

	private boolean mBlackCastlingLong = true;
	private boolean mBlackCastlingShort = true;

	private String mStartingFenConfig = ChessManager.FEN_DEFAULT_BOARD_STRING;

	public ChessBoard(String name, ChessBoardType type) {
		mName = name;
		mType = type;
		INSTANCE = this;
		mBoardState = BoardState.STARTED;
	}

	public ChessBoardType getBoardType() {
		return mType;
	}

	public String getName() {
		return mName;
	}

	private int canMoveTo(ChessPiece piece, int newLocation) {
		if (piece == null || newLocation < 0 || newLocation > 63) {
			return -1;
		}

		ChessPiece targetPiece = mBoard[newLocation];
		if (targetPiece != null) {
			return (targetPiece.mTeam == piece.mTeam ? -1 : 1);
		}

		return 0;
	}

	private int canMoveToWithLeftBorder(ChessPiece piece, int newLocation) {
		return canMoveTo(piece, newLocation) + ((newLocation % 8) < (piece.mBoardLoc % 8) ? 0 : -10);
	}

	private int canMoveToWithRightBorder(ChessPiece piece, int newLocation) {
		return canMoveTo(piece, newLocation) + ((newLocation % 8) > (piece.mBoardLoc % 8) ? 0 : -10);
	}

	private List<Integer> getPossibleMovesPawns(ChessPiece pawn) {
		List<Integer> list = new ArrayList<>();
		int canMove = 0;
		if (pawn.mTeam == ChessTeam.BLACK) {
			canMove += canMoveTo(pawn, pawn.mBoardLoc + 8);
			if (canMove == 0) {
				list.add(pawn.mBoardLoc + 8);
				if (pawn.mBoardLoc < 16 && canMoveTo(pawn, pawn.mBoardLoc + 16) == 0) {
					list.add(pawn.mBoardLoc + 16);
				}
			}

			ChessPiece enemyPiece = pawn.mBoardLoc + 9 < 64 ? mBoard[pawn.mBoardLoc + 9] : null;
			if (enemyPiece != null) {
				if (canMoveToWithRightBorder(pawn, pawn.mBoardLoc + 9) == 1) {
					list.add(pawn.mBoardLoc + 9);
				}
			}

			if (canMoveToWithLeftBorder(pawn, pawn.mBoardLoc + 7) == 1) {
				enemyPiece = mBoard[pawn.mBoardLoc + 7];
				if (enemyPiece != null) {
					list.add(pawn.mBoardLoc + 7);
				}
			}
		} else {
			canMove += canMoveTo(pawn, pawn.mBoardLoc - 8);
			if (canMove == 0) {
				list.add(pawn.mBoardLoc - 8);
				if (pawn.mBoardLoc > 47 && canMoveTo(pawn, pawn.mBoardLoc - 16) == 0) {
					list.add(pawn.mBoardLoc - 16);
				}
			}
			ChessPiece enemyPiece = pawn.mBoardLoc - 9 >= 0 ? mBoard[pawn.mBoardLoc - 9] : null;
			if (enemyPiece != null) {
				if (canMoveToWithLeftBorder(pawn, pawn.mBoardLoc - 9) == 1) {
					list.add(pawn.mBoardLoc - 9);
				}
			}
			if (canMoveToWithRightBorder(pawn, pawn.mBoardLoc - 7) == 1) {
				enemyPiece = mBoard[pawn.mBoardLoc - 7];
				if (enemyPiece != null) {
					list.add(pawn.mBoardLoc - 7);
				}
			}
		}

		//enpassant left and right
		ChessPiece enemyPiece = mBoard[pawn.mBoardLoc + 1];
		if (enemyPiece != null && enemyPiece.mTeam != pawn.mTeam && enemyPiece.mType == ChessPieceType.PAWNS && enemyPiece.mPieceTags.contains(PAWNS_EN_PASSANT_TAG)) {
			if (canMoveToWithRightBorder(pawn, pawn.mBoardLoc + (pawn.mTeam == ChessTeam.WHITE ? -7 : 9)) == 0) {
				list.add(pawn.mBoardLoc + (pawn.mTeam == ChessTeam.WHITE ? -7 : 9));
			}
		}

		enemyPiece = mBoard[pawn.mBoardLoc - 1];
		if (enemyPiece != null && enemyPiece.mTeam != pawn.mTeam && enemyPiece.mType == ChessPieceType.PAWNS && enemyPiece.mPieceTags.contains(PAWNS_EN_PASSANT_TAG)) {
			if (canMoveToWithLeftBorder(pawn, pawn.mBoardLoc + (pawn.mTeam == ChessTeam.WHITE ? -9 : 7)) == 0) {
				list.add(pawn.mBoardLoc + (pawn.mTeam == ChessTeam.WHITE ? -9 : 7));
			}
		}
		return list;
	}

	private List<Integer> getPossibleMovesKings(ChessPiece piece) {
		List<Integer> list = new ArrayList<>();
		int canMove = 0;

		for (int move: KING_MOVES) {
			canMove = canMoveTo(piece, piece.mBoardLoc + move);
			if (canMove >= 0 && !squareUnderAttack(piece.mTeam, piece.mBoardLoc + move)) {
				list.add(piece.mBoardLoc + move);
			}
		}
		for (int move: KING_MOVES_LEFT) {
			canMove = canMoveToWithLeftBorder(piece, piece.mBoardLoc + move);
			if (canMove >= 0 && !squareUnderAttack(piece.mTeam, piece.mBoardLoc + move)) {
				list.add(piece.mBoardLoc + move);
			}
		}
		for (int move: KING_MOVES_RIGHT) {
			canMove = canMoveToWithRightBorder(piece, piece.mBoardLoc + move);
			if (canMove >= 0 && !squareUnderAttack(piece.mTeam, piece.mBoardLoc + move)) {
				list.add(piece.mBoardLoc + move);
			}
		}

		if (piece.mTeam == ChessTeam.WHITE) {
			if (!squareUnderAttack(piece.mTeam, piece.mBoardLoc)) {
				if (mWhiteCastlingLong) {
					boolean canCast = true;
					//checking positions 57 - 58 - 59
					for (int i = 57; i < 60; i++) {
						if (squareUnderAttack(piece.mTeam, i) || canMoveTo(piece, i) != 0) {
							canCast = false;
							break;
						}
					}

					if (canCast) {
						list.add(58);
					}
				}

				if (mWhiteCastlingShort) {
					boolean canCast = !(squareUnderAttack(piece.mTeam, 60) || squareUnderAttack(piece.mTeam, 61) || squareUnderAttack(piece.mTeam, 62)) &&
										canMoveTo(piece, 60) == 0 && canMoveTo(piece, 61) == 0 && canMoveTo(piece, 62) == 0;

					if (canCast) {
						list.add(62);
					}
				}
			}

		} else {
			if (!squareUnderAttack(piece.mTeam, piece.mBoardLoc)) {
				if (mBlackCastlingShort) {
					boolean canCast = true;
					//checking positions 1 - 2 - 3
					for (int i = 2; i < 3; i++) {
						if (squareUnderAttack(piece.mTeam, i) || canMoveTo(piece, i) != 0) {
							canCast = false;
							break;
						}
					}
					if (canCast) {
						list.add(2);
					}

				}

				if (mBlackCastlingLong) {
					boolean canCast = !(squareUnderAttack(piece.mTeam, 5) || squareUnderAttack(piece.mTeam, 6) || squareUnderAttack(piece.mTeam, 4)) &&
										canMoveTo(piece, 5) == 0 && canMoveTo(piece, 6) == 0 && canMoveTo(piece, 4) == 0;

					if (canCast) {
						list.add(6);
					}
				}
			}
		}

		return list;
	}

	public List<Integer> getPossibleMoves(ChessPiece piece) {
		List<Integer> list = new ArrayList<>();
		int canMove = 0;
		switch (piece.mType) {
			case KING:
				return getPossibleMovesKings(piece);
			case QUEEN:
				for (int[] moves: QUEEN_MOVES) {
					canMove = 0;
					for (int move: moves) {
						canMove += canMoveTo(piece, piece.mBoardLoc + move);
						if (canMove == 0) {
							list.add(piece.mBoardLoc + move);
						} else if (canMove > 0) {
							list.add(piece.mBoardLoc + move);
							break;
						} else {
							break;
						}
					}
				}
				for (int[] moves: QUEEN_MOVES_LEFT) {
					canMove = 0;
					for (int move : moves) {
						canMove += canMoveToWithLeftBorder(piece, piece.mBoardLoc + move);
						if (canMove == 0) {
							list.add(piece.mBoardLoc + move);
						} else if (canMove > 0) {
							list.add(piece.mBoardLoc + move);
							break;
						} else {
							break;
						}
					}
				}
				for (int[] moves: QUEEN_MOVES_RIGHT) {
					canMove = 0;
					for (int move : moves) {
						canMove += canMoveToWithRightBorder(piece, piece.mBoardLoc + move);
						if (canMove == 0) {
							list.add(piece.mBoardLoc + move);
						} else if (canMove > 0) {
							list.add(piece.mBoardLoc + move);
							break;
						} else {
							break;
						}
					}
				}
				break;
			case BISHOPS:
				for (int[] moves: BISHOPS_MOVES_LEFT) {
					canMove = 0;
					for (int move : moves) {
						canMove += canMoveToWithLeftBorder(piece, piece.mBoardLoc + move);
						if (canMove == 0) {
							list.add(piece.mBoardLoc + move);
						} else if (canMove > 0) {
							list.add(piece.mBoardLoc + move);
							break;
						} else {
							break;
						}
					}
				}
				for (int[] moves: BISHOPS_MOVES_RIGHT) {
					canMove = 0;
					for (int move : moves) {
						canMove += canMoveToWithRightBorder(piece, piece.mBoardLoc + move);
						if (canMove == 0) {
							list.add(piece.mBoardLoc + move);
						} else if (canMove > 0) {
							list.add(piece.mBoardLoc + move);
							break;
						} else {
							break;
						}
					}
				}
				break;
			case ROOKS:
				for (int[] moves: ROOKS_MOVES) {
					canMove = 0;
					for (int move : moves) {
						canMove += canMoveTo(piece, piece.mBoardLoc + move);
						if (canMove == 0) {
							list.add(piece.mBoardLoc + move);
						} else if (canMove > 0) {
							list.add(piece.mBoardLoc + move);
							break;
						} else {
							break;
						}
					}
				}

				for (int[] moves: ROOKS_MOVES_LEFT) {
					canMove = 0;
					for (int move : moves) {
						canMove += canMoveToWithLeftBorder(piece, piece.mBoardLoc + move);
						if (canMove == 0) {
							list.add(piece.mBoardLoc + move);
						} else if (canMove > 0) {
							list.add(piece.mBoardLoc + move);
							break;
						} else {
							break;
						}
					}
				}

				for (int[] moves: ROOKS_MOVES_RIGHT) {
					canMove = 0;
					for (int move : moves) {
						canMove += canMoveToWithRightBorder(piece, piece.mBoardLoc + move);
						if (canMove == 0) {
							list.add(piece.mBoardLoc + move);
						} else if (canMove > 0) {
							list.add(piece.mBoardLoc + move);
							break;
						} else {
							break;
						}
					}
				}

				break;
			case KNIGHTS:
				canMove = 0;
				for (int move: KNIGHTS_MOVES_LEFT) {
					canMove = canMoveToWithLeftBorder(piece, piece.mBoardLoc + move);
					if (canMove >= 0) {
						list.add(piece.mBoardLoc + move);
					}
				}
				canMove = 0;
				for (int move: KNIGHTS_MOVES_RIGHT) {
					canMove = canMoveToWithRightBorder(piece, piece.mBoardLoc + move);
					if (canMove >= 0) {
						list.add(piece.mBoardLoc + move);
					}
				}
				break;
			case PAWNS:
				list = getPossibleMovesPawns(piece);
				break;
			default:
				break;

		}

		return list;
	}

	public boolean movePiece(int startingLocation, int endLocation) {
		ChessPiece startPiece = mBoard[startingLocation];
		if (startPiece == null) {
			return false;
		}

		//Should check if the king is under attack after this move
		Set<Integer> possibleMoves = new HashSet<>();
		possibleMoves.addAll(getPossibleMoves(startPiece));

		if (!possibleMoves.contains(endLocation)) {
			return false;
		}

		mBoardState = BoardState.WAIT_TURN;

		//launch movingPiece event
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getPluginManager().callEvent(new MovePieceChessEvent(INSTANCE, mWhitePlayer, mBlackPlayer, startingLocation, endLocation));
			}
		}.runTaskLater(Plugin.getInstance(), 1);


		if (startPiece.mType == ChessPieceType.PAWNS) {
			if (startPiece.mTeam == ChessTeam.BLACK && endLocation > 55) {
				new BukkitRunnable() {
					@Override
					public void run() {
						Bukkit.getPluginManager().callEvent(new PromotingChessEvent(INSTANCE, mBlackPlayer, startPiece));
					}
				}.runTaskLater(Plugin.getInstance(), 3);
			} else if (startPiece.mTeam == ChessTeam.WHITE && endLocation < 8) {
				new BukkitRunnable() {
					@Override
					public void run() {
						Bukkit.getPluginManager().callEvent(new PromotingChessEvent(INSTANCE, mWhitePlayer, startPiece));
					}
				}.runTaskLater(Plugin.getInstance(), 3);
			}
		}

		ChessPiece endPiece = mBoard[endLocation];
		if (endPiece != null) {
			if (endPiece.mType == ChessPieceType.KING) {
				mBoardState = BoardState.ENDED;
				new BukkitRunnable() {
					@Override
					public void run() {
						//GAME ENDED
						EndGameChessEvent chessEvent = new EndGameChessEvent(INSTANCE, mWhitePlayer, mBlackPlayer);
						chessEvent.setEndGameScore(endPiece.mTeam == ChessTeam.BLACK ? 0 : 1);
						Bukkit.getPluginManager().callEvent(chessEvent);
					}
				}.runTaskLater(Plugin.getInstance(), 5);
			}

			if (endPiece.mType == ChessPieceType.ROOKS) {
				if (endPiece.mBoardLoc == 0 || endPiece.mBoardLoc == 56) {
					if (endPiece.mTeam == ChessTeam.WHITE) {
						mWhiteCastlingLong = false;
					} else {
						mBlackCastlingShort = false;
					}
				}

				if (endPiece.mBoardLoc == 7 || endPiece.mBoardLoc == 63) {
					if (endPiece.mTeam == ChessTeam.WHITE) {
						mWhiteCastlingShort = false;
					} else {
						mBlackCastlingLong = false;
					}
				}

			}

			if (startPiece.mTeam == ChessTeam.BLACK) {
				mWhitePieces.remove(endPiece);
			} else {
				mBlackPieces.remove(endPiece);
			}
		} else {
			//Castle
			if (startPiece.mType == ChessPieceType.KING) {
				if (startPiece.mTeam == ChessTeam.WHITE) {

					if (endLocation == 58 && mWhiteCastlingLong) {
						//at position 56 should be a tower
						ChessPiece rook = mBoard[56];
						rook.mBoardLoc = 59;
						mBoard[56] = null;
						mBoard[59] = rook;

						new BukkitRunnable() {
							@Override
							public void run() {
								ChessEvent chessEvent = new MovePieceChessEvent(INSTANCE, mWhitePlayer, mBlackPlayer, 56, 59);
								Bukkit.getPluginManager().callEvent(chessEvent);
							}
						}.runTaskLater(Plugin.getInstance(), 1);
					}

					if (endLocation == 62 && mWhiteCastlingShort) {
						//at position 63 should be a tower
						ChessPiece rook = mBoard[63];
						rook.mBoardLoc = 61;
						mBoard[63] = null;
						mBoard[61] = rook;

						new BukkitRunnable() {
							@Override
							public void run() {
								ChessEvent chessEvent = new MovePieceChessEvent(INSTANCE, mWhitePlayer, mBlackPlayer, 63, 61);
								Bukkit.getPluginManager().callEvent(chessEvent);
							}
						}.runTaskLater(Plugin.getInstance(), 1);
					}

					mWhiteCastlingLong = false;
					mWhiteCastlingShort = false;
				} else {
					if (endLocation == 2 && mBlackCastlingLong) {
						//at position 0 should be a tower
						ChessPiece rook = mBoard[0];
						rook.mBoardLoc = 3;
						mBoard[0] = null;
						mBoard[3] = rook;

						new BukkitRunnable() {
							@Override
							public void run() {
								ChessEvent chessEvent = new MovePieceChessEvent(INSTANCE, mWhitePlayer, mBlackPlayer, 0, 3);
								Bukkit.getPluginManager().callEvent(chessEvent);
							}
						}.runTaskLater(Plugin.getInstance(), 1);
					}

					if (endLocation == 6 && mBlackCastlingShort) {
						//at position 63 should be a tower
						ChessPiece rook = mBoard[7];
						rook.mBoardLoc = 5;
						mBoard[7] = null;
						mBoard[5] = rook;

						new BukkitRunnable() {
							@Override
							public void run() {
								ChessEvent chessEvent = new MovePieceChessEvent(INSTANCE, mWhitePlayer, mBlackPlayer, 7, 5);
								Bukkit.getPluginManager().callEvent(chessEvent);
							}
						}.runTaskLater(Plugin.getInstance(), 1);
					}

					mBlackCastlingLong = false;
					mBlackCastlingShort = false;
				}

			}

		}

		if (startPiece.mType == ChessPieceType.ROOKS) {
			if (startPiece.mTeam == ChessTeam.WHITE) {
				if (startPiece.mBoardLoc == 56) {
					mWhiteCastlingLong = false;
				}

				if (startPiece.mBoardLoc == 63) {
					mWhiteCastlingShort = false;
				}
			} else {
				if (startPiece.mBoardLoc == 0) {
					mBlackCastlingShort = false;
				}

				if (startPiece.mBoardLoc == 7) {
					mBlackCastlingLong = false;
				}
			}
		}

		//en-passant
		if (startPiece.mType == ChessPieceType.PAWNS) {
			if (startingLocation == endLocation + 16 || startingLocation == endLocation - 16) {
				startPiece.mPieceTags.add(PAWNS_EN_PASSANT_TAG);
			} else if (startingLocation == endLocation + 7 || startingLocation == endLocation - 9) {
				ChessPiece possibleEnemy = getChessPiece(startingLocation + 1);
				if (possibleEnemy != null && possibleEnemy.mPieceTags.contains(PAWNS_EN_PASSANT_TAG)) {
					mBoard[startingLocation + 1] = null;
					ChessManager.updateGuiSlot(this, startingLocation + 1);
					mBlackPieces.remove(possibleEnemy);
					mWhitePieces.remove(possibleEnemy);
				}

			} else if (startingLocation == endLocation - 7 || startingLocation == endLocation + 9) {
				ChessPiece possibleEnemy = getChessPiece(startingLocation - 1);
				if (possibleEnemy != null && possibleEnemy.mPieceTags.contains(PAWNS_EN_PASSANT_TAG)) {
					mBoard[startingLocation - 1] = null;
					ChessManager.updateGuiSlot(this, startingLocation - 1);
					mBlackPieces.remove(possibleEnemy);
					mWhitePieces.remove(possibleEnemy);
				}

			}
		}

		//removing en-passant tag
		if (startPiece.mTeam == ChessTeam.WHITE) {
			for (ChessPiece piece : mBlackPieces) {
				piece.mPieceTags.remove(PAWNS_EN_PASSANT_TAG);
			}
		} else {
			for (ChessPiece piece : mWhitePieces) {
				piece.mPieceTags.remove(PAWNS_EN_PASSANT_TAG);
			}
		}


		mBoard[startingLocation] = null;
		mBoard[endLocation] = startPiece;
		startPiece.mBoardLoc = endLocation;


		updateEatable();
		return true;

	}

	public void updateEatable() {
		//update the positions
		new BukkitRunnable() {
			@Override
			public void run() {
				mBlackCellsEatable.clear();
				mWhiteCellsEatable.clear();

				for (ChessPiece piece : mBlackPieces) {
					piece.setPossibleMoves(getPossibleMoves(piece));
					mBlackCellsEatable.addAll(piece.mPossibleMoves);
				}

				for (ChessPiece piece : mWhitePieces) {
					piece.setPossibleMoves(getPossibleMoves(piece));
					mWhiteCellsEatable.addAll(piece.mPossibleMoves);
				}
			}
		}.runTaskLater(Plugin.getInstance(), 3);
	}

	public boolean squareUnderAttack(ChessTeam team, int newLocation) {
		if (team == ChessTeam.BLACK) {
			return mWhiteCellsEatable.contains(newLocation);
		} else {
			return mBlackCellsEatable.contains(newLocation);
		}
	}

	public ChessPiece getChessPiece(int pos) {
		return mBoard[pos];
	}

	//loading a board from a fen string. generic sting: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
	public void buildBoardFromString(String board) {
		clearBoard();
		mStartingFenConfig = board;
		int pos = 0;

		String[] section = board.split(" ");

		for (char piece : section[0].toCharArray()) {
			if (piece == '/') {
				continue;
			} else {
				if (Character.isDigit(piece)) {
					pos += Integer.parseInt("" + piece);
				} else {
					ChessTeam team = Character.isUpperCase(piece) ? ChessTeam.WHITE : ChessTeam.BLACK;
					ChessPieceType type = null;
					switch (Character.toLowerCase(piece)) {
						case 'k' -> type = ChessPieceType.KING;
						case 'q' -> type = ChessPieceType.QUEEN;
						case 'b' -> type = ChessPieceType.BISHOPS;
						case 'r' -> type = ChessPieceType.ROOKS;
						case 'n' -> type = ChessPieceType.KNIGHTS;
						case 'p' -> type = ChessPieceType.PAWNS;
						default -> {
							pos++;
							continue;
						}
					}
					ChessPiece chessPiece = new ChessPiece(type, team, pos);
					mBoard[pos] = chessPiece;
					if (team == ChessTeam.WHITE) {
						mWhitePieces.add(chessPiece);
					} else {
						mBlackPieces.add(chessPiece);
					}

					pos++;

				}
			}
		}

		mBoardState = (section[1].contains("w") ? BoardState.WHITE_TURN : BoardState.BLACK_TURN);

		if (section.length > 2) {
			String castlings = section[2];
			if (castlings.contains("K")) {
				mWhiteCastlingShort = true;
			} else {
				mWhiteCastlingShort = false;
			}
			if (castlings.contains("Q")) {
				mWhiteCastlingLong = true;
			} else {
				mWhiteCastlingLong = false;
			}
			if (castlings.contains("k")) {
				mBlackCastlingShort = true;
			} else {
				mBlackCastlingShort = false;
			}
			if (castlings.contains("q")) {
				mBlackCastlingLong = true;
			} else {
				mBlackCastlingLong = false;
			}
		}

		//TODO-implement rules of 5 move and remaning stuff
	}

	public String convertBoardToFenString() {
		String fen = "";
		int count = 0;
		for (int pos = 0; pos < 64; pos++) {
			if (pos != 0 && pos % 8 == 0) {
				if (count != 0) {
					fen += count;
				}
				fen += "/";
				count = 0;
			}

			if (mBoard[pos] == null) {
				count++;
			} else {
				if (count != 0) {
					fen += count;
					count = 0;
				}
				ChessPiece piece = mBoard[pos];
				char charPiece = ' ';
				switch (piece.mType) {
					case ROOKS:
						charPiece = piece.mTeam == ChessTeam.WHITE ? 'R' : 'r';
						break;
					case PAWNS:
						charPiece = piece.mTeam == ChessTeam.WHITE ? 'P' : 'p';
						break;
					case QUEEN:
						charPiece = piece.mTeam == ChessTeam.WHITE ? 'Q' : 'q';
						break;
					case KING:
						charPiece = piece.mTeam == ChessTeam.WHITE ? 'K' : 'k';
						break;
					case KNIGHTS:
						charPiece = piece.mTeam == ChessTeam.WHITE ? 'N' : 'n';
						break;
					case BISHOPS:
						charPiece = piece.mTeam == ChessTeam.WHITE ? 'B' : 'b';
						break;
					default:
						break;
				}
				fen += charPiece;
			}
		}

		fen += " ";

		fen += isWhiteTurn() ? "w" : "b";

		fen += " ";

		if (mBlackCastlingLong) {
			fen += "q";
		}

		if (mBlackCastlingShort) {
			fen += "k";
		}

		if (mWhiteCastlingLong) {
			fen += "Q";
		}

		if (mWhiteCastlingShort) {
			fen += "K";
		}

		return fen;
	}

	public void restart() {
		mBoardState = BoardState.STARTED;
		buildBoardFromString(mStartingFenConfig);
	}

	public void clearBoard() {
		for (int i = 0; i < 64; i++) {
			mBoard[i] = null;
		}
		mBlackPieces.clear();
		mWhitePieces.clear();
	}

	public void updateState(BoardState newState) {
		mBoardState = newState;
	}

	public boolean isWhitePiece(int pos) {
		return mBoard[pos] != null && mBoard[pos].mTeam == ChessTeam.WHITE;
	}

	public boolean isBlackPiece(int pos) {
		return mBoard[pos] != null && mBoard[pos].mTeam == ChessTeam.BLACK;
	}

	public void setPlayer(Player player, ChessTeam team) {
		if (team == ChessTeam.WHITE) {
			mWhitePlayer = new ChessPlayer(player, INSTANCE, ChessTeam.WHITE);
		} else {
			mBlackPlayer = new ChessPlayer(player, INSTANCE, ChessTeam.BLACK);
		}
	}

	public @Nullable ChessPlayer getPlayer(ChessTeam team) {
		if (team == ChessTeam.WHITE) {
			return mWhitePlayer;
		} else {
			return mBlackPlayer;
		}
	}

	public void changePieceType(ChessPiece piece, ChessPieceType type) {
		piece.mType = type;
		ChessManager.updateGuiSlot(INSTANCE, piece.mBoardLoc);
	}

	public void setPiece(ChessPiece piece, int pos) {
		if (piece != null) {
			mBoard[pos] = piece;
			if (piece.mTeam == ChessTeam.BLACK) {
				mBlackPieces.add(piece);
			} else {
				mWhitePieces.add(piece);
			}
		} else {
			mBoard[pos] = null;
		}
		ChessManager.updateGuiSlot(INSTANCE, pos);
	}

	public boolean isBlackTurn() {
		return mBoardState == BoardState.BLACK_TURN;
	}

	public boolean isWhiteTurn() {
		return mBoardState == BoardState.WHITE_TURN;
	}

}
