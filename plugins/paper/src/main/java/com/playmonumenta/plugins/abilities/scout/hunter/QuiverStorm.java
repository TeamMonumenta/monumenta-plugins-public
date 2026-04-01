package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.QuiverStormCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class QuiverStorm extends Ability implements AbilityWithChargesOrStacks {
	public static final String ARROW_METADATA = "QuiverStormMetadata";
	public static final double ENCHANT_RATIO = 0.25;
	private static final String LOCKDOWN_HIT = "LockdownHitThisTick";
	private static final String PREDATOR_HIT = "PredatorStrikeHitThisTick";

	// List of Enchantments to reduce
	private static final List<EnchantmentType> ENCHANT_LIST = new ArrayList<>(List.of(
		EnchantmentType.EXPLODING,
		EnchantmentType.EARTH_ASPECT,
		EnchantmentType.FIRE_ASPECT,
		EnchantmentType.ICE_ASPECT,
		EnchantmentType.WIND_ASPECT,
		EnchantmentType.THUNDER_ASPECT,
		EnchantmentType.BLEEDING,
		EnchantmentType.HARPOON,
		EnchantmentType.PUNCH,
		EnchantmentType.CURSE_OF_SHRAPNEL
		// EnchantmentType.PIERCING
	));

	private static final double DAMAGE_PERCENT_L1 = 0.25;
	private static final double DAMAGE_PERCENT_L2 = 0.35;
	private static final int PASSIVE_ARROW_L1 = 1;
	private static final int PASSIVE_ARROW_L2 = 2;
	private static final int MAX_ARROW_L1 = 3;
	private static final int MAX_ARROW_L2 = 5;
	private static final int DELAY_1 = 4;
	private static final int DELAY_2 = 3;
	private static final int PSTRIKE_ARROW = 3;
	private static final int LD_ARROW = 1;

	public static final String CHARM_DAMAGE = "Quiver Storm Damage";
	public static final String CHARM_MAX_STACKS = "Quiver Storm Max Stacks";
	public static final String CHARM_PASSIVE_ARROW = "Quiver Storm Passive Arrows";
	public static final String CHARM_PIERCE = "Quiver Storm Pierce";
	public static final String CHARM_DELAY = "Quiver Storm Arrow Delay";
	public static final String CHARM_PSTRIKE_REFUND = "Quiver Storm Predator Strike Arrow Recharge";
	public static final String CHARM_LOCKDOWN_REFUND = "Quiver Storm Lockdown Arrow Recharge";

	public static final AbilityInfo<QuiverStorm> INFO =
		new AbilityInfo<>(QuiverStorm.class, "Quiver Storm", QuiverStorm::new)
			.linkedSpell(ClassAbility.QUIVER_STORM)
			.scoreboardId("QuiverStorm")
			.shorthandName("QS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Projectiles will fire additional arrows.")
			.displayItem(Material.BLAZE_ROD);

	private final double mDamagePercent;
	private final int mDelay;
	private final int mPierce;
	private final int mMaxCharges;
	private int mCharges;
	private final int mPassive;
	private final int mPstrikeArrowRefund;
	private final int mLockdownRefund;
	private final QuiverStormCS mCosmetic;

	private int mCastTime;
	private @Nullable Sharpshooter mSharpshooter;

	public QuiverStorm(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_PERCENT_L1 : DAMAGE_PERCENT_L2);
		mMaxCharges = (isLevelOne() ? MAX_ARROW_L1 : MAX_ARROW_L2) + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_STACKS);
		mDelay = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DELAY, isLevelOne() ? DELAY_1 : DELAY_2);
		mPierce = Math.clamp((int) CharmManager.getLevel(mPlayer, CHARM_PIERCE), 0, 100);
		mPassive = (isLevelOne() ? PASSIVE_ARROW_L1 : PASSIVE_ARROW_L2) + (int) CharmManager.getLevel(mPlayer, CHARM_PASSIVE_ARROW);
		mPstrikeArrowRefund = Math.clamp(PSTRIKE_ARROW + (int) CharmManager.getLevel(mPlayer, CHARM_PSTRIKE_REFUND), 0, mMaxCharges);
		mLockdownRefund = Math.clamp(LD_ARROW + (int) CharmManager.getLevel(mPlayer, CHARM_LOCKDOWN_REFUND), 0, mMaxCharges);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new QuiverStormCS());

		mCastTime = Bukkit.getServer().getCurrentTick();

		mCharges = Math.min(AbilityManager.getManager().getTrackedCharges(mPlayer, ClassAbility.QUIVER_STORM), mMaxCharges);

		Bukkit.getScheduler().runTask(plugin, () ->
			mSharpshooter = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Sharpshooter.class));
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		int currTick = Bukkit.getServer().getCurrentTick();

		if (!EntityUtils.isAbilityTriggeringProjectile(projectile, true)
			|| projectile.hasMetadata(ARROW_METADATA)
			|| currTick - mCastTime < 1
			|| Grappling.playerHoldingHook(mPlayer)
			|| PredatorStrike.hasPredatorStrikeReady(mPlayer)) {
			return true;
		}

		final ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		final ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();

		if ((ItemStatUtils.hasEnchantment(inMainHand, EnchantmentType.TWO_HANDED)
			&& !(ItemUtils.isNullOrAir(inOffHand) || ItemStatUtils.hasEnchantment(inOffHand, EnchantmentType.WEIGHTLESS)))
			|| ItemUtils.isShootableItem(inOffHand)) {
			return false;
		}

		ItemStatManager.PlayerItemStats stats = Plugin.getInstance().mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		DamageListener.appendProjectileStats(stats, projectile);

		final ItemStatManager.PlayerItemStats.ItemStatsMap map = stats.getItemStats();
		if (map != null) {
			ItemStat piercing = Objects.requireNonNull(EnchantmentType.PIERCING.getItemStat());
			double piercingLvl = map.get(piercing);
			piercingLvl += mSharpshooter != null ? mSharpshooter.getAdditionalPierce() : 0;
			map.set(piercing, piercingLvl * ENCHANT_RATIO);

			for (EnchantmentType enchant : ENCHANT_LIST) {
				double lvl = map.get(Objects.requireNonNull(enchant.getItemStat()));

				map.set(Objects.requireNonNull(enchant.getItemStat()), lvl * ENCHANT_RATIO);
			}
		}

		mCastTime = currTick;

		final int arrows = mPassive + consumeAllCharges();

		cancelOnDeath(new BukkitRunnable() {
			int mArrows = arrows;

			@Override
			public void run() {
				if (mArrows <= 0) {
					this.cancel();
					return;
				}
				mCosmetic.arrowLaunch(mPlayer);
				shootProjectile(inMainHand, stats);
				mArrows--;
			}
		}.runTaskTimer(mPlugin, mDelay, mDelay));

		return true;
	}

	private void shootProjectile(final ItemStack inMainHand, ItemStatManager.PlayerItemStats stats) {
		float projSpeed = ItemUtils.getVanillaProjectileSpeed(inMainHand);
		AbstractArrow proj = (AbstractArrow) EntityUtils.spawnProjectile(mPlayer, 0, 0, new Vector(0, 0, 0), projSpeed, EntityType.ARROW);

		proj.setMetadata(ARROW_METADATA, new FixedMetadataValue(mPlugin, 0));
		proj.setShooter(mPlayer);
		proj.setPierceLevel(mPierce);

		proj.setMetadata(DamageListener.DO_NOT_REPLACE_METADATA, new FixedMetadataValue(Plugin.getInstance(), 0));
		DamageListener.addProjectileItemStats(proj.getUniqueId(), stats);

		ProjectileLaunchEvent event = new ProjectileLaunchEvent(proj);
		Bukkit.getPluginManager().callEvent(event);

		if (!event.isCancelled()) {
			mCosmetic.arrowEffect(mPlugin, proj);
		}

		proj.setCritical(true);
		proj.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getDamager() instanceof AbstractArrow proj && proj.hasMetadata(ARROW_METADATA)) {
			event.setCancelled(true);

			double dmg = AbilityUtils.projectileFinalDamage(proj, enemy, 0, mDamagePercent);
			DamageUtils.damage(mPlayer, enemy,
				new DamageEvent.Metadata(DamageEvent.DamageType.PROJECTILE_SKILL,
					mInfo.getLinkedSpell(),
					DamageListener.getProjectileItemStats(proj)),
				dmg, true, false, false);

			return false;
		}

		ClassAbility ability = event.getAbility();

		boolean hitLockdown = ability == ClassAbility.LOCKDOWN
			&& MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, LOCKDOWN_HIT);

		boolean hitPredatorStrike = ability == ClassAbility.PREDATOR_STRIKE
			&& MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, PREDATOR_HIT);

		if (hitLockdown) {
			addCharge(mLockdownRefund);
		} else if (hitPredatorStrike) {
			addCharge(mPstrikeArrowRefund);
		}

		return false;
	}

	private void addCharge(int count) {
		int prevCharges = mCharges;
		mCharges = Math.min(mMaxCharges, mCharges + count);

		if (mMaxCharges != prevCharges) {
			showChargesMessage();
		}

		updateAbility();
	}

	private int consumeAllCharges() {
		if (mCharges <= 0) {
			return 0;
		}

		int charges = mCharges;
		mCharges = 0;

		if (mMaxCharges > 1) {
			showChargesMessage();
		}

		updateAbility();

		return charges;
	}

	@Override
	public void updateAbility() {
		AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.QUIVER_STORM, mCharges);
		ClientModHandler.updateAbility(mPlayer, this);
	}

	private static Description<QuiverStorm> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Firing a projectile will fire extra arrows that")
			.addLine("inherits %p of non-damage enchants.")
			.statValues(stat(ENCHANT_RATIO))
			.addLine()
			.addStat("Damage: %p1 (of weapon damage) (p) (per arrow)")
			.statValues(stat(a -> a.mDamagePercent, DAMAGE_PERCENT_L1))
			.addStat("Fire Rate: %t1")
			.statValues(stat(a -> a.mDelay, DELAY_1))
			.addStat("Arrows: %d1")
			.statValues(stat(a -> a.mPassive, PASSIVE_ARROW_L1))
			.addLine()
			.addLine("Landing *Lockdown* adds %d arrows to your next").styles(UNDERLINED)
			.statValues(stat(a -> a.mLockdownRefund, LD_ARROW))
			.addLine("shot, whereas landing *Predator Strike* adds %d arrows.").styles(UNDERLINED)
			.statValues(stat(a -> a.mPstrikeArrowRefund, PSTRIKE_ARROW))
			.addLine()
			.addStat("Max Arrows: %d1")
			.statValues(stat(a -> a.mMaxCharges, MAX_ARROW_L1))
			.addDashedLine();
	}

	private static Description<QuiverStorm> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Quiver Storm*'s damage,").styles(UNDERLINED)
			.addLine("fire rate, and max arrow count.")
			.addLine()
			.addStatComparison("Damage: %p1 -> %p2")
			.statValues(stat(DAMAGE_PERCENT_L1), stat(a -> a.mDamagePercent, DAMAGE_PERCENT_L2))
			.addStatComparison("Fire Rate: %t1 -> %t2")
			.statValues(stat(DELAY_1), stat(a -> a.mDelay, DELAY_2))
			.addStatComparison("Max Arrows: %d1 -> %d2")
			.statValues(stat(MAX_ARROW_L1), stat(a -> a.mMaxCharges, MAX_ARROW_L2))
			.addLine()
			.addLine("Gain an additional passive arrow.")
			.addLine()
			.addStatComparison("Arrows: %d1 -> %d2")
			.statValues(stat(PASSIVE_ARROW_L1), stat(a -> a.mPassive, PASSIVE_ARROW_L2))
			.addDashedLine();
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}
}
