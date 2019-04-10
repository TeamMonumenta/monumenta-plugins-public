package com.playmonumenta.plugins.abilities.rogue.assassin;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Perfect Kill: While sprinting, instantly
 * kill a non-boss/elite mob with a melee attack.
 * At level 2, gain +5 damage for 5 seconds after
 * activating the ability. (Cooldown: 30 / 25 seconds)
 */

public class PerfectKill extends Ability {

	private static final int PERFECT_1_COOLDOWN = 20 * 30;
	private static final int PERFECT_2_COOLDOWN = 20 * 25;
	private static final int PERFECT_DAMAGE_BONUS = 5;
	private static final int PERFECT_DAMAGE_BONUS_DURATION = 20 * 5;
	private static final int HADOUKEN_LASER = 9001;

	private boolean active = false;

	public PerfectKill(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "PerfectKill";
		mInfo.linkedSpell = Spells.PERFECT_KILL;
		mInfo.cooldown = getAbilityScore() == 1 ? PERFECT_1_COOLDOWN : PERFECT_2_COOLDOWN;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (active) {
			event.setDamage(event.getDamage() + PERFECT_DAMAGE_BONUS);
		}

		if (mPlayer.isSprinting() && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.PERFECT_KILL)) {
			LivingEntity le = (LivingEntity) event.getEntity();
			if (!EntityUtils.isBoss(le) && !EntityUtils.isElite(le)) {
				//Setting health does not count for a kill. Deal damage beyond god-level tiers
				EntityUtils.damageEntity(mPlugin, le, HADOUKEN_LASER, mPlayer);
				le.getWorld().playSound(le.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1, 1.75f);
				mWorld.spawnParticle(Particle.SPELL_WITCH, le.getLocation().add(0, 1.15, 0), 50, 0.3, 0.35, 0.3, 1);
				mWorld.spawnParticle(Particle.SPELL_MOB, le.getLocation().add(0, 1.15, 0), 50, 0.2, 0.35, 0.2, 0);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, le.getLocation().add(0, 1.15, 0), 5, 0.3, 0.35, 0.3, 0);
				if (getAbilityScore() > 1) {
					active = true;
					new BukkitRunnable() {
						int t = 0;
						@Override
						public void run() {
							t++;
							mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation(), 1, 0.3, 0.35, 0.3, 0);
							if (t > PERFECT_DAMAGE_BONUS_DURATION) {
								active = false;
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);
				}

				putOnCooldown();
			}
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return InventoryUtils.isSwordItem(mHand) && InventoryUtils.isSwordItem(oHand);
	}

}
