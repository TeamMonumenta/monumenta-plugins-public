package pe.project.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.item.properties.ItemPropertyManager.ItemSlot;
import pe.project.utils.particlelib.ParticleEffect;

public class Frost implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Frost";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.HAND);
	}

	@Override
	public double onShootAttack(Plugin plugin, Player player, int level, Projectile proj, EntityDamageByEntityEvent event) {
		double damage = event.getDamage();
		int duration = 20 * 4;

		LivingEntity entity = (LivingEntity) event.getEntity();
		entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 1, true, true));
		ParticleEffect.SNOWBALL.display(0.2f, 0.35f, 0.2f, 0.05f, 10, entity.getLocation().add(0, 1.15, 0), 40);
		return damage;
	}

}
