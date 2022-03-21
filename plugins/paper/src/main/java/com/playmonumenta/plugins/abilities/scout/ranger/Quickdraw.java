package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;



public class Quickdraw extends Ability {

	private static final int QUICKDRAW_1_COOLDOWN = 20 * 15;
	private static final int QUICKDRAW_2_COOLDOWN = 20 * 7;
	private static final int QUICKDRAW_SLOWNESS_DURATION = 20 * 2;
	private static final int QUICKDRAW_SLOWNESS_LEVEL = 1;
	private static final int QUICKDRAW_PIERCING_BONUS = 1;

	public Quickdraw(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Quickdraw");
		mInfo.mLinkedSpell = ClassAbility.QUICKDRAW;
		mInfo.mScoreboardId = "Quickdraw";
		mInfo.mShorthandName = "Qd";
		mInfo.mDescriptions.add("Left-clicking with a bow instantly fires a fully charged arrow with +1 Piercing that afflicts Slowness 2 for 2 seconds. Cooldown: 15s.");
		mInfo.mDescriptions.add("Cooldown: 7s.");
		mInfo.mCooldown = isLevelOne() ? QUICKDRAW_1_COOLDOWN : QUICKDRAW_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.BLAZE_POWDER, 1);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell) && ItemUtils.isSomeBow(inMainHand) && !ItemUtils.isShootableItem(inOffHand) && !ItemStatUtils.isShattered(inMainHand)) {
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.4f);
			new PartialParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);

			boolean launched = shootArrow(inMainHand, 0);
			if (launched) {
				putOnCooldown();

				if (inMainHand.containsEnchantment(Enchantment.MULTISHOT)) {
					for (int i = 0; i < 2; i++) {
						shootArrow(inMainHand, 2 * i - 1);
					}
				}
			}
		}
	}

	private boolean shootArrow(ItemStack inMainHand, int deviation) {
		if (mPlayer == null) {
			return true;
		}
		Vector direction = mPlayer.getLocation().getDirection();
		if (deviation != 0) {
			direction.rotateAroundY(deviation * 10.0 * Math.PI / 180);
		}
		Arrow arrow = mPlayer.getWorld().spawnArrow(mPlayer.getEyeLocation(), direction, 3.0f, 0, Arrow.class);
		arrow.setShooter(mPlayer);
		if (inMainHand.containsEnchantment(Enchantment.ARROW_FIRE)) {
			arrow.setFireTicks(20 * 15);
		}
		arrow.setPierceLevel(inMainHand.getEnchantmentLevel(Enchantment.PIERCING) + QUICKDRAW_PIERCING_BONUS);
		arrow.setCritical(true);
		arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		arrow.addCustomEffect(new PotionEffect(PotionEffectType.SLOW, QUICKDRAW_SLOWNESS_DURATION, QUICKDRAW_SLOWNESS_LEVEL), false);

		ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
		Bukkit.getPluginManager().callEvent(eventLaunch);

		if (!eventLaunch.isCancelled()) {
			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FIREWORKS_SPARK);
		}

		return !eventLaunch.isCancelled();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}
}
