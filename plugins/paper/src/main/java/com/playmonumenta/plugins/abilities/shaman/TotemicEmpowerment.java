package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SharedEmpowerment;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TotemicEmpowerment extends Ability {

	public static final double SPEED = 0.05;
	public static final double RESISTANCE = 0.05;
	public static final int RADIUS = 10;

	private static final HashMap<UUID, List<LivingEntity>> TOTEM_LIST = new HashMap<>();

	public static final String CHARM_SPEED = "Totemic Empowerment Speed";
	public static final String CHARM_RESISTANCE = "Totemic Empowerment Damage Reduction";
	public static final String CHARM_RADIUS = "Totemic Empowerment Radius";

	public static final AbilityInfo<TotemicEmpowerment> INFO =
		new AbilityInfo<>(TotemicEmpowerment.class, null, TotemicEmpowerment::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0) == Shaman.CLASS_ID);

	private @Nullable SharedEmpowerment mSharedEmpowerment;

	public TotemicEmpowerment(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		Bukkit.getScheduler().runTask(plugin, () -> mSharedEmpowerment = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, SharedEmpowerment.class));
	}

	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 8;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && mPlayer != null && !mPlayer.isDead()) {
			if (!mPlayer.hasPermission(Shaman.PERMISSION_STRING)) {
				AuditListener.logSevere(mPlayer.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
				AbilityUtils.resetClass(mPlayer);
			}
			List<LivingEntity> activeList = TOTEM_LIST.get(mPlayer.getUniqueId());
			if (activeList == null || activeList.isEmpty()) {
				return;
			}

			double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
			for (LivingEntity totem : activeList) {
				if (mPlayer.getLocation().distance(totem.getLocation()) <= radius) {
					double selfSpeed = SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
					double selfResist = RESISTANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
					if (mSharedEmpowerment != null) {
						selfSpeed += mSharedEmpowerment.getSelfBoost();
						selfResist += mSharedEmpowerment.getSelfBoost();

						double otherSpeed = SPEED + mSharedEmpowerment.getOtherSpeed();
						double otherResist = RESISTANCE + mSharedEmpowerment.getOtherResist();
						List<Player> affectedPlayers = PlayerUtils.otherPlayersInRange(mPlayer, radius, true);
						affectedPlayers.forEach(p -> applyEffects(p, otherSpeed, otherResist));
					}
					applyEffects(mPlayer, selfSpeed, selfResist);
					break;
				}
			}
		}
	}

	private void applyEffects(Player player, double speed, double resist) {
		mPlugin.mEffectManager.addEffect(player, "ShamanPassiveSpeed", new PercentSpeed(40, speed, "ShamanPassiveSpeed"));
		mPlugin.mEffectManager.addEffect(player, "ShamanPassiveResistance", new PercentDamageReceived(40, -resist));
	}

	public static void addTotem(Player player, LivingEntity stand) {
		TOTEM_LIST.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>()).add(stand);
	}

	public static List<LivingEntity> getTotemList(Player player) {
		List<LivingEntity> totemList = TOTEM_LIST.get(player.getUniqueId());
		if (totemList != null) {
			return totemList;
		}
		return new ArrayList<>();
	}

	public static List<Location> getTotemLocations(Player player) {
		List<LivingEntity> totems = TOTEM_LIST.get(player.getUniqueId());
		ArrayList<Location> locs = new ArrayList<>();
		if (totems == null || totems.isEmpty()) {
			return locs;
		}
		for (LivingEntity totem : totems) {
			locs.add(totem.getEyeLocation());
		}
		return locs;
	}

	public static void removeTotem(Player player, LivingEntity stand) {
		List<LivingEntity> totemList = TOTEM_LIST.get(player.getUniqueId());
		if (totemList != null) {
			totemList.remove(stand);
		}
		stand.remove();
	}
}
