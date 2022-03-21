package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SwordAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Sword";
	public static final double DAMAGE = 1.5;

	public SwordAspect(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.IRON_SWORD;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer != null && event.getType() == DamageType.MELEE && ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(event.getDamage() + DAMAGE);
		}
		return false; // only changes event damage
	}

	@Override
	public String getDescription(int rarity) {
		return "You deal 1.5 extra melee damage when holding a sword.";
	}
}

