package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
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
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class ThunderAspect implements BaseEnchantment {
	public static final String PROPERTY_NAME = ChatColor.GRAY + "Thunder Aspect";
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);
	public static final String LEVEL_METAKEY = "ThunderAspectLevelMetakey";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
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
			} else {
				EntityUtils.applyStun(plugin, 50, target);
			}

			if (target instanceof Guardian || target instanceof IronGolem) {
				event.setDamage(event.getDamage() + 1.0);
			}

			if (!(EntityUtils.isBoss(target) || target.getScoreboardTags().contains(CrowdControlImmunityBoss.identityTag))) {
				Location loc = target.getLocation();
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.65f, 1.5f);
				loc = loc.add(0, 1, 0);
				world.spawnParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, YELLOW_1_COLOR);
				world.spawnParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, YELLOW_2_COLOR);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 15, 0, 0, 0, 0.15);
			}
		}
	}

	// Thrown trident damage handling - DOES NOT WORK, implemented in AttributeThrowRate instead
	@Override
	@Deprecated
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (proj instanceof Trident) {
			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
		}
	}

	//Trident hit effect
	/*
	 * TODO: This needs some kind of better registration than expecting it to be called directly
	 *
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageByEntityEvent !
	 *
	 * This works this way because you might have the enchantment when you fire the arrow, but switch to a different item before it hits
	 */
	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(LEVEL_METAKEY) && proj instanceof Trident && proj.getShooter() instanceof Player) {
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
			Player player = (Player)proj.getShooter();
			thunderAspectHit(plugin, player, level * 0.1, target, event);
		}
	}
}
