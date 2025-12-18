package com.playmonumenta.plugins.abilities.mage.arcanist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.arcanist.CosmicMoonbladeCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class CosmicMoonblade extends Ability {

	public static final String NAME = "Cosmic Moonblade";
	private static final double DAMAGE_1 = 5;
	private static final double DAMAGE_2 = 7;
	private static final int SWINGS = 2;
	private static final int RADIUS = 5;
	private static final int COOLDOWN = 20 * 8;
	private static final double ANGLE = 55;
	private static final int SLASH_INTERVAL_TICKS = 7;
	public static final double REDUCTION_MULTIPLIER_1 = 0.05;
	public static final double REDUCTION_MULTIPLIER_2 = 0.1;
	public static final double REDUCTION_MULTIPLIER_KILL = 0.075;
	public static final int CAP_TICKS_1 = (int) (0.5 * Constants.TICKS_PER_SECOND);
	public static final int CAP_TICKS_2 = Constants.TICKS_PER_SECOND;
	public static final int CAP_TICKS_KILL = (int) (0.75 * Constants.TICKS_PER_SECOND);

	public static final String CHARM_DAMAGE = "Cosmic Moonblade Damage";
	public static final String CHARM_SPELL_COOLDOWN = "Cosmic Moonblade Cooldown Reduction";
	public static final String CHARM_DEATH_COOLDOWN = "Cosmic Moonblade On Kill Cooldown Reduction";
	public static final String CHARM_COOLDOWN = "Cosmic Moonblade Cooldown";
	public static final String CHARM_SLASH_INTERVAL = "Cosmic Moonblade Slash Interval";
	public static final String CHARM_CAP = "Cosmic Moonblade Cooldown Cap";
	public static final String CHARM_DEATH_CAP = "Cosmic Moonblade On Kill Cooldown Cap";
	public static final String CHARM_RANGE = "Cosmic Moonblade Range";
	public static final String CHARM_SLASH = "Cosmic Moonblade Slashes";

	public static final AbilityInfo<CosmicMoonblade> INFO =
		new AbilityInfo<>(CosmicMoonblade.class, "Cosmic Moonblade", CosmicMoonblade::new)
			.linkedSpell(ClassAbility.COSMIC_MOONBLADE)
			.scoreboardId("CosmicMoonblade")
			.shorthandName("CM")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Damage mobs in a cone twice and reduce cooldowns if mobs are damaged.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CosmicMoonblade::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.DIAMOND_SWORD);

	private final double mDamage;
	private final double mLevelReduction;
	private final int mLevelCap;
	private final double mRange;
	private final int mSlashInterval;
	private final int mTotalSwings;
	private final double mKillCDR;
	private final int mKillCDRCap;
	private final CosmicMoonbladeCS mCosmetic;

	private boolean mTriggered = false;

	public CosmicMoonblade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mLevelReduction = (isLevelOne() ? REDUCTION_MULTIPLIER_1 : REDUCTION_MULTIPLIER_2) + CharmManager.getLevelPercentDecimal(player, CHARM_SPELL_COOLDOWN);
		mLevelCap = CharmManager.getDuration(player, CHARM_CAP, (isLevelOne() ? CAP_TICKS_1 : CAP_TICKS_2));
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RADIUS);
		mSlashInterval = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SLASH_INTERVAL, SLASH_INTERVAL_TICKS);
		mTotalSwings = (int) CharmManager.getLevel(mPlayer, CHARM_SLASH) + SWINGS;
		mKillCDR = REDUCTION_MULTIPLIER_KILL + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DEATH_COOLDOWN);
		mKillCDRCap = CharmManager.getDuration(mPlayer, CHARM_DEATH_CAP, CAP_TICKS_KILL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CosmicMoonbladeCS());
	}

	public boolean cast() {
		mTriggered = false;
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();
		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mDamage);
		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		cancelOnDeath(new BukkitRunnable() {
			int mSwings = 0;

			@Override
			public void run() {
				mSwings++;
				Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), mRange, Math.toRadians(ANGLE));
				List<LivingEntity> hitMobs = hitbox.getHitMobs();
				if (!hitMobs.isEmpty()) {
					updateCooldowns(mLevelReduction, mInfo.getLinkedSpell(), mLevelCap);
					for (LivingEntity mob : hitMobs) {
						DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, false, false);
					}
				}

				World world = mPlayer.getWorld();
				Location origin = mPlayer.getLocation();
				mCosmetic.moonbladeSwingEffect(world, mPlayer, origin, mRange, mSwings, mTotalSwings);

				if (mSwings >= mTotalSwings) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, mSlashInterval));

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == mInfo.getLinkedSpell() &&
			event.getFinalDamage(true) > enemy.getHealth() &&
			event.getDamager() == mPlayer && !mTriggered) {
			mTriggered = true;
			// update all abil cd, 1 tick delay to make sure moon blade is also reduced
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> updateCooldowns(mKillCDR, null, mKillCDRCap), 1);
		}

		return false;
	}

	public void updateCooldowns(double percent, @Nullable ClassAbility ignoredAbil, int cap) {
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
			if (linkedSpell == null || linkedSpell == ignoredAbil) {
				continue;
			}
			int totalCD = abil.getModifiedCooldown();
			int reducedCD = Math.min((int) (totalCD * percent), cap);
			mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, reducedCD);
		}
	}

	private static Description<CosmicMoonblade> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Slash %d times in front of you, dealing")
				.statValues(stat(a -> a.mTotalSwings, SWINGS))
			.addLine("*Arcane* damage with each slash.").styles(Mage.ARCANE_COLOR)
			.addLine()
			.addLine("Each slash reduces all your other ability")
			.addLine("cooldowns if at least *1* mob was hit.").styles(WHITE)
			.addLine()
			.addStat("Damage: %d1 (s) per slash")
				.statValues(stat(a -> a.mDamage, DAMAGE_1))
			.addStat("Radius: %r (Cone-Shaped)")
				.statValues(stat(a -> a.mRange, RADIUS))
			.addStat("Cooldown Reduction: %p1 (max %t1) per slash ")
				.statValues(stat(a -> a.mLevelReduction, REDUCTION_MULTIPLIER_1), stat(a -> a.mLevelCap, CAP_TICKS_1))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}


	private static Description<CosmicMoonblade> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Cosmic Moonblade*'s damage").styles(UNDERLINED)
			.addLine("and cooldown reduction.")
			.addLine()
			.addLine("If *Cosmic Moonblade* killed a mob, reduce").styles(UNDERLINED)
			.addLine("your ability cooldowns even further.")
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (s)")
				.statValues(stat(DAMAGE_1), stat(a -> a.mDamage, DAMAGE_2))
			.addStatComparison("Cooldown Reduction: %p1 -> %p2 (max %t2)")
				.statValues(stat(REDUCTION_MULTIPLIER_1), stat(a -> a.mLevelReduction, REDUCTION_MULTIPLIER_2), stat(a -> a.mLevelCap, CAP_TICKS_2))
			.addStat("Kill Cooldown Reduction: %p (max %t)")
				.statValues(stat(a -> a.mKillCDR, REDUCTION_MULTIPLIER_KILL), stat(a -> a.mKillCDRCap, CAP_TICKS_KILL))
			.addDashedLine();
	}
}
