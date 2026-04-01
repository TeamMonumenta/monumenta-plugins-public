package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.abilities.scout.ShrapnelBomb;
import com.playmonumenta.plugins.cosmetics.skills.scout.ShrapnelBombCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class ShrapnelBombMark extends Effect {
	public static final String effectID = "ShrapnelBombMark";

	private final double mDamageBoost;
	private int mHits;
	private final Player mPlayer;
	private final ShrapnelBomb mBomb;
	private final ItemStatManager.PlayerItemStats mStats;
	private final ShrapnelBombCS mCosmetic;
	private final boolean mEnhancementMarked;

	public ShrapnelBombMark(double damage, int hits, int duration, Player player, ShrapnelBombCS cosmetic,
							ShrapnelBomb bomb, ItemStatManager.PlayerItemStats stats, boolean enhancementMarked) {
		super(duration, effectID);
		mDamageBoost = damage;
		mHits = hits;
		mPlayer = player;
		mBomb = bomb;
		mStats = stats;
		mEnhancementMarked = enhancementMarked;
		mCosmetic = cosmetic;
	}

	@Override
	public void onHurt(LivingEntity livingEntity, DamageEvent event) {
		DamageEvent.DamageType type = event.getType();
		if (type != DamageEvent.DamageType.PROJECTILE
			|| mPlayer != (event.getDamager() instanceof Projectile ? ((Projectile) event.getDamager()).getShooter() : event.getDamager())
			|| mHits <= 0) {
			return;
		}
		mHits--;
		mCosmetic.firstStrike(livingEntity.getWorld(), mPlayer, livingEntity);

		if (mBomb.isLevelTwo()) {
			event.updateDamageWithMultiplier(1 + mDamageBoost);
		}

		if (mEnhancementMarked) {
			Location loc = LocationUtils.getHalfHeightLocation(livingEntity);
			mBomb.explodeEnhancement(loc, mStats);
		}

		if (mHits <= 0) {
			clearEffect();
		}
	}

	@Override
	public String toString() {
		return String.format("ShrapnelBombMark duration:%d", this.getDuration());
	}
}
