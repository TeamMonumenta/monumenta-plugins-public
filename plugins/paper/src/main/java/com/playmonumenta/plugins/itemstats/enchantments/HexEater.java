package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warlock.CholericFlames;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionEffectType;

public class HexEater implements Enchantment {

	private static final double DAMAGE = 0.5;
	public static final String CHARM_DAMAGE = "Hex Eater Damage";

	@Override
	public String getName() {
		return "Hex Eater";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.HEX_EATER;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 18;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		int level = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.HEX_EATER);
		if (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE) {
			applyHexDamage(plugin, event.getDamager() instanceof Projectile, player, level, enemy, event);
		}
	}

	public static void applyHexDamage(Plugin plugin, boolean isProjectile, Player player, int level, LivingEntity target, DamageEvent event) {
		List<PotionEffectType> e = PotionUtils.getNegativeEffects(plugin, target);
		int effects = e.size();

		if (EntityUtils.isStunned(target)) {
			effects++;
		}

		if (EntityUtils.isParalyzed(plugin, target)) {
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

		if (EntityUtils.isVulnerable(plugin, target)) {
			effects++;
		}

		if (target.getFireTicks() > 0 || Inferno.hasInferno(plugin, target)) {
			effects++;
		}

		if (EntityUtils.hasDamageOverTime(plugin, target)) {
			effects++;
		}

		// interaction with choleric flames 2
		if (Plugin.getInstance().mEffectManager.hasEffect(target, CholericFlames.ANTIHEAL_EFFECT)) {
			effects++;
		}

		if (effects > 0) {
			//Projectiles do not rely on player attack strength
			double damage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, level * effects * DAMAGE);
			if (isProjectile) {
				event.setDamage(event.getFlatDamage() + damage);
			} else {
				event.setDamage(event.getFlatDamage() + damage * player.getCooledAttackStrength(0));
			}
			new PartialParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.001).spawnAsPlayerActive(player);

			World world = target.getWorld();
			Location loc = target.getLocation();
			if (!isProjectile) {
				world.playSound(loc, Sound.BLOCK_SCULK_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
		}
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false) && !AbilityUtils.isVolley(player, projectile)) {
			Location loc = player.getLocation();
			AbilityUtils.playPassiveAbilitySound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.1f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_VEX_HURT, 1.5f, 0.4f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.BLOCK_SCULK_BREAK, 1.0f, 1.0f);
		}
	}
}
