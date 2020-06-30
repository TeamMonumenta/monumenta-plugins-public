package com.playmonumenta.plugins.abilities.cleric;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class DivineJustice extends Ability {

	private static final int DIVINE_JUSTICE_CRIT_DAMAGE = 5;
	private static final int DIVINE_JUSTICE_CRIT_HEAL = 1;
	private static final int DIVINE_JUSTICE_KILL_HEAL = 3;

	public DivineJustice(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Divine Justice");
		mInfo.mScoreboardId = "DivineJustice";
		mInfo.mShorthandName = "DJ";
		mInfo.mDescriptions.add("Your critical strikes deal 5 additional damage to undead enemies and heal you for 1 health.");
		mInfo.mDescriptions.add("In addition, when you kill an undead enemy with a critical strike, heal yourself for 3 health.");
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity damagee = (LivingEntity) event.getEntity();
		if (event.getCause() == DamageCause.ENTITY_ATTACK && EntityUtils.isUndead(damagee)) {
			event.setDamage(event.getDamage() + DIVINE_JUSTICE_CRIT_DAMAGE);
			PlayerUtils.healPlayer(mPlayer, DIVINE_JUSTICE_CRIT_HEAL);

			Location loc = damagee.getLocation().add(0, damagee.getHeight() / 2, 0);
			double xz = damagee.getWidth() / 2 + 0.1;
			double y = damagee.getHeight() / 3;
			mWorld.spawnParticle(Particle.END_ROD, loc, 5, xz, y, xz, 0.065);
			mWorld.spawnParticle(Particle.FLAME, loc, 6, xz, y, xz, 0.05);
			mWorld.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.15f, 1.5f);

			/*
			 * Check for entity death here since Player.isCritical() checks for player attack
			 * meter being full, which is not the case when the death event triggers
			 */
			new BukkitRunnable() {
				@Override
				public void run() {
					if (damagee.isDead() && mPlayer.equals(damagee.getKiller())) {
						PlayerUtils.healPlayer(mPlayer, DIVINE_JUSTICE_KILL_HEAL);
						mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1, 0), 16, 0.4, 0.4, 0.4, 1);
						mWorld.spawnParticle(Particle.END_ROD, loc, 5, xz, y, xz, 0.125);
						mWorld.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.65f, 1.5f);
					}
				}
			}.runTaskLater(mPlugin, 0);
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		return PlayerUtils.isCritical(mPlayer);
	}

}
