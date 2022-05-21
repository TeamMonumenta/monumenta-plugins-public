package com.playmonumenta.plugins.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.google.gson.JsonElement;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class SignUtils {

	private static final int ACTION_INDEX = 9;
	private static final int SIGN_LINES = 4;

	private static final String NBT_BLOCK_ID = "minecraft:oak_sign";

	private final Plugin mPlugin;

	private final Map<Player, Menu> mInputs;
	private static @Nullable SignUtils INSTANCE = null;

	public SignUtils(Plugin plugin) {
		INSTANCE = this;
		mPlugin = plugin;
		mInputs = new HashMap<>();
		listen();
	}

	public static @Nullable Menu newMenu(List<String> text) {
		return newMenu(text, true);
	}

	public static @Nullable Menu newMenu(List<String> text, boolean allowColor) {
		if (INSTANCE == null) {
			return null;
		}
		return INSTANCE.newMenuInternal(text, allowColor);
	}

	private Menu newMenuInternal(List<String> text, boolean allowColor) {
		return new Menu(text, allowColor);
	}

	public static void edit(Block signBlock, Player player, boolean allowColor) {
		if (INSTANCE == null) {
			return;
		}
		INSTANCE.editInternal(signBlock, player, allowColor);
	}

	private void editInternal(Block signBlock, Player player, boolean allowColor) {
		new Menu(signBlock, player, allowColor);
	}

	private void listen() {
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(mPlugin, PacketType.Play.Client.UPDATE_SIGN) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				Player player = event.getPlayer();

				Menu menu = mInputs.remove(player);

				if (menu == null) {
					return;
				}
				event.setCancelled(true);

				boolean success = menu.mResponse != null && menu.mResponse.test(player, event.getPacket().getStringArrays().read(0));

				if (!success && menu.mReopenIfFail && !menu.mForceClose) {
					new BukkitRunnable() {
						@Override
						public void run() {
							menu.open(player);
						}
					}.runTaskLater(plugin, 2);
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						if (player.isOnline() && menu.mPosition != null) {
							Location location = menu.mPosition.toLocation(player.getWorld());
							player.sendBlockChange(location, location.getBlock().getBlockData());
				        }
					}
				}.runTaskLater(plugin, 2);
			}
		});
	}

	public final class Menu {

		private final List<String> mText;
		private final boolean mAllowColor;

		private @Nullable BiPredicate<Player, String[]> mResponse;
		private boolean mReopenIfFail = false;

		private @Nullable BlockPosition mPosition;

		private boolean mForceClose = false;

		Menu(List<String> text, boolean allowColor) {
			mText = text;
			mAllowColor = allowColor;
		}

		Menu(final Block signBlock, final Player player, final boolean allowColor) {
			List<String> text = new ArrayList<>(SIGN_LINES);
			BlockState blockState = signBlock.getState();
			if (!(blockState instanceof Sign sign)) {
				throw new RuntimeException("No sign block at requested location");
			} else {
				for (Component lineComponent : sign.lines()) {
					if (allowColor) {
						text.add(MessagingUtils.LEGACY_SERIALIZER.serialize(lineComponent));
					} else {
						text.add(MessagingUtils.PLAIN_SERIALIZER.serialize(lineComponent));
					}
				}
			}
			mText = text;
			mAllowColor = allowColor;
			reopenIfFail(false);
			response((respondingPlayer, updatedLines) -> {
				new BukkitRunnable() {
					@Override
					public void run() {
						BlockState updatedBlockState = signBlock.getState();
						if (!(updatedBlockState instanceof Sign updatedSign)) {
							return;
						} else {
							CoreProtectIntegration.logRemoval(player, signBlock);
							for (int lineNum = 0; lineNum < updatedLines.length; ++lineNum) {
								Component lineComponent;
								if (allowColor) {
									lineComponent = MessagingUtils.LEGACY_SERIALIZER.deserialize(updatedLines[lineNum]);
								} else {
									lineComponent = MessagingUtils.PLAIN_SERIALIZER.deserialize(updatedLines[lineNum]);
								}
								updatedSign.line(lineNum, lineComponent);
							}
							updatedSign.update();
							CoreProtectIntegration.logPlacement(player, signBlock);
						}
					}
				}.runTask(mPlugin);
				return true;
			});
			open(player, signBlock);
		}

		public Menu reopenIfFail(boolean value) {
			mReopenIfFail = value;
			return this;
		}

		public Menu response(BiPredicate<Player, String[]> response) {
			mResponse = response;
			return this;
		}

		public void open(Player player) {
			open(player, null);
		}

		public void open(Player player, @Nullable Block existingBlock) {
			Objects.requireNonNull(player, "player");
			if (!player.isOnline()) {
				return;
			}
			Material signMat;
			Location location;
			if (existingBlock != null) {
				signMat = existingBlock.getType();
				location = existingBlock.getLocation();
			} else {
				signMat = Material.OAK_SIGN;
				location = player.getLocation().clone();
				location.setY(255.0);
			}
			mPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());

			player.sendBlockChange(location, signMat.createBlockData());

			PacketContainer openSign = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
			PacketContainer signData = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);

			openSign.getBlockPositionModifier().write(0, mPosition);

			NbtCompound signNBT = (NbtCompound) signData.getNbtModifier().read(0);

			for (int lineNum = 0; lineNum < SIGN_LINES; lineNum++) {
				String lineLegacy = "";
				if (mText.size() > lineNum) {
					lineLegacy = mText.get(lineNum);
					if (mAllowColor) {
						lineLegacy = color(lineLegacy);
					}
				}
				Component lineComponent = Component.text(lineLegacy);
				JsonElement lineJson = MessagingUtils.toGson(lineComponent);
				signNBT.put("Text" + (lineNum + 1), lineJson.toString());
			}

			signNBT.put("x", mPosition.getX());
			signNBT.put("y", mPosition.getY());
			signNBT.put("z", mPosition.getZ());
			signNBT.put("id", NBT_BLOCK_ID);

			signData.getBlockPositionModifier().write(0, mPosition);
			signData.getIntegers().write(0, ACTION_INDEX);
			signData.getNbtModifier().write(0, signNBT);

			try {
				ProtocolLibrary.getProtocolManager().sendServerPacket(player, signData);
				ProtocolLibrary.getProtocolManager().sendServerPacket(player, openSign);
			} catch (InvocationTargetException exception) {
				exception.printStackTrace();
			}
			mInputs.put(player, this);
		}

		/**
		 * closes the menu. if force is true, the menu will close and will ignore the reopen
		 * functionality. false by default.
		 *
		 * @param player the player
		 * @param force decides whether it will reopen if reopen is enabled
		 */
		public void close(Player player, boolean force) {
			mForceClose = force;
			if (player.isOnline()) {
				player.closeInventory();
		    }
		}

		public void close(Player player) {
			close(player, false);
		}

		private String color(String input) {
			return ChatColor.translateAlternateColorCodes('&', input);
		}
	}
}
