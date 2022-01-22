package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellAbyssalCharge extends Spell {
	private int mCooldownTicks;

	private final LivingEntity mBoss;
	private static final double DAMAGE_MULTIPLIER = 1.5;
	private static final int DURATION = 6 * 20;

	public boolean mEmpoweredAttack;

	public SpellAbyssalCharge(LivingEntity boss, int cooldown) {
		mBoss = boss;
		mCooldownTicks = cooldown;
		mEmpoweredAttack = false;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		Entity e = ((Mob) mBoss).getTarget();
		if (e == null || !(e instanceof Player)) {
			e = EntityUtils.getNearestPlayer(loc, 20.0);
			((Mob) mBoss).setTarget(((LivingEntity) e));
		}
		if (e == null) {
			return;
		}
		//Jump back
		MovementUtils.knockAway(e, mBoss, 2.0f, false);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, DURATION, 1));
		loc.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 5, 1.25f);
		loc.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 5, 0.5f);
		mEmpoweredAttack = true;

		//Disabled empowered attack later
		new BukkitRunnable() {

			@Override
			public void run() {
				mEmpoweredAttack = false;
				loc.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 5, 1.25f);
			}


		}.runTaskLater(Plugin.getInstance(), DURATION);
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		//Extra damage
		if (mEmpoweredAttack && damagee instanceof Player) {
			event.setDamage(event.getDamage() * DAMAGE_MULTIPLIER);
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 5, 1.25f);
			mEmpoweredAttack = false;
		}
	}
}
