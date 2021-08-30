package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class CounterStrike extends Ability {

	private static final int COUNTER_STRIKE_1_DAMAGE = 3;
	private static final int COUNTER_STRIKE_2_DAMAGE = 5;
	private static final double COUNTER_STRIKE_1_REFLECT = 0.2;
	private static final double COUNTER_STRIKE_2_REFLECT = 0.4;
	private static final float COUNTER_STRIKE_RADIUS = 3.0f;

	private double mReflect;
	private int mDamage;

	public CounterStrike(Plugin plugin, Player player) {
		super(plugin, player, "Counter Strike");
		mInfo.mScoreboardId = "CounterStrike";
		mInfo.mShorthandName = "CS";
		mInfo.mDescriptions.add("When you take melee damage, deal 3 + 20% of pre-mitigation damage taken to all mobs in a 3 block radius.");
		mInfo.mDescriptions.add("The damage is increased to 5 + 40% of pre-mitigation damage.");
		mInfo.mLinkedSpell = ClassAbility.COUNTER_STRIKE;
		mReflect = getAbilityScore() == 1 ? COUNTER_STRIKE_1_REFLECT : COUNTER_STRIKE_2_REFLECT;
		mDamage = getAbilityScore() == 1 ? COUNTER_STRIKE_1_DAMAGE : COUNTER_STRIKE_2_DAMAGE;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (!AbilityUtils.isBlocked(event)) {
			LivingEntity damager = (LivingEntity) event.getDamager();
			if (event.getCause() == DamageCause.ENTITY_ATTACK && !(damager instanceof Guardian)) {
				Location loc = mPlayer.getLocation().add(0, 1, 0);
				World world = mPlayer.getWorld();
				world.spawnParticle(Particle.SWEEP_ATTACK, loc, 6, 0.75, 0.5, 0.75, 0.001);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 8, 0.75, 0.5, 0.75, 0.1);
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.6f, 0.7f);
				double eventDamage = event.getDamage() * mReflect;

				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), COUNTER_STRIKE_RADIUS, mPlayer)) {
					EntityUtils.damageEntity(mPlugin, mob, mDamage + eventDamage, mPlayer, MagicType.SHADOWS, true, mInfo.mLinkedSpell);
				}
			}
		}
		return true;
	}
}
