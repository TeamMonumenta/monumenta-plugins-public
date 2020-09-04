package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

import net.md_5.bungee.api.ChatColor;

//Cloaking: Gives invisibility when held or worn.
public class Cloaking implements BaseEnchantment {
    private static String PROPERTY_NAME = ChatColor.GRAY + "Cloaking";

    @Override
    public String getProperty() {
        return PROPERTY_NAME;
    }

    @Override
    public EnumSet<ItemSlot> validSlots() {
        return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
    }

    @Override
    public void applyProperty(Plugin plugin, Player player, int level) {
    	plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 0, true, false));
    }

    @Override
    public void removeProperty(Plugin plugin, Player player) {
        plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.INVISIBILITY);
    }
}