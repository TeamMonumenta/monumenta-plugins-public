package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AxeAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Axe";
	public static final int DAMAGE = 1;
	public static final double ATTACK_SPEED = 0.15;

	public static final DepthsAbilityInfo<AxeAspect> INFO =
		new DepthsAbilityInfo<>(AxeAspect.class, ABILITY_NAME, AxeAspect::new, null, DepthsTrigger.WEAPON_ASPECT)
			.displayItem(new ItemStack(Material.IRON_AXE))
			.description("You deal " + DAMAGE + " extra melee damage and gain " + StringUtils.multiplierToPercentage(ATTACK_SPEED) + "% attack speed when holding an axe.");

	public AxeAspect(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && ItemUtils.isAxe(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(event.getDamage() + DAMAGE);
		}
		return false; // only changes event damage
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (ItemUtils.isAxe(mPlayer.getInventory().getItemInMainHand())) {
			mPlugin.mEffectManager.addEffect(mPlayer, ABILITY_NAME, new PercentAttackSpeed(6, ATTACK_SPEED, ABILITY_NAME).displaysTime(false));
		}
	}

}

