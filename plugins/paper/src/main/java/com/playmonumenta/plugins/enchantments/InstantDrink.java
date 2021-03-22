package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;

public class InstantDrink implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Instant Drink";

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
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event, int level) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || (event.getAction() == Action.RIGHT_CLICK_BLOCK && !ItemUtils.interactableBlocks.contains(event.getClickedBlock().getBlockData().getMaterial()))) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.POTION) {
				PotionMeta meta = (PotionMeta) item.getItemMeta();
				if (meta.hasCustomEffects()) {
					for (PotionEffect effect : meta.getCustomEffects()) {
						if (effect.getType().equals(PotionEffectType.HEAL) || effect.getType().equals(PotionEffectType.HARM)) {
							PotionUtils.apply(player, new PotionInfo(effect.getType(), effect.getDuration() + 1, effect.getAmplifier(),false, false));
						} else {
							plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effect);
						}
					}
				} else {
					PotionInfo info = PotionUtils.getPotionInfo(meta.getBasePotionData(), 1);
					PotionUtils.apply(player, info);
				}

				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1, 1);
				World world = player.getWorld();
				if (meta.hasColor()) {
					Color color = meta.getColor();
					double red = color.getRed() / 255D;
					double green = color.getGreen() / 255D;
					double blue = color.getBlue() / 255D;
					for (int i = 0; i < 30; i++) {
						double y = FastUtils.randomDoubleInRange(0.25, 1.75);
						world.spawnParticle(Particle.SPELL_MOB, player.getLocation().add(0, y, 0), 0, red, green, blue, 1);
					}
				} else {
					world.spawnParticle(Particle.SPELL, player.getLocation().add(0, 0.75, 0), 30, 0, 0.45, 0, 1);
				}

				//Wait, this is illegal for a potion to have.
				if (item.containsEnchantment(Enchantment.ARROW_INFINITE)) {
					event.setUseItemInHand(Result.DENY);
				} else {
					item.setAmount(item.getAmount() - 1);
					player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
				}
			}
		}
	}
}
