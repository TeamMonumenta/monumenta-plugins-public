package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class GalleryWidowWebEffect extends GalleryConsumableEffect {
	/**
	 * - When you would be hit, stun mobs around you for 2s (10s cooldown), lasting 3 waves.
	 */

	private static final int EFFECT_COOLDOWN = 20 * 10;
	private static final int EFFECT_STUN_DURATION = 20 * 2;
	private static final double EFFECT_STUN_RADIUS = 3;

	private int mTimer = 0;

	public GalleryWidowWebEffect() {
		super(GalleryEffectType.WIDOW_WEB);
	}

	@Override
	public void tick(GalleryPlayer player, boolean oneSecond, boolean twoHertz, int ticks) {
		super.tick(player, oneSecond, twoHertz, ticks);
		mTimer--;
	}

	@Override
	public void onPlayerHurt(GalleryPlayer galleryPlayer, DamageEvent event, @Nullable LivingEntity enemy) {
		Player player = galleryPlayer.getPlayer();
		if (mTimer <= 0 && enemy != null && player != null) {
			new PartialParticle(Particle.SOUL, player.getEyeLocation()).delta(3, 1, 3).count(50).spawnAsPlayerBuff(player);
			player.playSound(player.getEyeLocation(), Sound.ENTITY_SPIDER_DEATH, SoundCategory.HOSTILE, 0.64f, 0.5f);
			player.playSound(player.getEyeLocation(), Sound.ENTITY_SPIDER_DEATH, SoundCategory.HOSTILE, 2, 0.5f);
			mTimer = EFFECT_COOLDOWN;
			for (LivingEntity le : EntityUtils.getNearbyMobs(player.getLocation(), EFFECT_STUN_RADIUS)) {
				EntityUtils.applyStun(Plugin.getInstance(), EFFECT_STUN_DURATION, le);
			}
		}
	}


}
