package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.IchorCooldown;
import com.playmonumenta.plugins.effects.IchorSteelEffect;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class IchorSteelsage implements Infusion {
	private static final int COOLDOWN = 20 * 20;
	private static final String ICHOR_STEELSAGE_COOLDOWN = IchorListener.ITEM_NAME + " - Steelsage";
	private static final String EFFECT = "IchorSteelEffect";
	private static final int JUMP_AMPLIFIER = 1;
	private static final double DAMAGE = 0.08;
	private static final int JUMP_DURATION = 6 * 20;
	private static final int EFFECT_DURATION = 8 * 20;
	public static final String DESCRIPTION = String.format("Gain +%s Jump Boost for %s seconds, additionally gain %s%% projectile damage while in midair for %s seconds. Cooldown: %s seconds.",
		JUMP_AMPLIFIER,
		StringUtils.ticksToSeconds(JUMP_DURATION),
		StringUtils.multiplierToPercentage(DAMAGE),
		StringUtils.ticksToSeconds(EFFECT_DURATION),
		StringUtils.ticksToSeconds(COOLDOWN)
	);

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ICHOR_STEELSAGE;
	}

	@Override
	public String getName() {
		return IchorListener.ITEM_NAME + " - Steelsage";
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double value, PlayerItemConsumeEvent event) {
		if (plugin.mEffectManager.hasEffect(player, ICHOR_STEELSAGE_COOLDOWN)) {
			return;
		}
		plugin.mEffectManager.addEffect(player, ICHOR_STEELSAGE_COOLDOWN, new IchorCooldown(COOLDOWN, ICHOR_STEELSAGE_COOLDOWN));
		ichorSteelsage(plugin, player, 1);
	}

	public static void ichorSteelsage(Plugin plugin, Player player, double multiplier) {
		PotionEffect playerJumpBoost = player.getPotionEffect(PotionEffectType.JUMP);
		// -1 because Jump Boost level starts counting from 0, and we're adding 1 to it in that case...
		int jumpAmplifier = playerJumpBoost == null ? -1 : playerJumpBoost.getAmplifier();
		plugin.mPotionManager.addPotion(player, PotionManager.PotionID.ITEM, new PotionEffect(PotionEffectType.JUMP, JUMP_DURATION, jumpAmplifier + JUMP_AMPLIFIER));
		plugin.mEffectManager.addEffect(player, EFFECT, new IchorSteelEffect(EFFECT_DURATION, DAMAGE * multiplier));

		player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1f, 2f);
		player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.8f, 1.3f);
		player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.3f, 1.5f);

		new PPCircle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 6).data(Material.ICE.createBlockData()).ringMode(false).count(20).spawnAsPlayerPassive(player);
		ParticleUtils.drawParticleCircleExplosion(player, player.getLocation(), 0, 0.5, 0, 0, 40, 0.5f, true, 0, 0.1, Particle.EXPLOSION_NORMAL);
	}
}
