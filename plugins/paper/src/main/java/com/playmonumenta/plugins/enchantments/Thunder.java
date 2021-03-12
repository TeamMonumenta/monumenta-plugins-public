package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class Thunder implements BaseEnchantment {
	public static final String PROPERTY_NAME = ChatColor.GRAY + "Thunder Aspect";
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);
	private static final String LEVEL_METAKEY = "ThunderAspectLevelMetakey";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		thunderAspectHit(plugin, player, level * 0.1 * player.getCooledAttackStrength(0), target, event);
	}

	//Run thunder check and application if true
	//randChance is the chance for the attack to stun
	private static void thunderAspectHit(Plugin plugin, Player player, double randChance, LivingEntity target, EntityDamageByEntityEvent event) {
		double rand = FastUtils.RANDOM.nextDouble();
		World world = target.getWorld();

		if (rand < randChance) {
			if (EntityUtils.isElite(target)) {
				EntityUtils.applyStun(plugin, 10, target);
			} else if (!EntityUtils.isBoss(target)) {
				EntityUtils.applyStun(plugin, 50, target);
			}

			if (target instanceof Guardian || target instanceof IronGolem) {
				event.setDamage(event.getDamage() + 1.0);
			}

			world.spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, YELLOW_1_COLOR);
			world.spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, YELLOW_2_COLOR);
			world.spawnParticle(Particle.FIREWORKS_SPARK, target.getLocation().add(0, 1, 0), 15, 0, 0, 0, 0.15);
			player.getWorld().playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.65f, 1.5f);
		}
	}

	// Thrown trident damage handling
	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (proj instanceof Trident) {
			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
		}
	}

	//Trident hit effect
	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(LEVEL_METAKEY) && proj instanceof Trident && proj.getShooter() instanceof Player) {
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
			Player player = (Player)proj.getShooter();
			thunderAspectHit(plugin, player, level * 0.1, target, event);
		}
	}
}
