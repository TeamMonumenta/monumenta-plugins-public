package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SwordAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Sword";
	public static final double DAMAGE = 2.5;

	public static final DepthsAbilityInfo<SwordAspect> INFO =
		new DepthsAbilityInfo<>(SwordAspect.class, ABILITY_NAME, SwordAspect::new, null, DepthsTrigger.WEAPON_ASPECT)
			.displayItem(new ItemStack(Material.IRON_SWORD))
			.description("You deal 2.5 extra melee damage when holding a sword.");

	public SwordAspect(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(event.getDamage() + DAMAGE);
		}
		return false; // only changes event damage
	}

}

