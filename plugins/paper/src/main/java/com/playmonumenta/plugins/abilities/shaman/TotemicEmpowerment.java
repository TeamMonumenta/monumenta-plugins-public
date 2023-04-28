package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.*;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SharedEmpowerment;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.*;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TotemicEmpowerment extends Ability {

	private static final int RADIUS = 10;

	private static final HashMap<UUID, List<LivingEntity>> TOTEM_LIST = new HashMap<>();

	public static final AbilityInfo<TotemicEmpowerment> INFO =
		new AbilityInfo<>(TotemicEmpowerment.class, null, TotemicEmpowerment::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0) == Shaman.CLASS_ID);

	public TotemicEmpowerment(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
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
			int sharedEmpScore = 0;
			Ability sharedEmpowerment = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, SharedEmpowerment.class);
			if (sharedEmpowerment != null) {
				sharedEmpScore = sharedEmpowerment.getAbilityScore();
			}
			if (activeList == null) {
				return;
			}
			for (LivingEntity totem : activeList) {
				if (mPlayer.getLocation().distance(totem.getLocation()) <= RADIUS) {
					double selfDRAmount = Shaman.PASSIVE_DR + getSharedEmpPersonalImpact(sharedEmpScore);
					double selfSpeedAmount = Shaman.PASSIVE_SPEED + getSharedEmpPersonalImpact(sharedEmpScore);
					applyEffects(mPlayer, selfSpeedAmount, selfDRAmount);

					if (sharedEmpScore > 0) {
						double otherAmount = getSharedEmpExternalImpact(sharedEmpScore);
						List<Player> affectedPlayers = PlayerUtils.otherPlayersInRange(mPlayer, RADIUS, true);
						affectedPlayers.forEach(p -> applyEffects(p, otherAmount, otherAmount));
					}
					break;
				}
			}
		}
	}

	private void applyEffects(Player player, double speed, double resist) {
		mPlugin.mEffectManager.addEffect(player, "ShamanPassiveSpeed", new PercentSpeed(40, speed, "ShamanPassiveSpeed"));
		mPlugin.mEffectManager.addEffect(player, "ShamanPassiveResistance", new PercentDamageReceived(40, -resist));
	}

	public static void addTotem(@NotNull Player player, LivingEntity stand) {
		TOTEM_LIST.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>()).add(stand);
	}

	public static int getActiveTotems(Player player) {
		List<LivingEntity> totemList = TOTEM_LIST.get(player.getUniqueId());
		if (totemList != null) {
			return totemList.size();
		}
		return 0;
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

	public static double getSharedEmpPersonalImpact(int score) {
		return switch (score) {
			case 1, 3 -> 0.03;
			case 2, 4 -> 0.05;
			default -> 0;
		};
	}

	public static double getSharedEmpExternalImpact(int score) {
		return switch (score) {
			case 1, 3 -> 0.05;
			case 2, 4 -> 0.1;
			default -> 0;
		};
	}
}
