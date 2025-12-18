package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.CleansingTotemCS;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class CleansingTotem extends TotemAbility {
	private static final int COOLDOWN = 30 * 20;
	private static final int AOE_RANGE = 6;
	private static final double HEAL_PERCENT = 0.04;
	private static final int INTERVAL = 20;
	private static final int DURATION_1 = 8 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final int CLEANSES = 2;
	private static final double ENHANCE_HEALING_PERCENT = 0.01;
	private static final int ENHANCE_ABSORB_CAP = 4;
	private static final int ABSORPTION_DURATION = 15 * 20;

	public static String CHARM_DURATION = "Cleansing Totem Duration";
	public static String CHARM_RADIUS = "Cleansing Totem Radius";
	public static String CHARM_COOLDOWN = "Cleansing Totem Cooldown";
	public static String CHARM_HEALING = "Cleansing Totem Healing Amplifier";
	public static String CHARM_ENHANCE_HEALING = "Cleansing Totem Enhance Bonus Healing Amplifier";
	public static String CHARM_ENHANCE_ABSORB_MAX = "Cleansing Totem Enhance Absorption Maximum";
	public static String CHARM_PULSE_DELAY = "Cleansing Totem Pulse Delay";
	public static String CHARM_CLEANSES = "Cleansing Totem Cleanses";

	public static final AbilityInfo<CleansingTotem> INFO =
		new AbilityInfo<>(CleansingTotem.class, "Cleansing Totem", CleansingTotem::new)
			.linkedSpell(ClassAbility.CLEANSING_TOTEM)
			.scoreboardId("CleansingTotem")
			.shorthandName("CT")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Summon a totem that heals and cleanses players over its duration.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CleansingTotem::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.addAltPresetTrigger(new AbilityTriggerInfo<>("cast", "cast", CleansingTotem::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.BLUE_STAINED_GLASS);

	private final int mInterval;
	private final double mHealPercentBase;
	private final double mHealPercentEnhance;
	private final double mHealPercent;
	private final double mAbsorbCap;
	private final int mCleanses;
	private final CleansingTotemCS mCosmetic;

	public CleansingTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Cleansing Totem Projectile", "CleansingTotem", "Cleansing Totem");
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		setRadius(CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE));
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, INTERVAL);
		mHealPercentBase = HEAL_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEALING);
		mHealPercentEnhance = ENHANCE_HEALING_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_HEALING);
		mHealPercent = mHealPercentBase + (isEnhanced() ? mHealPercentEnhance : 0);
		mAbsorbCap = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_ABSORB_MAX, ENHANCE_ABSORB_CAP);
		mCleanses = CLEANSES + (int) CharmManager.getLevel(mPlayer, CHARM_CLEANSES);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CleansingTotemCS());
	}

	@Override
	public void placeTotem(Location standLocation, Player player, ArmorStand stand) {
		mCosmetic.cleansingTotemSpawn(mPlayer.getWorld(), standLocation);
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}
		if (isLevelTwo() && !isEnhanced() && ticks == mDuration / mCleanses - 1) {
			List<Player> cleansePlayers = PlayerUtils.playersInRange(standLocation, getTotemRadius(), true);
			cleanseTargets(cleansePlayers);
			mCosmetic.cleansingTotemCleanse(mPlayer, standLocation, getTotemRadius());
		}
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean chainLightning) {
		List<Player> affectedPlayers = getPlayersInRange();
		if (chainLightning) {
			double chainLightningModifier = ChainLightning.ENHANCE_SUPPORT_EFFICIENCY + CharmManager.getLevelPercentDecimal(mPlayer, ChainLightning.CHARM_SUPPORT_TOTEM_EFFICIENCY);
			for (Player p : affectedPlayers) {
				PlayerUtils.healPlayer(mPlugin, p, EntityUtils.getMaxHealth(p) * mHealPercent * chainLightningModifier);
				mCosmetic.cleansingTotemHeal(mPlayer);
			}
		} else {
			if (isEnhanced()) {
				cleanseTargets(affectedPlayers);
				mCosmetic.cleansingTotemCleanse(mPlayer, standLocation, getTotemRadius());
			}

			for (Player p : affectedPlayers) {
				double maxHealth = EntityUtils.getMaxHealth(p);
				double totalHealing = maxHealth * mHealPercent;
				double healed = PlayerUtils.healPlayer(mPlugin, p, totalHealing);
				double remainingHealing = totalHealing - healed;
				if (remainingHealing > 0 && isEnhanced()) {
					AbsorptionUtils.addAbsorption(p, remainingHealing, mAbsorbCap, ABSORPTION_DURATION);
				}
			}
			mCosmetic.cleansingTotemPulse(mPlayer, standLocation, getTotemRadius());
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		mCosmetic.cleansingTotemExpire(world, standLocation, mPlayer);
	}

	private void cleanseTargets(List<Player> cleansePlayers) {
		for (Player player : cleansePlayers) {
			PotionUtils.clearNegatives(mPlugin, player);
			EntityUtils.setWeakenTicks(mPlugin, player, 0);
			EntityUtils.setSlowTicks(mPlugin, player, 0);

			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}
	}

	private static Description<CleansingTotem> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Summon a *Totem* that periodically heals").styles(Shaman.TOTEM_COLOR)
			.addLine("all nearby players.")
			.addLine()
			.addStat("Healing: %p1e_only HP every 1s")
				.statValues(stat(a -> a.mHealPercentBase, HEAL_PERCENT))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, AOE_RANGE))
			.addStat("Duration: %t1")
				.statValues(stat(a -> a.mDuration, DURATION_1))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<CleansingTotem> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Cleansing Totem*'s duration.").styles(UNDERLINED)
			.addLine()
			.addLine("*Cleansing Totem* now cleanses debuffs").styles(UNDERLINED)
			.addLine("from players %d times evenly spread over")
				.statValues(stat(a -> a.mCleanses, CLEANSES))
			.addLine("its duration.")
			.addLine()
			.addStatComparison("Duration: %t1 -> %t2")
				.statValues(stat(DURATION_1), stat(a -> a.mDuration, DURATION_2))
			.addDashedLine();
	}

	private static Description<CleansingTotem> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Increase *Cleansing Totem*'s healing, and").styles(UNDERLINED)
			.addLine("it now cleanses debuffs on every pulse.")
			.addLine()
			.addStatComparison("Healing: +%p1e -> +%p3 HP every 1s")
				.statValues(stat(HEAL_PERCENT), stat(a -> a.mHealPercent, HEAL_PERCENT + ENHANCE_HEALING_PERCENT))
			.addLine()
			.addLine("Excess healing from *Cleansing Totem* is").styles(UNDERLINED)
			.addLine("now converted into up to %d absorption, ")
				.statValues(stat(a -> a.mAbsorbCap, ENHANCE_ABSORB_CAP))
			.addLine("which lasts for %t.")
				.statValues(stat(ABSORPTION_DURATION))
			.addDashedLine();
	}
}
