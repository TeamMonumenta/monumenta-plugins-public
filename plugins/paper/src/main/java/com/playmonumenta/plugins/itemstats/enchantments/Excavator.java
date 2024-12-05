package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.listeners.RepairExplosionsListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
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

		if (!SpawnerUtils.tryBreakSpawner(event.getBlock(), 1 + Plugin.getInstance().mItemStatManager.getEnchantmentLevel(event.getPlayer(), EnchantmentType.DRILLING), false)) {
			return;
		}

		Block block = event.getBlock();
		if (mIgnoredMats.contains(block.getType())) {
			return;
		}

		int currentTick = Bukkit.getCurrentTick();
		if (mLastUpdateTick != currentTick) {
			mLastUpdateTick = currentTick;
			mAlreadyBrokenLocations.clear();
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				if (mLastUpdateTick == currentTick) {
					// This event did not run again the next tick;
					// clear Location list to avoid keeping chunks/worlds loaded
					mAlreadyBrokenLocations.clear();
				}
			}, 1L);
		}

		if (mAlreadyBrokenLocations.contains(block.getLocation())) {
			return;
		}

		BlockFace brokenFace = player.getTargetBlockFace(7);
		if (brokenFace == null) {
			return;
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
				break;
		}
	}

	private void breakBlock(Player player, ItemStack mainHand, Block block, int x, int y, int z) {
		if (x == 0 && y == 0 && z == 0) {
			return;
		}
		Block relative = block.getRelative(x, y, z);
		mAlreadyBrokenLocations.add(relative.getLocation());
		if (canBreakBlock(relative, player)) {
			BlockBreakEvent event = new BlockBreakEvent(relative, player);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				CoreProtectIntegration.logRemoval(player, relative);
				RepairExplosionsListener.getInstance().playerReplacedBlockViaPlugin(player, relative);
				relative.breakNaturally(mainHand, true);
				ItemUtils.damageItem(mainHand, 1, true);
			}
		}
	}

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.AIR,
		Material.CAVE_AIR,
		Material.VOID_AIR,
		Material.STRUCTURE_VOID,
		Material.LIGHT,
		Material.MOVING_PISTON,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.STRUCTURE_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.CHEST,
		Material.TRAPPED_CHEST,
		Material.BARREL,
		Material.WATER,
		Material.LAVA
	);

	private int mLastUpdateTick = Integer.MIN_VALUE;
	private final Set<Location> mAlreadyBrokenLocations = new HashSet<>();

	private boolean canBreakBlock(Block block, Player player) {
		if (block.isLiquid() || block.isEmpty()) {
			return false;
		}

		ItemStack mainHand = player.getInventory().getItemInMainHand();
		mainHand = mainHand.clone();
		mainHand.addEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH, 1);
		if (block.getDrops(mainHand).isEmpty()) {
			return false;
		}

		if (ServerProperties.getUnbreakableBlocks().contains(block.getType())) {
			return false;
		}

		if (mIgnoredMats.contains(block.getType())) {
			return false;
		}

		if (!ZoneUtils.playerCanMineBlock(player, block)) {
			return false;
		}

		return true;
	}
}
