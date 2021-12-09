package com.playmonumenta.plugins.minigames.chess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.BoardState;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPiece;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPieceType;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessTeam;
import com.playmonumenta.plugins.minigames.chess.ChessInterface.InterfaceType;
import com.playmonumenta.plugins.minigames.chess.events.ChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.EndGameChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.MovePieceChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.PromotingChessEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.BoardState;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPiece;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPieceType;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessTeam;
import com.playmonumenta.plugins.minigames.chess.ChessInterface.InterfaceType;
import com.playmonumenta.plugins.minigames.chess.events.ChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.EndGameChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.MovePieceChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.PromotingChessEvent;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ChessManager implements Listener {

	public static final String FEN_DEFAULT_BOARD_STRING = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

	private static final String[] CHESSBOARD_COMMANDS = {"chessboard", "chess", "cb"};

	public enum ChessBoardType {
		PVP,
		PVAI;
	}

	private static @Nullable Plugin mPlugin;
	private static final Map<String, ChessBoard> mBoards = new HashMap<>();
	private static final Map<ChessBoard, List<ChessInterface>> mBoardsInterfaces = new HashMap<>();

	private static final Boolean DEBUG = false;

	public ChessManager(Plugin plugin) {
		mPlugin = plugin;

		//Register commands.
		CommandPermission perms = CommandPermission.fromString("monumenta.command.chess");
		List<Argument> arguments = new ArrayList<>();

		for (String command : CHESSBOARD_COMMANDS) {

			arguments.clear();
			arguments.add(new StringArgument("Board Name"));
			arguments.add(new MultiLiteralArgument("create"));
			arguments.add(new MultiLiteralArgument(
						ChessBoardType.PVP.name(),
						ChessBoardType.PVAI.name()));

			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					createBoard((String)args[0], ChessBoardType.valueOf((String) args[2]), FEN_DEFAULT_BOARD_STRING);
				}).register();

			arguments.add(new GreedyStringArgument("fen String"));
			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					createBoard((String)args[0], ChessBoardType.valueOf((String) args[2]), (String) args[3]);
				}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions((info) -> {
				return mBoards.keySet().toArray(new String[0]);
			}));
			arguments.add(new MultiLiteralArgument("get"));
			arguments.add(new MultiLiteralArgument("fen"));
			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					printCurrentChessBoardFenString(sender, (String)args[0]);
				}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions((info) -> {
				return mBoards.keySet().toArray(new String[0]);
			}));
			arguments.add(new MultiLiteralArgument("restart"));
			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					restartBoard((String) args[0]);
				}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions((info) -> {
				return mBoards.keySet().toArray(new String[0]);
			}));
			arguments.add(new MultiLiteralArgument("set"));
			arguments.add(new MultiLiteralArgument("fen"));
			arguments.add(new GreedyStringArgument("fen String"));
			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					if (!mBoards.containsKey((String)args[0])) {
						CommandAPI.fail("Invalid name, Board: " + (String)args[0] + " doesn't exists");
					}
					ChessBoard board = mBoards.get((String)args[0]);
					board.buildBoardFromString((String)args[3]);

					for (ChessInterface chessInterface : mBoardsInterfaces.get(board)) {
						chessInterface.refresh();
					}
				}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions((info) -> {
				return mBoards.keySet().toArray(new String[0]);
			}));
			arguments.add(new MultiLiteralArgument("delete"));
			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					deleteBoard((String)args[0]);
				}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions((info) -> {
				return mBoards.keySet().toArray(new String[0]);
			}));
			arguments.add(new MultiLiteralArgument("refresh"));
			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					refreshGuis((String)args[0]);
				}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions((info) -> {
				return mBoards.keySet().toArray(new String[0]);
			}));
			arguments.add(new MultiLiteralArgument("set"));
			arguments.add(new MultiLiteralArgument("piece"));
			arguments.add(new MultiLiteralArgument(ChessPieceType.BISHOPS.name(),
												ChessPieceType.KING.name(),
												ChessPieceType.KNIGHTS.name(),
												ChessPieceType.PAWNS.name(),
												ChessPieceType.QUEEN.name(),
												ChessPieceType.ROOKS.name()));

			arguments.add(new MultiLiteralArgument(ChessTeam.BLACK.name(),
													ChessTeam.WHITE.name()));
			arguments.add(new IntegerArgument("Position", 0, 63));

			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					setPiece((String)args[0], (String)args[3], (String)args[4], (int)args[5]);
				}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions((info) -> {
				return mBoards.keySet().toArray(new String[0]);
			}));
			arguments.add(new MultiLiteralArgument("set"));
			arguments.add(new MultiLiteralArgument("player"));
			arguments.add(new MultiLiteralArgument("white"));
			arguments.add(new PlayerArgument("player"));
			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					setPlayer((String)args[0], (Player)args[4], "white");
				}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions((info) -> {
				return mBoards.keySet().toArray(new String[0]);
			}));
			arguments.add(new MultiLiteralArgument("set"));
			arguments.add(new MultiLiteralArgument("player"));
			arguments.add(new MultiLiteralArgument("black"));
			arguments.add(new PlayerArgument("player"));
			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					setPlayer((String)args[0], (Player)args[4], "black");
				}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions((info) -> {
				return mBoards.keySet().toArray(new String[0]);
			}));
			arguments.add(new MultiLiteralArgument("set"));
			arguments.add(new MultiLiteralArgument("gui"));
			arguments.add(new MultiLiteralArgument(ChessInterface.InterfaceType.WHITEPLAYER.name().toLowerCase(),
												ChessInterface.InterfaceType.BLACKPLAYER.name().toLowerCase(),
												ChessInterface.InterfaceType.SPECTATOR.name().toLowerCase()));
			arguments.add(new LocationArgument("starting positions", LocationType.BLOCK_POSITION));
			arguments.add(new MultiLiteralArgument(ChessInterface.FacingPosition.NORTH.getLabel(),
													ChessInterface.FacingPosition.SOUTH.getLabel()));
			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					createGui((String)args[0], (String)args[3], (Location)args[4], (String) args[5]);
				}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions((info) -> {
				return mBoards.keySet().toArray(new String[0]);
			}));
			arguments.add(new MultiLiteralArgument("surrend"));
			arguments.add(new PlayerArgument("Surrender"));
			new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(arguments)
				.executes((sender, args) -> {
					surrender((String)args[0], (Player)args[2]);
				}).register();
		}
	}

	public static void printCurrentChessBoardFenString(CommandSender sender, String boardname) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(boardname)) {
			CommandAPI.fail("Invalid name, Board: " + boardname + " doesn't exists");
		}
		ChessBoard board = mBoards.get(boardname);
		String fenString = board.convertBoardToFenString();

		sender.sendMessage(Component.text("Chessboard: ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(Component.text(boardname, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false)));
		sender.sendMessage(Component.text("FEN: ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(Component.text(fenString, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false)).clickEvent(ClickEvent.copyToClipboard(fenString)));
	}

	public static void createGui(String name, String type, Location startingLoc, String facing) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			CommandAPI.fail("Invalid name, Board: " + name + " doesn't exists");
		}
		ChessBoard board = mBoards.get(name);
		final ChessInterface newChessInterface = new ChessInterface(board, InterfaceType.valueOf(type.toUpperCase()));
		for (ChessInterface chessInterface : new HashSet<>(mBoardsInterfaces.get(board))) {
			if (chessInterface.mType == newChessInterface.mType) {
				chessInterface.removeFrames();
				mBoardsInterfaces.get(board).remove(chessInterface);
			}
		}
		//annoing stuff.... we need to delay the placing of frames so minecraft don't fucked up.
		new BukkitRunnable() {
			@Override
			public void run() {
				newChessInterface.buildBoard(startingLoc, ChessInterface.FacingPosition.valueOfLabel(facing));
				mBoardsInterfaces.get(board).add(newChessInterface);
			}
		}.runTaskLater(mPlugin, 1);
	}

	public static void createBoard(String name, ChessBoardType type, String fenString) throws WrapperCommandSyntaxException {
		if (mBoards.containsKey(name)) {
			CommandAPI.fail("Invalid name, name " + name + " already used for a different board");
		}

		ChessBoard board = new ChessBoard(name, type);
		board.buildBoardFromString(fenString);
		mBoards.put(name, board);
		mBoardsInterfaces.put(board, new ArrayList<>());
	}

	public static void deleteBoard(String name) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			CommandAPI.fail("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessBoard board = mBoards.get(name);
		List<ChessInterface> chessInterfaces = mBoardsInterfaces.get(board);
		if (chessInterfaces != null) {
			for (ChessInterface ci: chessInterfaces) {
				ci.destroy();
			}
			chessInterfaces.clear();
		}
		mBoardsInterfaces.remove(board);
	}

	public static void restartBoard(String name) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			CommandAPI.fail("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessBoard board = mBoards.get(name);
		board.restart();
		List<ChessInterface> chessInterfaces = mBoardsInterfaces.get(board);
		for (ChessInterface ci: chessInterfaces) {
			ci.removeRunnable();
			ci.refresh();
		}
	}

	public static void surrender(String name, Player loser) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			CommandAPI.fail("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessBoard board = mBoards.get(name);
		board.updateState(BoardState.ENDED);
		ChessPlayer whitePlayer = board.getPlayer(ChessTeam.WHITE);
		ChessPlayer blackPlayer = board.getPlayer(ChessTeam.BLACK);

		EndGameChessEvent event = new EndGameChessEvent(board, whitePlayer, blackPlayer);


		if (whitePlayer.mPlayer.equals(loser)) {
			event.setEndGameScore(1);
		} else if (blackPlayer.mPlayer.equals(loser)) {
			event.setEndGameScore(0);
		}

		Bukkit.getPluginManager().callEvent(event);
	}

	public static void refreshGuis(String name) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			CommandAPI.fail("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessBoard board = mBoards.get(name);
		for (ChessInterface ci: mBoardsInterfaces.get(board)) {
			ci.refresh();
		}
	}

	public static void setPlayer(String name, Player player, String role) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			CommandAPI.fail("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessBoard board = mBoards.get(name);
		board.setPlayer(player, ChessTeam.valueOf(role.toUpperCase()));
	}

	public static void setPiece(String name, String piece, String teamString, int pos) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			CommandAPI.fail("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessPieceType type = ChessPieceType.valueOf(piece.toUpperCase());
		ChessTeam team = ChessTeam.valueOf(teamString.toUpperCase());
		if (type == null || team == null || pos < 0 || pos > 63) {
			CommandAPI.fail("Error. type: " + type + " team: " + team + " pos: " + pos);
		}

		ChessPiece newPiece = new ChessPiece(type, team, pos);
		mBoards.get(name).setPiece(newPiece, pos);
	}

	public static void updateGuiSlot(ChessBoard board, int slot) {
		for (ChessInterface ci : mBoardsInterfaces.get(board)) {
			ci.updateSlot(slot);
		}
	}

	public static void updateGuiInteractable(ChessBoard board) {
		for (ChessInterface cInterface : mBoardsInterfaces.get(board)) {
			cInterface.updateInteractable();
		}
	}

	public static void animationWin(Player player) {
		Location loc = player.getLocation();

		new BukkitRunnable() {
			int mTimer = 0;
			@Override
			public void run() {
				if (mTimer >= 15) {
					cancel();
					return;
				}

				Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK);
				FireworkMeta fwm = fw.getFireworkMeta();
				FireworkEffect.Builder fwBuilder = FireworkEffect.builder();
				fwBuilder.withColor(Color.LIME, Color.YELLOW, Color.ORANGE, Color.AQUA);
				fwBuilder.with(FireworkEffect.Type.BURST);
				FireworkEffect fwEffect = fwBuilder.build();
				fwm.addEffect(fwEffect);
				fw.setFireworkMeta(fwm);
				fw.detonate();

				mTimer += 5;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 5);

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerClicks(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (player != null) {
			if (ChessPlayer.isChessPlayer(player) && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
				Entity entity = player.getTargetEntity(20);
				if (entity instanceof ItemFrame) {
					ChessBoard board = null;
					Set<String> tags = entity.getScoreboardTags();
					for (String boardName : mBoards.keySet()) {
						if (tags.contains(boardName)) {
							board = mBoards.get(boardName);
							break;
						}
					}

					if (board == null) {
						return;
					}

					for (ChessInterface chessInterface : mBoardsInterfaces.get(board)) {
						if (chessInterface.playerInteract((ItemFrame)entity, (Player)player)) {
							break;
						}
					}
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public static void onChessEvent(ChessEvent event) {
		final ChessBoard board = event.getBoard();
		if (board != null) {
			String msg = "[ChessManager] On board: " + board.getName();
			if (event instanceof MovePieceChessEvent) {

				int from = ((MovePieceChessEvent) event).getOldPos();
				int to = ((MovePieceChessEvent) event).getNewPos();

				msg += " | MovePieceEvent (" + board.getChessPiece(to).toString() + ")";

				for (ChessInterface cInterface : mBoardsInterfaces.get(board)) {
					cInterface.moveEvent(from, to);
				}

				ChessTeam teamPlaying = board.getChessPiece(to).getPieceTeam();
				ChessPlayer nextPlayer = teamPlaying == ChessTeam.WHITE ? event.getBlackPlayer() : event.getWhitePlayer();

				if (nextPlayer != null && nextPlayer.mPlayer != null) {
					nextPlayer.mPlayer.playSound(nextPlayer.mPlayer.getLocation(), Sound.ENTITY_ARMOR_STAND_HIT, SoundCategory.MASTER, 10f, 0.6f);
				}

				board.updateState(teamPlaying == ChessTeam.WHITE ? BoardState.BLACK_TURN : BoardState.WHITE_TURN);

				for (ChessInterface cInterface : mBoardsInterfaces.get(board)) {
					cInterface.updateInteractable();
				}

				if (DEBUG) {
					Plugin.getInstance().getLogger().warning(msg);
				}

			} else if (event instanceof EndGameChessEvent) {
				ChessPlayer winnerPlayer = ((EndGameChessEvent) event).getWinner();
				ChessPlayer loserPlayer = ((EndGameChessEvent) event).getLoser();
				float result = ((EndGameChessEvent) event).getEndGameScore();

				msg += " | EndGameEvent Players-> W: " + (winnerPlayer != null ? winnerPlayer.mPlayer.getName() : "null") + " L: " + (loserPlayer != null ? loserPlayer.mPlayer.getName() : "null") + " Result: " + result;

				if (DEBUG) {
					Plugin.getInstance().getLogger().warning(msg);
				}

				if (winnerPlayer != null && winnerPlayer.mPlayer != null) {
					animationWin(winnerPlayer.mPlayer);
				}

				ChessPlayer.removeChessPlayer(winnerPlayer);
				ChessPlayer.removeChessPlayer(loserPlayer);

			} else if (event instanceof PromotingChessEvent) {
				ChessPlayer promotingPlayer = ((PromotingChessEvent)event).getPlayer();
				ChessPiece promotingPiece = ((PromotingChessEvent)event).getPiece();
				if (promotingPlayer != null && promotingPlayer.mPlayer != null) {
					new ChessPromotingCustomInventory(promotingPlayer.mPlayer, board, promotingPiece).openInventory(promotingPlayer.mPlayer, mPlugin);
					msg += " | PromotingEvent Player: " + promotingPlayer.mPlayer.getName() + " (" + promotingPiece + ")";

				} else {
					board.changePieceType(promotingPiece, ChessPieceType.QUEEN);
				}


				if (DEBUG) {
					Plugin.getInstance().getLogger().warning(msg);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public static void onChunkUnload(ChunkUnloadEvent event) {
		boolean shouldCheckInterfaces = false;
		for (Entity entity : event.getChunk().getEntities()) {
			if ((entity instanceof ItemFrame) && entity.getScoreboardTags().contains(ChessInterface.ITEM_FRAME_TAG)) {
				entity.remove();
				shouldCheckInterfaces = true;
			}
		}

		if (shouldCheckInterfaces) {
			for (ChessBoard board : mBoards.values()) {
				for (ChessInterface chessInterface : new HashSet<ChessInterface>(mBoardsInterfaces.get(board))) {
					if (chessInterface.shouldDestroy()) {
						chessInterface.destroy();
						mBoardsInterfaces.get(board).remove(chessInterface);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public static void onPlayerQuit(PlayerQuitEvent event) {
		//this is only to be sure that we are not gonna have infinite chessplayer
		Player player = event.getPlayer();
		ChessPlayer.removeChessPlayer(player);
	}

	public void unloadAll() {
		for (ChessBoard board : mBoardsInterfaces.keySet()) {
			for (ChessInterface chessInterface : mBoardsInterfaces.get(board)) {
				chessInterface.unload();
			}
		}
		mBoards.clear();
		mBoardsInterfaces.clear();
	}
}
