package com.playmonumenta.plugins.abilities.scout.ranger;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Quickdraw extends Ability {

	private static final int QUICKDRAW_1_COOLDOWN = 20 * 15;
	private static final int QUICKDRAW_2_COOLDOWN = 20 * 7;
	private static final int QUICKDRAW_SLOWNESS_DURATION = 20 * 2;
	private static final int QUICKDRAW_SLOWNESS_LEVEL = 2;
	private static final int QUICKDRAW_PIERCING_BONUS = 1;

	public Quickdraw(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Quickdraw");
		mInfo.mLinkedSpell = Spells.QUICKDRAW;
		mInfo.mScoreboardId = "Quickdraw";
		mInfo.mShorthandName = "Qd";
		mInfo.mDescriptions.add("Left-clicking with a bow instantly fires a fully charged arrow with +1 Piercing that afflicts Slowness 3 for 2 seconds. Cooldown: 15s.");
		mInfo.mDescriptions.add("Cooldown is reduced to 7s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? QUICKDRAW_1_COOLDOWN : QUICKDRAW_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public void cast(Action action) {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)
				&& InventoryUtils.isBowItem(inMainHand)) {
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.4f);
			mWorld.spawnParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f);
			mWorld.spawnParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f);

			Arrow arrow = mPlayer.launchProjectile(Arrow.class);
			if (inMainHand.containsEnchantment(Enchantment.ARROW_FIRE)) {
				arrow.setFireTicks(20 * 15);
			}
			if (inMainHand.containsEnchantment(Enchantment.PIERCING)) {
				arrow.setPierceLevel(inMainHand.getEnchantmentLevel(Enchantment.PIERCING) + QUICKDRAW_PIERCING_BONUS);
			} else {
				arrow.setPierceLevel(QUICKDRAW_PIERCING_BONUS);
			}
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
			arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(3.0));
			arrow.addCustomEffect(new PotionEffect(PotionEffectType.SLOW, QUICKDRAW_SLOWNESS_DURATION, QUICKDRAW_SLOWNESS_LEVEL), false);

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
		if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			cast(Action.LEFT_CLICK_AIR);
		}

		return true;
	}

}
