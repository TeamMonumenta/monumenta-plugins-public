package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warlock.SoulRend;
import com.playmonumenta.plugins.cosmetics.skills.warlock.SoulRendCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class SoulRendLifeSteal extends Effect {
	public static final String effectID = "SoulRendLifeSteal";

	private final Plugin mPlugin;
	private final Player mPlayer;
	private int mMarks;
	private final double mHealPercent;
	private final double mHealCap;
	private final double mRemainingHeal;
	private final double mRadius;
	private final boolean mEnhanced;
	private final double mAbsorptionCap;
	private final int mAbsorptionDuration;
	private final SoulRendCS mCosmetic;

	private boolean mIsFirstCrit;

	public SoulRendLifeSteal(Plugin plugin, Player player, int duration, int marks, double healPercent, double healCap,
							 double remainingHeal, double radius, boolean enhanced, double absorptionCap, int absorptionDuration, SoulRendCS cosmetic) {
		super(duration, effectID);
		mPlugin = plugin;
		mPlayer = player;
		mMarks = marks;
		mHealPercent = healPercent;
		mHealCap = healCap;
		mRemainingHeal = remainingHeal;
		mRadius = radius;
		mEnhanced = enhanced;
		mCosmetic = cosmetic;
		mAbsorptionCap = absorptionCap;
		mAbsorptionDuration = absorptionDuration;

		mIsFirstCrit = true;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (event.getDamager() instanceof Player player
			&& event.getType() == DamageEvent.DamageType.MELEE
			&& PlayerUtils.isFallingAttack(player)
			&& ItemUtils.isHoe(player.getInventory().getItemInMainHand())) {

			// the same crit that adds the effect will also deduct a mark, so prevent this
			// also visually display a fake "third mark" getting depleted as part of the 3-part rend
			if (mIsFirstCrit) {
				mCosmetic.rendLoseMark(mPlayer, entity, mMarks + 1, false);
				mIsFirstCrit = false;
				return;
			}

			mCosmetic.rendLoseMark(mPlayer, entity, mMarks, true);

			mCosmetic.rendHealEffect(mPlayer, mPlayer, entity);
			double heal = Math.min(event.getDamage() * mHealPercent, mHealCap);
			PlayerUtils.healPlayer(mPlugin, player, heal, player);
			new PartialParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(mPlayer);

			for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, mRadius, true)) {
				mCosmetic.rendHealEffect(mPlayer, p, entity);
				double healed = PlayerUtils.healPlayer(mPlugin, p, CharmManager.calculateFlatAndPercentValue(player, SoulRend.CHARM_ALLY, heal), mPlayer);
				if (mEnhanced) {
					mCosmetic.rendAbsorptionEffect(mPlayer, p, entity);
					AbsorptionUtils.addAbsorption(player, heal - healed, mAbsorptionCap, mAbsorptionDuration);
				}
			}

			mMarks--;
			if (mMarks <= 0) {
				clearEffect();
			}
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		if (event.getEntity().getKiller() != null) {
			Player player = event.getEntity().getKiller();

			mCosmetic.rendMarkDied(mPlayer, event.getEntity(), mMarks);

			mCosmetic.rendHealEffect(mPlayer, mPlayer, event.getEntity());
			double heal = mMarks * mRemainingHeal;
			PlayerUtils.healPlayer(mPlugin, player, heal, player);
			new PartialParticle(Particle.HEART, player.getLocation().add(0, 1, 0), mMarks * 2, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(mPlayer);

			for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, mRadius, true)) {
				mCosmetic.rendHealEffect(mPlayer, p, event.getEntity());
				double healed = PlayerUtils.healPlayer(mPlugin, p, CharmManager.calculateFlatAndPercentValue(player, SoulRend.CHARM_ALLY, heal), mPlayer);
				if (mEnhanced) {
					mCosmetic.rendAbsorptionEffect(mPlayer, p, event.getEntity());
					AbsorptionUtils.addAbsorption(player, heal - healed, mAbsorptionCap, mAbsorptionDuration);
				}
			}
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity livingEntity) {
			mCosmetic.rendMarkTick(mPlayer, livingEntity, mMarks);
		}

		if (mMarks <= 0) {
			clearEffect();
		}
	}

	@Override
	public double getMagnitude() {
		return mMarks;
	}

	@Override
	public String toString() {
		return String.format("SoulRendLifeSteal duration:%d marks:%d", this.getDuration(), mMarks);
	}
}
