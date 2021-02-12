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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

public class Quickdraw extends Ability {

	private static final int QUICKDRAW_1_COOLDOWN = 20 * 15;
	private static final int QUICKDRAW_2_COOLDOWN = 20 * 7;
	private static final int QUICKDRAW_SLOWNESS_DURATION = 20 * 2;
	private static final int QUICKDRAW_SLOWNESS_LEVEL = 1;
	private static final int QUICKDRAW_PIERCING_BONUS = 1;

	public Quickdraw(Plugin plugin, Player player) {
		super(plugin, player, "Quickdraw");
		mInfo.mLinkedSpell = Spells.QUICKDRAW;
		mInfo.mScoreboardId = "Quickdraw";
		mInfo.mShorthandName = "Qd";
		mInfo.mDescriptions.add("Left-clicking with a bow instantly fires a fully charged arrow with +1 Piercing that afflicts Slowness 2 for 2 seconds. Cooldown: 15s.");
		mInfo.mDescriptions.add("Cooldown: 7s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? QUICKDRAW_1_COOLDOWN : QUICKDRAW_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public void cast(Action action) {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)
				&& InventoryUtils.isBowItem(inMainHand)) {
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.4f);
			world.spawnParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f);
			world.spawnParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f);

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

			//Delete bow if durability is 0 and isn't shattered.
			//This is needed because QuickDraw doesn't consume durability, but there is a high-damage uncommon bow
			//with 0 durability that should not be infinitely usable with the QuickDraw ability
			Damageable damageable = (Damageable)inMainHand.getItemMeta();
			if ((damageable.getDamage() >= inMainHand.getType().getMaxDurability())  && !ItemUtils.isItemShattered(inMainHand)) {
				inMainHand.setAmount(0);
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
