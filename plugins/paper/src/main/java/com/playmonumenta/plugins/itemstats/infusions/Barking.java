package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Arrays;
import java.util.List;

public class Barking implements Infusion {

	public static List<Material> listOfStrippedWood = Arrays.asList(Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_WOOD, Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_JUNGLE_WOOD, Material.STRIPPED_OAK_LOG, Material.STRIPPED_OAK_WOOD, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_SPRUCE_WOOD);
	private boolean mRun = false;

	@Override
	public String getName() {
		return "Barking";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.BARKING;
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {
		if (value == 1 && plugin.mItemStatManager.getInfusionLevel(player, InfusionType.DEBARKING) == 0) {
			if (listOfStrippedWood.contains(event.getBlock().getType())) {
				if (FastUtils.RANDOM.nextInt(5) == 0) {
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);
				}
			}
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (value > 1 && plugin.mItemStatManager.getInfusionLevel(player, InfusionType.DEBARKING) == 0) {
			if (PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
				return;
			}

			if (oneHz && mRun) {
				mRun = false;
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);
			} else if (oneHz) {
				mRun = true;
			}
		}
	}
}
