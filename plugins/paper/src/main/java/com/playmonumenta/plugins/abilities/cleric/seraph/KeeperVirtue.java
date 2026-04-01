package com.playmonumenta.plugins.abilities.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithCustomDisplay;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.seraph.KeeperVirtueCS;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Allay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.GREY;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class KeeperVirtue extends Ability implements AbilityWithCustomDisplay {
	public static final int HARMING_COOLDOWN_1 = 5 * 20;
	public static final int HARMING_COOLDOWN_2 = 4 * 20;
	public static final int SHIELDING_COOLDOWN_1 = 10 * 20;
	public static final int SHIELDING_COOLDOWN_2 = 8 * 20;
	private static final int DAMAGE_R2 = 12;
	private static final int DAMAGE_TOR_R2 = 16;
	private static final int DAMAGE_R3 = 16;
	private static final int DAMAGE_TOR_R3 = 20;
	private static final double HARMING_RADIUS = 1.5;
	private static final int FIRE_DURATION = 5 * 20;
	private static final double ABSORPTION = 0.2;
	private static final int ABSORPTION_DURATION = 6 * 20;
	private static final int HIT_NEGATIONS = 1;
	private static final int HIT_NEGATION_DURATION = 4 * 20;
	private static final int ENHANCE_STUN_DURATION = 10;
	private static final int ACTION_TIME = 8;
	private static final int ACTION_RANGE = 30;
	private static final double REDIRECT_RANGE = 3;
	public static final int HARMING_FLARE_CAPACITY = 2;
	public static final int SHIELDING_FLARE_CAPACITY = 2;
	private static final double VIRTUE_MOVESPEED = 4;

	public static final String CHARM_DAMAGE = "Keeper Virtue Damage";
	public static final String CHARM_HARMING_RADIUS = "Keeper Virtue Radius";
	public static final String CHARM_FIRE_DURATION = "Keeper Virtue Fire Duration";
	public static final String CHARM_ABSORPTION = "Keeper Virtue Absorption";
	public static final String CHARM_ABSORPTION_DURATION = "Keeper Virtue Absorption Duration";
	public static final String CHARM_HIT_NEGATIONS = "Keeper Virtue Hit Negations";
	public static final String CHARM_HIT_NEGATION_DURATION = "Keeper Virtue Hit Negation Duration";
	public static final String CHARM_TOR_DAMAGE = "Keeper Virtue Touch of Radiance Damage";
	public static final String CHARM_TOR_STUN_DURATION = "Keeper Virtue Touch of Radiance Stun Duration";
	public static final String CHARM_HARMING_COOLDOWN = "Keeper Virtue Harming Flare Cooldown";
	public static final String CHARM_SHIELDING_COOLDOWN = "Keeper Virtue Shielding Flare Cooldown";
	public static final String CHARM_ACTION_TIME = "Keeper Virtue Action Time";
	public static final String CHARM_ACTION_RANGE = "Keeper Virtue Action Range";
	public static final String CHARM_REDIRECT_RANGE = "Keeper Virtue Redirect Range";
	public static final String CHARM_HARMING = "Keeper Virtue Harming Flares";
	public static final String CHARM_SHIELDING = "Keeper Virtue Shielding Flares";

	public static final Style VIRTUE_COLOR = Style.style(TextColor.color(0xD9A336));
	public static final Style SHIELDING_COLOR = Style.style(TextColor.color(0x33DDCC));
	public static final Style HARMING_COLOR = Style.style(TextColor.color(0xDD3333));

	public static final Map<Allay, Player> mVirtuePlayerMap = new HashMap<>();
	public @Nullable Allay mBoss = null;
	public @Nullable LivingEntity mTarget = null; // Only used for turning the Virtue

	public static final AbilityInfo<KeeperVirtue> INFO =
		new AbilityInfo<>(KeeperVirtue.class, "Keeper Virtue", KeeperVirtue::new)
			.linkedSpell(ClassAbility.KEEPER_VIRTUE)
			.scoreboardId("KeeperVirtue")
			.shorthandName("KV")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("An angelic spirit follows you, supporting nearby players and attacking mobs.")
			.displayItem(Material.MUSIC_DISC_RELIC);

	public final double mDamage;
	public final double mDamageToR;
	public final double mHarmingRadius;
	public final int mFireDuration;
	public final double mAbsorption;
	public final int mAbsorptionDuration;
	public final int mHitNegations;
	public final int mHitNegationDuration;
	public final int mEnhanceStunDuration;
	public final int mActionTime;
	public final double mActionRange;
	public final double mRedirectRange;
	public final int mHarmingCooldown;
	public final int mShieldingCooldown;

	private final int mHarmingFlareCapacity;
	private final int mShieldingFlareCapacity;
	public int mTicksSinceTargetChance = 0;

	private @Nullable KeeperVirtueShieldingFlare mKeeperVirtueShieldingFlare;
	private @Nullable KeeperVirtueHarmingFlare mKeeperVirtueHarmingFlare;
	public final KeeperVirtueCS mCosmetic;

	public KeeperVirtue(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, ServerProperties.getAbilityEnhancementsEnabled(player) ? DAMAGE_R3 : DAMAGE_R2);
		mDamageToR = CharmManager.calculateFlatAndPercentValue(player, CHARM_TOR_DAMAGE, CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, ServerProperties.getAbilityEnhancementsEnabled(player) ? DAMAGE_TOR_R3 : DAMAGE_TOR_R2));
		mHarmingRadius = CharmManager.getRadius(player, CHARM_HARMING_RADIUS, HARMING_RADIUS);
		mFireDuration = CharmManager.getDuration(player, CHARM_FIRE_DURATION, FIRE_DURATION);
		mAbsorption = ABSORPTION + CharmManager.getLevelPercentDecimal(player, CHARM_ABSORPTION);
		mAbsorptionDuration = CharmManager.getDuration(player, CHARM_ABSORPTION_DURATION, ABSORPTION_DURATION);
		mHitNegations = HIT_NEGATIONS + (int) CharmManager.getLevel(player, CHARM_HIT_NEGATIONS);
		mHitNegationDuration = CharmManager.getDuration(player, CHARM_HIT_NEGATION_DURATION, HIT_NEGATION_DURATION);
		mEnhanceStunDuration = CharmManager.getDuration(player, CHARM_TOR_STUN_DURATION, ENHANCE_STUN_DURATION);
		mActionTime = CharmManager.getDuration(player, CHARM_ACTION_TIME, ACTION_TIME);
		mActionRange = CharmManager.getRadius(player, CHARM_ACTION_RANGE, ACTION_RANGE);
		mRedirectRange = CharmManager.getRadius(player, CHARM_REDIRECT_RANGE, REDIRECT_RANGE);
		mHarmingCooldown = CharmManager.getCooldown(player, CHARM_HARMING_COOLDOWN, isLevelTwo() ? HARMING_COOLDOWN_2 : HARMING_COOLDOWN_1);
		mShieldingCooldown = CharmManager.getCooldown(player, CHARM_SHIELDING_COOLDOWN, isLevelTwo() ? SHIELDING_COOLDOWN_2 : SHIELDING_COOLDOWN_1);
		mHarmingFlareCapacity = HARMING_FLARE_CAPACITY + (int) CharmManager.getLevel(player, CHARM_HARMING);
		mShieldingFlareCapacity = SHIELDING_FLARE_CAPACITY + (int) CharmManager.getLevel(player, CHARM_SHIELDING);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new KeeperVirtueCS());
		Bukkit.getScheduler().runTask(plugin, () -> mKeeperVirtueShieldingFlare = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, KeeperVirtueShieldingFlare.class));
		Bukkit.getScheduler().runTask(plugin, () -> mKeeperVirtueHarmingFlare = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, KeeperVirtueHarmingFlare.class));
	}

	@Override
	public void invalidate() {
		if (mBoss != null) {
			mBoss.remove();
			mVirtuePlayerMap.remove(mBoss);
			mBoss = null;
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mBoss != null && !mBoss.isValid()) {
			mBoss = null;
		}
		if (mBoss == null) {
			mBoss = (Allay) LibraryOfSoulsIntegration.summon(mPlayer.getLocation().add(mPlayer.getLocation().getDirection().setY(0).normalize().multiply(2)).add(0, 1.5, 0), mCosmetic.getLosName());
			if (mBoss == null) {
				MMLog.warning("Failed to summon KeeperVirtue");
				return;
			}
			mBoss.customName(mCosmetic.getComponentName());
			mVirtuePlayerMap.put(mBoss, mPlayer);

			cancelOnDeath(new BukkitRunnable() {
				double mRadian = FastUtils.randomDoubleInRange(0, Math.PI);

				@Override
				public void run() {
					if (mBoss == null || !mBoss.isValid() || !mPlayer.isValid() || !mPlayer.isOnline() || mBoss.getWorld() != mPlayer.getWorld()) {
						if (mBoss != null) {
							invalidate();
						}
						this.cancel();
						return;
					}

					// movement
					if (mBoss == null) {
						return;
					}
					Location allayLoc = mBoss.getLocation();

					Location playerLoc = LocationUtils.getEntityCenter(mPlayer);
					Vector direction = LocationUtils.getDirectionTo(playerLoc, allayLoc);
					Vector direction2 = direction.clone();
					double distance = allayLoc.distance(playerLoc);
					// Teleport to the player when very far away
					if (distance > 32) {
						allayLoc = playerLoc.clone().subtract(direction2.multiply(-2.5));
					} else {
						if (distance < 4.5) {
							direction2.multiply(0.5 * (distance - 2.5)).setY(direction.getY());
						} else {
							direction2.multiply(Math.pow(1.25, Math.min(4.5, distance - 4.5))).setY(direction.getY());
						}
						allayLoc.add(direction2.clone().multiply(VIRTUE_MOVESPEED / 20));
					}
					if (mTarget != null) {
						// Face the target when shooting flares
						allayLoc.setDirection(LocationUtils.getDirectionTo(mTarget.getLocation(), allayLoc));
					} else {
						allayLoc.setDirection(direction.clone().setY(0));
					}

					// bobbing
					mBoss.teleport(allayLoc.clone().add(0, FastMath.sin(mRadian) * 0.05, 0));
					mRadian += Math.PI / 20D; // Finishes a sin bob in (20 * 2) ticks
				}
			}.runTaskTimer(mPlugin, 0, 1));
		}

		if (mTicksSinceTargetChance > 15 && mTarget != null) {
			mTarget = null;
		}
		mTicksSinceTargetChance += 5;
	}

	public static boolean virtueBelongsTo(Allay allay, Player player) {
		return mVirtuePlayerMap.get(allay) != null && mVirtuePlayerMap.get(allay) == player;
	}

	@Override
	public Component customDisplayComponent() {
		Component display = Component.text("");
		if (mKeeperVirtueShieldingFlare == null || mKeeperVirtueHarmingFlare == null) {
			return display;
		}
		for (int i = 0; i < mShieldingFlareCapacity; i++) {
			display = display.append(Component.text("⏺", i < mKeeperVirtueShieldingFlare.getCharges() ? SHIELDING_COLOR : GREY));
		}
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ClassAbility.KEEPER_VIRTUE_SHIELDING)) {
			display = display.append(Component.text(" " + (int) Math.ceil(mPlugin.mTimers.getCooldown(mPlayer.getUniqueId(), ClassAbility.KEEPER_VIRTUE_SHIELDING) / 20.0) + "s", SHIELDING_COLOR));
		}

		if (mShieldingFlareCapacity > 0 && mHarmingFlareCapacity > 0) {
			display = display.append(Component.text(" "));
		}

		for (int i = 0; i < mHarmingFlareCapacity; i++) {
			display = display.append(Component.text("⏺", i < mKeeperVirtueHarmingFlare.getCharges() ? HARMING_COLOR : GREY));
		}
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ClassAbility.KEEPER_VIRTUE_HARMING)) {
			display = display.append(Component.text(" " + (int) Math.ceil(mPlugin.mTimers.getCooldown(mPlayer.getUniqueId(), ClassAbility.KEEPER_VIRTUE_HARMING) / 20.0) + "s", HARMING_COLOR));
		}
		return display;
	}

	private static Description<KeeperVirtue> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Summon a *Virtue* that holds %d *Shielding Flares*").styles(VIRTUE_COLOR, SHIELDING_COLOR)
				.statValues(stat(a -> a.mShieldingFlareCapacity, SHIELDING_FLARE_CAPACITY))
			.addLine("and %d *Harming Flares*.").styles(HARMING_COLOR)
				.statValues(stat(a -> a.mHarmingFlareCapacity, HARMING_FLARE_CAPACITY))
			.addLine()
			.addLine("Casting *Hand of Light*, *Touch of Radiance* or").styles(UNDERLINED, UNDERLINED)
			.addLine("*Hallowed Beam* on a player will send a *Shielding*").styles(UNDERLINED, SHIELDING_COLOR)
			.addLine("*Flare* to that player, buffing them.").styles(SHIELDING_COLOR)
			.addLine()
			.addStat("Effect: +%p Absorption for %t")
				.statValues(stat(a -> a.mAbsorption, ABSORPTION), stat(a -> a.mAbsorptionDuration, ABSORPTION_DURATION))
			.addStat("Effect: %d Hit Blocked (m/p) for %t")
				.statValues(stat(a -> a.mHitNegations, HIT_NEGATIONS), stat(a -> a.mHitNegationDuration, HIT_NEGATION_DURATION))
			.addStat("Cooldown: %t1 (per Shielding Flare)")
				.statValues(stat(a -> a.mShieldingCooldown, SHIELDING_COOLDOWN_1))
			.addLine()
			.addLine("Attacks, projectiles, *Hallowed Beam* and *Ethereal*").styles(UNDERLINED, UNDERLINED)
			.addLine("*Ascension* will send a *Harming Flare* to the attacked").styles(UNDERLINED, HARMING_COLOR)
			.addLine("mob, or the closest mob within %d blocks if it died.")
				.statValues(stat(a -> a.mRedirectRange, REDIRECT_RANGE))
			.addLine()
			.addStat("Damage: %d0R (s)")
				.statValues(perRegion(a -> a.mDamage, DAMAGE_R2, DAMAGE_R3))
			.addStat("Effect: Fire for %t")
				.statValues(stat(a -> a.mFireDuration, FIRE_DURATION))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mHarmingRadius, HARMING_RADIUS))
			.addStat("Cooldown: %t1 (per Harming Flare)")
				.statValues(stat(a -> a.mHarmingCooldown, HARMING_COOLDOWN_1))
			.addDashedLine();
	}

	private static Description<KeeperVirtue> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Reduce the cooldowns of both flares.")
			.addLine()
			.addStatComparison("Cooldown: %t1 -> %t2 (per Shielding Flare)")
				.statValues(stat(SHIELDING_COOLDOWN_1), stat(a -> a.mShieldingCooldown, SHIELDING_COOLDOWN_2))
			.addStatComparison("Cooldown: %t1 -> %t2 (per Harming Flare)")
				.statValues(stat(HARMING_COOLDOWN_1), stat(a -> a.mHarmingCooldown, HARMING_COOLDOWN_2))
			.addLine()
			.addLine("*Sanctified Armor* activating will now send a").styles(UNDERLINED)
			.addLine("*Shielding Flare* to yourself.").styles(SHIELDING_COLOR)
			.addLine()
			.addLine("*Touch of Radiance* can now be cast on the *Virtue*,").styles(UNDERLINED, VIRTUE_COLOR)
			.addLine("making *Harming Flares* deal more damage and").styles(HARMING_COLOR)
			.addLine("briefly stun targets during the effect.")
			.addLine("(Does not stun in their radius.)")
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2R (s)")
				.statValues(perRegion(DAMAGE_R2, DAMAGE_R3), perRegion(a -> a.mDamageToR, DAMAGE_TOR_R2, DAMAGE_TOR_R3))
			.addStat("Effect: Stun for %t")
				.statValues(stat(a -> a.mEnhanceStunDuration, ENHANCE_STUN_DURATION))
			.addDashedLine();
	}
}
