package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class MagmaShield extends Ability {

	private static final int MAGMA_SHIELD_COOLDOWN = 12 * 20;
	private static final int MAGMA_SHIELD_RADIUS = 6;
	private static final int MAGMA_SHIELD_FIRE_DURATION = 4 * 20;
	private static final int MAGMA_SHIELD_1_DAMAGE = 7;
	private static final int MAGMA_SHIELD_2_DAMAGE = 14;
	private static final float MAGMA_SHIELD_KNOCKBACK_SPEED = 0.5f;
	private static final double MAGMA_SHIELD_DOT_ANGLE = 0.33;

	public MagmaShield(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.MAGMA_SHIELD;
		mInfo.scoreboardId = "Magma";
		mInfo.cooldown = MAGMA_SHIELD_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean cast() {
		int magmaShield = getAbilityScore();
		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), MAGMA_SHIELD_RADIUS, mPlayer)) {
			Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0)
			                     .normalize();
			if (playerDir.dot(toMobVector) > MAGMA_SHIELD_DOT_ANGLE) {
				MovementUtils.KnockAway(mPlayer, mob, MAGMA_SHIELD_KNOCKBACK_SPEED);
				mob.setFireTicks(MAGMA_SHIELD_FIRE_DURATION);

				int extraDamage = magmaShield == 1 ? MAGMA_SHIELD_1_DAMAGE : MAGMA_SHIELD_2_DAMAGE;
				Spellshock.spellDamageMob(mPlugin, mob, extraDamage, mPlayer, MagicType.FIRE);
			}
		}

		ParticleUtils.explodingConeEffect(mPlugin, mPlayer, MAGMA_SHIELD_RADIUS, Particle.FLAME, 0.75f, Particle.LAVA,
		                                  0.25f, MAGMA_SHIELD_DOT_ANGLE);

		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 1.5f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.25f, 1.0f);

		PlayerUtils.callAbilityCastEvent(mPlayer, Spells.MAGMA_SHIELD);
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.isWandItem(mainHand)
		    || InventoryUtils.isWandItem(offHand)) {
			return mPlayer.isSneaking();
		}
		return false;
	}

}
