package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Permafrost extends DepthsAbility {

	public static final String ABILITY_NAME = "Permafrost";
	public static final int[] ICE_TICKS = {8 * 20, 11 * 20, 14 * 20, 17 * 20, 20 * 20, 26 * 20};
	public static final int[] ICE_BONUS_DURATION_SECONDS = {2, 3, 4, 5, 6, 8};
	public static final Material PERMAFROST_ICE_MATERIAL = Material.PACKED_ICE;

	public static final DepthsAbilityInfo<Permafrost> INFO =
		new DepthsAbilityInfo<>(Permafrost.class, ABILITY_NAME, Permafrost::new, DepthsTree.FROSTBORN, DepthsTrigger.SPAWNER)
			.displayItem(new ItemStack(Material.QUARTZ))
			.descriptions(Permafrost::getDescription);

	public Permafrost(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		//If we break a spawner with a pickaxe
		if (ItemUtils.isPickaxe(event.getPlayer().getInventory().getItemInMainHand()) && event.getBlock().getType() == Material.SPAWNER) {
			ArrayList<Block> blocksToIce = new ArrayList<>();
			Block block = event.getBlock().getRelative(BlockFace.DOWN);
			if (block.isSolid() || block.getType() == Material.WATER) {
				DepthsUtils.spawnIceTerrain(block.getLocation(), ICE_TICKS[mRarity - 1], mPlayer);
			}
			blocksToIce.add(block.getRelative(BlockFace.NORTH));
			blocksToIce.add(block.getRelative(BlockFace.EAST));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH));
			blocksToIce.add(block.getRelative(BlockFace.WEST));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH));
			blocksToIce.add(block.getRelative(BlockFace.WEST).getRelative(BlockFace.WEST));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.SOUTH));
			blocksToIce.add(block.getRelative(BlockFace.EAST).getRelative(BlockFace.EAST));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.WEST));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.EAST));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.WEST));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.EAST));

			for (Block b : blocksToIce) {
				DepthsUtils.iceExposedBlock(b, ICE_TICKS[mRarity - 1], mPlayer);
			}
		}
		return true;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		Component twisted = rarity == 6 ? Component.text(" Additionally, all ice you place is packed ice, which when Avalanched becomes normal ice.", color) : Component.empty();
		return Component.text("Breaking a spawner spawns ice around it that lasts for ")
			.append(Component.text(ICE_TICKS[rarity - 1] / 20, color))
			.append(Component.text(" seconds. All ice you place with abilities lasts "))
			.append(Component.text(ICE_BONUS_DURATION_SECONDS[rarity - 1], color))
			.append(Component.text(" seconds longer."))
			.append(twisted);
	}


}

