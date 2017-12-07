package pe.project.items;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;

public class FireworkOverride extends OverrideItem {
    @Override
    public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
    	if (item != null && block != null) {
    		ItemMeta meta = item.getItemMeta();
    		if (meta != null) {
    			if (meta.hasDisplayName() && meta.getDisplayName().equals("Signal Flare")) {
    				plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.GLOWING, 10 * 20, 0));
    			}
    		}
    	}

        return true;
    }
}
