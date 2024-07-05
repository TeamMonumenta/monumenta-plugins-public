package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.warlock.SanguineHarvestCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class SanguineMark extends Effect {
	public static final String effectID = "SanguineMark";

	private final boolean mLevelTwo;
	private final double mHealPercent;
	private final double mDamageBoost;
	private final Player mPlayer;
	private final Plugin mPlugin;
	private final SanguineHarvestCS mCosmetic;

	public SanguineMark(boolean isLevelTwo, double healPercent, double damage, int duration, Player player, Plugin plugin, SanguineHarvestCS cosmetic) {
		super(duration, effectID);
		mLevelTwo = isLevelTwo;
		mHealPercent = healPercent;
		mDamageBoost = damage;
		mPlayer = player;
		mPlugin = plugin;
		mCosmetic = cosmetic;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		mCosmetic.entityGainEffect(entity);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		mCosmetic.entityTickEffect(entity, fourHertz, twoHertz, oneHertz);
	}

	@Override
	public void onHurt(LivingEntity livingEntity, DamageEvent event) {
		if (mLevelTwo && event.getDamager() instanceof Player player && event.getType() == DamageEvent.DamageType.MELEE && mDuration != 0) {
			mCosmetic.onHurt(livingEntity, player);

			event.updateDamageWithMultiplier(1 + mDamageBoost);

			double maxHealth = EntityUtils.getMaxHealth(player);
			PlayerUtils.healPlayer(mPlugin, player, mHealPercent * maxHealth, mPlayer);

			clearEffect();
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		if (event.getEntity().getKiller() != null) {
			Player player = event.getEntity().getKiller();

			mCosmetic.onDeath(event.getEntity(), player);

			double maxHealth = EntityUtils.getMaxHealth(player);
			PlayerUtils.healPlayer(mPlugin, player, mHealPercent * maxHealth, mPlayer);
		}
	}

	@Override
	public String toString() {
		return String.format("SanguineMark duration:%d", this.getDuration());
	}
}
