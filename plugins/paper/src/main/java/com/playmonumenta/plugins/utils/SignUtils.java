package com.playmonumenta.plugins.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;

public final class SignUtils {

	private static final int ACTION_INDEX = 9;
	private static final int SIGN_LINES = 4;

	private static final String NBT_FORMAT = "{\"text\":\"%s\"}";
	private static final String NBT_BLOCK_ID = "minecraft:oak_sign";

	private final Plugin mPlugin;

	private final Map<Player, Menu> mInputs;
	private static SignUtils INSTANCE = null;

	public SignUtils(Plugin plugin) {
		INSTANCE = this;
		this.mPlugin = plugin;
		this.mInputs = new HashMap<>();
		this.listen();
	}

	public static @Nullable Menu newMenu(List<String> text) {
		if (INSTANCE == null) {
			return null;
		}
		return INSTANCE.newMenuInternal(text);
	}

	private Menu newMenuInternal(List<String> text) {
		return new Menu(text);
	}

	private void listen() {
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this.mPlugin, PacketType.Play.Client.UPDATE_SIGN) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				Player player = event.getPlayer();

				Menu menu = mInputs.remove(player);

				if (menu == null) {
					return;
				}
				event.setCancelled(true);

				boolean success = menu.mResponse.test(player, event.getPacket().getStringArrays().read(0));

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
						if (player.isOnline()) {
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

		private @Nullable BiPredicate<Player, String[]> mResponse;
		private boolean mReopenIfFail;

		private @Nullable BlockPosition mPosition;

		private boolean mForceClose;

		Menu(List<String> text) {
			this.mText = text;
		}

		public Menu reopenIfFail(boolean value) {
			this.mReopenIfFail = value;
			return this;
		}

		public Menu response(BiPredicate<Player, String[]> response) {
			this.mResponse = response;
			return this;
		}

		public void open(Player player) {
			Objects.requireNonNull(player, "player");
			if (!player.isOnline()) {
				return;
			}
			Location location = player.getLocation();
			this.mPosition = new BlockPosition(location.getBlockX(), location.getBlockY() + (255 - location.getBlockY()), location.getBlockZ());

			player.sendBlockChange(this.mPosition.toLocation(location.getWorld()), Material.OAK_SIGN.createBlockData());

			PacketContainer openSign = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
			PacketContainer signData = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);

			openSign.getBlockPositionModifier().write(0, this.mPosition);

			NbtCompound signNBT = (NbtCompound) signData.getNbtModifier().read(0);

			for (int line = 0; line < SIGN_LINES; line++) {
				signNBT.put("Text" + (line + 1), this.mText.size() > line ? String.format(NBT_FORMAT, color(this.mText.get(line))) : "");
			}

			signNBT.put("x", this.mPosition.getX());
			signNBT.put("y", this.mPosition.getY());
			signNBT.put("z", this.mPosition.getZ());
			signNBT.put("id", NBT_BLOCK_ID);

			signData.getBlockPositionModifier().write(0, this.mPosition);
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
		 * @param force decides whether or not it will reopen if reopen is enabled
		 */
		public void close(Player player, boolean force) {
			this.mForceClose = force;
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
