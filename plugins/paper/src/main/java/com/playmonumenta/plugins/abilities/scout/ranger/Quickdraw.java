package com.playmonumenta.plugins.abilities.scout.ranger;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
* Left Clicking with a bow while not sneaking instantly fires a fast arrow that deals 12 damage + any
* other bonuses from skills and inflicts Slowness 3 for 2 seconds (Cooldown: 10
* seconds). Level 2 decreases the cooldown to 8 seconds and increases the arrow
* damage to 20 + effects.
*/

public class Quickdraw extends Ability {

	private static final int QUICKDRAW_1_COOLDOWN = 10 * 20;
	private static final int QUICKDRAW_2_COOLDOWN = 8 * 20;
	private static final int QUICKDRAW_1_DAMAGE = 15;
	private static final int QUICKDRAW_2_DAMAGE = 24;
	private static final int QUICKDRAW_SLOWNESS_DURATION = 20 * 2;
	private static final int QUICKDRAW_SLOWNESS_LEVEL = 2;

	public Quickdraw(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Quickdraw");
		mInfo.linkedSpell = Spells.QUICKDRAW;
		mInfo.scoreboardId = "Quickdraw";
		mInfo.mShorthandName = "Qd";
		mInfo.mDescriptions.add("Left-clicking, while not sneaking, with a bow instantly fires an arrow that deals 15 damage (Skill bonuses, but not enchantments apply) and inflicts Slowness 3 for 2 seconds. This skill can trigger Volley if you have skill points in Volley. Cooldown: 10s.");
		mInfo.mDescriptions.add("Damage increases to 24 and cooldown is reduced to 8s.");
		mInfo.cooldown = getAbilityScore() == 1 ? QUICKDRAW_1_COOLDOWN : QUICKDRAW_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		return InventoryUtils.isBowItem(inMainHand);
	}

	@Override
	public void cast(Action action) {
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.4f);
			mWorld.spawnParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f);
			mWorld.spawnParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f);
			Arrow arrow = mPlayer.launchProjectile(Arrow.class);
			ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
			if (inMainHand.containsEnchantment(Enchantment.ARROW_FIRE)) {
				arrow.setFireTicks(20 * 15);
			}
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
			arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(3.0));
			arrow.setMetadata("ArrowQuickdraw", new FixedMetadataValue(mPlugin, null));

			double baseDamage = getAbilityScore() == 1 ? QUICKDRAW_1_DAMAGE : QUICKDRAW_2_DAMAGE;
			AbilityUtils.setArrowBaseDamage(mPlugin, arrow, baseDamage);

			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FIREWORKS_SPARK);
			ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
			Bukkit.getPluginManager().callEvent(eventLaunch);
			if (!eventLaunch.isCancelled()) {
				putOnCooldown();
			}
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause().equals(DamageCause.ENTITY_ATTACK) && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity le, EntityDamageByEntityEvent event) {
		if (arrow.hasMetadata("ArrowQuickdraw")) {
			PotionUtils.applyPotion(mPlayer, le, new PotionEffect(PotionEffectType.SLOW, QUICKDRAW_SLOWNESS_DURATION, QUICKDRAW_SLOWNESS_LEVEL, true, false));
		}
		return true;
	}

}
