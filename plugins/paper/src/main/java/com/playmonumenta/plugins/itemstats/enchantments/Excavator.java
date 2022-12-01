package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Excavator implements Enchantment {

	@Override
	public String getName() {
		return "Excavator";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.EXCAVATOR;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double level, BlockBreakEvent event) {
		if (player.isSneaking()) {
			return;
		}

		Block block = event.getBlock();
		if (mIgnoredMats.contains(block.getType())) {
			return;
		}

		if (mAlreadyBrokenLocations.contains(block.getLocation())) {
			return;
		}

		BlockFace brokenFace = player.getTargetBlockFace(7);
		if (brokenFace == null) {
			player.sendMessage("Somehow, the blockface was null");
		}

		ItemStack mainHand = player.getInventory().getItemInMainHand();
		switch (brokenFace) {
			case UP:
			case DOWN:
				for (int x = -1; x <= 1; x++) {
					for (int z = -1; z <= 1; z++) {
						breakBlock(player, mainHand, block, x, 0, z);
					}
				}
				break;
			case WEST:
			case EAST:
				for (int z = -1; z <= 1; z++) {
					for (int y = -1; y <= 1; y++) {
						breakBlock(player, mainHand, block, 0, y, z);
					}
				}
				break;
			case NORTH:
			case SOUTH:
				for (int x = -1; x <= 1; x++) {
					for (int y = -1; y <= 1; y++) {
						breakBlock(player, mainHand, block, x, y, 0);
					}
				}
				break;
			default:
				player.sendMessage("Block face was Non-Cartesian.");
				break;
		}
		mAlreadyBrokenLocations.clear();
	}

	private void breakBlock(Player player, ItemStack mainHand, Block block, int x, int y, int z) {
		Block relative = block.getRelative(x, y, z);
		mAlreadyBrokenLocations.add(relative.getLocation());
		BlockBreakEvent event = new BlockBreakEvent(relative, player);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			if (canBreakBlock(relative, player)) {
				CoreProtectIntegration.logRemoval(player, relative);
				relative.breakNaturally(mainHand, true);
				ItemUtils.damageItem(mainHand, 1, true);
			}
		}
	}

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.AIR,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.CHEST,
		Material.TRAPPED_CHEST,
		Material.WATER,
		Material.LAVA
	);

	private List<Location> mAlreadyBrokenLocations = new ArrayList<>();

	private boolean canBreakBlock(Block block, Player player) {
		if (block.isLiquid() || block.isEmpty()) {
			return false;
		}

		ItemStack mainHand = player.getInventory().getItemInMainHand();
		mainHand = mainHand.clone();
		mainHand.addEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH, 1);
		if (block.getDrops(mainHand).size() == 0) {
			return false;
		}

		if (ServerProperties.getUnbreakableBlocks().contains(block.getType())) {
			return false;
		}

		if (mIgnoredMats.contains(block.getType())) {
			return false;
		}

		if (!ZoneUtils.playerCanInteractWithBlock(player, block, false)) {
			return false;
		}

		return true;
	}
}
