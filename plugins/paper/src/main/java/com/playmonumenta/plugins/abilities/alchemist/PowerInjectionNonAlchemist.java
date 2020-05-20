package com.playmonumenta.plugins.abilities.alchemist;


import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

public class PowerInjectionNonAlchemist extends Ability {

	private int mBonusDamage = 0;

	public PowerInjectionNonAlchemist(Plugin plugin, World world, Player player) {
		super(plugin, world, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			event.setDamage(event.getDamage() + mBonusDamage);
		}

		return true;
	}

	public void applyBonusDamage(int damage) {
		mBonusDamage = damage;

		new BukkitRunnable() {
			@Override
			public void run() {
				mBonusDamage = 0;
			}
		}.runTaskLater(mPlugin, PowerInjection.POWER_INJECTION_DURATION);
	}
}
