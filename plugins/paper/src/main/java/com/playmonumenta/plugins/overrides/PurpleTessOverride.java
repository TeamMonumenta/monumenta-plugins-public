package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.listeners.PotionBarrelListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.EnumSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Nullable;

public class PurpleTessOverride extends BaseOverride {
	public static final String TESSERACT_NAME = "Tesseract of Emotions";
	public static final String TESSERACT_NAME_FESTIVE = "The Gift Wrapper";
	public static final String TESSERACT_NAME_UPGRADED = "Tesseract of Emotions (u)";

	private static final EnumSet<GameMode> DISALLOWED_GAMEMODES = EnumSet.of(GameMode.ADVENTURE, GameMode.SPECTATOR);
	private static final Set<Material> CHESTS = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);

	private final boolean mFestive;

	public PurpleTessOverride(boolean festive) {
		mFestive = festive;
	}

	@Override
	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (isTesseract(item, mFestive)) {
			return true;
		}

		if (!checkCanUse(player, item)) {
			return false;
		}

		handleClick(player, item, block);
		return false;
	}

	private static boolean isTesseract(ItemStack item, boolean festive) {
		return festive
			? !InventoryUtils.testForItemWithName(item, TESSERACT_NAME_FESTIVE, false)
			: !InventoryUtils.testForItemWithName(item, TESSERACT_NAME, false);
	}

	private static boolean isUpgraded(ItemStack item) {
		return InventoryUtils.testForItemWithName(item, TESSERACT_NAME_UPGRADED, false);
	}

	private static void replaceContents(Inventory to, Inventory from) {
		for (int i = 0; i < to.getSize(); i++) {
			to.setItem(i, from.getItem(i));
		}
	}

	public void handleClick(Player player, ItemStack item, @Nullable Block block) {
		if (block == null) {
			return;
		}

		boolean isUpgraded = isUpgraded(item);
		boolean isChest = block.getType() == Material.CHEST;
		boolean isBarrel = block.getType() == Material.BARREL;

		if (!isChest && !(isBarrel && isUpgraded)) {
			return; // exclude everything but Chests and (Barrels + using upgraded tess)
		}

		World world = block.getWorld();
		Location centerLoc = block.getLocation().add(0.5, 0.5, 0.5);

		Container originalContainer = (Container) block.getState();
		if (originalContainer.isLocked()) {
			return;
		}
		Inventory originalInventory = originalContainer instanceof Chest chest ? chest.getBlockInventory() : originalContainer.getInventory();

		if (player.isSneaking() && isUpgraded && !PotionBarrelListener.isPotionBarrel(block)) {
			world.playSound(centerLoc, Sound.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 0.6f, 1.4f, 1);

			Material material = isChest ? Material.BARREL : Material.CHEST;
			BlockData blockData = material.createBlockData(newData -> {
				if (newData instanceof Directional directional) {
					directional.setFacing(block.getBlockData() instanceof Directional oldData ? oldData.getFacing() : BlockFace.NORTH);
				}
			});

			block.setBlockData(blockData);
			if (block.getState() instanceof Container newContainer) {
				replaceContents(newContainer.getInventory(), originalInventory);
			}
		} else {
			boolean containerEmpty = true;
			for (@Nullable ItemStack itemStack : originalInventory.getContents()) {
				if (itemStack == null) {
					continue;
				}
				containerEmpty = false;

				Material type = itemStack.getType();
				if (ItemUtils.isShulkerBox(type)) {
					player.sendMessage(Component.text("You cannot compress a chest that has a shulker box in it!", NamedTextColor.RED));
					return;
				}
				if (CHESTS.contains(type)) {
					player.sendMessage(Component.text("You cannot compress a chest that has another chest in it!", NamedTextColor.RED));
					return;
				}
				if (ItemUtils.getPlainLore(itemStack).stream().anyMatch(s -> s.contains("Taking this item outside of the dungeon"))) {
					player.sendMessage(Component.text("You cannot compress a chest that has non-transferable items in it!", NamedTextColor.RED));
					return;
				}
			}
			if (containerEmpty) {
				player.sendMessage(Component.text("You cannot use this on a chest that is empty or has an unopened loot table.", NamedTextColor.RED));
				return;
			}

			ItemStack shulkerItem = new ItemStack(mFestive ? Material.GREEN_SHULKER_BOX : (isBarrel ? Material.YELLOW_SHULKER_BOX : Material.PURPLE_SHULKER_BOX));
			if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta blockStateMeta)) {
				return;
			}
			if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
				return;
			}

			replaceContents(shulkerBox.getInventory(), originalInventory);

			@Nullable
			Component name = originalContainer.customName();
			if (name != null) {
				shulkerBox.setLock(MessagingUtils.toGson(name).toString());
			}

			blockStateMeta.setBlockState(shulkerBox);
			shulkerItem.setItemMeta(blockStateMeta);

			// Visuals //
			String plainName = mFestive ? "Carrier of Festivity" : "Carrier of Emotion";
			ItemUtils.setDisplayName(shulkerItem, Component.text(plainName, mFestive ? NamedTextColor.DARK_RED : NamedTextColor.DARK_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			ItemUtils.setPlainName(shulkerItem, plainName);

			world.spawn(centerLoc, Item.class, itemEntity -> itemEntity.setItemStack(shulkerItem));

			world.playSound(centerLoc, Sound.ENTITY_SHULKER_TELEPORT, SoundCategory.BLOCKS, 0.4f, 1.9f, 1);
			new PartialParticle(Particle.BLOCK_CRACK, centerLoc)
				.data(Material.SHULKER_BOX.createBlockData())
				.delta(0.3, 1, 0.3)
				.extra(0.1)
				.count(50)
				.spawnFull();

			// Remove original block
			block.setBlockData(Material.AIR.createBlockData());
			CoreProtectIntegration.logRemoval(player, block);
		}

		block.getState().update(false, true);
	}

	public static boolean checkCanUse(Player player, ItemStack item) {
		if (item.getAmount() > 1) {
			player.sendMessage(Component.text("Cannot use stacked tesseracts!", NamedTextColor.RED));
			return false;
		}
		if (ScoreboardUtils.getScoreboardValue(player, "Quest114").orElse(0) < 30) {
			player.sendMessage(Component.text("You must finish Primeval Creations VI before you can use this tesseract.", NamedTextColor.RED));
			return false;
		}
		if (isUpgraded(item) && !AdvancementUtils.checkAdvancement(player, "monumenta:quests/r2/primevalcreations013")) {
			player.sendMessage(Component.text("You must finish Primeval Creations 013-2 Wools before you can use this tesseract.", NamedTextColor.RED));
			return false;
		}
		return !ZoneUtils.hasZoneProperty(player.getLocation(), ZoneUtils.ZoneProperty.DISABLE_PURPLE_TESS) &&
			!DISALLOWED_GAMEMODES.contains(player.getGameMode()) &&
			!ItemStatUtils.hasInfusion(item, InfusionType.SHATTERED) &&
			player.hasPermission("monumenta.tesseract.purple") &&
			!player.getScoreboardTags().contains("DungeonRace");
	}
}
