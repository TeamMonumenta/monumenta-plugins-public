package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.listeners.IchorListener;
import java.util.EnumSet;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class IchorEarthEffect extends Effect {
	public static final String effectID = "IchorEarthEffect";
	private static final String SOURCE_RESISTANCE = "IchorEarthResistance";
	private static final String SOURCE_DAMAGE = "IchorEarthDamage";

	private boolean mWasHit = false;
	private final double mEffectMultiplier;
	private final double mResistance;
	private final double mDamage;
	private final int mBuffDuration;
	private final String mSource;

	public IchorEarthEffect(int duration, double effectMultiplier, double resistance, double damage, int buffDuration, String source) {
		super(duration, effectID);
		mEffectMultiplier = effectMultiplier;
		mResistance = resistance;
		mDamage = damage;
		mBuffDuration = buffDuration;
		mSource = source;
	}

	@Override
	public void onHurtByEntity(LivingEntity entity, DamageEvent event, Entity damager) {
		mWasHit = true;
		DamageType type = event.getType();
		EffectManager.getInstance().addEffect(entity, SOURCE_RESISTANCE, new PercentDamageReceived(mBuffDuration, mResistance * mEffectMultiplier, EnumSet.of(type)));
		EffectManager.getInstance().clearEffects(entity, mSource);

		Player player = (Player) entity;
		player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.3f, 1.2f);
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (!mWasHit) {
			EffectManager.getInstance().addEffect(entity, SOURCE_DAMAGE, new PercentDamageDealt(mBuffDuration, mDamage * mEffectMultiplier, EnumSet.of(DamageType.MELEE, DamageType.MELEE_SKILL, DamageType.MELEE_ENCH)));

			Player player = (Player) entity;
			player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.3f, 1.5f);
		}
	}

	@Override
	public String toString() {
		return String.format("IchorEarthEffect duration:%d", this.getDuration());
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return IchorListener.ITEM_NAME + " - Earthbound";
	}
}
