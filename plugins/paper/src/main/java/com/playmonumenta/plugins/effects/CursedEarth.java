package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.shaman.EarthenTremorCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;

public class CursedEarth extends Effect {
	public static final String effectID = "CursedEarth";

	private final Player mPlayer;
	private final EarthenTremorCS mCosmetic;
	private final double mMeleeScaling;
	private final double mProjScaling;
	private final double mRadius;
	private final String mSource;

	public CursedEarth(String source, int duration, Player player, EarthenTremorCS cosmetic, double meleeScaling, double projScaling, double radius) {
		super(duration, effectID);
		mSource = source;
		mPlayer = player;
		mCosmetic = cosmetic;
		mMeleeScaling = meleeScaling;
		mProjScaling = projScaling;
		mRadius = radius;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		double damage;
		boolean meleeHit = true;
		if (event.getDamager() instanceof Player damager && mPlayer.equals(damager) && event.getType() == DamageEvent.DamageType.MELEE) {
			// Melee sclaing
			final ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
			double attackDamageAdd = ItemStatUtils.getAttributeAmount(inMainHand, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) + 1;
			damage = attackDamageAdd * mMeleeScaling;
		} else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter && shooter.equals(mPlayer)) {
			// Projectile scaling
			ItemStatManager.PlayerItemStats projectileItemStats = DamageListener.getProjectileItemStats(projectile);
			if (projectileItemStats == null) {
				return;
			}
			double projDamageAdd = projectileItemStats.getMainhandAddStats().get(AttributeType.PROJECTILE_DAMAGE_ADD);
			damage = projDamageAdd * mProjScaling;
			meleeHit = false;
		} else {
			return;
		}

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(entity.getLocation(), mRadius);
		mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));
		for (LivingEntity mob : mobs) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, damage, ClassAbility.EARTHEN_TREMOR, true);
		}
		mCosmetic.cursedEarthEffect(mPlayer, entity.getLocation(), mRadius, meleeHit);

		EffectManager.getInstance().clearEffects(entity, mSource);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		mCosmetic.cursedEarthTick(mPlayer, entity, oneHertz);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		mCosmetic.cursedEarthGain(mPlayer, entity);
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		mCosmetic.cursedEarthExpire(mPlayer, entity);
	}

	@Override
	public String toString() {
		return effectID + " duration: " + getDuration();
	}
}
