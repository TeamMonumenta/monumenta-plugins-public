package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Bleed;
import com.playmonumenta.plugins.effects.CholericFlamesAntiHeal;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;

import static com.playmonumenta.plugins.abilities.warlock.CholericFlames.ANTIHEAL_EFFECT;

public class Infection implements Enchantment {

	private static final int MOB_SPREAD_PER_LEVEL = 1;
	private static final int DURATION_INCREASE_PER_LEVEL = 20;
	private static final int BASE_CUSTOM_DURATION = 20;
	private static final int SPREAD_RADIUS = 2;
	private static final String DAMAGED_THIS_TICK_METADATA = "InfectionThisTick";
	private static final String DAMAGE_DEALT_METADATA = "InfectionDamageDealt";

	private static final EnumSet<DamageEvent.DamageType> ACTIVATION_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.MELEE_ENCH,
		DamageEvent.DamageType.PROJECTILE,
		DamageEvent.DamageType.PROJECTILE_SKILL
	);

	@Override
	public String getName() {
		return "Infection";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.INFECTION;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}


	@Override
	public void onKill(Plugin plugin, Player player, double level, EntityDeathEvent event, LivingEntity enemy) {
		EntityDamageEvent e = enemy.getLastDamageCause();
		if (e != null && (MetadataUtils.happenedThisTick(enemy, DAMAGED_THIS_TICK_METADATA) && enemy.hasMetadata(DAMAGE_DEALT_METADATA))) {
			//vanilla debuffs
			Collection<PotionEffect> mobEffects = event.getEntity().getActivePotionEffects();

			// Other debuffs
			List<EffectManager.EffectPair> unfilteredEffectPairList = EffectManager.getInstance().getEffectPairs(enemy);
			Map<String, Double> effectPairList = new HashMap<>();

			if (unfilteredEffectPairList != null) {
				for (EffectManager.EffectPair ef : unfilteredEffectPairList) {
					effectPairList.put(ef.mSource(), ef.mEffect().getMagnitude());
				}
			}
			Double bleed = effectPairList.get(Bleed.BLEED_EFFECT_NAME);
			Double slow = effectPairList.get(EntityUtils.SLOW_EFFECT_NAME);
			Double weaken = effectPairList.get(EntityUtils.WEAKEN_EFFECT_NAME);
			Double vulnerable = effectPairList.get(EntityUtils.VULNERABILITY_EFFECT_NAME);
			Double cholericFlames = effectPairList.get(ANTIHEAL_EFFECT);
			boolean customdebuffs = (
				bleed != null
				|| slow != null
				|| weaken != null
				|| vulnerable != null
				|| cholericFlames != null
			);
			boolean vanilladebuffs = false;
			int applications = 0;

			for (LivingEntity entity : EntityUtils.getNearbyMobs(event.getEntity().getLocation(), SPREAD_RADIUS)) {
				if (applications < level * MOB_SPREAD_PER_LEVEL) {
					for (PotionEffect effect : mobEffects) {
						if (AbilityUtils.DEBUFFS.contains(effect.getType())) {
							vanilladebuffs = true;
							entity.addPotionEffect(new PotionEffect(effect.getType(), effect.getDuration() + (DURATION_INCREASE_PER_LEVEL * (int) level), effect.getAmplifier()));
						}
					}
					if (customdebuffs) {
						applyCustomDebuffs(plugin, player, level, entity, effectPairList);
					}
					if (customdebuffs || vanilladebuffs) {
						playSounds(entity);
					}
					applications++;
				}
			}
		}
	}

	public void playSounds(LivingEntity entity) {
		entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_SCULK_SPREAD, SoundCategory.PLAYERS, 0.8f, 0.6f);
		entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 0.5f, 1.35f);
	}

	public void applyCustomDebuffs(Plugin plugin, Player player, Double level, LivingEntity entity,
								   Map<String, Double> effectPairList) {

		if (effectPairList.get(Bleed.BLEED_EFFECT_NAME) != null) {
			EntityUtils.applyBleed(plugin, player, entity, effectPairList.get(Bleed.BLEED_EFFECT_NAME).intValue());
		}
		if (effectPairList.get(EntityUtils.SLOW_EFFECT_NAME) != null) {
			EntityUtils.applySlow(plugin, BASE_CUSTOM_DURATION + DURATION_INCREASE_PER_LEVEL * level.intValue(), effectPairList.get(EntityUtils.SLOW_EFFECT_NAME), entity);
		}
		if (effectPairList.get(EntityUtils.WEAKEN_EFFECT_NAME) != null) {
			EntityUtils.applyWeaken(plugin, BASE_CUSTOM_DURATION + DURATION_INCREASE_PER_LEVEL * level.intValue(), effectPairList.get(EntityUtils.WEAKEN_EFFECT_NAME), entity);
		}
		if (effectPairList.get(EntityUtils.VULNERABILITY_EFFECT_NAME) != null) {
			EntityUtils.applyVulnerability(plugin, BASE_CUSTOM_DURATION + DURATION_INCREASE_PER_LEVEL * level.intValue(), effectPairList.get(EntityUtils.VULNERABILITY_EFFECT_NAME), entity);
		}
		if (effectPairList.get(ANTIHEAL_EFFECT) != null) {
			plugin.mEffectManager.addEffect(entity, ANTIHEAL_EFFECT, new CholericFlamesAntiHeal(BASE_CUSTOM_DURATION + DURATION_INCREASE_PER_LEVEL * level.intValue()));
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (ACTIVATION_DAMAGE_TYPES.contains(event.getType())) {
			double damage = event.getDamage();
			if (MetadataUtils.checkOnceThisTick(plugin, enemy, DAMAGED_THIS_TICK_METADATA) && enemy.hasMetadata(DAMAGE_DEALT_METADATA) && enemy.getMetadata(DAMAGE_DEALT_METADATA).getFirst().asDouble() > damage) {
				return;
			}
			enemy.setMetadata(DAMAGE_DEALT_METADATA, new FixedMetadataValue(plugin, damage));
		}
	}
}

