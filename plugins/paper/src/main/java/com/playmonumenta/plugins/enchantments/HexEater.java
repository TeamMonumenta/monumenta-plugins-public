package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Hex Eater - When you hit with a Melee attack, +X damage per debuff on the target per level
 */

public class HexEater implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Hex Eater";
	private static final String LEVEL_METAKEY = "HexEaterLevelMetakey";

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
		applyHexDamage(plugin, false, player, level, target, event);
	}

	//Apply the damage from effects
	public static void applyHexDamage(Plugin plugin, boolean tridentThrow, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		List<PotionEffectType> e = PotionUtils.getNegativeEffects(plugin, target);
		int effects = e.size();

		if (EntityUtils.isStunned(target)) {
			effects++;
		}

		if (EntityUtils.isConfused(target)) {
			effects++;
		}

		if (EntityUtils.isSilenced(target)) {
			effects++;
		}

		if (EntityUtils.isBleeding(plugin, target)) {
			effects++;
		}

		if (EntityUtils.isSlowed(plugin, target) && !e.contains(PotionEffectType.SLOW)) {
			effects++;
		}

		if (EntityUtils.isWeakened(plugin, target) && !e.contains(PotionEffectType.WEAKNESS)) {
			effects++;
		}

		if (target.getFireTicks() > 0 || Inferno.getMobInfernoLevel(plugin, target) > 0) {
			effects++;
		}

		if (effects > 0) {
			//Trident throw does not rely on player attack strength
			if (tridentThrow) {
				event.setDamage(event.getDamage() + level * effects);
			} else {
				event.setDamage(event.getDamage() + level * effects * player.getCooledAttackStrength(0));
			}
			player.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.001);
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
			applyHexDamage(plugin, true, player, level, target, event);
		}
	}
}
