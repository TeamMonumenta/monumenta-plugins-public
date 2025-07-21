package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.HangingSign;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SignOverride extends BaseOverride {
	public static final String COPIED_SIGN_HEADER = "Sign contents:";
	public static final String COPIED_SIGN_DIVIDER = "========";
	public static final String SIGN_IS_GLOWING = "Sign is glowing";

	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || player.getGameMode().equals(GameMode.CREATIVE)) {
			return true;
		}

		List<String> loreLines = ItemUtils.getPlainLoreIfExists(item);
		if (loreLines.isEmpty()) {
			return true;
		}

		// If possible, copy the contents of a sign item to the block
		if (COPIED_SIGN_HEADER.equals(loreLines.get(0))) {
			if (item.hasItemMeta()) {
				ItemMeta meta = item.getItemMeta();
				if (meta instanceof BlockStateMeta blockStateMeta) {
					BlockState blockState = blockStateMeta.getBlockState();
					if (blockState instanceof Sign signItem) {
						Block placedBlock = event.getBlockPlaced();
						final Location loc = event.getBlock().getLocation();
						final Material blockType = placedBlock.getType();
						final BlockData originalBlockData = placedBlock.getBlockData();
						final Sign finalSignItem = signItem;

						List<Component> chatFilterLines = new ArrayList<>();
						for (Side side : Side.values()) {
							chatFilterLines.addAll(signItem.getSide(side).lines());
						}

						Component allSignLines = MessagingUtils.concatenateComponents(chatFilterLines, Component.newline());
						if (MonumentaNetworkChatIntegration.hasBadWord(player, allSignLines)) {
							AuditListener.logSevere(player.getName()
								+ " attempted to place a sign with a bad word: `/s "
								+ ServerProperties.getShardName()
								+ "` `/world " + loc.getWorld().getName()
								+ "` `/tp @s " + loc.getBlockX()
								+ " " + loc.getBlockY()
								+ " " + loc.getBlockZ()
								+ "`"
							);
							item.setAmount(0);
							return false;
						}

						new BukkitRunnable() {
							@Override
							public void run() {
								boolean shopsPossible = ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.SHOPS_POSSIBLE);
								Block signBlock = loc.getBlock();
								signBlock.setType(blockType);
								signBlock.setBlockData(originalBlockData, true);
								Sign sign = (Sign) signBlock.getState();

								for (Side side : Side.values()) {
									SignSide signSideItem = finalSignItem.getSide(side);
									SignSide signSideBlock = sign.getSide(side);

									List<Component> signItemLines = signSideItem.lines();
									for (int lineNum = 0; lineNum < signItemLines.size(); lineNum++) {
										signSideBlock.line(lineNum, signItemLines.get(lineNum));
									}
									signSideBlock.setColor(signSideItem.getColor());
									signSideBlock.setGlowingText(signSideItem.isGlowingText() && !shopsPossible);
								}
								sign.update();
								loc.getWorld().playSound(loc, Sound.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
								CoreProtectIntegration.logPlacement(player, signBlock);
							}
						}.runTask(plugin);
						item.subtract();
						return false;
					}
				}
			}
			return true;
		}

		return false;
	}

	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		Material type = item == null ? null : item.getType();
		if (type == Material.GLOW_INK_SAC && ZoneUtils.hasZoneProperty(block.getLocation(), ZoneUtils.ZoneProperty.SHOPS_POSSIBLE)) {
			return false;
		}

		Sign sign = (Sign) block.getState();
		SignSide facingSide = sign.getSide(sign.getInteractableSideFor(player));
		boolean usingItem = item != null
			&& item.hasItemMeta()
			&& item.getItemMeta().hasLore()
			&& (
			ItemUtils.isDye(type)
				|| (type == Material.GLOW_INK_SAC && !facingSide.isGlowingText())
				|| (type == Material.INK_SAC && facingSide.isGlowingText())
				|| (type == Material.HONEYCOMB && !sign.isWaxed())
				|| (ItemUtils.isAxe(item) && sign.isWaxed())
		);
		boolean output = ItemUtils.isNullOrAir(item) || !usingItem;

		// Compile all the lines of text together and make sure it is not a leaderboard that is being clicked
		if (item == null || !ItemUtils.isSign(item.getType())) {
			DyeColor dyeColor = facingSide.getColor();
			Color color = dyeColor == null ? DyeColor.BLACK.getColor() : dyeColor.getColor();
			float r = color.getRed() / 255.0f;
			float g = color.getGreen() / 255.0f;
			float b = color.getBlue() / 255.0f;
			TextColor textColor = TextColor.color(
				0.2f + 0.8f * r,
				0.2f + 0.8f * g,
				0.2f + 0.8f * b
			);

			boolean hasText = false;
			List<Component> displayedLines = new ArrayList<>();
			for (Component component : facingSide.lines()) {
				if (component.clickEvent() != null) {
					return output;
				}
				String line = MessagingUtils.PLAIN_SERIALIZER.serialize(component).trim();
				if (line.matches("^[-=+~]*$")) {
					// When dumping signs to chat, skip decoration lines
					continue;
				}
				line = line.replaceAll("[${}]", "");
				Component part;
				if (component.hasDecoration(TextDecoration.OBFUSCATED)) {
					part = Component.text("no spoiler for you")
						.decoration(TextDecoration.OBFUSCATED, true);
				} else {
					part = Component.text(line);
				}
				hasText = true;
				displayedLines.add(part);
			}

			if (hasText) {
				player.sendMessage(Component.text("", textColor)
					.append(Component.text("[Sign] ", NamedTextColor.BLUE))
					.append(MessagingUtils.concatenateComponents(displayedLines, Component.space())));
			}
		}

		if (player.isSneaking()) {
			return output;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			// If clicking a sign with another sign, copy the contents to the item
			if (!meta.hasDisplayName()) {
				if (ItemUtils.isSign(item.getType()) &&
					meta instanceof BlockStateMeta blockStateMeta) {
					BlockState blockState = blockStateMeta.getBlockState();
					if (blockState instanceof Sign signItem) {
						boolean blockIsHangingSign = sign instanceof HangingSign;
						boolean itemIsHangingSign = signItem instanceof HangingSign;

						if (itemIsHangingSign && !blockIsHangingSign) {
							// Don't allow copying text from a normal sign to a hanging sign;
							// the width limit is smaller on hanging signs
							// This can be updated if someone wants to check the width of the sign contents, but
							// that depends on the pixel width of each character, and I don't know the limits
							player.sendMessage(Component.text("You cannot copy a sign to a hanging sign; there might not be enough room.", NamedTextColor.RED));
							return output;
						}

						List<Component> loreLines = new ArrayList<>();
						loreLines.add(Component.text(COPIED_SIGN_HEADER, NamedTextColor.GOLD)
							.decoration(TextDecoration.ITALIC, false));

						List<Component> chatFilterLines = new ArrayList<>();
						for (Side side : Side.values()) {
							SignSide signSideBlock = sign.getSide(side);
							SignSide signSideItem = signItem.getSide(side);
							List<Component> tempLoreLines = new ArrayList<>();

							DyeColor dyeColor = signSideBlock.getColor();

							signSideItem.setColor(dyeColor);
							Color color = dyeColor == null ? DyeColor.BLACK.getColor() : dyeColor.getColor();
							float r = color.getRed() / 255.0f;
							float g = color.getGreen() / 255.0f;
							float b = color.getBlue() / 255.0f;
							Component loreHeader = Component.text("> ", TextColor.color(r, g, b))
								.decoration(TextDecoration.ITALIC, false);
							Component loreBase = Component.text("", TextColor.color(
								0.2f + 0.8f * r,
								0.2f + 0.8f * g,
								0.2f + 0.8f * b
							));

							List<Component> signLines = signSideBlock.lines();
							boolean sideHasText = false;
							for (int lineNum = 0; lineNum < signLines.size(); ++lineNum) {
								Component line = signLines.get(lineNum);
								chatFilterLines.add(line);
								signSideItem.line(lineNum, line);
								Component loreLine = loreHeader.append(loreBase.append(line));
								tempLoreLines.add(loreLine);
								if(!MessagingUtils.plainText(line).isBlank()) {
									sideHasText = true;
								}
							}

							boolean sideGlowing = signSideBlock.isGlowingText();
							signSideItem.setGlowingText(sideGlowing);
							if (sideGlowing) {
								tempLoreLines.add(Component.text(SIGN_IS_GLOWING, NamedTextColor.WHITE));
							}

							tempLoreLines.add(Component.text(COPIED_SIGN_DIVIDER, NamedTextColor.GOLD));

							if(side != Side.BACK || sideHasText) {
								// back of sign shouldn't show in lore if there's no text on it
								loreLines.addAll(tempLoreLines);
							}
						}
						// Remove last divider
						loreLines.remove(loreLines.size() - 1);

						Component allSignLines = MessagingUtils.concatenateComponents(chatFilterLines, Component.newline());
						if (MonumentaNetworkChatIntegration.hasBadWord(player, allSignLines)) {
							Location loc = block.getLocation();
							AuditListener.logSevere(player.getName()
								+ " attempted to copy a sign with a bad word: `/s "
								+ ServerProperties.getShardName()
								+ "` `/world " + loc.getWorld().getName()
								+ "` `/tp @s " + loc.getBlockX()
								+ " " + loc.getBlockY()
								+ " " + loc.getBlockZ()
								+ "`"
							);
							item.setAmount(0);
							return false;
						}

						blockStateMeta.setBlockState(blockState);
						blockStateMeta.lore(loreLines);
						item.setItemMeta(blockStateMeta);
						ItemUtils.setPlainLore(item);
						player.sendMessage(Component.text("Copied sign data."));
						return false;
					}
				}
			}
		}

		return output;
	}
}
