package com.playmonumenta.plugins.depths.abilities.aspects;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.utils.ItemUtils;

public class AxeAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Axe";
	public static final int DAMAGE = 1;
	public static final double ATTACK_SPEED = 0.15;

	public AxeAspect(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.IRON_AXE;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {

		if (event.getCause().equals(DamageCause.ENTITY_ATTACK) && ItemUtils.isAxe(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(event.getDamage() + DAMAGE);
		}

		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mPlayer != null && ItemUtils.isAxe(mPlayer.getInventory().getItemInMainHand())) {
			mPlugin.mEffectManager.addEffect(mPlayer, ABILITY_NAME,
			                                 new PercentAttackSpeed(40, ATTACK_SPEED, ABILITY_NAME));
		}
	}

	@Override
	public String getDescription(int rarity) {
		return "You deal " + DAMAGE + " extra damage with axe attacks and gain " + (int) DepthsUtils.roundPercent(ATTACK_SPEED) + "% attack speed.";
	}
}

