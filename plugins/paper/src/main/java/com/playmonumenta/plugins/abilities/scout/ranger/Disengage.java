package com.playmonumenta.plugins.abilities.scout.ranger;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

/*
* Sneak right click (without a bow) to leap backwards 6 ish blocks from your
* position, with a bit of vertical velocity as well. Blocks fallen during
* this period are halved for the purposes of fall damage calculations. Enemies within melee range
* of your previous position take 12 damage (Cooldown: 12 seconds).
* At Level 2, stun non-elites for 4 seconds (Cooldown: 10 seconds).
*/

public class Disengage extends Ability {

	private static final double DISENGAGE_VELOCITY_MULTIPLIER = 1.65;
	private static final double DISENGAGE_Y_VELOCITY = 0.65;
	private static final double DISENGAGE_STUN_RADIUS = 3;
	private static final int DISENGAGE_STUN_DURATION = 4 * 20;
	private static final int DISENGAGE_DAMAGE = 12;
	private static final int DISENGAGE_1_COOLDOWN = 12 * 20;
	private static final int DISENGAGE_2_COOLDOWN = 10 * 20;

	private boolean mStillInAir = false;
	private int mLandedTick = 0;

	public Disengage(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Disengage");
		mInfo.linkedSpell = Spells.DISENGAGE;
		mInfo.scoreboardId = "Disengage";
		mInfo.mShorthandName = "Dis";
		mInfo.mDescriptions.add("Right-clicking while shifted while not holding a bow, food, trident, potion, compass, or block causes you to leap backwards 6 blocks. Enemies within melee range of your previous position take 12 damage. If you land after being propelled by this skill's effect fall damage is calculated as if you fell for half as many blocks as you actually fell for. This cannot be used in towns. Cooldown: 12s.");
		mInfo.mDescriptions.add("Non-elite/boss enemies in melee range when you activate this skill are stunned for 4 seconds. Cooldown: 10s.");
		mInfo.cooldown = getAbilityScore() == 1 ? DISENGAGE_1_COOLDOWN : DISENGAGE_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean runCheck() {
		return true;
	}

	public boolean runCheckActiveCast() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		if (!ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			// Checks for bows in offhand, and bows, pickaxes, potions, blocks, food, and tridents in mainhand
			return mPlayer.isSneaking() && !InventoryUtils.isBowItem(inMainHand) && !InventoryUtils.isBowItem(inOffHand) &&
			       !InventoryUtils.isPotionItem(inMainHand) && !inMainHand.getType().isBlock() && !inMainHand.getType().isEdible()
			       && inMainHand.getType() != Material.TRIDENT && inMainHand.getType() != Material.COMPASS
			       && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell);
		}
		return false;
	}

	@Override
	public void cast(Action action) {
		if (!runCheckActiveCast()) {
			return;
		}
		for (LivingEntity le : EntityUtils.getNearbyMobs(mPlayer.getLocation(), DISENGAGE_STUN_RADIUS, mPlayer)) {
			if (getAbilityScore() > 1 && !EntityUtils.isElite(le) && !EntityUtils.isBoss(le)) {
				EntityUtils.applyStun(mPlugin, DISENGAGE_STUN_DURATION, le);
			}

			EntityUtils.damageEntity(mPlugin, le, DISENGAGE_DAMAGE, mPlayer, MagicType.PHYSICAL, true, mInfo.linkedSpell);
		}

		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1.2f);
		mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 15, 0.1f, 0, 0.1f, 0.125f);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 10, 0.1f, 0, 0.1f, 0.15f);
		mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 25, 0.1f, 0, 0.1f, 0.15f);
		Vector dir = mPlayer.getLocation().getDirection().setY(0).normalize();
		double xVelocity = dir.getX() * DISENGAGE_VELOCITY_MULTIPLIER;
		double zVelocity = dir.getZ() * DISENGAGE_VELOCITY_MULTIPLIER;
		mPlayer.setVelocity(new Vector(-xVelocity, DISENGAGE_Y_VELOCITY, -zVelocity));

		mStillInAir = true;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayer.isOnGround()) {
					mStillInAir = false;
					mLandedTick = mPlayer.getTicksLived();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 2, 1); // Don't run immediately - wait for player to be in the air

		putOnCooldown();
	}

	@Override
	public boolean playerDamagedEvent(EntityDamageEvent event) {
		/* Reduce falling damage if player is still falling or they landed very recently */
		if (event.getCause().equals(DamageCause.FALL)
		    && (mStillInAir || ((mPlayer.getTicksLived() - mLandedTick) < 5))) {
			event.setDamage(event.getDamage() / 2);
		}
		return true;
	}
}
