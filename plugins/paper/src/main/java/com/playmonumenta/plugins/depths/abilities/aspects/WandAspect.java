package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WandAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Wand";
	public static final int DAMAGE = 1;
	public static final double SPELL_MOD = 0.35;

	public static final DepthsAbilityInfo<WandAspect> INFO =
		new DepthsAbilityInfo<>(WandAspect.class, ABILITY_NAME, WandAspect::new, null, DepthsTrigger.WEAPON_ASPECT)
			.displayItem(new ItemStack(Material.STICK))
			.description("You deal " + DAMAGE + " extra melee damage while holding a wand, and all ability damage is increased by " + (int) (SPELL_MOD * 100) + "% of the wand's spell power.");

	public WandAspect(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(ItemStatUtils.EnchantmentType.MAGIC_WAND) > 0) {
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

}

