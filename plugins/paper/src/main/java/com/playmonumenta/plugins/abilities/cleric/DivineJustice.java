package com.playmonumenta.plugins.abilities.cleric;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class DivineJustice extends Ability {

	private static final int DIVINE_JUSTICE_DAMAGE = 5;
	private static final int DIVINE_JUSTICE_HEAL = 4;
	private static final int DIVINE_JUSTICE_CRIT_HEAL = 1;

	public DivineJustice(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Divine Justice");
		mInfo.scoreboardId = "DivineJustice";
		mInfo.mShorthandName = "DJ";
		mInfo.mDescriptions.add("Your critical strikes deal 5 additional damage to undead enemies. When you critically strike an undead enemy, you heal yourself for 0.5 hearts.");
		mInfo.mDescriptions.add("In addition, when you kill an undead enemy with a critical strike, you heal yourself for 1.5 hearts.");
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity damagee = (LivingEntity)event.getEntity();
		if (event.getCause() == DamageCause.ENTITY_ATTACK && EntityUtils.isUndead(damagee)) {
			event.setDamage(event.getDamage() + DIVINE_JUSTICE_DAMAGE);

			PlayerUtils.healPlayer(mPlayer, DIVINE_JUSTICE_CRIT_HEAL);

			World world = mPlayer.getWorld();
			Location loc = damagee.getLocation();
			world.spawnParticle(Particle.END_ROD, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 5,
			                    (damagee.getWidth() / 2) + 0.1, damagee.getHeight() / 3, (damagee.getWidth() / 2) + 0.1, 0.065);
			world.spawnParticle(Particle.FLAME, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 6,
			                    (damagee.getWidth() / 2) + 0.1, damagee.getHeight() / 3, (damagee.getWidth() / 2) + 0.1, 0.05);
			world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.15f, 1.5f);
		}
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity killedEntity = event.getEntity();
		if (getAbilityScore() > 1
		    && killedEntity.getLastDamageCause() != null
		    && !killedEntity.getLastDamageCause().getCause().equals(DamageCause.PROJECTILE)
		    && EntityUtils.isUndead(killedEntity)) {
			PlayerUtils.healPlayer(mPlayer, DIVINE_JUSTICE_HEAL);

			World world = mPlayer.getWorld();
			Location loc = killedEntity.getLocation();
			world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1, 0), 16, 0.4, 0.4, 0.4, 1);
			world.spawnParticle(Particle.END_ROD, killedEntity.getLocation().add(0, killedEntity.getHeight() / 2, 0), 5,
			                    (killedEntity.getWidth() / 2) + 0.1, killedEntity.getHeight() / 3, (killedEntity.getWidth() / 2) + 0.1, 0.125);
			world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.65f, 1.5f);
		}
	}

	@Override
	public boolean runCheck() {
		return PlayerUtils.isCritical(mPlayer);
	}

}
