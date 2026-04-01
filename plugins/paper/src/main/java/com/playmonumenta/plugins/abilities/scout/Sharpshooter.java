package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.scout.hunter.QuiverStorm;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.SharpshooterCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Multiload;
import com.playmonumenta.plugins.itemstats.enchantments.ThrowingKnife;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.math3.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.QUARTER_TICKS_PER_SECOND;
import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class Sharpshooter extends Ability implements AbilityWithChargesOrStacks {
	public static final String NO_TRACKING_METADATA = "SharpshooterUntrackable";

	private static final int SHARPSHOOTER_DECAY_TIMER = TICKS_PER_SECOND * 6;
	private static final int MISS_PENALTY = 3;
	private static final int MAX_STACKS_L1 = 20;
	private static final int MAX_STACKS_L2 = 30;
	private static final double PERCENT_DAMAGE_PER_STACK = 0.01;
	private static final double ARROW_SAVE_CHANCE = 0.3;
	private static final double PROJECTILE_SPEED = 0.01;
	private static final int STACKS_PER_PIERCE = 15;

	public static final String CHARM_STACK_DAMAGE = "Sharpshooter Stack Damage";
	public static final String CHARM_STACKS = "Sharpshooter Max Stacks";
	public static final String CHARM_RETRIEVAL = "Sharpshooter Arrow Save Chance";
	public static final String CHARM_DECAY = "Sharpshooter Stack Decay Time";
	public static final String CHARM_MISS = "Sharpshooter Stacks On Miss";
	public static final String CHARM_HIT = "Sharpshooter Stacks On Hit";
	public static final String CHARM_PROJ_SPEED = "Sharpshooter Stack Projectile Speed";
	public static final String CHARM_PIERCE = "Sharpshooter Stack Per Pierce";


	public static final AbilityInfo<Sharpshooter> INFO =
		new AbilityInfo<>(Sharpshooter.class, "Sharpshooter", Sharpshooter::new)
			.scoreboardId("Sharpshooter")
			.shorthandName("Ss")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Gain increased projectile damage. Landing shots further increases damage.")
			.displayItem(Material.TARGET);

	private final HashMap<Integer, Pair<Set<Projectile>, Integer>> TIME_TO_ARROW = new HashMap<>();
	private static final String SPAWN_TICK = "SPAWN_TICK";
	private static final String DO_NOT_PENALIZE = "SHARPSHOOTER_NO_MISS";

	private final int mMaxStacks;
	private final int mDecayTime;
	private final double mDamagePerStack;
	private final double mArrowSaveChance;
	private final int mMissPenalty;
	private final double mProjectileSpeed;
	private final int mStacksPerPierce;
	private final SharpshooterCS mCosmetic;
	private @Nullable PartingShot mPartingShot;

	private int mStacks = 0;
	private int mTicksToStackDecay = 0;
	private double mOverflow;

	public Sharpshooter(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mMaxStacks = (isLevelTwo() ? MAX_STACKS_L2 : MAX_STACKS_L1) + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS);
		mDecayTime = CharmManager.getDuration(mPlayer, CHARM_DECAY, SHARPSHOOTER_DECAY_TIMER);
		mDamagePerStack = PERCENT_DAMAGE_PER_STACK + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_STACK_DAMAGE);
		mArrowSaveChance = ARROW_SAVE_CHANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RETRIEVAL);
		mMissPenalty = MISS_PENALTY + (int) CharmManager.getLevel(mPlayer, CHARM_MISS);
		mProjectileSpeed = PROJECTILE_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_PROJ_SPEED);
		mStacksPerPierce = STACKS_PER_PIERCE + (int) CharmManager.getLevelPercentDecimal(mPlayer, CHARM_PIERCE);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new SharpshooterCS());
		Bukkit.getScheduler().runTask(plugin, () ->
			mPartingShot = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, PartingShot.class));
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		final DamageEvent.DamageType type = event.getType();

		mCosmetic.hitEffect(mPlayer, enemy);
		if (DamageEvent.DamageType.getAllProjectileTypes().contains(type)) {
			double multiplier = 1;
			multiplier += mStacks * mDamagePerStack;
			event.updateDamageWithMultiplier(multiplier);
		}

		return false; // only changes event damage
	}

	// This is to track arrows from a single shot instance (within a tick)
	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		trackArrow(projectile);
		return true;
	}

	public void trackArrow(Projectile projectile) {
		int time = Bukkit.getServer().getCurrentTick();

		if (!(projectile.getShooter() instanceof Player player)
			|| projectile.hasMetadata(NO_TRACKING_METADATA)) {
			return;
		}

		ItemStack item = ((Player) projectile.getShooter()).getInventory().getItemInMainHand();

		int sharpshooterCount = checkSharpshooterType(projectile, item);
		TIME_TO_ARROW.computeIfAbsent(time, (k) -> new Pair<>(new HashSet<>(), sharpshooterCount)).getFirst().add(projectile);
		MetadataUtils.setMetadata(projectile, SPAWN_TICK, time);

		// Want to track without accounting for penalty
		if (ItemStatUtils.hasEnchantment(item, EnchantmentType.RECOIL)
			|| projectile.hasMetadata(QuiverStorm.ARROW_METADATA)) {
			MetadataUtils.setMetadata(projectile, DO_NOT_PENALIZE, true);
		}

		if (isEnhanced()) {
			final ItemStatManager.PlayerItemStats stats = DamageListener.getProjectileItemStats(projectile);
			final ItemStatManager.PlayerItemStats.ItemStatsMap map = stats != null ? stats.getItemStats() : null;
			if (map != null) {
				// Quiver Storm handles pierce modification (QuiverStorm.java line 159)
				if (projectile instanceof AbstractArrow arrow && !arrow.hasMetadata(QuiverStorm.ARROW_METADATA)) {
					int pierce = (int) (mStacks * (1.0 / mStacksPerPierce));
					arrow.setPierceLevel(arrow.getPierceLevel() + pierce);
				}

				double projSpeed = 1;
				projSpeed *= 1 + mStacks * mProjectileSpeed;
				if (projSpeed != 1 && !ZoneUtils.hasZoneProperty(player.getLocation(), ZoneUtils.ZoneProperty.MASK_GEAR_PROJECTILE_SPEED)) {
					projectile.setVelocity(projectile.getVelocity().multiply(projSpeed));
				}
			}
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (EntityUtils.isAbilityTriggeringProjectile(proj, false)) {
			int time = MetadataUtils.getMetadata(proj, SPAWN_TICK, -1);

			Entity e = event.getHitEntity();
			if (e != null) {
				Pair<Set<Projectile>, Integer> pair = TIME_TO_ARROW.remove(time);

				if (pair != null) {
					int stackGain = pair.getSecond();
					addStacks(stackGain);
				}

			} else {
				Pair<Set<Projectile>, Integer> pair = TIME_TO_ARROW.get(time);

				if (pair != null) {
					Set<Projectile> set = pair.getFirst();
					set.remove(proj);
					if (set.isEmpty()) {
						if (!MetadataUtils.getMetadata(proj, DO_NOT_PENALIZE, false)) {
							miss();
						}
						TIME_TO_ARROW.remove(time);
					}
				}

			}
		}
	}

	@Override
	public void periodicTrigger(final boolean twoHertz, final boolean oneSecond, final int ticks) {
		if (mStacks > 0) {
			mTicksToStackDecay -= 5;

			if (mTicksToStackDecay <= 0) {
				mTicksToStackDecay = mDecayTime;
				mStacks--;
				showChargesMessage();
				ClientModHandler.updateAbility(mPlayer, this);
			}

			// Ran every 5 ticks, see AbilityCooldownDecrease
			if (mPartingShot != null && mPartingShot.isLevelTwo()) {
				int reduction = (int) Math.floor(QUARTER_TICKS_PER_SECOND * (mStacks * mPartingShot.getRechargeRate()));

				// handle overflow: we can't reduce by decimal ticks, so sum up the overflow
				// each time and reduce by an additional tick when overflow > 1
				double overflow = QUARTER_TICKS_PER_SECOND * mStacks - reduction;
				mOverflow += overflow;

				if (mOverflow >= 1) {
					mOverflow -= 1;
					reduction += 1;
				}

				mPlugin.mTimers.updateCooldown(mPlayer, ClassAbility.PARTING_SHOT, reduction);
			}
		}

		// For whatever reason an arrow exist for longer than 10s, exclude it outright
		if (oneSecond) {
			TIME_TO_ARROW.keySet().removeIf(t -> Bukkit.getServer().getCurrentTick() - t > 200);
		}
	}

	@Override
	public boolean playerConsumeArrowEvent() {
		if (isLevelTwo() && FastUtils.RANDOM.nextDouble() < mArrowSaveChance) {
			mCosmetic.arrowSave(mPlayer);
			return false;
		}
		return true;
	}

	public void addStacks(int stackGain) {
		mTicksToStackDecay = mDecayTime;
		mStacks = Math.min(mStacks + stackGain + (int) CharmManager.getLevel(mPlayer, CHARM_HIT), mMaxStacks);
		showChargesMessage();
		ClientModHandler.updateAbility(mPlayer, this);
	}

	public void miss() {
		mStacks = Math.max(mStacks - mMissPenalty, 0);
		showChargesMessage();
		ClientModHandler.updateAbility(mPlayer, this);
	}

	public void doNotTrack(Projectile proj) {
		proj.setMetadata(NO_TRACKING_METADATA, new FixedMetadataValue(mPlugin, 0));

		int time = Bukkit.getServer().getCurrentTick();
		Pair<Set<Projectile>, Integer> pair = TIME_TO_ARROW.get(time);
		if (pair != null) {
			pair.getFirst().remove(proj);
		}
	}

	// Exclusively for Quiver Storm as it inherits a % of an enchant
	public int getAdditionalPierce() {
		return isEnhanced() ? mStacks / mStacksPerPierce : 0;
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		final TextColor color = INFO.getActionBarColor();
		final String name = INFO.getHotbarName();
		final int charges = getCharges();
		final int maxCharges = getMaxCharges();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		output = output.append(Component.text(charges + "/" + maxCharges,
			(charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));

		return output;
	}

	// Sharpshooter stack is based on the draw time, not charge rate
	public static int checkSharpshooterType(Projectile proj, ItemStack item) {
		if (proj instanceof Trident) { // This extends AbstractArrow, check first
			return 2;
		}

		if ((proj instanceof AbstractArrow arrow
			&& (arrow.hasMetadata(QuiverStorm.ARROW_METADATA) || ThrowingKnife.isThrowingKnife(arrow) || !arrow.isCritical()))
			|| !Multiload.isAmmoFull(item)
			|| proj instanceof Snowball) {
			return 1;
		}

		if (item.getType() == Material.BOW) {
			return 5;
		}

		// Assume crossbow from here

		int quickChargeLevel = item.getEnchantmentLevel(Enchantment.QUICK_CHARGE);

		return Math.max(1, 6 - quickChargeLevel);
	}

	private static Description<Sharpshooter> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Hitting a mob with a projectile grants")
			.addLine("*Sharpshooter* stacks, which decay after %t").styles(UNDERLINED)
			.statValues(stat(a -> a.mDecayTime, SHARPSHOOTER_DECAY_TIMER))
			.addLine("not gaining any. Each stack grants projectile damage.")
			.addLine("(Stack gain is based on weapon's draw speed)")
			.addLine()
			.addLine("Missing will deduct %d stacks of *Sharpshooter*.").styles(UNDERLINED)
			.statValues(stat(a -> a.mMissPenalty, MISS_PENALTY))
			.addLine()
			.addStat("Damage Boost: +%p (p) per stack")
			.statValues(stat(a -> a.mDamagePerStack, PERCENT_DAMAGE_PER_STACK))
			.addStat("Max Stacks: %d1")
			.statValues(stat(a -> a.mMaxStacks, MAX_STACKS_L1))
			.addDashedLine();
	}

	private static Description<Sharpshooter> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Sharpshooter*'s max stacks.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Max Stacks: %d1 -> %d2 (p)")
			.statValues(stat(MAX_STACKS_L1), stat(a -> a.mMaxStacks, MAX_STACKS_L2))
			.addLine()
			.addLine("Gain a %p chance to save arrows.")
			.statValues(stat(a -> a.mArrowSaveChance, ARROW_SAVE_CHANCE))
			.addDashedLine();
	}

	private static Description<Sharpshooter> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Sharpshooter* stacks grant projectile speed.").styles(UNDERLINED)
			.addLine()
			.addStat("Projectile Speed: %p per stack")
			.statValues(stat(a -> a.mProjectileSpeed, PROJECTILE_SPEED))
			.addLine()
			.addLine("Increase arrow pierce by *1* for every %d stacks.").styles(WHITE)
			.statValues(stat(a -> a.mStacksPerPierce, STACKS_PER_PIERCE))
			.addDashedLine();
	}
}
