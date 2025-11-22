package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.ZeroArgumentEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Reflexes implements Enchantment {
	public static final int REFLEXES_TOTAL_DURATION = 20; // 20 ticks = 1 second
	public static final int REFLEXES_MAIN_DURATION = 13; // 13 ticks = 0.65 seconds
	private static final String REFLEXES_EFFECT_NAME = "ReflexesEffect";
	private static final List<ClassAbility> DOT_ABILITY_LIST = List.of(
		ClassAbility.BLIZZARD,
		ClassAbility.SPELLSHOCK_ARCANE,
		ClassAbility.ELEMENTAL_SPIRIT_ICE,
		ClassAbility.BRUTAL_ALCHEMY,
		ClassAbility.PANACEA,
		ClassAbility.SCORCHED_EARTH,
		ClassAbility.DECAYED_TOTEM,
		ClassAbility.FLAME_TOTEM,
		ClassAbility.LIGHTNING_TOTEM,
		ClassAbility.INTERCONNECTED_HAVOC,
		ClassAbility.CRYSTALLINE_COMBOS,
		ClassAbility.SANCTIFIED_ARMOR,
		ClassAbility.ILLUMINATE_DOT,
		ClassAbility.KEEPER_VIRTUE,
		ClassAbility.BRUTE_FORCE,
		ClassAbility.BRUTE_FORCE_AOE,
		ClassAbility.HUNTING_COMPANION,
		ClassAbility.PHLEGMATIC_RESOLVE,
		ClassAbility.CURSED_WOUND,
		ClassAbility.VOODOO_BONDS, // VOODOO_BONDS_PIN is allowed
		ClassAbility.RESTLESS_SOULS,
		ClassAbility.BRAMBLE_SHELL,
		ClassAbility.SNOWSTORM,
		ClassAbility.REFRACTION,
		ClassAbility.DISCO_BALL,
		ClassAbility.INFERNO,
		ClassAbility.REVERB,
		ClassAbility.REFLECTION
	);
	private static final List<DamageType> ALLOWED_TYPES = List.of(
		DamageType.MELEE,
		DamageType.MELEE_ENCH,
		DamageType.MELEE_SKILL,
		DamageType.PROJECTILE,
		DamageType.PROJECTILE_SKILL,
		DamageType.PROJECTILE_ENCH,
		DamageType.MAGIC
	);

	@Override
	public String getName() {
		return "Reflexes";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REFLEXES;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		ClassAbility eventAbility = event.getAbility();
		if (eventAbility != null && DOT_ABILITY_LIST.contains(eventAbility)) {
			return;
		}
		if (!ALLOWED_TYPES.contains(event.getType()) && eventAbility != ClassAbility.ERUPTION) { // ALLOW reflexes to work with Eruption as a special interaction
			return;
		}
		if (event.getDamage() < 0.01) {
			return;
		}
		plugin.mEffectManager.addEffect(player, REFLEXES_EFFECT_NAME, new ZeroArgumentEffect(REFLEXES_TOTAL_DURATION, REFLEXES_EFFECT_NAME) {
			@Override
			public String toString() {
				return String.format("%s duration:%d", REFLEXES_EFFECT_NAME, getDuration());
			}
		});
	}

	public static double applyReflexes(DamageEvent event, Plugin plugin, Player player) {
		Effect reflexesEffect = plugin.mEffectManager.getActiveEffect(player, REFLEXES_EFFECT_NAME);
		if (reflexesEffect == null) {
			return 0;
		}
		if (reflexesEffect.getDuration() <= REFLEXES_TOTAL_DURATION - REFLEXES_MAIN_DURATION) {
			return (double) plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.REFLEXES) / 2;
		}
		return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.REFLEXES);
	}
}
