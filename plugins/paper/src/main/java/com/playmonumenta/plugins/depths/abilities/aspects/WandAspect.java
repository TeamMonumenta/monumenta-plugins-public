package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class WandAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Wand";
	public static final int DAMAGE = 1;
	public static final double SPELL_MOD = 0.35;

	public WandAspect(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.STICK;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer != null && ItemUtils.isWand(mPlayer.getInventory().getItemInMainHand())) {
			if (event.getType() == DamageType.MELEE) {
				event.setDamage(event.getDamage() + DAMAGE);
			} else if (event.getAbility() != null) {
				double initialDamage = event.getDamage();

				//Find out what the damage with full spell power would be here
				float fullDamage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) initialDamage);

				//Get the difference, divide it by 4 and add it to the damage
				event.setDamage(initialDamage + ((fullDamage - initialDamage) * SPELL_MOD));
			}
		}
		return false; // only changes event damage
	}

	@Override
	public String getDescription(int rarity) {
		return "You deal " + DAMAGE + " extra damage with wand attacks, and all abilities casted with a wand benefit from " + (int) (SPELL_MOD * 100) + "% of the wands spell power.";
	}
}

