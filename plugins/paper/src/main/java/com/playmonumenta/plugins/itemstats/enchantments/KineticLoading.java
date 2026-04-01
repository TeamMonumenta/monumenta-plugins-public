package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class KineticLoading implements Enchantment {
	@Override
	public String getName() {
		return "Kinetic Loading";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.KINETICLOADING;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MELEE && player.getCooledAttackStrength(0.5f) > 0.9) {
			ItemStack mainhand = player.getInventory().getItemInMainHand();
			if (mainhand.getItemMeta() instanceof CrossbowMeta crossbowMeta && !crossbowMeta.hasChargedProjectiles()) {
				PlayerUtils.loadCrossbow(player, mainhand);

				player.playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_HIT, SoundCategory.PLAYERS, 1.5f, 0.7f);
				player.playSound(player.getLocation(), Sound.ITEM_SPYGLASS_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, SoundCategory.PLAYERS, 0.5f, 1.3f);
				new BukkitRunnable() {
					@Override
					public void run() {
						player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 0.4f, 0.6f);
					}
				}.runTaskLater(plugin, 2);
				new BukkitRunnable() {
					@Override
					public void run() {
						player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS, 0.5f, 1.4f);
						player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.1f, 1.3f);
						player.playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, SoundCategory.PLAYERS, 1.4f, 1.3f);
					}
				}.runTaskLater(plugin, 6);
			} else if (mainhand.getType().equals(Material.TRIDENT)
				|| mainhand.getType().equals(Material.SNOWBALL)
				|| ItemStatUtils.hasEnchantment(mainhand, EnchantmentType.THROWING_KNIFE)) {
				player.setCooldown(mainhand.getType(), 0);
			} else if (ItemUtils.isAlchemistItem(mainhand) && AbilityUtils.getClassNum(player) == Alchemist.CLASS_ID) {
				AlchemistPotions alchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
				if (alchemistPotions != null) {
					alchemistPotions.incrementCharge();
				}
			}
		}
	}
}
