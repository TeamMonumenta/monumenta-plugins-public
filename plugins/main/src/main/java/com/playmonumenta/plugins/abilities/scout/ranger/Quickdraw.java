package com.playmonumenta.plugins.abilities.scout.ranger;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Arrow.PickupStatus;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.scout.BowMastery;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
* Left Clicking with a bow while not sneaking instantly fires a fast arrow that deals 12 damage + any
* other bonuses from skills and inflicts Slowness 3 for 2 seconds (Cooldown: 10
* seconds). Level 2 decreases the cooldown to 8 seconds and increases the arrow
* damage to 15 + effects.
*/

public class Quickdraw extends Ability {

	private static final int QUICKDRAW_1_COOLDOWN = 10 * 20;
	private static final int QUICKDRAW_2_COOLDOWN = 8 * 20;
	private static final int QUICKDRAW_1_DAMAGE = 12;
	private static final int QUICKDRAW_2_DAMAGE = 20;
	private static final int QUICKDRAW_SLOWNESS_DURATION = 2;
	private static final int QUICKDRAW_SLOWNESS_LEVEL = 2;

	public Quickdraw(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.QUICKDRAW;
		mInfo.scoreboardId = "Quickdraw";
		mInfo.cooldown = getAbilityScore() == 1 ? QUICKDRAW_1_COOLDOWN : QUICKDRAW_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		return InventoryUtils.isBowItem(inMainHand) && !mPlayer.isSneaking();
	}

	@Override
	public boolean cast() {
		if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.4f);
			mWorld.spawnParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f);
			mWorld.spawnParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f);
			Arrow arrow = mPlayer.launchProjectile(Arrow.class);
			ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
			if (inMainHand.containsEnchantment(Enchantment.ARROW_FIRE)) {
				arrow.setFireTicks(20 * 15);
			}
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
			arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(1.5));
			arrow.setMetadata("QuickdrawDamage", new FixedMetadataValue(mPlugin, 0));
			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FIREWORKS_SPARK);
		}

		return true;
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity le, EntityDamageByEntityEvent event) {
		if (arrow.hasMetadata("QuickdrawDamage")) {
			int damage = getAbilityScore() == 1 ? QUICKDRAW_1_DAMAGE : QUICKDRAW_2_DAMAGE;
			BowMastery bm = (BowMastery) AbilityManager.getManager().getPlayerAbility(mPlayer, BowMastery.class);
			if (bm != null) {
				damage += bm.getBonusDamage();
			}
			Sharpshooter ss = (Sharpshooter) AbilityManager.getManager().getPlayerAbility(mPlayer, Sharpshooter.class);
			if (ss != null) {
				damage += ss.getSharpshot();
			}
			event.setDamage(damage);
			le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, QUICKDRAW_SLOWNESS_DURATION, QUICKDRAW_SLOWNESS_LEVEL, true, false));
		}
		return true;
	}
}
