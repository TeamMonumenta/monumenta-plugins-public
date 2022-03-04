package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.entity.WitchThrowPotionEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Witch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class WitchListener implements Listener {

	Plugin mPlugin;

	public WitchListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void potionSplashEvent(PotionSplashEvent event) {
		// Does damage to user equal to witch's attack statistic
		if (event.getPotion().getShooter() instanceof Witch witch) {
			double damage = EntityUtils.getAttributeOrDefault(witch, Attribute.GENERIC_ATTACK_DAMAGE, 1);
			for (LivingEntity entity : event.getAffectedEntities()) {
				if (entity instanceof Player player) {
					DamageUtils.damage(witch, player, DamageType.MAGIC, damage);
				}
			}
		}
	}

	//changes the potion in the witches mainhand to throw
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void witchThrowPotionEvent(WitchThrowPotionEvent event) {
		Witch witch = event.getEntity();
		ItemStack potion = event.getPotion();
		EntityEquipment equipment = witch.getEquipment();
		if (equipment == null) {
			return;
		}
		ItemStack heldPotion = equipment.getItemInOffHand();
		if (potion != null && potion.getType() == Material.SPLASH_POTION && heldPotion != null) {
			potion = heldPotion;
		}
		if (witch.isDrinkingPotion()) { //different ideas: something about checking how long the witch is drinking and make this not work until then?
			event.setCancelled(true);
		}
		event.setPotion(potion);

		new BukkitRunnable() {
			@Override
			public void run() {
				equipment.setItemInMainHand(heldPotion);
			}
		}.runTaskLater(mPlugin, 1);
	}
}
