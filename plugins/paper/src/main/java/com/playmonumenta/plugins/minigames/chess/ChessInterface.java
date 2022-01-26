package com.playmonumenta.plugins.minigames.chess;

import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPiece;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPieceType;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessTeam;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.scriptedquests.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChessInterface {
	public enum InterfaceType {
		WHITEPLAYER,
		BLACKPLAYER,
		SPECTATOR;
	}

	public enum FacingPosition {
		NORTH("north", +1, -1, +1),
		SOUTH("south", -1, +1, +1);

		private final String mLabel;
		private final double mX;
		private final double mX2;
		private final double mZ;

		FacingPosition(String label, double x, double y, double z) {
			mLabel = label;
			mX = x;
			mX2 = y;
			mZ = z;
		}

		public String getLabel() {
			return mLabel;
		}

		public static @Nullable FacingPosition valueOfLabel(String label) {
			for (FacingPosition fp : FacingPosition.values()) {
				if (fp.mLabel.equalsIgnoreCase(label)) {
					return fp;
				}
			}
			return null;
		}

	}

	//white -> black -> green
	private static final int[] MAP_ID_BOARD = {1, 2, 3};

	//white -> black
	private static final int[][] MAP_ID_BISHOPS = {{22, 23, 24}, {25, 26, 27}};
	private static final int[][] MAP_ID_KING = {{28, 29, 30}, {31, 32, 33}};
	private static final int[][] MAP_ID_KNIGHTS = {{16, 17, 18}, {19, 20, 21}};
	private static final int[][] MAP_ID_PWANS = {{4, 5, 6}, {7, 8, 9}};
	private static final int[][] MAP_ID_QUEEN = {{34, 35, 36}, {37, 38, 39}};
	private static final int[][] MAP_ID_ROOKS = {{10, 11, 12}, {13, 14, 15}};

	/**Offset since on R1 the first 482 map are used*/
	private static final int MAP_ID_OFFSET = 482;

	private static final int MAX_TICK_ALIVE_RUNNABLE = 30 * 20;

	public static final String ITEM_FRAME_TAG = "ChessFrame";

	private static final List<ItemStack> MAP_BOARD = new ArrayList<>();
	private static final Map<ChessPieceType, Map<ChessTeam, List<ItemStack>>> MAP_PIECES = new HashMap<>();

	static {
		MAP_BOARD.add(getMapByID(MAP_ID_BOARD[0]));
		MAP_BOARD.add(getMapByID(MAP_ID_BOARD[1]));
		MAP_BOARD.add(getMapByID(MAP_ID_BOARD[2]));

		//bishop
		Map<ChessTeam, List<ItemStack>> bishopMAP = new HashMap<>();
		List<ItemStack> bishopBlackList = new ArrayList<>();
		bishopBlackList.add(getMapByID(MAP_ID_BISHOPS[0][0]));
		bishopBlackList.add(getMapByID(MAP_ID_BISHOPS[0][1]));
		bishopBlackList.add(getMapByID(MAP_ID_BISHOPS[0][2]));
		bishopMAP.put(ChessTeam.WHITE, bishopBlackList);
		List<ItemStack> bishopWhiteList = new ArrayList<>();
		bishopWhiteList.add(getMapByID(MAP_ID_BISHOPS[1][0]));
		bishopWhiteList.add(getMapByID(MAP_ID_BISHOPS[1][1]));
		bishopWhiteList.add(getMapByID(MAP_ID_BISHOPS[1][2]));
		bishopMAP.put(ChessTeam.BLACK, bishopWhiteList);
		MAP_PIECES.put(ChessPieceType.BISHOPS, bishopMAP);

		//king
		Map<ChessTeam, List<ItemStack>> kingMAP = new HashMap<>();
		List<ItemStack> kingBlackList = new ArrayList<>();
		kingBlackList.add(getMapByID(MAP_ID_KING[0][0]));
		kingBlackList.add(getMapByID(MAP_ID_KING[0][1]));
		kingBlackList.add(getMapByID(MAP_ID_KING[0][2]));
		kingMAP.put(ChessTeam.WHITE, kingBlackList);
		List<ItemStack> kingWhiteList = new ArrayList<>();
		kingWhiteList.add(getMapByID(MAP_ID_KING[1][0]));
		kingWhiteList.add(getMapByID(MAP_ID_KING[1][1]));
		kingWhiteList.add(getMapByID(MAP_ID_KING[1][2]));
		kingMAP.put(ChessTeam.BLACK, kingWhiteList);
		MAP_PIECES.put(ChessPieceType.KING, kingMAP);

		//KNIGHTS
		Map<ChessTeam, List<ItemStack>> knightsMAP = new HashMap<>();
		List<ItemStack> knightsBlackList = new ArrayList<>();
		knightsBlackList.add(getMapByID(MAP_ID_KNIGHTS[0][0]));
		knightsBlackList.add(getMapByID(MAP_ID_KNIGHTS[0][1]));
		knightsBlackList.add(getMapByID(MAP_ID_KNIGHTS[0][2]));
		knightsMAP.put(ChessTeam.WHITE, knightsBlackList);
		List<ItemStack> knightsWhiteList = new ArrayList<>();
		knightsWhiteList.add(getMapByID(MAP_ID_KNIGHTS[1][0]));
		knightsWhiteList.add(getMapByID(MAP_ID_KNIGHTS[1][1]));
		knightsWhiteList.add(getMapByID(MAP_ID_KNIGHTS[1][2]));
		knightsMAP.put(ChessTeam.BLACK, knightsWhiteList);
		MAP_PIECES.put(ChessPieceType.KNIGHTS, knightsMAP);

		//pawns
		Map<ChessTeam, List<ItemStack>> pawnsMAP = new HashMap<>();
		List<ItemStack> pawnsBlackList = new ArrayList<>();
		pawnsBlackList.add(getMapByID(MAP_ID_PWANS[0][0]));
		pawnsBlackList.add(getMapByID(MAP_ID_PWANS[0][1]));
		pawnsBlackList.add(getMapByID(MAP_ID_PWANS[0][2]));
		pawnsMAP.put(ChessTeam.WHITE, pawnsBlackList);
		List<ItemStack> pawnsWhiteList = new ArrayList<>();
		pawnsWhiteList.add(getMapByID(MAP_ID_PWANS[1][0]));
		pawnsWhiteList.add(getMapByID(MAP_ID_PWANS[1][1]));
		pawnsWhiteList.add(getMapByID(MAP_ID_PWANS[1][2]));
		pawnsMAP.put(ChessTeam.BLACK, pawnsWhiteList);
		MAP_PIECES.put(ChessPieceType.PAWNS, pawnsMAP);

		//Queen
		Map<ChessTeam, List<ItemStack>> queenMAP = new HashMap<>();
		List<ItemStack> queenBlackList = new ArrayList<>();
		queenBlackList.add(getMapByID(MAP_ID_QUEEN[0][0]));
		queenBlackList.add(getMapByID(MAP_ID_QUEEN[0][1]));
		queenBlackList.add(getMapByID(MAP_ID_QUEEN[0][2]));
		queenMAP.put(ChessTeam.WHITE, queenBlackList);
		List<ItemStack> queenWhiteList = new ArrayList<>();
		queenWhiteList.add(getMapByID(MAP_ID_QUEEN[1][0]));
		queenWhiteList.add(getMapByID(MAP_ID_QUEEN[1][1]));
		queenWhiteList.add(getMapByID(MAP_ID_QUEEN[1][2]));
		queenMAP.put(ChessTeam.BLACK, queenWhiteList);
		MAP_PIECES.put(ChessPieceType.QUEEN, queenMAP);

		//Rooks
		Map<ChessTeam, List<ItemStack>> rooksMAP = new HashMap<>();
		List<ItemStack> rooksBlackList = new ArrayList<>();
		rooksBlackList.add(getMapByID(MAP_ID_ROOKS[0][0]));
		rooksBlackList.add(getMapByID(MAP_ID_ROOKS[0][1]));
		rooksBlackList.add(getMapByID(MAP_ID_ROOKS[0][2]));
		rooksMAP.put(ChessTeam.WHITE, rooksBlackList);
		List<ItemStack> rooksWhiteList = new ArrayList<>();
		rooksWhiteList.add(getMapByID(MAP_ID_ROOKS[1][0]));
		rooksWhiteList.add(getMapByID(MAP_ID_ROOKS[1][1]));
		rooksWhiteList.add(getMapByID(MAP_ID_ROOKS[1][2]));
		rooksMAP.put(ChessTeam.BLACK, rooksWhiteList);
		MAP_PIECES.put(ChessPieceType.ROOKS, rooksMAP);

	}

	private final ChessBoard mBoard;
	private final Map<ItemFrame, Integer> mFramePosMap;
	private final Map<Integer, ItemFrame> mFrames;
	protected final InterfaceType mType;

	private Set<Integer> mPossibleMoves = null;
	private int mPieceSelected;
	private @Nullable Boolean mPlayerTurn;
	private @Nullable BukkitRunnable mParticleRunnable;

	public ChessInterface(ChessBoard board, InterfaceType type) {
		mBoard = board;
		mType = type;
		mFramePosMap = new HashMap<>();
		mFrames = new HashMap<>();
		updateInteractable();
	}

	public void buildBoard(Location startingPoint, FacingPosition facing) {
		World world = startingPoint.getWorld();
		for (int y = 0; y < 8; y++) {
				for (int x = 8 * y; x < 8 * (y + 1); x++) {
				ItemFrame itemFrame = (ItemFrame) world.spawn(startingPoint.clone().add((7 * facing.mX) + (x % 8) * facing.mX2, y, 0 * facing.mZ), ItemFrame.class);
				itemFrame.addScoreboardTag(mBoard.getName());
				itemFrame.addScoreboardTag(ITEM_FRAME_TAG);
				itemFrame.addScoreboardTag(mType.name());
				itemFrame.setFixed(true);

				if ((y + x) % 2 == 0) {
					itemFrame.setItem(getMapByID(MAP_ID_BOARD[0]), false);
				} else {
					itemFrame.setItem(getMapByID(MAP_ID_BOARD[1]), false);
				}

				mFramePosMap.put(itemFrame, x);
				mFrames.put(x, itemFrame);
			}
		}
		refresh();
	}

	public void moveEvent(int from, int to) {
		ChessPiece movedPiece = mBoard.getChessPiece(to);
		if (mType == InterfaceType.WHITEPLAYER) {
			from = 63 - from;
			to = 63 - to;
		}
		boolean fromSetWhite = ((from / 8) % 2 == 0 && from % 2 == 0) || ((from / 8) % 2 == 1 && from % 2 == 1);
		Boolean toSetWhite = ((to / 8) % 2 == 0 && to % 2 == 0) || ((to / 8) % 2 == 1 && to % 2 == 1);
		mFrames.get(from).setItem(MAP_BOARD.get(fromSetWhite ? 0 : 1), false);
		mFrames.get(to).setItem(MAP_PIECES.get(movedPiece.getPieceType()).get(movedPiece.getPieceTeam()).get(toSetWhite ? 0 : 1), false);

		//spawning particles

		if (mParticleRunnable != null) {
			mParticleRunnable.cancel();
		}

		Location fromLoc = mFrames.get(from).getLocation();
		Location toLoc = mFrames.get(to).getLocation();
		World world = mFrames.get(to).getWorld();


		mParticleRunnable = new BukkitRunnable() {
			Location mFrom = fromLoc.clone();
			Location mTo = toLoc.clone();
			World mWorld = world;
			Vector mDistance = mTo.clone().toVector().subtract(mFrom.clone().toVector());
			Vector mDirection = mDistance.clone().normalize();
			int mTimer = 0;

			@Override
			public void run() {
				if (isCancelled()) {
					return;
				}

				if (!mFrom.isChunkLoaded() || !mTo.isChunkLoaded()) {
					this.cancel();
					return;
				}

				if (mTimer >= MAX_TICK_ALIVE_RUNNABLE) {
					this.cancel();
					return;
				}


				for (double i = 0.05; i < mDistance.length(); i += 0.05) {
					mWorld.spawnParticle(Particle.REDSTONE, mFrom.clone().add(mDirection.clone().multiply(i)), 25, 0.05, 0.05, 0.05, 0, new DustOptions(Color.ORANGE, 0.3f));

				}
				mTimer += 3;
			}

		};

		mParticleRunnable.runTaskTimer(Plugin.getInstance(), 1, 3);
	}

	private static ItemStack getMapByID(int id) {
		ItemStack map = new ItemStack(Material.FILLED_MAP);
		MapMeta meta = (MapMeta) map.getItemMeta();
		//no other methods for pick the right id.
		if (ServerProperties.getShardName().contains("valley")) {
			meta.setMapId(id + MAP_ID_OFFSET);
		} else {
			meta.setMapId(id);
		}
		map.setItemMeta(meta);
		return map;
	}

	public boolean playerInteract(ItemFrame frame, Player p) {
		if (mType == InterfaceType.SPECTATOR) {
			return false;
		}


		if (!mFramePosMap.containsKey(frame)) {
			return false;
		}

		if (Boolean.TRUE.equals(mPlayerTurn)) {
			int pos = mFramePosMap.get(frame);

			if (mPossibleMoves == null) {
				if (mType == InterfaceType.WHITEPLAYER && mBoard.isWhitePiece(63 - pos)) {
					mPossibleMoves = new HashSet<>();
					for (int possible : mBoard.getPossibleMoves(mBoard.getChessPiece(63 - pos))) {
						mPossibleMoves.add(63 - possible);
					}
					mPieceSelected = 63 - pos;
				} else if (mType == InterfaceType.BLACKPLAYER && mBoard.isBlackPiece(pos)) {
					mPossibleMoves = new HashSet<>(mBoard.getPossibleMoves(mBoard.getChessPiece(pos)));
					mPieceSelected = pos;
				} else {
					return true;
				}

				for (int i : mPossibleMoves) {
					ItemFrame possibleFrame = mFrames.get(i);
					ChessPiece piece = mBoard.getChessPiece(i);
					if (mType == InterfaceType.WHITEPLAYER) {
						piece = mBoard.getChessPiece(63 - i);
					}
					if (piece == null) {
						possibleFrame.setItem(MAP_BOARD.get(2), false);
					} else {
						possibleFrame.setItem(MAP_PIECES.get(piece.getPieceType()).get(piece.getPieceTeam()).get(2), false);
					}
				}
			} else if (mPossibleMoves.contains(pos)) {
				for (int i : mPossibleMoves) {
					ItemFrame possibleFrame = mFrames.get(i);
					if (mType == InterfaceType.WHITEPLAYER) {
						i = 63 - i;
					}
					ChessPiece piece = mBoard.getChessPiece(i);
					boolean white = ((i / 8) % 2 == 0 && i % 2 == 0) || ((i / 8) % 2 == 1 && i % 2 == 1);
					if (piece == null) {
						possibleFrame.setItem(MAP_BOARD.get(white ? 0 : 1), false);
					} else {
						possibleFrame.setItem(MAP_PIECES.get(piece.getPieceType()).get(piece.getPieceTeam()).get(white ? 0 : 1), false);
					}
				}
				if (mType == InterfaceType.WHITEPLAYER) {
					pos = 63 - pos;
				}
				if (!mBoard.movePiece(mPieceSelected, pos)) {
					p.sendMessage(Component.text("Illegal move, Try again!", TextColor.fromCSSHexString("#FF0000")));
				}
				mPossibleMoves = null;
				updateInteractable();
			} else {
				for (int i : mPossibleMoves) {
					ItemFrame possibleFrame = mFrames.get(i);

					if (mType == InterfaceType.WHITEPLAYER) {
						i = 63 - i;
					}
					ChessPiece piece = mBoard.getChessPiece(i);
					boolean white = ((i / 8) % 2 == 0 && i % 2 == 0) || ((i / 8) % 2 == 1 && i % 2 == 1);
					if (piece == null) {
						possibleFrame.setItem(MAP_BOARD.get(white ? 0 : 1), false);
					} else {
						possibleFrame.setItem(MAP_PIECES.get(piece.getPieceType()).get(piece.getPieceTeam()).get(white ? 0 : 1), false);
					}
				}
				mPossibleMoves = null;
			}
		}
		return true;
	}


	public boolean shouldDestroy() {
		for (ItemFrame frame : mFramePosMap.keySet()) {
			if (frame.isDead()) {
				return true;
			}
		}
		return false;
	}

	public void destroy() {
		//no need to care about frames since they will be removed if the chunk get unloaded.
		mFramePosMap.clear();
		mFrames.clear();

		if (mParticleRunnable != null) {
			mParticleRunnable.cancel();
		}
	}

	public void removeFrames() {
		for (ItemFrame frame : mFramePosMap.keySet()) {
			if (!frame.isDead()) {
				frame.remove();
			}
		}
		mFramePosMap.clear();
		mFrames.clear();

		if (mParticleRunnable != null) {
			mParticleRunnable.cancel();
		}
	}

	public void removeRunnable() {
		if (mParticleRunnable != null) {
			mParticleRunnable.cancel();
		}
	}

	public void refresh() {
		for (int i = 0; i < 64; i++) {
			ChessPiece piece = mBoard.getChessPiece(i);
			ItemFrame possibleFrame = mFrames.get(i);
			if (mType == InterfaceType.WHITEPLAYER) {
				possibleFrame = mFrames.get(63 - i);
			}
			boolean white = ((i / 8) % 2 == 0 && i % 2 == 0) || ((i / 8) % 2 == 1 && i % 2 == 1);
			if (piece != null) {
				possibleFrame.setItem(MAP_PIECES.get(piece.getPieceType()).get(piece.getPieceTeam()).get(white ? 0 : 1), false);
			} else {
				possibleFrame.setItem(MAP_BOARD.get(white ? 0 : 1), false);
			}
		}
		updateInteractable();
	}

	public void updateSlot(int slot) {
		ChessPiece piece = mBoard.getChessPiece(slot);
		if (mType == InterfaceType.WHITEPLAYER) {
			slot = 63 - slot;
		}
		ItemFrame possibleFrame = mFrames.get(slot);
		boolean white = ((slot / 8) % 2 == 0 && slot % 2 == 0) || ((slot / 8) % 2 == 1 && slot % 2 == 1);
		if (piece != null) {
			possibleFrame.setItem(MAP_PIECES.get(piece.getPieceType()).get(piece.getPieceTeam()).get(white ? 0 : 1), false);
		} else {
			possibleFrame.setItem(MAP_BOARD.get(white ? 0 : 1), false);
		}
	}

	public void updateInteractable() {
		mPlayerTurn = (mType == InterfaceType.BLACKPLAYER && mBoard.isBlackTurn()) || (mType == InterfaceType.WHITEPLAYER && mBoard.isWhiteTurn());
	}

	public void unload() {
		for (ItemFrame frame : mFramePosMap.keySet()) {
			try {
				frame.remove();
			} catch (Exception e) {
				Plugin.getInstance().getLogger().warning("[Chess Interface] Error during unload. Can't remove frame!");
			}
		}
		mFramePosMap.clear();
		mFrames.clear();
	}
}
