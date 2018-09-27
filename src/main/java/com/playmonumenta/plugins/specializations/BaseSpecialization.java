package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.BaseClass;
import com.playmonumenta.plugins.classes.magic.AbilityCastEvent;
import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.timers.CooldownTimers;

import java.util.Random;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SplashPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class BaseSpecialization {

	protected Random mRandom;
	protected Plugin mPlugin;

	protected CooldownTimers mCooldowns = null;

	public BaseSpecialization(Plugin plugin, Random random) {
		mRandom = random;
		mPlugin = plugin;

		mCooldowns = new CooldownTimers(plugin);
	}

	public void PeriodicTrigger(Player player, boolean twoHertz, boolean oneSecond, boolean twoSeconds, boolean fourtySeconds, boolean sixtySeconds, int originalTime) {}

	public void ModifyDamage(Player player, BaseClass owner, EntityDamageByEntityEvent event) {}

	public boolean PlayerDamagedByLivingEntityEvent(Player player, EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerDamagedEvent(Player player, EntityDamageEvent event) {
		return true;
	}

	public boolean PlayerCombustByEntityEvent(Player player, Entity damager) {
		return true;
	}

	public boolean PlayerDamagedByProjectileEvent(Player player, Projectile damager) {
		return true;
	}

	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerDamagedByPlayerEvent(Player player, Player damagee) {
		return false;
	}

	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {}

	public boolean PlayerShotArrowEvent(Player player, Arrow arrow) {
		return true;
	}

	public void PlayerThrewSplashPotionEvent(Player player, SplashPotion potion) {}

	public void ProjectileHitEvent(Player player, Arrow arrow) {}

	public void ProjectileHitPlayerEvent(Player player, Projectile damager) {}

	public void PlayerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) {};

	public void PlayerRespawnEvent(Player player) {}

	public void EntityDeathEvent(Player player, EntityDeathEvent event) {}

	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {}

	public boolean PlayerDamagedByLivingEntityRadiusEvent(Player player, Player caster, LivingEntity damager, EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerExtendedSneakEvent(Player player) {
		return true;
	}

	public boolean EntityCustomDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, MagicType type, CustomDamageEvent event) {
		return true;
	}

	public void AbilityCastEvent(Player player, AbilityCastEvent event) {}
}
