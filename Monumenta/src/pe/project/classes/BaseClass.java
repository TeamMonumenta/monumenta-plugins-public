package pe.project.classes;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.timers.CooldownTimers;
import pe.project.utils.PotionUtils;

public class BaseClass {
	protected Random mRandom;
	protected Plugin mPlugin;

	protected CooldownTimers mCooldowns = null;

	public BaseClass(Plugin plugin, Random random) {
		mRandom = random;
		mPlugin = plugin;

		mCooldowns = new CooldownTimers(plugin);
	}

	public void FakeAbilityOffCooldown(Player player, Spells ability) {
		if (ability == Spells.CELESTIAL_FAKE_1) {
			player.removeMetadata(ClericClass.CELESTIAL_1_TAGNAME, mPlugin);
		} else if (ability == Spells.CELESTIAL_FAKE_2) {
			player.removeMetadata(ClericClass.CELESTIAL_2_TAGNAME, mPlugin);
		} else if (ability == Spells.STANDARD_BEARER_FAKE) {
			player.removeMetadata(ScoutClass.STANDARD_BEARER_TAG_NAME, mPlugin);
		}
	}

	public void setupClassPotionEffects(Player player) {}

	public void AbilityOffCooldown(Player player, Spells spell) {}

	public void PulseEffectApplyEffect(Player owner, Location loc, Player effectedPlayer, int abilityID) {}
	public void PulseEffectRemoveEffect(Player owner, Location loc, Player effectedPlayer, int abilityID) {}

	public boolean has1SecondTrigger() {
		return false;
	}

	public boolean has2SecondTrigger() {
		return false;
	}

	public boolean has40SecondTrigger() {
		return false;
	}

	public boolean has60SecondTrigger() {
		return false;
	}

	public void PeriodicTrigger(Player player, boolean oneSecond, boolean twoSeconds, boolean fourtySeconds, boolean sixtySeconds, int originalTime) {}

	public void ModifyDamage(Player player, BaseClass owner, EntityDamageByEntityEvent event) {}

	public boolean PlayerDamagedByLivingEntityEvent(Player player, LivingEntity damager, double damage) { return true; }

	public boolean PlayerCombustByEntityEvent(Player player, Entity damager) { return true; }

	public boolean PlayerDamagedByProjectileEvent(Player player, Projectile damager) { return true; }

	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {
		return true;
	}

	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {}

	public void PlayerShotArrowEvent(Player player, Arrow arrow) {}

	public void PlayerThrewSplashPotionEvent(Player player, SplashPotion potion) {}

	public void ProjectileHitEvent(Player player, Arrow arrow) {}

	public void ProjectileHitPlayerEvent(Player player, Projectile damager) {}

	public void PlayerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) {};

	public void PlayerRespawnEvent(Player player) {}

	public void EntityDeathEvent(Player player, LivingEntity killedEntity, DamageCause cause, boolean shouldGenDrops) {}

	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {}

	// Called when a player throws a splash potion
	public boolean PlayerSplashPotionEvent(Player player, Collection<LivingEntity> affectedEntities,
										   ThrownPotion potion, PotionSplashEvent event) {
		//  All affected players need to have the effect added to their potion manager.
		for (LivingEntity entity : affectedEntities) {
			if (entity instanceof Player) {
				// Thrown by a player - negative effects are not allowed for other players
				// Don't remove negative effects if it was the same player that threw the potion
				if ((!entity.equals(player)) && PotionUtils.hasNegativeEffects(potion.getEffects())) {
					// If a thrown potion contains any negative effects, don't apply *any* effects to other players
					event.setIntensity(entity, 0);
				}

				mPlugin.mPotionManager.addPotion((Player)entity, PotionID.APPLIED_POTION, potion.getEffects(),
												 event.getIntensity(entity));
			}
		}

		return true;
	}

	public void AreaEffectCloudApplyEvent(Collection<LivingEntity> entities, Player player) {}
}
