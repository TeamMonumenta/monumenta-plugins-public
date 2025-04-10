package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.effects.RetaliationEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.Nullable;

public class Retaliation implements Enchantment {
	private static final String[] VALID_DEBUFFS = {"fire", "CustomSlow", "slow", "wither", "poison", "hunger", "CustomAntiHeal", "silence"};
	private static final String EFFECT_SOURCE = "RetaliationEffect";
	private static final int EFFECT_DURATION = 4 * 20;
	private static final double BASE_DAMAGE = 0.25;
	private static final double ELITE_BONUS = 0.15;
	private static final double BOSS_BONUS = 0.25;

	private int mLastProcTick = 0;

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

		startEffect(player, event.getEffects(), event.getDamager(), false);
	}

	public void startEffect(Player player, List<EffectsList.Effect> effectsList, @Nullable Entity damager, boolean isBossAbility) {
		if (Bukkit.getCurrentTick() - mLastProcTick < 10) {
			return;
		}
		mLastProcTick = Bukkit.getCurrentTick();

		// Clear active effect to apply new debuffs
		Plugin.getInstance().mEffectManager.clearEffects(player, EFFECT_SOURCE);

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
				case WITHER_SKELETON, WITHER_SKULL -> debuffTypes.append("wither");
				case BEE, CAVE_SPIDER, PUFFERFISH -> debuffTypes.append("poison");
				case HUSK -> debuffTypes.append("hunger");
				default -> {}
			}
		}

		if (damager instanceof Projectile projectile) {
			damager = (Entity) projectile.getShooter();
			// Blazes and flame arrows
			if (projectile.getFireTicks() > 0) {
				debuffTypes.append("fire");
			}
			// Stray arrows
			if (damager != null && damager.getType() == EntityType.STRAY) {
				debuffTypes.append("slowness");
			}
		}

		double damage = BASE_DAMAGE + (damager != null && EntityUtils.isElite(damager) ? ELITE_BONUS : (damager != null && EntityUtils.isBoss(damager) ? BOSS_BONUS : 0));

		if (damage == 0.5) {
			player.playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.4f);
			player.playSound(player, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 0.65f, 1.2f);
		} else if (damage == 0.4) {
			player.playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.6f);
			player.playSound(player, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 0.5f, 1.4f);
		} else {
			player.playSound(player, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 1.6f);
			player.playSound(player, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.8f, 1.0f);
		}

		Plugin.getInstance().mEffectManager.addEffect(player, EFFECT_SOURCE, new RetaliationEffect(EFFECT_DURATION, damage, debuffTypes.toString(), getDebuffIcons(debuffTypes.toString())));
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
