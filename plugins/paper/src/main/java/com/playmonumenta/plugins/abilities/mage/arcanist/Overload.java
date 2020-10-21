package com.playmonumenta.plugins.abilities.mage.arcanist;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Overload extends Ability {

	private static final int SPELLS_PER_OVERLOAD = 3;
	private static final int DAMAGE_1 = 4;
	private static final int DAMAGE_2 = 8;
	private static final int STUN_DURATION = 20 * 1;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(222, 219, 36), 1.0f);
	private static final Particle.DustOptions COLOR2 = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);

	private final int mDamage;

	private int mSpellsToOverload = SPELLS_PER_OVERLOAD;
	private Spells mAffectedSpell = null;

	public Overload(Plugin plugin, Player player) {
		super(plugin, player, "Overload");
		mInfo.mScoreboardId = "Overload";
		mInfo.mShorthandName = "Ov";
		mInfo.mDescriptions.add("Every 3rd spell cast is \"overloaded\" and deals +4 damage. When your next spell is overloaded, non-elite and non-boss mobs that melee or projectile attack you are stunned for 1 second.");
		mInfo.mDescriptions.add("Overloaded spell damage is increased to +8.");
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (mSpellsToOverload == 1) {
			Location loc = mPlayer.getLocation().add(0, 1, 0);
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.REDSTONE, loc, 3, 0.4, 0.4, 0.4, COLOR);
			world.spawnParticle(Particle.REDSTONE, loc, 3, 0.5, 0.5, 0.5, COLOR2);
		}
	}

	/*
	 * Something annoying to note; this triggers when abilities go on cooldown, but
	 * we also need this to trigger before the actual damage events. End result is
	 * that until we get something better to tie the AbilityCastEvent to, abilities
	 * for Mage must be put on cooldown before they do any damage.
	 */
	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		mSpellsToOverload--;
		if (mSpellsToOverload == 0) {
			mSpellsToOverload = SPELLS_PER_OVERLOAD;

			Location loc = mPlayer.getLocation().add(0, 1, 0);
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.REDSTONE, loc, 35, 0.4, 0.4, 0.4, COLOR);
			world.spawnParticle(Particle.REDSTONE, loc, 35, 0.5, 0.5, 0.5, COLOR2);

			// This is necessary for skills triggering over periods of time, such as Flash Sword and the soon to exist Arcane Barrage
			mAffectedSpell = event.getAbility();
			ArcaneBarrage barrage = AbilityManager.getManager().getPlayerAbility(mPlayer, ArcaneBarrage.class);
			if (mAffectedSpell == Spells.ARCANE_BARRAGE) {
				mAffectedSpell = null;
				AbilityManager.getManager().getPlayerAbility(mPlayer, ArcaneBarrage.class).activateOverload();
			} else if (mAffectedSpell == Spells.MANA_LANCE && barrage != null && barrage.getAbilityScore() == 2 && mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.ARCANE_BARRAGE) && barrage.getMissiles() > 0) {
				mAffectedSpell = null;
				mSpellsToOverload = 1;
			} else {
				new BukkitRunnable() {
					@Override
					public void run() {
						mAffectedSpell = null;
					}
				}.runTaskLater(mPlugin, 40);
			}
		}

		if (mSpellsToOverload == 1) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2, 1.2f);
		}

		return true;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		LivingEntity damagee = event.getDamaged();
		Location locD = damagee.getLocation().add(0, 1, 0);
		if (event.getSpell() == mAffectedSpell) {
			event.setDamage(event.getDamage() + mDamage);
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.REDSTONE, locD, 35, 0.4, 0.4, 0.4, COLOR);
			world.spawnParticle(Particle.EXPLOSION_NORMAL, locD, 35, 0, 0, 0, 0.2);
		}
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (mSpellsToOverload == 1 && event.getCause() == DamageCause.ENTITY_ATTACK) {
			Entity entity = event.getDamager();
			if (entity instanceof LivingEntity && !EntityUtils.isElite(entity) && !EntityUtils.isBoss(entity)) {
				stun((LivingEntity) entity);
			}
		}

		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		if (mSpellsToOverload == 1) {
			ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
			if (source instanceof LivingEntity) {
				LivingEntity mob = (LivingEntity) source;
				if (!EntityUtils.isElite(mob) && !EntityUtils.isBoss(mob)) {
					stun(mob);
				}
			}
		}

		return true;
	}

	private void stun(LivingEntity mob) {
		mob.getWorld().playSound(mob.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 1.5f);
		EntityUtils.applyStun(mPlugin, STUN_DURATION, mob);
	}

}
