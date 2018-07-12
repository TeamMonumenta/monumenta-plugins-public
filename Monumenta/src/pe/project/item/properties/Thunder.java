package pe.project.item.properties;

import java.util.EnumSet;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.item.properties.ItemPropertyManager.ItemSlot;

public class Thunder implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Thunder";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public double onAttack(Plugin plugin, World world, Player player, LivingEntity target, double damage, int level, DamageCause cause) {
		Random mRandom = new Random();
		double rand = mRandom.nextDouble();

		if (damage >= 4 && rand < level * 0.1) {
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 50, 8, false, true));
			target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 50, 8, false, true));

			if (target instanceof Guardian) {
				damage = damage + 1.0;
			}

			world.playSound(player.getLocation(), "entity.lightning.impact", 0.4f, 1.0f);
		}

		return damage;
	}
}
