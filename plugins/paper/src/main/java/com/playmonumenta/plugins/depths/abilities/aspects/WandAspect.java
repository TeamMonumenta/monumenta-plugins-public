package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import net.kyori.adventure.text.Component;
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
			.gain(WandAspect::gain)
			.descriptions(WandAspect::getDescription);

	public WandAspect(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mWandAspectCharges = 3;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlugin.mItemStatManager.getPlayerItemStats(mPlayer).getItemStats().get(EnchantmentType.MAGIC_WAND) > 0) {
			if (event.getType() == DamageType.MELEE) {
				event.setFlatDamage(event.getFlatDamage() + DAMAGE);
			} else if (event.getAbility() != null && !event.getAbility().isFake()) {
				float spellMultiplier = SpellPower.getSpellDamage(mPlugin, mPlayer, 1);
				event.updateDamageWithMultiplier(1 + (spellMultiplier - 1) * SPELL_MOD);
			}
		}
		return false; // only changes event damage
	}

	public static Description<WandAspect> getDescription() {
		return new DescriptionBuilder<WandAspect>()
			.add("You deal " + DAMAGE + " extra melee damage while holding a wand, and all ability damage is increased by ")
			.addPercent(SPELL_MOD)
			.add(" of the wand's Spell Power. The next three abilities you find are guaranteed to be in active trigger slots.")
			.add((a, p) -> {
				if (p == null) {
					return Component.empty();
				}
				DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(p);
				if (dp == null) {
					return Component.empty();
				}
				return Component.text("\nRemaining uses: " + dp.mWandAspectCharges);
			});
	}

}

