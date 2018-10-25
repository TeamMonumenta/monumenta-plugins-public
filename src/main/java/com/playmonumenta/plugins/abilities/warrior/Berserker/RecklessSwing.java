package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import java.util.Random;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.World;

/*
 * RecklessSwing: Shift left clicking (hit) with an axe or a sword
 * in hand causes you to wildly swing your weapon in a circle to deal
 * as much damage as possible. Dealing 9/12 damage in a 2.5 block radius,
 * knocking enemies hit back. You take 2/1 damage. This skill is affected
 * by Weapon Mastery and Psychosis. (cooldown 12s)
 */

public class RecklessSwing extends Ability {

	private static final int RECKLESS_SWING_1_DAMAGE = 9;
	private static final int RECKLESS_SWING_2_DAMAGE = 12;
	private static final double RECKLESS_SWING_RADIUS = 2.5;
	private static final int RECKLESS_SWING_1_DAMAGE_TAKEN = 2;
	private static final int RECKLESS_SWING_2_DAMAGE_TAKEN = 1;
	private static final int RECKLESS_SWING_COOLDOWN = 12 * 20;
	private static final float RECKLESS_SWING_KNOCKBACK_SPEED = 0.3f;

	public RecklessSwing(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 1;
		mInfo.specId = 11;
		mInfo.linkedSpell = Spells.METEOR_SLAM;
		mInfo.scoreboardId = "RecklessSwing";
		mInfo.cooldown = RECKLESS_SWING_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public boolean cast() {
		int recklessSwing = getAbilityScore();
		if (recklessSwing > 0) {
			int damage = recklessSwing == 1 ? RECKLESS_SWING_1_DAMAGE : RECKLESS_SWING_2_DAMAGE;
			double radius = RECKLESS_SWING_RADIUS;
			Location loc = mPlayer.getLocation();
			for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
				if (EntityUtils.isHostileMob(e)) {
					LivingEntity le = (LivingEntity) e;
					EntityUtils.damageEntity(mPlugin, le, damage, mPlayer);
					MovementUtils.KnockAway(mPlayer, le, RECKLESS_SWING_KNOCKBACK_SPEED);
				}
			}
			int selfDamage = recklessSwing == 1 ? RECKLESS_SWING_1_DAMAGE_TAKEN : RECKLESS_SWING_2_DAMAGE_TAKEN;
			mPlayer.damage(selfDamage);
			putOnCooldown();
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking()) {
			ItemStack item = mPlayer.getInventory().getItemInMainHand();
			if (InventoryUtils.isAxeItem(item) || InventoryUtils.isSwordItem(item)) {
				return true;
			}
		}
		return false;
	}
}
