package com.playmonumenta.plugins.abilities.scout.ranger;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
* Sprint right click (without a bow) to execute a fast dash that launches the
* player a short distance, stopping at the first enemy hit, dealing 6 damage and
* applying 20% vulnerability for 3 seconds. The hit enemy is also stunned for 1
* second. If the player hits the ground before reaching an enemy, nothing
* happens. At level 2 the damage increases to 10, the vulnerability increases to
* 30%, and all mobs in a 3 block radius are stunned. (CD: 6 seconds)
*/

public class PrecisionStrike extends Ability {

	private static final double PRECISION_STRIKE_Y_VELOCITY_MULTIPLIER = 0.5;
	private static final double PRECISION_STRIKE_Y_VELOCITY_BONUS = 0.4;
	private static final double PRECISION_STRIKE_ACTIVATION_RADIUS = 1;
	private static final int PRECISION_STRIKE_1_DAMAGE = 6;
	private static final int PRECISION_STRIKE_2_DAMAGE = 10;
	private static final double PRECISION_STRIKE_STUN_RADIUS = 3;
	private static final int PRECISION_STRIKE_STUN_DURATION = 1 * 20;
	private static final int PRECISION_STRIKE_VULNERABILITY_DURATION = 3 * 20;
	private static final int PRECISION_STRIKE_1_VULNERABILITY_LEVEL = 3;
	private static final int PRECISION_STRIKE_2_VULNERABILITY_LEVEL = 5;
	private static final int PRECISION_STRIKE_COOLDOWN = 6 * 20;

	private Material blockClicked;

	public PrecisionStrike(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 6;
		mInfo.specId = 8;
		mInfo.linkedSpell = Spells.PRECISION_STRIKE;
		mInfo.scoreboardId = "PrecisionStrike";
		mInfo.cooldown = PRECISION_STRIKE_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		this.blockClicked = blockClicked;
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		return mPlayer.isSprinting() && !InventoryUtils.isBowItem(inMainHand) && !InventoryUtils.isBowItem(inOffHand) && !blockClicked.isInteractable();
	}

	@Override
	public boolean cast() {
		mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 63, 0.25, 0.1, 0.25, 0.2);
		mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.1, 0.25, 0.125);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1.7f);
		int damage = getAbilityScore() == 1 ? PRECISION_STRIKE_1_DAMAGE : PRECISION_STRIKE_2_DAMAGE;
		int level = getAbilityScore() == 1 ? PRECISION_STRIKE_1_VULNERABILITY_LEVEL : PRECISION_STRIKE_2_VULNERABILITY_LEVEL;
		Vector velocity = mPlayer.getLocation().getDirection();
		velocity.setY(velocity.getY() * PRECISION_STRIKE_Y_VELOCITY_MULTIPLIER);
		velocity.add(new Vector(0, PRECISION_STRIKE_Y_VELOCITY_BONUS, 0));
		mPlayer.setVelocity(velocity);

		new BukkitRunnable() {
			@Override
			public void run() {
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 5, 0.25, 0.1, 0.25, 0.1);
				for (Entity e : mPlayer.getNearbyEntities(PRECISION_STRIKE_ACTIVATION_RADIUS, PRECISION_STRIKE_ACTIVATION_RADIUS, PRECISION_STRIKE_ACTIVATION_RADIUS)) {
					if (EntityUtils.isHostileMob(e)) {
						LivingEntity le = (LivingEntity) e;
						EntityUtils.damageEntity(mPlugin, le, damage, mPlayer);
						le.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, PRECISION_STRIKE_VULNERABILITY_DURATION, level, false, true));
						EntityUtils.applyStun(mPlugin, PRECISION_STRIKE_STUN_DURATION, le);
						this.cancel();
						break;
					}
				}
				if (mPlayer.isOnGround()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 1, 1);

		if (mPlayer.isOnGround()) {
			return false;
		}

		mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 63, 0.25, 0.1, 0.25, 0.2);
		mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.1, 0.25, 0.125);
		mWorld.playSound(mPlayer.getLocation(), Sound.ITEM_SHIELD_BREAK, 2.0f, 0.5f);
		if (getAbilityScore() == 2) {
			for (Entity e : mPlayer.getNearbyEntities(PRECISION_STRIKE_STUN_RADIUS, PRECISION_STRIKE_STUN_RADIUS, PRECISION_STRIKE_STUN_RADIUS)) {
				if (EntityUtils.isHostileMob(e)) {
					LivingEntity le = (LivingEntity) e;
					EntityUtils.applyStun(mPlugin, PRECISION_STRIKE_STUN_DURATION, le);
				}
			}
		}

		return true;
	}
}
