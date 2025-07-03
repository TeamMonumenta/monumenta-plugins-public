package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class TouchofRadianceEnhancement extends Effect {
	public static final String effectID = "TouchofRadianceEnhancement";
	private final Plugin mPlugin;
	private final Player mPlayer;
	private final double mDamage;
	private final int mStunDuration;
	private boolean mIsReady;
	private @Nullable LivingEntity mTarget;

	public TouchofRadianceEnhancement(Plugin plugin, Player player, double damage, int stunDuration, int duration, @Nullable LivingEntity target) {
		super(duration, effectID);
		mPlugin = plugin;
		mPlayer = player;
		mDamage = damage;
		mStunDuration = stunDuration;
		mTarget = target;
		mIsReady = false;
	}

	public TouchofRadianceEnhancement(Plugin plugin, Player player, double damage, int stunDuration, int duration, @Nullable LivingEntity target, boolean isReady) {
		super(duration, effectID);
		mPlugin = plugin;
		mPlayer = player;
		mDamage = damage;
		mStunDuration = stunDuration;
		mTarget = target;
		mIsReady = isReady;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (enemy != mTarget && mIsReady && DamageEvent.DamageType.getScalableDamageType().contains(event.getType()) && Crusade.enemyTriggersAbilities(enemy)) {
			mIsReady = false;
			mTarget = enemy;
			GlowingManager.startGlowing(mTarget, NamedTextColor.YELLOW, mDuration, 1, null, "ToRenhanceGlowing");
			DamageUtils.damage(mPlayer, mTarget, DamageEvent.DamageType.MAGIC, mDamage, ClassAbility.TOUCH_OF_RADIANCE, true);
			EntityUtils.applyStun(mPlugin, mStunDuration, mTarget);
		}
	}

	//Check for if the target is dead instead of taking a kill event, that way anyone can kill the target
	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (mTarget != null && mTarget.isDead()) {
			mIsReady = true;
			mTarget = null;
		}
	}

	@Override
	public String toString() {
		return String.format("TouchofRadianceEnhancement duration:%d", this.getDuration());
	}
}
