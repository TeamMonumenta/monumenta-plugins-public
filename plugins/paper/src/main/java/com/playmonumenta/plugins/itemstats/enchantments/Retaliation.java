package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.effects.RetaliationEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Retaliation implements Enchantment {
	private static final String[] VALID_DEBUFFS = {"fire", "CustomSlow", "slow", "wither", "poison", "hunger", "CustomAntiHeal", "silence"};

	private static final String EFFECT_SOURCE = "RetaliationEffect";
	private static final int EFFECT_DURATION = 5 * 20;
	private static final double EFFECT_CONVERSION = 1.00; // % damage dealt bonus per damage taken
	private static final double MAX_DAMAGE = 40; // max damage to consider for damage bonus

	@Override
	public String getName() {
		return "Retaliation";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.OFFHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RETALIATION;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!event.isBlockedByShield()) {
			return;
		}
		// Reset Retaliation to allow it to change effect
		plugin.mEffectManager.clearEffects(player, EFFECT_SOURCE);

		startEffect(player, event.getEffects(), event.getOriginalDamage(), event.getDamager(), false);
	}

	public void startEffect(Player player, List<EffectsList.Effect> effectsList, double damage, @Nullable Entity damager, boolean isBossAbility) {
		// Round down to nearest multiple of 5% and subtract 5%, but keep a lower cap of 10%
		double damageBonus = Math.min((damage - damage % 5) - 5, MAX_DAMAGE) * EFFECT_CONVERSION / 100;
		damageBonus = Math.max(damageBonus, 0.1);

		if (damageBonus >= 0.4) {
			player.playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.4f);
			player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 0.8f, 1.4f);
			player.playSound(player, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.HOSTILE, 1f, 0.5f);
			player.playSound(player, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 1f, 1.8f);
			player.playSound(player, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 0.7f, 1.2f);
			player.playSound(player, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 1f, 0.7f);
		} else if (damageBonus >= 0.3) {
			player.playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.6f);
			player.playSound(player, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.HOSTILE, 1f, 0.5f);
			player.playSound(player, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 1f, 1.4f);
			player.playSound(player, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 0.6f, 1.4f);
			player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 0.5f, 1.2f);
		} else if (damageBonus >= 0.2) {
			player.playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.6f);
			player.playSound(player, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.HOSTILE, 1f, 0.5f);
			player.playSound(player, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 1f, 1.2f);
			player.playSound(player, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 0.2f, 1.6f);
			player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 0.3f, 1f);
		} else {
			player.playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 0.8f, 1.6f);
			player.playSound(player, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.HOSTILE, 0.8f, 0.5f);
			player.playSound(player, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 1f, 1f);
		}

		// Bosstag effects
		StringBuilder debuffTypes = new StringBuilder();
		for (EffectsList.Effect effect : effectsList) {
			if (!Arrays.stream(VALID_DEBUFFS).toList().contains(effect.mName)) {
				continue;
			}
			debuffTypes.append(effect.mName);
		}

		// Mob type effects, don't apply if this was triggered by an ability
		if (damager != null && !isBossAbility) {
			switch (damager.getType()) {
				case SMALL_FIREBALL -> debuffTypes.append("fire");
				case WITHER_SKELETON, WITHER_SKULL -> debuffTypes.append("wither");
				case BEE, CAVE_SPIDER, PUFFERFISH -> debuffTypes.append("poison");
				case HUSK -> debuffTypes.append("hunger");
				default -> {}
			}
		}
		Plugin.getInstance().mEffectManager.addEffect(player, EFFECT_SOURCE, new RetaliationEffect(EFFECT_DURATION, damageBonus, debuffTypes.toString(), getDebuffIcons(debuffTypes.toString())));
	}

	public String getDebuffIcons(String debuffTypes) {
		StringBuilder icons = new StringBuilder();

		if (debuffTypes.contains("fire")) {
			icons.append("\uD83D\uDD25");
		}
		if (debuffTypes.contains("CustomSlow") || debuffTypes.contains("slowness")) {
			icons.append("⚓");
		}
		if (debuffTypes.contains("wither") || debuffTypes.contains("poison")) {
			icons.append("☠");
		}
		if (debuffTypes.contains("hunger")) {
			icons.append("\uD83C\uDF56");
		}
		if (debuffTypes.contains("CustomAntiHeal")) {
			icons.append("❤");
		}
		if (debuffTypes.contains("silence")) {
			icons.append("⏳");
		}

		return icons.toString();
	}
}
