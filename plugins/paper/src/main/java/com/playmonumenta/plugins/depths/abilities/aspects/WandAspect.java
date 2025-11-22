package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class WandAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Wand";
	public static final int DAMAGE = 1;
	public static final double SPELL_MOD = 0.35;

	public static final DepthsAbilityInfo<WandAspect> INFO =
		new DepthsAbilityInfo<>(WandAspect.class, ABILITY_NAME, WandAspect::new, null, DepthsTrigger.WEAPON_ASPECT)
			.displayItem(Material.STICK)
			.descriptions(WandAspect::getDescription);

	public WandAspect(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(EnchantmentType.MAGIC_WAND) > 0) {
			if (event.getType() == DamageType.MELEE) {
				event.setFlatDamage(event.getFlatDamage() + DAMAGE);
			} else if (event.getAbility() != null && !event.getAbility().isFake() && event.getType() == DamageType.MAGIC) {
				float spellMultiplier = SpellPower.getSpellDamage(mPlugin, mPlayer, 1);
				event.updateDamageWithMultiplier(1 + (spellMultiplier - 1) * SPELL_MOD);
			}
		}
		return false; // only changes event damage
	}

	public static Description<WandAspect> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("You deal " + DAMAGE + " extra melee damage while holding a wand, and magic damage is increased by ")
			.addPercent(SPELL_MOD)
			.add(" of the wand's Spell Power.");
	}
}

