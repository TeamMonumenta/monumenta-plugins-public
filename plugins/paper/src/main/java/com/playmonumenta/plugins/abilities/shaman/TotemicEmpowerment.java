package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SupportExpertise;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
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
		new AbilityInfo<>(TotemicEmpowerment.class, "Totemic Empowerment", TotemicEmpowerment::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getClassNum(player) == Shaman.CLASS_ID);

	private final double mRadius;
	private final double mSpeed;
	private final double mResistance;

	private @Nullable SupportExpertise mSupportExpertise;

	public TotemicEmpowerment(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mSpeed = SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mResistance = RESISTANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
		Bukkit.getScheduler().runTask(plugin, () -> mSupportExpertise = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, SupportExpertise.class));
	}

	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 8;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && mPlayer != null && !mPlayer.isDead()) {
			List<LivingEntity> activeList = TOTEM_LIST.get(mPlayer.getUniqueId());
			if (activeList == null || activeList.isEmpty()) {
				return;
			}

			for (LivingEntity totem : activeList) {
				if (mPlayer.getWorld().equals(totem.getWorld()) && mPlayer.getLocation().distanceSquared(totem.getLocation()) <= mRadius * mRadius) {
					double selfSpeed = mSpeed;
					double selfResist = mResistance;
					if (mSupportExpertise != null) {
						selfSpeed += SupportExpertise.SELF_BOOST;
						selfResist += SupportExpertise.SELF_BOOST;

						List<Player> affectedPlayers = PlayerUtils.otherPlayersInRange(mPlayer, mSupportExpertise.mRadius, true);
						affectedPlayers.forEach(p -> applyEffects(p, mSpeed, mResistance));
					}
					applyEffects(mPlayer, selfSpeed, selfResist);
					break;
				}
			}
		}
	}

	private void applyEffects(Player player, double speed, double resist) {
		mPlugin.mEffectManager.addEffect(player, "ShamanPassiveSpeed", new PercentSpeed(40, speed, "ShamanPassiveSpeed").displaysTime(false).deleteOnAbilityUpdate(true));
		mPlugin.mEffectManager.addEffect(player, "ShamanPassiveResistance", new PercentDamageReceived(40, -resist).displaysTime(false).deleteOnAbilityUpdate(true));
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

	private static Description<TotemicEmpowerment> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("While standing within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks of any of your totems, gain ")
			.addPercent(a -> a.mSpeed, SPEED)
			.add(" speed and ")
			.addPercent(a -> a.mResistance, RESISTANCE)
			.add(" resistance.");
	}
}
