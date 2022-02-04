package com.playmonumenta.plugins.abilities.warrior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import javax.annotation.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;


public class BruteForce extends Ability {

	private static final float BRUTE_FORCE_RADIUS = 2.0f;
	private static final int BRUTE_FORCE_DAMAGE = 2;
	private static final double BRUTE_FORCE_2_DAMAGE = 4;
	private static final float BRUTE_FORCE_KNOCKBACK_SPEED = 0.7f;


	public BruteForce(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Brute Force");
		mInfo.mLinkedSpell = ClassAbility.BRUTE_FORCE;
		mInfo.mScoreboardId = "BruteForce";
		mInfo.mShorthandName = "BF";
		mInfo.mDescriptions.add("Attacking an enemy with a critical attack passively deals 2 more damage to the mob and 2 damage to all enemies in a 2-block cube around it, and knocks all non-boss enemies away from you.");
		mInfo.mDescriptions.add("Damage is increased from 2 to 4.");
		mDisplayItem = new ItemStack(Material.STONE_AXE, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer != null && event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer)) {
			double damageBonus = getAbilityScore() == 1 ? BRUTE_FORCE_DAMAGE : BRUTE_FORCE_2_DAMAGE;

			Location loc = enemy.getLocation().add(0, 0.75, 0);
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 1);
			world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.135);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, BRUTE_FORCE_RADIUS, mPlayer)) {
				DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damageBonus, mInfo.mLinkedSpell);
				if (!EntityUtils.isBoss(mob)) {
					MovementUtils.knockAway(mPlayer.getLocation(), mob, BRUTE_FORCE_KNOCKBACK_SPEED, BRUTE_FORCE_KNOCKBACK_SPEED / 2, true);
				}
			}
			return true;
		}
		return false;
	}
}
