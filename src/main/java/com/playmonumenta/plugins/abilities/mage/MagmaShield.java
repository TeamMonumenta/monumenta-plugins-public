package com.playmonumenta.plugins.abilities.mage;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class MagmaShield extends Ability {

	private static final int MAGMA_SHIELD_COOLDOWN = 13 * 20;
	private static final int MAGMA_SHIELD_RADIUS = 6;
	private static final int MAGMA_SHIELD_FIRE_DURATION = 4 * 20;
	private static final int MAGMA_SHIELD_1_DAMAGE = 6;
	private static final int MAGMA_SHIELD_2_DAMAGE = 12;
	private static final float MAGMA_SHIELD_KNOCKBACK_SPEED = 0.5f;
	private static final double MAGMA_SHIELD_DOT_ANGLE = 0.33;

	@Override
	public boolean cast(Player player) {
		int magmaShield = getAbilityScore(player);
		Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), MAGMA_SHIELD_RADIUS)) {
			Vector toMobVector = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0)
					.normalize();
			if (playerDir.dot(toMobVector) > MAGMA_SHIELD_DOT_ANGLE) {
				MovementUtils.KnockAway(player, mob, MAGMA_SHIELD_KNOCKBACK_SPEED);
				mob.setFireTicks(MAGMA_SHIELD_FIRE_DURATION);

				int extraDamage = magmaShield == 1 ? MAGMA_SHIELD_1_DAMAGE : MAGMA_SHIELD_2_DAMAGE;
				AbilityUtils.mageSpellshock(mPlugin, mob, extraDamage, player, MagicType.FIRE);
			}
		}

		ParticleUtils.explodingConeEffect(mPlugin, player, MAGMA_SHIELD_RADIUS, Particle.FLAME, 0.75f, Particle.LAVA,
				0.25f, MAGMA_SHIELD_DOT_ANGLE);

		mWorld.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 1.5f);
		mWorld.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.25f, 1.0f);

		PlayerUtils.callAbilityCastEvent(player, Spells.MAGMA_SHIELD);
		return true;
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 1;
		info.specId = -1;
		info.linkedSpell = Spells.MAGMA_SHIELD;
		info.scoreboardId = "MagmaShield";
		info.cooldown = MAGMA_SHIELD_COOLDOWN;
		info.trigger = AbilityTrigger.RIGHT_CLICK;
		return info;
	}

	@Override
	public boolean runCheck(Player player) {
		ItemStack offHand = player.getInventory().getItemInOffHand();
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		if ((offHand.getType() == Material.SHIELD && InventoryUtils.isWandItem(mainHand))
				|| (mainHand.getType() == Material.SHIELD))
			return player.isSneaking();
		return false;
	}

}
