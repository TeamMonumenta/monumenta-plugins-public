package pe.project.item.properties;

import java.util.EnumSet;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import pe.project.Plugin;
import pe.project.utils.ParticleUtils;
import pe.project.item.properties.ItemPropertyManager.ItemSlot;

public class Chaotic implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Chaotic";

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
		int rand = mRandom.nextInt(2 * level + 1) - level;

		if (rand > 0) {
			ParticleUtils.playParticlesInWorld(world, Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.001);
		}

		if (cause == DamageCause.ENTITY_SWEEP_ATTACK) {
			rand = rand / 2;
		}

		damage = damage + rand;

		return damage;
	}
}
