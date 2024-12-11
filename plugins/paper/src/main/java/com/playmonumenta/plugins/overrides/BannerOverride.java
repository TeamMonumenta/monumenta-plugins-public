package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class BannerOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		Banner banner = (Banner) block.getState();

		if (item != null) {
			if (player.isSneaking()) {
				return true;
			}

			EquipmentSlot handSlot = event.getHand();
			if (handSlot == null) {
				// Must be an item in a specific hand to override
				return true;
			}

			Material blockFloorMat = ItemUtils.toFloorBanner(banner.getType());
			if (blockFloorMat == null || !blockFloorMat.isItem()) {
				// The returned material must be a valid item type (which floor banners should be)
				return true;
			}

			// Item must have no lore text
			ItemMeta meta = item.getItemMeta();
			List<Component> lore = meta.lore();
			if (lore != null && !lore.isEmpty()) {
				return true;
			}

			// Must have block meta for a banner
			if (!(meta instanceof BannerMeta bannerMeta)) {
				return true;
			} else {
				while (bannerMeta.numberOfPatterns() >= 1) {
					bannerMeta.removePattern(bannerMeta.numberOfPatterns() - 1);
				}

				for (Pattern pattern : banner.getPatterns()) {
					bannerMeta.addPattern(pattern);
				}

				ItemStack newItemStack = new ItemStack(blockFloorMat, item.getAmount());
				newItemStack.setItemMeta(meta);
				player.getInventory().setItem(handSlot, newItemStack);

				return false;
			}
		}

		return true;
	}
}
