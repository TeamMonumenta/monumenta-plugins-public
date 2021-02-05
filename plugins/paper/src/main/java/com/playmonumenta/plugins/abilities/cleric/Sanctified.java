package com.playmonumenta.plugins.abilities.cleric;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Sanctified extends Ability {

	private static final int PERCENT_DAMAGE_RETURNED_1 = 2;
	private static final int PERCENT_DAMAGE_RETURNED_2 = 3;
	private static final double SLOWNESS_AMPLIFIER_2 = 0.2;
	private static final int SLOWNESS_DURATION = 20 * 3;
	private static final float KNOCKBACK_SPEED = 0.4f;

	private final int mPercentDamageReturned;

	public Sanctified(Plugin plugin, Player player) {
		super(plugin, player, "Sanctified Armor");
		mInfo.mLinkedSpell = Spells.SANCTIFIED;
		mInfo.mScoreboardId = "Sanctified";
		mInfo.mShorthandName = "Sa";
		mInfo.mDescriptions.add("Whenever a non-boss undead enemy hits you with a melee or projectile attack, it takes twice the final damage you took and is knocked away from you.");
		mInfo.mDescriptions.add("Deal triple the final damage instead, and the undead enemy is also afflicted with 20% Slowness for 3 seconds (even if you are blocking).");
		mPercentDamageReturned = getAbilityScore() == 1 ? PERCENT_DAMAGE_RETURNED_1 : PERCENT_DAMAGE_RETURNED_2;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		LivingEntity mob = (LivingEntity) event.getDamager();
		if (event.getCause() == DamageCause.ENTITY_ATTACK && EntityUtils.isUndead(mob) && !EntityUtils.isBoss(mob)) {
			trigger(mob, event);
		}

		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
		if (source instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) source;
			if (EntityUtils.isUndead(mob) && !EntityUtils.isBoss(mob)) {
				trigger(mob, event);
			}
		}

		return true;
	}

	private void trigger(LivingEntity mob, EntityDamageByEntityEvent event) {
		Location loc = mob.getLocation();
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.FIREWORKS_SPARK, loc.add(0, mob.getHeight() / 2, 0), 7, 0.35, 0.35, 0.35, 0.125);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.7f, 1.2f);

		MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED, KNOCKBACK_SPEED);
		if (!mPlayer.isBlocking() || event.getFinalDamage() > 0) {
			EntityUtils.damageEntity(mPlugin, mob, mPercentDamageReturned * EntityUtils.getRealFinalDamage(event), mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
		}

		if (getAbilityScore() > 1) {
			EntityUtils.applySlow(mPlugin, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER_2, mob);
		}
	}

}
