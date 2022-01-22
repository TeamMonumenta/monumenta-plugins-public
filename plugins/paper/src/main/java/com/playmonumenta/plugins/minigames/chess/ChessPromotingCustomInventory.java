package com.playmonumenta.plugins.minigames.chess;

import com.playmonumenta.plugins.minigames.chess.ChessBoard.BoardState;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPiece;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPieceType;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessTeam;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ChessPromotingCustomInventory extends CustomInventory {

	private static final List<ItemStack> GUI_ITEM_WHITE = new ArrayList<>();
	private static final List<ItemStack> GUI_ITEM_BLACK = new ArrayList<>();

	static {
		ItemStack rocksW = new ItemStack(Material.WHITE_CONCRETE);
		ItemMeta rocksmetaW = rocksW.getItemMeta();
		rocksmetaW.displayName(Component.text("ROCK", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
		rocksW.setItemMeta(rocksmetaW);
		GUI_ITEM_WHITE.add(rocksW);

		ItemStack rocksB = new ItemStack(Material.BLACK_CONCRETE);
		ItemMeta rocksmetaB = rocksB.getItemMeta();
		rocksmetaB.displayName(Component.text("ROCK", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true));
		rocksB.setItemMeta(rocksmetaB);
		GUI_ITEM_BLACK.add(rocksB);

		ItemStack queenB = new ItemStack(Material.BLACK_CONCRETE);
		ItemMeta queenmetaB = queenB.getItemMeta();
		queenmetaB.displayName(Component.text("QUEEN", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true));
		queenB.setItemMeta(queenmetaB);
		GUI_ITEM_BLACK.add(queenB);

		ItemStack queenW = new ItemStack(Material.WHITE_CONCRETE);
		ItemMeta queenmetaW = queenW.getItemMeta();
		queenmetaW.displayName(Component.text("QUEEN", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
		queenW.setItemMeta(queenmetaW);
		GUI_ITEM_WHITE.add(queenW);

		ItemStack bishopB = new ItemStack(Material.BLACK_CONCRETE);
		ItemMeta bishopmetaB = bishopB.getItemMeta();
		bishopmetaB.displayName(Component.text("BISHOP", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true));
		bishopB.setItemMeta(bishopmetaB);
		GUI_ITEM_BLACK.add(bishopB);

		ItemStack bishopW = new ItemStack(Material.WHITE_CONCRETE);
		ItemMeta bishopmeta = bishopW.getItemMeta();
		bishopmeta.displayName(Component.text("BISHOP", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
		bishopW.setItemMeta(bishopmeta);
		GUI_ITEM_WHITE.add(bishopW);

		ItemStack knightW = new ItemStack(Material.WHITE_CONCRETE);
		ItemMeta knightmetaW = knightW.getItemMeta();
		knightmetaW.displayName(Component.text("KNIGHT", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
		knightW.setItemMeta(knightmetaW);
		GUI_ITEM_WHITE.add(knightW);

		ItemStack knightB = new ItemStack(Material.BLACK_CONCRETE);
		ItemMeta knightmetaB = knightB.getItemMeta();
		knightmetaB.displayName(Component.text("KNIGHT", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true));
		knightB.setItemMeta(knightmetaB);
		GUI_ITEM_BLACK.add(knightB);

	}

	private ChessBoard mBoard;
	private ChessPiece mPiece;
	private Boolean mPromoted;

	public ChessPromotingCustomInventory(Player owner, ChessBoard board, ChessPiece promotingPiece) {
		super(owner, 9, "Promotion : Click to choose");
		mPromoted = false;
		mBoard = board;
		mBoard.updateState(BoardState.WAIT_TURN);
		mPiece = promotingPiece;
		for (int i = 0; i < 4; i++) {
			mInventory.setItem(3 + i, mPiece.getPieceTeam() == ChessTeam.WHITE ? GUI_ITEM_WHITE.get(i) : GUI_ITEM_BLACK.get(i));
		}
	}

	protected void inventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
		event.setCancelled(true);

		if (event.getClickedInventory() != mInventory) {
			return;
		}

		int slotClicked = event.getSlot();

		switch (slotClicked) {
			case 3:
				mBoard.changePieceType(mPiece, ChessPieceType.ROOKS);
				mPromoted = true;
				event.getWhoClicked().closeInventory();
				break;
			case 4:
				mBoard.changePieceType(mPiece, ChessPieceType.QUEEN);
				mPromoted = true;
				event.getWhoClicked().closeInventory();
				break;
			case 5:
				mBoard.changePieceType(mPiece, ChessPieceType.BISHOPS);
				mPromoted = true;
				event.getWhoClicked().closeInventory();
				break;
			case 6:
				mBoard.changePieceType(mPiece, ChessPieceType.KNIGHTS);
				mPromoted = true;
				event.getWhoClicked().closeInventory();
				break;
			default:
				return;

		}
	}

	protected void inventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
		if (!mPromoted) {
			mBoard.changePieceType(mPiece, ChessPieceType.QUEEN);
		}

		mBoard.updateState(mPiece.getPieceTeam() == ChessTeam.WHITE ? BoardState.BLACK_TURN : BoardState.WHITE_TURN);

		ChessManager.updateGuiInteractable(mBoard);
	}
}
