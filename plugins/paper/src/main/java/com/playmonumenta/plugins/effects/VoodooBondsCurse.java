package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.warlock.reaper.VoodooBondsCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class VoodooBondsCurse extends Effect {
	public static final String effectID = "VoodooBondsCurse";
	private final Player mPlayer;
	private final int mStartDuration;
	private final double mDamage;
	private final double mRadius;
	private final int mSpreadCount;
	private final double mSpreadRadius;
	private final VoodooBondsCS mCosmetic;
	private final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_SKILL,
		DamageType.MELEE_ENCH,
		DamageType.PROJECTILE,
		DamageType.PROJECTILE_SKILL,
		DamageType.MAGIC
	);

	public VoodooBondsCurse(Player player, int duration, double damage, double radius, int spreadCount, double spreadRadius, VoodooBondsCS cosmetic) {
		super(duration, effectID);
		mPlayer = player;
		mStartDuration = duration;
		mDamage = damage;
		mRadius = radius;
		mSpreadCount = spreadCount;
		mSpreadRadius = spreadRadius;
		mCosmetic = cosmetic;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (AFFECTED_DAMAGE_TYPES.contains(event.getType()) && event.getAbility() != ClassAbility.VOODOO_BONDS) {
			double damage = event.getDamage() * mDamage;
			List<LivingEntity> cursedMobs = new Hitbox.SphereHitbox(entity.getLocation(), mRadius).getHitMobs();
			cursedMobs.remove(entity);
			cursedMobs.removeIf(e -> !Plugin.getInstance().mEffectManager.hasEffect(e, VoodooBonds.CURSE_EFFECT));
			for (LivingEntity mob : cursedMobs) {
				mCosmetic.curseSpread(mPlayer, mob, entity);
				DamageUtils.damage(mPlayer, mob, DamageType.OTHER, damage, ClassAbility.VOODOO_BONDS, true, false);
			}
		}

		if (event.getDamager() instanceof Player player
			&& event.getType() == DamageType.MELEE
			&& PlayerUtils.isFallingAttack(player)) {

			List<LivingEntity> notCursedMobs = new Hitbox.SphereHitbox(entity.getLocation(), mSpreadRadius).getHitMobs();
			notCursedMobs.removeIf(e -> Plugin.getInstance().mEffectManager.hasEffect(e, VoodooBonds.CURSE_EFFECT));
			int i = 0;
			for (LivingEntity mob : notCursedMobs) {
				mCosmetic.curseSpread(mPlayer, mob, entity);
				Plugin.getInstance().mEffectManager.addEffect(mob, VoodooBonds.CURSE_EFFECT, new VoodooBondsCurse(mPlayer, mStartDuration, mDamage, mRadius, mSpreadCount, mSpreadRadius, mCosmetic));

				i++;
				if (i >= mSpreadCount) {
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
