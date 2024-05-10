package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.warlock.reaper.VoodooBondsCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.NavigableSet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class VoodooBondsCurse extends Effect {
	public static final String effectID = "VoodooBondsCurse";
	private final Player mPlayer;
	private final double mDamage;
	private final double mRadius;
	private final boolean mLevelTwo;
	private final double mDeathDamage;
	private final int mCurseExtension;
	private final VoodooBondsCS mCosmetic;
	private final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_SKILL,
		DamageType.MELEE_ENCH,
		DamageType.PROJECTILE,
		DamageType.PROJECTILE_SKILL,
		DamageType.MAGIC
	);

	public VoodooBondsCurse(Player player, int duration, double damage, double radius, boolean isLevelTwo, double deathDamage, int curseExtension, VoodooBondsCS cosmetic) {
		super(duration, effectID);
		mPlayer = player;
		mDamage = damage;
		mRadius = radius;
		mLevelTwo = isLevelTwo;
		mDeathDamage = deathDamage;
		mCurseExtension = curseExtension;
		mCosmetic = cosmetic;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (AFFECTED_DAMAGE_TYPES.contains(event.getType()) && event.getAbility() != ClassAbility.VOODOO_BONDS) {
			EntityType type = entity.getType();
			double damage = event.getDamage() * mDamage;
			for (LivingEntity mob : EntityUtils.getNearbyMobs(entity.getLocation(), mRadius, entity)) {
				if (mob.getType().equals(type)) {
					mCosmetic.curseSpread(mPlayer, mob, entity);
					DamageUtils.damage(mPlayer, mob, DamageType.OTHER, damage, ClassAbility.VOODOO_BONDS, true, false);
				}
			}
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		if (mLevelTwo) {
			List<LivingEntity> cursedMobs = EntityUtils.getNearbyMobs(event.getEntity().getLocation(), 30, 30, 30,
				e -> EntityUtils.isHostileMob(e) && Plugin.getInstance().mEffectManager.hasEffect(e, VoodooBonds.CURSE_EFFECT));
			Collections.shuffle(cursedMobs);

			int i = 0;
			for (LivingEntity mob : cursedMobs) {
				mCosmetic.curseDeath(mPlayer, mob, event.getEntity());

				DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDeathDamage, ClassAbility.VOODOO_BONDS, true, true);
				if (mCurseExtension > 0) {
					NavigableSet<Effect> curseEffects = Plugin.getInstance().mEffectManager.getEffects(mob, VoodooBonds.CURSE_EFFECT);
					if (curseEffects != null) {
						for (Effect effect : curseEffects) {
							effect.setDuration(effect.getDuration() + mCurseExtension);
						}
					}
				}

				i++;
				if (i >= 3) {
					break;
				}
			}
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		mCosmetic.curseTick(mPlayer, entity, fourHertz, twoHertz, oneHertz);
	}

	@Override
	public double getMagnitude() {
		return mDamage;
	}

	@Override
	public String toString() {
		return String.format("VoodooBondsCurse duration:%d", this.getDuration());
	}
}
