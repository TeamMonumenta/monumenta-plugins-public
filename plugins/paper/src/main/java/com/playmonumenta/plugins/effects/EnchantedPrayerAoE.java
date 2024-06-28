package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant.EnchantedPrayerCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class EnchantedPrayerAoE extends Effect {
	public static final String effectID = "EnchantedPrayerAoE";

	private final Plugin mPlugin;
	private final double mDamageAmount;
	private final double mHealAmount;
	private final Player mPlayer;
	private final EnumSet<DamageType> mAffectedDamageTypes;
	private final double mEffectSize;
	private final Player mCleric;
	private final @Nullable Crusade mCrusade;
	private final EnchantedPrayerCS mCosmetic;

	public EnchantedPrayerAoE(Plugin plugin, int duration, double damageAmount, double healAmount, Player player, EnumSet<DamageType> affectedDamageTypes, double size, Player cleric, @Nullable Crusade crusade, EnchantedPrayerCS cosmetic) {
		super(duration, effectID);
		mPlugin = plugin;
		mDamageAmount = damageAmount;
		mHealAmount = healAmount;
		mPlayer = player;
		mAffectedDamageTypes = affectedDamageTypes;
		mEffectSize = size;
		mCleric = cleric;
		mCrusade = crusade;
		mCosmetic = cosmetic;
	}

	// This needs to trigger after any percent damage
	@Override
	public EffectPriority getPriority() {
		return EffectPriority.LATE;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mDamageAmount);
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		// Prevents effect triggering multiple times in the same tick
		if (getDuration() == 0) {
			return;
		}

		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())) {
			World world = entity.getWorld();
			mCosmetic.onEffectTrigger(mPlayer, world, enemy.getLocation(), enemy);
			for (LivingEntity le : new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(enemy), mEffectSize).getHitMobs()) {
				DamageUtils.damage(mPlayer, le, DamageType.MAGIC, mDamageAmount, ClassAbility.ENCHANTED_PRAYER, true, true);
				Crusade.addCrusadeTag(le, mCrusade);
			}
			double maxHealth = EntityUtils.getMaxHealth(mPlayer);
			PlayerUtils.healPlayer(mPlugin, mPlayer, maxHealth * mHealAmount, mCleric);
			setDuration(0);
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			mCosmetic.effectTick(mPlayer);
		}
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Enchanted Prayer";
	}

	@Override
	public String toString() {
		return String.format("EnchantedPrayerAoE duration:%d player:%s amount:%f", this.getDuration(), mPlayer.getName(), mDamageAmount);
	}
}
