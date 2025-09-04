package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class EscapeDeathEliteHunt extends Effect {
	public static final String effectID = "EscapeDeathEliteHunt";

	private final Player mEffectApplicator;
	private final double mHealPercent;

	public EscapeDeathEliteHunt(int duration, Player effectApplicator, double healPercent) {
		super(duration, effectID);
		mEffectApplicator = effectApplicator;
		mHealPercent = healPercent;
	}

	@Override
	public String toString() {
		return String.format("%s duration:%d applicator:%s heal:%s", effectID, mDuration, mEffectApplicator, mHealPercent);
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		if (event.getEntity().getKiller() != mEffectApplicator || mEffectApplicator == null || !mEffectApplicator.isValid()) {
			return;
		}

		// heal and cleanse
		// the cleanse is copy pasted from shaman cleanse
		// i should make this a general util function but i am lazy
		Plugin plugin = Plugin.getInstance();
		PlayerUtils.healPlayer(plugin, mEffectApplicator, EntityUtils.getMaxHealth(mEffectApplicator) * mHealPercent);
		PotionUtils.clearNegatives(plugin, mEffectApplicator);
		EntityUtils.setWeakenTicks(plugin, mEffectApplicator, 0);
		EntityUtils.setSlowTicks(plugin, mEffectApplicator, 0);

		if (mEffectApplicator.getFireTicks() > 1) {
			mEffectApplicator.setFireTicks(1);
		}
	}

	@Override
	public void entityGainEffect(Entity entity) {
		GlowingManager.startGlowing(entity, NamedTextColor.DARK_PURPLE, mDuration, GlowingManager.PLAYER_ABILITY_PRIORITY);
	}
}
