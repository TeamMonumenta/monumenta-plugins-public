package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

public class CurseOfCrippling implements BaseEnchantment{
	private static String PROPERTY_NAME = ChatColor.RED + "Curse of Crippling";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		if(event.getCause() == DamageCause.ENTITY_ATTACK) {
			plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.SLOW, 100, level, true, false));
			player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 16, 0.4, 0.5, 0.4);
			player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.HOSTILE, 0.25f, 0.8f);
		}
	}

}
