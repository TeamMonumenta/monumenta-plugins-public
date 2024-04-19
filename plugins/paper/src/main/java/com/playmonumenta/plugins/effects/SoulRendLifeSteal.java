package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.abilities.warlock.SoulRend;
import com.playmonumenta.plugins.cosmetics.skills.warlock.SoulRendCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class SoulRendLifeSteal extends Effect {
	public static final String effectID = "SoulRendLifeSteal";

	private final Player mPlayer;
	private int mMarks;
	private final double mHealPercent;
	private final double mHealCap;
	private final double mRemainingHeal;
	private final SoulRend mSoulRend;
	private final SoulRendCS mCosmetic;

	private boolean mIsFirstCrit;

	public SoulRendLifeSteal(Player player, int duration, int marks, double healPercent, double healCap,
							 double remainingHeal, SoulRend soulRend, SoulRendCS cosmetic) {
		super(duration, effectID);
		mPlayer = player;
		mMarks = marks;
		mHealPercent = healPercent;
		mHealCap = healCap;
		mRemainingHeal = remainingHeal;
		mCosmetic = cosmetic;
		mSoulRend = soulRend;

		mIsFirstCrit = true;
	}

	@Override
	public void onHurt(LivingEntity enemy, DamageEvent event) {
		if (event.getDamager() instanceof Player player
			&& player.getUniqueId().equals(mPlayer.getUniqueId())
			&& event.getType() == DamageEvent.DamageType.MELEE
			&& PlayerUtils.isFallingAttack(player)
			&& ItemUtils.isHoe(player.getInventory().getItemInMainHand())) {

			// the same crit that adds the effect will also deduct a mark, so prevent this
			// also visually display a fake "third mark" getting depleted as part of the 3-part rend
			if (mIsFirstCrit) {
				mCosmetic.rendLoseMark(mPlayer, enemy, mMarks + 1, false);
				mIsFirstCrit = false;
				return;
			}

			mCosmetic.rendLoseMark(mPlayer, enemy, mMarks, true);
			new PartialParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(mPlayer);

			double heal = Math.min(event.getDamage() * mHealPercent, mHealCap);
			mSoulRend.markHeal(heal, enemy);

			mMarks--;
			if (mMarks <= 0) {
				clearEffect();
			}
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		LivingEntity enemy = event.getEntity();
		if (enemy.getKiller() != null) {
			mCosmetic.rendMarkDied(mPlayer, enemy, mMarks);
			new PartialParticle(Particle.HEART, mPlayer.getLocation().add(0, 1, 0), mMarks * 2, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(mPlayer);

			double heal = mMarks * mRemainingHeal;
			mSoulRend.markHeal(heal, enemy);
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
