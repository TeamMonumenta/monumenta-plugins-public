package pe.project.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.utils.ParticleUtils;
import pe.project.item.properties.ItemPropertyManager.ItemSlot;

public class IceAspect implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Ice Aspect";

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
		int duration = 20 * 5;
		target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, level - 1, false, true));
		ParticleUtils.playParticlesInWorld(world, Particle.SNOWBALL, target.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.001);

		if (target instanceof Blaze) {
			damage = damage + 1.0;
		}

		return damage;
	}

	@Override
	public boolean hasOnAttack() {
		return true;
	}
}
