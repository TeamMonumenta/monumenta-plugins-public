package com.playmonumenta.plugins.abilities.warrior;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;



public class BruteForce extends Ability {

	public static class BruteForceRadiusEnchantment extends BaseAbilityEnchantment {
		public BruteForceRadiusEnchantment() {
			super("Brute Force Range", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}
	}

	public static class BruteForceDamageEnchantment extends BaseAbilityEnchantment {
		public BruteForceDamageEnchantment() {
			super("Brute Force Damage", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

	public static class BruteForceKnockbackEnchantment extends BaseAbilityEnchantment {
		public BruteForceKnockbackEnchantment() {
			super("Brute Force Knockback", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}

		private static float getKnockback(Player player, float base) {
			int level = PlayerTracking.getInstance().getPlayerCustomEnchantLevel(player, BruteForceKnockbackEnchantment.class);
			return base * (float) ((level / 100.0) + 1);
		}
	}

	private static final float BRUTE_FORCE_RADIUS = 2.0f;
	private static final int BRUTE_FORCE_DAMAGE = 2;
	private static final double SCALING_DAMAGE = 0.10;
	private static final double BRUTE_FORCE_2_DAMAGE = 3;
	private static final float BRUTE_FORCE_KNOCKBACK_SPEED = 0.7f;


	public BruteForce(Plugin plugin, Player player) {
		super(plugin, player, "Brute Force");
		mInfo.mLinkedSpell = ClassAbility.BRUTE_FORCE;
		mInfo.mScoreboardId = "BruteForce";
		mInfo.mShorthandName = "BF";
		mInfo.mDescriptions.add("Attacking an enemy with a cooled down falling attack passively deals 2 more damage to it, 2 physical damage to all enemies in a 2-block cube around it, and knocks all non-boss enemies away from you.");
		mInfo.mDescriptions.add("Damage is increased from 2, to 15% and then 3.");
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (PlayerUtils.isFallingAttack(mPlayer) && event.getCause() == DamageCause.ENTITY_ATTACK) {
			double damageBonus = getAbilityScore() == 1 ? BRUTE_FORCE_DAMAGE : BRUTE_FORCE_2_DAMAGE + event.getDamage() * SCALING_DAMAGE;
			damageBonus += BruteForceDamageEnchantment.getExtraDamage(mPlayer, BruteForceDamageEnchantment.class);
			event.setDamage(event.getDamage() + damageBonus);

			Location loc = event.getEntity().getLocation().add(0, 0.75, 0);
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 1);
			world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.135);

			float radius = BruteForceRadiusEnchantment.getRadius(mPlayer, BRUTE_FORCE_RADIUS, BruteForceRadiusEnchantment.class);
			float knockback = BruteForceKnockbackEnchantment.getKnockback(mPlayer, BRUTE_FORCE_KNOCKBACK_SPEED);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, radius, mPlayer)) {
				if (mob != event.getEntity()) {
					EntityUtils.damageEntity(mPlugin, mob, damageBonus, mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell);
				}
				if (!EntityUtils.isBoss(mob)) {
					MovementUtils.knockAway(mPlayer.getLocation(), mob, knockback, knockback / 2);
				}
			}
		}

		return true;
	}
}