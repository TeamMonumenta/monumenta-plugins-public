package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseOfTheVeil implements Enchantment {
	// How much damage should be dealt every damage tick
	private static final double DAMAGE_PER_TICK = 1.0;

	// How many seconds between each damage tick
	private static final int SECONDS = 3;

	// Ranges for when activation should be turned off if no entities are within them
	private static final double NEARBY_BOSS_RANGE = 60;
	private static final double NEARBY_NORMAL_RANGE = 24;

	// How many minimum damage ticks should be dealt after the player damages an entity, even if they are out of range
	private static final int TICKS_AFTER_DAMAGING = 4;

	private int mDamageCounter = 0;
	private int mLastDamageTick = 0;

	private final Map<UUID, Integer> mDamageUntil = new HashMap<>();

	@Override
	public String getName() {
		return "Curse of the Veil";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.OFFHAND, Slot.MAINHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_THE_VEIL;
	}

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			if (mDamageCounter == 0) {
				@Nullable PotionEffect resistance = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
				List<EffectManager.EffectPair> effectPairs = plugin.mEffectManager.getEffectPairs(player);
				if (player.isDead()
					|| player.getGameMode() == GameMode.CREATIVE
					|| player.getGameMode() == GameMode.SPECTATOR
					|| (resistance != null && resistance.getAmplifier() >= 4)
					|| (effectPairs != null && effectPairs.stream().anyMatch(pair ->
						pair.mEffect.mEffectID.equals(RespawnStasis.effectID)
						|| pair.mEffect.mEffectID.equals(Stasis.effectID)
						|| (pair.mEffect.mEffectID.equals(PercentDamageReceived.effectID)
							&& pair.mEffect.getMagnitude() >= 1
							&& pair.mEffect.isBuff())))) {
					return;
				}

				// Check if the player should be damaged, or if just an indicator should be shown
				if (shouldActivate(player)) {
					double newPlayerHealth = player.getHealth() - DAMAGE_PER_TICK * level;
					if (newPlayerHealth < 1) {
						newPlayerHealth = 1;
					}
					player.setHealth(newPlayerHealth);

					new PartialParticle(Particle.DUST_COLOR_TRANSITION, LocationUtils.getHalfHeightLocation(player))
						.count(10)
						.delta(0.4, 0.8, 0.4)
						.data(new Particle.DustTransition(
							Color.fromRGB(201, 48, 28),
							Color.fromRGB(117, 49, 28),
							1f))
						.spawnAsPlayerBuff(player);
					player.playSound(player.getLocation(), Sound.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.PLAYERS, 0.7f, 0.6f);
				} else {
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, LocationUtils.getHalfHeightLocation(player))
						.count(10)
						.delta(0.4, 0.8, 0.4)
						.data(new Particle.DustTransition(
							Color.fromRGB(92, 17, 49),
							Color.fromRGB(64, 6, 43),
							1f))
						.spawnAsPlayerBuff(player);
					player.playSound(player.getLocation(), Sound.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.PLAYERS, 0.35f, 0.9f);
				}
			}
			if (Bukkit.getCurrentTick() > mLastDamageTick) {
				mDamageCounter = (mDamageCounter + 1) % SECONDS;
				mLastDamageTick = Bukkit.getCurrentTick();
			}
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		Enchantment.super.onDamage(plugin, player, value, event, enemy);

		mDamageUntil.put(player.getUniqueId(), Bukkit.getCurrentTick() + TICKS_AFTER_DAMAGING * SECONDS * 20);
	}

	@Override
	public void onDeath(Plugin plugin, Player player, double value, PlayerDeathEvent event) {
		Enchantment.super.onDeath(plugin, player, value, event);

		mDamageUntil.remove(player.getUniqueId());
	}

	private boolean shouldActivate(Player player) {
		// Check if player has damaged anything recently
		if (mDamageUntil.containsKey(player.getUniqueId())) {
			if (Bukkit.getCurrentTick() <= mDamageUntil.get(player.getUniqueId())) {
				return true;
			} else {
				mDamageUntil.remove(player.getUniqueId());
			}
		}

		// Check nearby entities
		Collection<LivingEntity> nearbyEntities = player.getLocation().getNearbyLivingEntities(NEARBY_BOSS_RANGE);
		for (LivingEntity entity : nearbyEntities) {
			if (!(entity instanceof Player)
				&& (entity.getScoreboardTags().contains("Boss")
				|| entity.getLocation().distanceSquared(player.getLocation()) < NEARBY_NORMAL_RANGE * NEARBY_NORMAL_RANGE)) {
				return true;
			}
		}
		return false;
	}
}
