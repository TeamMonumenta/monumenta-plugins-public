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
import com.playmonumenta.plugins.cosmetics.skills.shaman.LightningTotemCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Chaotic;
import com.playmonumenta.plugins.itemstats.enchantments.Duelist;
import com.playmonumenta.plugins.itemstats.enchantments.HexEater;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Slayer;
import com.playmonumenta.plugins.itemstats.enchantments.Smite;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class LightningTotem extends TotemAbility {
	private static final int INTERVAL = 50;
	private static final int COOLDOWN = 26 * 20;
	private static final int TOTEM_DURATION = 16 * 20;
	private static final int AOE_RANGE_1 = 7;
	private static final int AOE_RANGE_2 = 9;
	private static final double DAMAGE_1_P = 0.35;
	private static final double DAMAGE_2_P = 0.45;
	private static final double DAMAGE_ELITE_2_P = 0.55;
	private static final double DAMAGE_1_M = 0.5;
	private static final double DAMAGE_2_M = 0.65;
	private static final double DAMAGE_ELITE_2_M = 0.8;
	private static final double DAMAGE_FLAT_1 = 2;
	private static final double DAMAGE_FLAT_2 = 3;
	private static final int SHOCK_DURATION = 15; // 15 ticks = 0.75s
	private static final double STORM_DAMAGE = 4;
	private static final double STORM_DAMAGE_RADIUS = 2.5;
	private static final int STORM_DURATION = 4 * 20;
	private static final int STORM_INTERVAL = 20;

	public static final String CHARM_PULSE_DELAY = "Lightning Totem Pulse Delay";
	public static final String CHARM_DURATION = "Lightning Totem Duration";
	public static final String CHARM_RADIUS = "Lightning Totem Radius";
	public static final String CHARM_COOLDOWN = "Lightning Totem Cooldown";
	public static final String CHARM_DAMAGE = "Lightning Totem Damage Multiplier";
	public static final String CHARM_DAMAGE_P = "Lightning Totem Projectile Damage Multiplier";
	public static final String CHARM_ELITE_DAMAGE_P = "Lightning Totem Elite Projectile Damage Multiplier";
	public static final String CHARM_DAMAGE_M = "Lightning Totem Melee Damage Multiplier";
	public static final String CHARM_ELITE_DAMAGE_M = "Lightning Totem Elite Melee Damage Multiplier";
	public static final String CHARM_ADDITIONAL_DAMAGE = "Lightning Totem Additional Damage";
	public static final String CHARM_SHOCK_DURATION = "Lightning Totem Shocked Duration";
	public static final String CHARM_STORM_DAMAGE = "Lightning Totem Lightning Storm Damage";
	public static final String CHARM_STORM_RADIUS = "Lightning Totem Lightning Storm Radius";
	public static final String CHARM_STORM_DURATION = "Lightning Totem Lightning Storm Duration";
	public static final String CHARM_STORM_PULSE_DELAY = "Lightning Totem Lightning Storm Pulse Delay";

	public static final AbilityInfo<LightningTotem> INFO =
		new AbilityInfo<>(LightningTotem.class, "Lightning Totem", LightningTotem::new)
			.linkedSpell(ClassAbility.LIGHTNING_TOTEM)
			.scoreboardId("LightningTotem")
			.shorthandName("LT")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Summon a totem that deals damage with your hits and shocks mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", LightningTotem::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.addAltPresetTrigger(new AbilityTriggerInfo<>("cast", "cast", LightningTotem::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.YELLOW_WOOL);

	private final int mInterval;
	private final double mLightningTotemDamageMultiplier;
	private final double mDamagePercentProj;
	private final double mEliteDamagePercentProj;
	private final double mDamagePercentMelee;
	private final double mEliteDamagePercentMelee;
	private final double mDamageFlat;
	private final int mShockDuration;
	private final double mStormDamage;
	private final double mStormRadius;
	private final int mStormDuration;
	private final int mStormInterval;
	private final LightningTotemCS mCosmetic;

	private final List<Location> mAllLocs = new ArrayList<>();
	private final List<BukkitTask> mStormTasks = new ArrayList<>();
	public double mDecayedTotemBuff = 0;

	public LightningTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Lightning Totem Projectile", "LightningTotem", "Lightning Totem");

		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, INTERVAL);
		mDamagePercentProj = (isLevelOne() ? DAMAGE_1_P : DAMAGE_2_P) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_P);
		mEliteDamagePercentProj = DAMAGE_ELITE_2_P + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ELITE_DAMAGE_P);
		mDamagePercentMelee = (isLevelOne() ? DAMAGE_1_M : DAMAGE_2_M) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_M);
		mEliteDamagePercentMelee = DAMAGE_ELITE_2_M + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ELITE_DAMAGE_M);
		mDamageFlat = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ADDITIONAL_DAMAGE, isLevelOne() ? DAMAGE_FLAT_1 : DAMAGE_FLAT_2);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, TOTEM_DURATION);
		setRadius(CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelOne() ? AOE_RANGE_1 : AOE_RANGE_2));
		mShockDuration = CharmManager.getDuration(mPlayer, CHARM_SHOCK_DURATION, SHOCK_DURATION);
		mStormDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_STORM_DAMAGE, STORM_DAMAGE);
		mStormRadius = CharmManager.getRadius(mPlayer, CHARM_STORM_RADIUS, STORM_DAMAGE_RADIUS);
		mStormDuration = CharmManager.getDuration(mPlayer, CHARM_STORM_DURATION, STORM_DURATION);
		mStormInterval = CharmManager.getDuration(mPlayer, CHARM_STORM_PULSE_DELAY, STORM_INTERVAL);
		mLightningTotemDamageMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, 1);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new LightningTotemCS());
	}

	@Override
	public void placeTotem(Location standLocation, Player player, ArmorStand stand) {
		mCosmetic.lightningTotemSpawn(standLocation, player, stand, getTotemRadius());
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}

		mCosmetic.lightningTotemTick(mPlayer, getTotemRadius(), standLocation, stand);
		mStormTasks.removeIf(BukkitTask::isCancelled);
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean chainLightning) {
		mCosmetic.lightningTotemPulse(mPlayer, standLocation, getTotemRadius());

		List<LivingEntity> affectedMobs = new Hitbox.SphereHitbox(standLocation, getTotemRadius()).getHitMobs();
		affectedMobs.removeIf(mob -> EntityUtils.isBoss(mob) || EntityUtils.isElite(mob) || EntityUtils.isCCImmuneMob(mob));

		double slowAmount = chainLightning ? ChainLightning.ENHANCE_OFFENSIVE_EFFICIENCY + CharmManager.getLevelPercentDecimal(mPlayer, ChainLightning.CHARM_OFFENSIVE_TOTEM_EFFICIENCY) : 1;
		slowAmount = Math.max(Math.min(slowAmount, 1), 0); // Ensures the slow amount is between 0 and 1 inclusive

		for (LivingEntity mob : affectedMobs) {
			EntityUtils.applySlow(mPlugin, mShockDuration, slowAmount, mob); // brief 100% slow effect
			mCosmetic.lightningTotemShock(mPlayer, mob);
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mTotem != null && mTotem.isValid()) {
			// Check if this is a critical strike
			double damage = 0;
			boolean meleeActivated = true;

			// Check for arrow critical
			if (event.getDamager() instanceof Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
				if (!MetadataUtils.checkOnceInRecentTicks(mPlugin, enemy, "LightningTotemHit", 5)) {
					return false;
				}

				ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(projectile);
				if (playerItemStats == null) {
					return false;
				}
				ItemStatManager.PlayerItemStats.ItemStatsMap itemStatsMap = playerItemStats.getItemStats();

				double projDamageAdd = itemStatsMap.get(AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat());
				projDamageAdd += Sniper.apply(mPlayer, enemy, itemStatsMap.get(EnchantmentType.SNIPER));
				projDamageAdd += PointBlank.apply(mPlayer, enemy, itemStatsMap.get(EnchantmentType.POINT_BLANK));
				projDamageAdd += HexEater.calculateHexDamage(mPlugin, true, mPlayer, (int) itemStatsMap.get(EnchantmentType.HEX_EATER), enemy);
				projDamageAdd += Smite.calculateSmiteDamage(true, mPlayer, (int) itemStatsMap.get(EnchantmentType.SMITE), enemy);
				projDamageAdd += Slayer.calculateSlayerDamage(true, mPlayer, (int) itemStatsMap.get(EnchantmentType.SLAYER), enemy);
				projDamageAdd += Duelist.calculateDuelistDamage(true, mPlayer, (int) itemStatsMap.get(EnchantmentType.DUELIST), enemy);
				projDamageAdd += Chaotic.calculateChaoticDamage(true, mPlayer, (int) itemStatsMap.get(EnchantmentType.CHAOTIC), enemy);

				boolean useEliteDamage = isLevelTwo() && (EntityUtils.isElite(enemy) || EntityUtils.isBoss(enemy) || EntityUtils.getMaxHealth(enemy) == enemy.getHealth());
				damage = (useEliteDamage ? mEliteDamagePercentProj : mDamagePercentProj) * projDamageAdd;
				meleeActivated = false;
			}
			// Check for melee (fully charged)
			else if (event.getType() == DamageEvent.DamageType.MELEE && mPlayer.getCooledAttackStrength(0) > 0.9) {
				if (!MetadataUtils.checkOnceInRecentTicks(mPlugin, enemy, "LightningTotemHit", 5)) {
					return false;
				}

				final ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();

				double attackDamageAdd = ItemStatUtils.getAttributeAmount(inMainHand, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) + 1;
				attackDamageAdd += HexEater.calculateHexDamage(mPlugin, true, mPlayer, ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.HEX_EATER), enemy);
				attackDamageAdd += Smite.calculateSmiteDamage(false, mPlayer, ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.SMITE), enemy);
				attackDamageAdd += Slayer.calculateSlayerDamage(false, mPlayer, ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.SLAYER), enemy);
				attackDamageAdd += Duelist.calculateDuelistDamage(false, mPlayer, ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.DUELIST), enemy);
				attackDamageAdd += Chaotic.calculateChaoticDamage(false, mPlayer, ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.CHAOTIC), enemy);

				boolean useEliteDamage = isLevelTwo() && (EntityUtils.isElite(enemy) || EntityUtils.isBoss(enemy) || EntityUtils.getMaxHealth(enemy) == enemy.getHealth());
				damage = (useEliteDamage ? mEliteDamagePercentMelee : mDamagePercentMelee) * attackDamageAdd;
			}

			if (damage > 0) {
				damage += mDamageFlat;
				damage *= mLightningTotemDamageMultiplier;
				damage += mDecayedTotemBuff;
				damage *= mSpiritualismMultiplier;

				// Check if either player or mob is in totem radius
				Location totemLocation = mTotem.getLocation();
				if (enemy.getLocation().distance(totemLocation) <= getTotemRadius()) {
					triggerLightningStrike(enemy, totemLocation, damage, meleeActivated);
				}
			}
		}

		return false;
	}

	private void triggerLightningStrike(LivingEntity target, Location totemLocation, double damage, boolean meleeActivated) {
		// Visual effects
		mCosmetic.lightningTotemStrike(mPlayer, totemLocation, target, meleeActivated);

		// Apply damage
		DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, false);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (isEnhanced() && mTotem != null && mTotem.isValid()) {
			Location targetLoc = event.getEntity().getLocation();
			List<Location> locsWithinKill = new ArrayList<>(mAllLocs);
			locsWithinKill.removeIf(loc -> loc.distance(targetLoc) > mStormRadius);
			if (locsWithinKill.isEmpty()) {
				mAllLocs.add(targetLoc);
				mStormTasks.add(new BukkitRunnable() {
					final Location mLoc = targetLoc.clone();
					final ItemStatManager.PlayerItemStats mStats = mPlugin.mItemStatManager
						.getPlayerItemStatsCopy(mPlayer);
					int mTicks = 0;

					@Override
					public void run() {
						mCosmetic.lightningTotemEnhancementStorm(mPlayer, mLoc, mStormRadius);

						if (mTicks % mStormInterval == 0) {
							mCosmetic.lightningTotemEnhancementStrike(mPlayer, mLoc, mStormRadius);
							for (LivingEntity entity : EntityUtils.getNearbyMobsInSphere(mLoc, mStormRadius, null)) {
								DamageUtils.damage(mPlayer, entity, new DamageEvent.Metadata(
										DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), mStats),
									mStormDamage, true, false, false);
							}
						}
						if (mTicks++ > mStormDuration) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1));
			}
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		mDecayedTotemBuff = 0;
		mCosmetic.lightningTotemExpire(mPlayer, standLocation, world);
		mAllLocs.clear();
	}

	private static Description<LightningTotem> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Summon a *Totem* that periodically roots").styles(Shaman.TOTEM_COLOR)
			.addLine("nearby mobs.")
			.addLine("(Elites/Bosses are immune)")
			.addLine()
			.addStat("Effect: Root for %t, every %t")
				.statValues(stat(a -> a.mShockDuration, SHOCK_DURATION), stat(a -> a.mInterval, INTERVAL))
			.addStat("Radius: %r1")
				.statValues(stat(a -> a.mRadius, AOE_RANGE_1))
			.addStat("Duration: %t")
				.statValues(stat(a -> a.mDuration, TOTEM_DURATION))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN))
			.addLine()
			.addLine("Mobs inside *Lightning Totem*'s area take").styles(UNDERLINED)
			.addLine("bonus magic damage (s) from your")
			.addLine("attacks and projectiles.")
			.addLine()
			.addStat("Bonus Damage (m): %d1 + %p1 (s)")
				.statValues(stat(a -> a.mDamageFlat, DAMAGE_FLAT_1), stat(a -> a.mDamagePercentMelee, DAMAGE_1_M))
			.tab().addLine("(of weapon's base damage)")
			.addStat("Bonus Damage (p): %d1 + %p1 (s)")
				.statValues(stat(a -> a.mDamageFlat, DAMAGE_FLAT_1), stat(a -> a.mDamagePercentProj, DAMAGE_1_P))
			.tab().addLine("(of weapon's base damage)")
			.addDashedLine();
	}

	private static Description<LightningTotem> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Lightning Totem*'s damage and radius.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Bonus Damage (m): %d1 + %p1 -> %d2 + %p2 (s)")
				.statValues(stat(DAMAGE_FLAT_1), stat(DAMAGE_1_M), stat(a -> a.mDamageFlat, DAMAGE_FLAT_2), stat(a -> a.mDamagePercentMelee, DAMAGE_2_M))
			.addStatComparison("Bonus Damage (p): %d1 + %p1 -> %d2 + %p2 (s)")
				.statValues(stat(DAMAGE_FLAT_1), stat(DAMAGE_1_P), stat(a -> a.mDamageFlat, DAMAGE_FLAT_2), stat(a -> a.mDamagePercentProj, DAMAGE_2_P))
			.addStatComparison("Radius: %r1 -> %r2")
				.statValues(stat(AOE_RANGE_1), stat(a -> a.mRadius, AOE_RANGE_2))
			.addLine()
			.addLine("*Lightning Totem* deals increased damage to").styles(UNDERLINED)
			.addLine("Elites, Bosses, and mobs with full health.")
			.addLine()
			.addStat("High-HP Damage (m): %d + %p (s)")
			.statValues(stat(a -> a.mDamageFlat, DAMAGE_FLAT_2), stat(a -> a.mEliteDamagePercentMelee, DAMAGE_ELITE_2_M))
			.tab().addLine("(of weapon's base damage)")
			.addStat("High-HP Damage (p): %d + %p (s)")
			.statValues(stat(a -> a.mDamageFlat, DAMAGE_FLAT_2), stat(a -> a.mEliteDamagePercentProj, DAMAGE_ELITE_2_P))
			.tab().addLine("(of weapon's base damage)")
			.addDashedLine();
	}

	private static Description<LightningTotem> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("When a mob dies inside *Lightning Totem*'s area,").styles(UNDERLINED)
			.addLine("create a lightning storm that periodically")
			.addLine("deals damage where it died.")
			.addLine("(Cannot create storms inside existing storms)")
			.addLine()
			.addStat("Storm Damage: %d (s) every %t")
				.statValues(stat(a -> a.mStormDamage, STORM_DAMAGE), stat(a -> a.mStormInterval, STORM_INTERVAL))
			.addStat("Storm Radius: %r")
				.statValues(stat(a -> a.mStormRadius, STORM_DAMAGE_RADIUS))
			.addStat("Storm Duration: %t")
				.statValues(stat(a -> a.mStormDuration, STORM_DURATION))
			.addDashedLine();
	}
}
