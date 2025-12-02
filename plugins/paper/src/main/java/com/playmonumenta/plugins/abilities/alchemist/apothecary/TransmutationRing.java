package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.TransmutationRingCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class TransmutationRing extends Ability implements PotionAbility, AbilityWithDuration, AbilityWithChargesOrStacks {
	private static final int COOLDOWN = 15 * 20;
	private static final int RADIUS = 6;
	private static final int DURATION = 8 * 20;
	private static final String DAMAGE_EFFECT_NAME = "TransmutationRingDamageEffect";
	private static final double DAMAGE_AMPLIFIER = 0.15;
	private static final int MAX_KILLS = 3;
	private static final double DAMAGE_INCREASE_PER_KILL_RAW = 3;
	private static final double DAMAGE_INCREASE_PER_KILL_MULT = 0.3;
	private static final double REFUND_POTION_AMOUNT = 1;
	private static final double ABSORPTION_HEALTH_PER_KILL = 2;
	private static final int ABSORPTION_HEALTH_DURATION = 8 * 20;
	public static final int EXTRA_BUFF_DURATION_ON_MAX_STACKS = 5 * 20;

	public static final String TRANSMUTATION_POTION_METAKEY = "TransmutationRingPotion";

	public static final String CHARM_COOLDOWN = "Transmutation Ring Cooldown";
	public static final String CHARM_RADIUS = "Transmutation Ring Radius";
	public static final String CHARM_DURATION = "Transmutation Ring Duration";
	public static final String CHARM_DAMAGE_AMPLIFIER = "Transmutation Ring Damage Amplifier";
	public static final String CHARM_MAX_KILLS = "Transmutation Ring Max Kills";
	public static final String CHARM_DAMAGE = "Transmutation Ring Explosion Damage";
	public static final String CHARM_ABSORPTION_PER_KILL = "Transmutation Ring Absorption Per Kill";
	public static final String CHARM_ABSORPTION_DURATION = "Transmutation Ring Absorption Duration";
	public static final String CHARM_POTION_REFUND_PER_KILL = "Transmutation Ring Potion Refund Per Kill";
	public static final String CHARM_EXTRA_BUFF_DURATION_ON_MAX_STACKS = "Transmutation Ring Extra Buff Duration On Max Stacks";

	public static final AbilityInfo<TransmutationRing> INFO =
		new AbilityInfo<>(TransmutationRing.class, "Transmutation Ring", TransmutationRing::new)
			.linkedSpell(ClassAbility.TRANSMUTATION_RING)
			.scoreboardId("Transmutation")
			.shorthandName("TR")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Deploy a circular zone that buffs the damage dealt by allies, and explodes upon expiring.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.GOLD_NUGGET);

	private final double mRadius;
	private final int mDuration;
	private final double mAmplifier;
	private final int mMaxKills;
	private final double mDamageRawPerKill;
	private final double mDamageMultPerKill;
	private final double mAbsorptionPerKill;
	private final double mPotionsPerKill;
	private final int mAbsorptionDuration;
	private final int mExtraBuffDurationOnMaxStacks;

	private @Nullable Location mCenter;
	private int mKills;

	private final TransmutationRingCS mCosmetic;

	private @Nullable BukkitTask mActiveTask;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable GruesomeAlchemy mGruesomeAlchemy;
	private @Nullable BrutalAlchemy mBrutalAlchemy;
	private int mCurrDuration = -1;

	public TransmutationRing(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mKills = 0;
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mAmplifier = DAMAGE_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_AMPLIFIER);
		mMaxKills = MAX_KILLS + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_KILLS);
		mDamageRawPerKill = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_INCREASE_PER_KILL_RAW);
		mDamageMultPerKill = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_INCREASE_PER_KILL_MULT);
		mAbsorptionPerKill = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_PER_KILL, ABSORPTION_HEALTH_PER_KILL);
		mPotionsPerKill = REFUND_POTION_AMOUNT + CharmManager.getLevel(mPlayer, CHARM_POTION_REFUND_PER_KILL);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, ABSORPTION_HEALTH_DURATION);
		mExtraBuffDurationOnMaxStacks = CharmManager.getDuration(mPlayer, CHARM_EXTRA_BUFF_DURATION_ON_MAX_STACKS,
			EXTRA_BUFF_DURATION_ON_MAX_STACKS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TransmutationRingCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
			mGruesomeAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, GruesomeAlchemy.class);
			mBrutalAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, BrutalAlchemy.class);
		});
	}

	@Override
	public void alchemistPotionThrown(ThrownPotion potion) {
		if (!isOnCooldown() && mPlayer.isSneaking() && (mActiveTask == null || mActiveTask.isCancelled())) {
			putOnCooldown();
			potion.setMetadata(TRANSMUTATION_POTION_METAKEY, new FixedMetadataValue(mPlugin, null));
		}
	}

	@Override
	public boolean createAura(Location loc, ThrownPotion potion, Vector originalPotionVelocity, ItemStatManager.PlayerItemStats playerItemStats) {
		if (!potion.hasMetadata(TRANSMUTATION_POTION_METAKEY)) {
			return false;
		}

		mCenter = loc;
		mCenter.setDirection(mCenter.toVector().subtract(mPlayer.getLocation().toVector()));

		mCosmetic.startEffect(mPlayer, mCenter, mRadius);

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);

		mActiveTask = new BukkitRunnable() {
			int mTicks = 0;
			final int mMaxTicks = mDuration;

			@Override
			public void run() {
				if (mTicks >= mMaxTicks || mKills >= mMaxKills || mCenter == null) {
					if (mCenter != null) {
						// Copy here and set to null before calling explode, so that mobs dying
						// from the explosion itself do not trigger the cosmetic and code again
						Location centerCopy = mCenter;
						mCenter = null;
						explode(Math.min(mMaxKills, mKills), centerCopy, Math.max(0, mMaxTicks - mTicks), playerItemStats);
					}
					mKills = 0;
					ClientModHandler.updateAbility(mPlayer, TransmutationRing.this);
					this.cancel();
					return;
				}

				List<Player> players = PlayerUtils.playersInRange(mCenter, mRadius, true);
				for (Player player : players) {
					mPlugin.mEffectManager.addEffect(
						player,
						DAMAGE_EFFECT_NAME,
						new PercentDamageDealt(20, mAmplifier)
							.deleteOnAbilityUpdate(true)
							.displaysTime(false)
					);
				}

				mCosmetic.periodicEffect(mPlayer, mCenter, mRadius, mTicks, mMaxTicks, mDuration, mKills, mMaxKills);

				mTicks += 5;
				if (mCurrDuration >= 0) {
					mCurrDuration += 5;
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, TransmutationRing.this);
			}
		}.runTaskTimer(mPlugin, 0, 5);

		return true;
	}

	private void explode(int kills, Location center, int remainingDuration, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null || kills == 0) {
			return;
		}

		double damage = (mDamageRawPerKill + mDamageMultPerKill * mAlchemistPotions.getDamage(playerItemStats)) * kills;
		double absorption = mAbsorptionPerKill * kills;
		Hitbox hitbox = new Hitbox.UprightCylinderHitbox(center.clone().add(0, -5, 0), 10, mRadius);
		mCosmetic.explode(mPlayer, center, mRadius);
		hitbox.getHitMobs().forEach(mob -> {
			DamageUtils.damage(
				mPlayer,
				mob,
				new DamageEvent.Metadata(
					DamageEvent.DamageType.MAGIC,
					mInfo.getLinkedSpell(),
					playerItemStats),
				damage,
				true,
				false,
				false);

			GruesomeAlchemy.tryDoEnhancementEffect(mGruesomeAlchemy, mob);
			BrutalAlchemy.tryDoEnhancementEffect(mBrutalAlchemy, mob);
		});

		hitbox.getHitPlayers(null, true)
			.forEach(player -> {
				if (remainingDuration != 0) {
					mPlugin.mEffectManager.clearEffects(player, DAMAGE_EFFECT_NAME);
					mPlugin.mEffectManager.addEffect(
						player,
						DAMAGE_EFFECT_NAME,
						new PercentDamageDealt(remainingDuration + mExtraBuffDurationOnMaxStacks, mAmplifier)
							.deleteOnAbilityUpdate(true)
							.displaysTime(true)
					);
				}
				if (isLevelTwo()) {
					AbsorptionUtils.addAbsorption(player, absorption, absorption, mAbsorptionDuration);
				}
			});
	}

	@Override
	public @Nullable Location entityDeathRadiusCenterLocation() {
		return mCenter;
	}

	@Override
	public double entityDeathRadius() {
		return mRadius;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (mKills >= mMaxKills) {
			return;
		}

		boolean isElite = EntityUtils.isElite(event.getEntity());
		if (isElite) {
			mKills += 2;
		} else {
			mKills++;
		}
		ClientModHandler.updateAbility(mPlayer, this);

		if (mCenter != null) {
			mCosmetic.effectOnKill(mPlayer, event.getEntity().getLocation(), mCenter, mKills, mMaxKills, isElite);
		}
		if (mAlchemistPotions != null && isLevelTwo()) {
			double potions = mAlchemistPotions.getChargeTime() * mPotionsPerKill * (isElite ? 2 : 1);
			mAlchemistPotions.modifyCurrentPotionTimer(potions);
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		ClassAbility classAbility = INFO.getLinkedSpell();
		int remainingCooldown = classAbility == null ? 0 : mPlugin.mTimers.getCooldown(mPlayer.getUniqueId(), classAbility);

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text("TR", INFO.getActionBarColor()))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		if (mCenter != null) {
			output = output.append(Component.text("%s/%s".formatted(getCharges(), mMaxKills), NamedTextColor.YELLOW));
		} else if (remainingCooldown > 0) {
			output = output.append(Component.text(((int) Math.ceil(remainingCooldown / 20.0)) + "s", NamedTextColor.GRAY));
		} else {
			output = output.append(Component.text("✓", NamedTextColor.GREEN, TextDecoration.BOLD));
		}

		return output;
	}

	private static Description<TransmutationRing> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Throw an Alchemist's Potion while sneaking to create an unstable Transmutation Ring at the potion's landing location. The ring lasts for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds and has a radius of ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks. While active, all players within the ring deal ")
			.addPercent(a -> a.mAmplifier, DAMAGE_AMPLIFIER)
			.add(" extra damage on all attacks. For each mob that dies within the ring, it gains a stack of instability, or two if it was an Elite, up to a maximum of ")
			.add(a -> a.mMaxKills, MAX_KILLS)
			.add(". Upon expiring or reaching maximum instability, the ring explodes, dealing ")
			.add(a -> a.mDamageRawPerKill, DAMAGE_INCREASE_PER_KILL_RAW)
			.add(" + ")
			.addPercent(a -> a.mDamageMultPerKill, DAMAGE_INCREASE_PER_KILL_MULT)
			.add(" of your potion's damage per stack to each mob still inside it. It also grants the remaining duration of its damage buff, plus ")
			.addDuration(a -> a.mExtraBuffDurationOnMaxStacks, EXTRA_BUFF_DURATION_ON_MAX_STACKS)
			.add("s extra if it had reached max stacks, to all players inside.")
			.addCooldown(COOLDOWN);
	}

	private static Description<TransmutationRing> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Each stack of instability gained now refunds you ")
			.add(a -> a.mPotionsPerKill, REFUND_POTION_AMOUNT)
			.add(" Alchemist's Potion. Additionally, all players caught inside the explosion now gain ")
			.add(a -> a.mAbsorptionPerKill, ABSORPTION_HEALTH_PER_KILL)
			.add(" absorption health for ")
			.addDuration(a -> a.mAbsorptionDuration, ABSORPTION_HEALTH_DURATION)
			.add("s for each stack of instability.");
	}

	@Override
	public int getCharges() {
		return Math.min(mMaxKills, mKills);
	}

	@Override
	public int getMaxCharges() {
		return mMaxKills;
	}
}
