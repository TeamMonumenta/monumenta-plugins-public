package com.playmonumenta.plugins.utils;

import com.bergerkiller.bukkit.common.resources.BlockStateType;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutTileEntityDataHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.google.gson.JsonElement;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class SignUtils {

	private static final int SIGN_LINES = 4;

	private static final String NBT_BLOCK_ID = "minecraft:oak_sign";

	private final Plugin mPlugin;

	private final ConcurrentHashMap<Player, Menu> mInputs;
	private static @Nullable SignUtils INSTANCE = null;

	public SignUtils(Plugin plugin) {
		INSTANCE = this;
		mPlugin = plugin;
		mInputs = new ConcurrentHashMap<>();
		listen();
	}

	public static Menu newMenu(List<String> text) {
		return newMenu(text, true);
	}

	public static Menu newMenu(List<String> text, boolean allowColor) {
		return Objects.requireNonNull(INSTANCE).newMenuInternal(text, allowColor);
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

				// This event handler is called asynchronously. Execute the rest of the code on the main thread via runTask()
				Bukkit.getScheduler().runTask(plugin, () -> {
					boolean success = menu.mResponse != null && menu.mResponse.test(player, event.getPacket().getStringArrays().read(0));

					if (!success && menu.mReopenIfFail && !menu.mForceClose) {
						menu.open(player);
					} else {
						if (player.isOnline() && menu.mPosition != null) {
							Location location = menu.mPosition.toLocation(player.getWorld());
							player.sendBlockChange(location, location.getBlock().getBlockData());
						}
					}
				});
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
				BlockState updatedBlockState = signBlock.getState();
				if (!(updatedBlockState instanceof Sign updatedSign)) {
					return true;
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
				return true;
			});
			open(player, signBlock);
		}

		public Menu reopenIfFail(boolean value) {
			mReopenIfFail = value;
			return this;
		}

		/**
		 * Define what happens when the user closes the sign GUI.
		 * If the passed function returns false and {@link #reopenIfFail(boolean)} is set to true, the GUI will be opened again.
		 */
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
			PacketPlayOutTileEntityDataHandle signPacket = PacketPlayOutTileEntityDataHandle.createHandle(signData.getHandle());

			openSign.getBlockPositionModifier().write(0, mPosition);

			NbtCompound signNBT = (NbtCompound) signData.getNbtModifier().read(0);
			if (signNBT == null) {
				signNBT = NbtFactory.ofCompound("sign");
			}

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
			signPacket.setType(BlockStateType.SIGN);
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
