package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseOfTheVeil implements Enchantment {
	private static final double DAMAGE_PER_SECOND = 0.5;

	// How many seconds between each damage tick
	private static final int SECONDS = 3;

	private int mDamageCounter = 0;
	private int mLastDamageTick = 0;

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
			if (Bukkit.getCurrentTick() > mLastDamageTick) {
				mDamageCounter = (mDamageCounter + 1) % SECONDS;
				mLastDamageTick = Bukkit.getCurrentTick();
			}
			if (mDamageCounter == 0) {
				@Nullable PotionEffect resistance = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
				List<EffectManager.EffectPair> effectPairs = plugin.mEffectManager.getEffectPairs(player);
				if (player.isDead()
					|| player.getGameMode() == GameMode.CREATIVE
					|| player.getGameMode() == GameMode.SPECTATOR
					|| (resistance != null && resistance.getAmplifier() >= 4)
					|| (effectPairs != null && effectPairs.stream().anyMatch(pair ->
						pair.mEffect.mEffectID.equals(PercentDamageReceived.effectID)
						&& pair.mEffect.getMagnitude() >= 1
						&& pair.mEffect.isBuff()))) {
					return;
				}

				double newPlayerHealth = player.getHealth() - harmPerTick(level);
				if (newPlayerHealth < 1) {
					newPlayerHealth = 1;
				}
				player.setHealth(newPlayerHealth);

				new PartialParticle(Particle.DUST_COLOR_TRANSITION, player.getLocation().clone().add(0, player.getBoundingBox().getHeight() / 2, 0))
					.count(10)
					.delta(0.4, 0.8, 0.4)
					.data(new Particle.DustTransition(
						Color.fromRGB(201, 48, 28),
						Color.fromRGB(117, 49, 28),
						1f))
					.spawnAsPlayerBuff(player);
				player.playSound(player.getLocation(), Sound.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.PLAYERS, 0.7f, 0.6f);
			}
		}
	}

	public static double harmPerTick(double level) {
		return SECONDS * DAMAGE_PER_SECOND * level;
	}

}
