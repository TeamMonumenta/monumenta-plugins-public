package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DivineJusticeCS;
import com.playmonumenta.plugins.effects.DivineJusticeInvuln;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.CritScaling;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import java.util.NavigableSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class DivineJustice extends Ability implements AbilityWithChargesOrStacks {
	public static final String NAME = "Divine Justice";
	public static final ClassAbility ABILITY = ClassAbility.DIVINE_JUSTICE;

	public static final int DAMAGE = 5;
	public static final double DAMAGE_MULTIPLIER_1 = 0.15;
	public static final double DAMAGE_MULTIPLIER_2 = 0.3;
	public static final double HEALING_MULTIPLIER_OWN = 0.1;
	public static final double HEALING_MULTIPLIER_OTHER = 0.05;
	public static final int RADIUS = 12;
	public static final double ENHANCEMENT_ASH_CHANCE = 0.33;
	public static final int ENHANCEMENT_ASH_DURATION = Constants.TICKS_PER_SECOND * 10;
	public static final double ENHANCEMENT_ASH_BONUS_DAMAGE = 0.04;
	public static final double ENHANCEMENT_BONUS_DAMAGE_MAX = 0.32;
	public static final int ENHANCEMENT_ASH_BONUS_DAMAGE_DURATION = Constants.TICKS_PER_SECOND * 20;
	public static final int ENHANCEMENT_BONE_SHARD_BONUS_DAMAGE_DURATION = Constants.TICKS_PER_MINUTE * 4;
	public static final String ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME = "DivineJusticeBonusDamageEffect";
	public static final String CHARM_DAMAGE = "Divine Justice Percent Damage";
	public static final String CHARM_SELF = "Divine Justice Self Heal";
	public static final String CHARM_ALLY = "Divine Justice Ally Heal";
	public static final String CHARM_HEAL_RADIUS = "Divine Justice Ally Heal Radius";
	public static final String CHARM_ENHANCE_DAMAGE = "Divine Justice Enhancement Damage Modifier";
	public static final String CHARM_ENHANCE_DURATION = "Divine Justice Enhancement Duration";

	public static final AbilityInfo<DivineJustice> INFO =
		new AbilityInfo<>(DivineJustice.class, NAME, DivineJustice::new)
			.linkedSpell(ABILITY)
			.scoreboardId("DivineJustice")
			.shorthandName("DJ")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Deal extra damage on critical melee or projectile attacks against Undead enemies and heal when killing Undead enemies.")
			.remove(DivineJustice::remove)
			.displayItem(Material.IRON_SWORD);

	public final DivineJusticeCS mCosmetic;

	// Passive damage to share with Holy Javelin
	public double mLastPassiveDJDamage = 0;

	private final double mPercentDamage;
	private final double mSelfHeal;
	private final double mAllyHeal;
	private final double mRadius;
	private final double mEnhanceDamage;
	private final int mEnhanceDuration;

	private int mComboNumber = 0;
	private @Nullable BukkitRunnable mComboRunnable = null;
	private double mPriorAmount = 0;

	public DivineJustice(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentDamage = (isLevelOne() ? DAMAGE_MULTIPLIER_1 : DAMAGE_MULTIPLIER_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mSelfHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SELF, HEALING_MULTIPLIER_OWN);
		mAllyHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ALLY, HEALING_MULTIPLIER_OTHER);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_HEAL_RADIUS, RADIUS);
		mEnhanceDamage = ENHANCEMENT_ASH_BONUS_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_DAMAGE);
		mEnhanceDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCE_DURATION, ENHANCEMENT_ASH_BONUS_DAMAGE_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new DivineJusticeCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		/* Prevent DJ from triggering if the enemy is not affected by Crusade */
		if (!Crusade.enemyTriggersAbilities(enemy)) {
			return false;
		}

		/* DamageEvents do not reliably store whether an event is a crit or not since Cumbersome modifies the crit boolean */
		final boolean isMeleeCrit = event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer);
		if (isMeleeCrit || (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile projectile
			&& EntityUtils.isAbilityTriggeringProjectile(projectile, true)
			&& MetadataUtils.checkOnceThisTick(mPlugin, enemy, "DivineJustice" + mPlayer.getName()))) { // for Multishot projectiles, we only want to trigger DJ on mobs once, not 3 times
			// TODO: Remove DivineJusticeInvuln and use OnHitTimerEffect
			final DivineJusticeInvuln divineJusticeInvuln = mPlugin.mEffectManager.getActiveEffect(enemy, DivineJusticeInvuln.class);
			if (divineJusticeInvuln == null) {
				mLastPassiveDJDamage = calculateDamage(event, mPercentDamage, isMeleeCrit, false);
				DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, mLastPassiveDJDamage, mInfo.getLinkedSpell(), true, false);
				mPlugin.mEffectManager.addEffect(enemy, DivineJusticeInvuln.SOURCE, new DivineJusticeInvuln(DivineJusticeInvuln.DURATION, mLastPassiveDJDamage));
				onDamageCosmeticEffects(enemy);
			} else {
				/* The enemy has been hit by DJ recently. Check to see if the new hit can apply more damage */
				final double attemptDamage = calculateDamage(event, mPercentDamage, isMeleeCrit, false);
				final int duration = divineJusticeInvuln.getDuration();
				final double magnitude = divineJusticeInvuln.getMagnitude();

				if (attemptDamage > magnitude) {
					final double extraDamage = attemptDamage - magnitude;
					mLastPassiveDJDamage = attemptDamage;
					DamageUtils.damage(mPlayer, enemy, DamageEvent.DamageType.MAGIC, extraDamage, mInfo.getLinkedSpell(), true, false);
					mPlugin.mEffectManager.addEffect(enemy, DivineJusticeInvuln.SOURCE, new DivineJusticeInvuln(duration, attemptDamage));
					onDamageCosmeticEffects(enemy);
				}
			}
		}
		return false; // keep the ability open for more Multishot crits this tick
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent entityDeathEvent, boolean dropsLoot) {
		if (isLevelTwo() && Crusade.enemyTriggersAbilities(entityDeathEvent.getEntity())) {
			PlayerUtils.healPlayer(mPlugin, mPlayer, EntityUtils.getMaxHealth(mPlayer) * mSelfHeal);
			final List<Player> players = PlayerUtils.otherPlayersInRange(mPlayer, mRadius, true);
			players.forEach(otherPlayer -> PlayerUtils.healPlayer(mPlugin, otherPlayer, EntityUtils.getMaxHealth(otherPlayer) * mAllyHeal, mPlayer));

			players.add(mPlayer);
			mCosmetic.justiceKill(mPlayer, entityDeathEvent.getEntity().getLocation());
			mCosmetic.justiceHealSound(players, mCosmetic.getHealPitchSelf());
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCosmetic.justiceHealSound(players, mCosmetic.getHealPitchOther()), 2);
		}

		if (isEnhanced() && Crusade.enemyTriggersAbilities(entityDeathEvent.getEntity())
			&& FastUtils.RANDOM.nextDouble() <= ENHANCEMENT_ASH_CHANCE) {
			spawnAsh(entityDeathEvent.getEntity().getLocation());
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		// Keep track of prior tick's effect magnitude,
		// if effect duration runs out (becomes null) decrement stacks rather than clear effect entirely.
		final Effect existingEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME);
		if (existingEffect != null && existingEffect.getMagnitude() > 0) {
			mPriorAmount = existingEffect.getMagnitude();
		}
		if ((existingEffect == null || existingEffect.getDuration() < 10) && mPriorAmount - mEnhanceDamage > 0) {
			mPlugin.mEffectManager.clearEffects(mPlayer, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME);
			addEnhancementEffect(mPlayer, mEnhanceDuration, mPriorAmount - mEnhanceDamage);
			mPriorAmount -= mEnhanceDamage;
		} else if (existingEffect == null && mPriorAmount > 0) {
			mPriorAmount = 0;
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	/**
	 * A terrible workaround to be able to deal Divine Justice damage with Luminous Infusion.<br>
	 * Notes: Luminous infusion does not apply DJ's flat damage or charm effects.<br>
	 * The event's flat damage does not include any damage multipliers. This formula readds the crit bonus if
	 * <code>isMeleeCrit</code> is true and the player's held weapon does not have the Cumbersome enchantment.
	 * @param event Event that caused the damage
	 * @param multiplier Damage multiplier to use
	 * @param isMeleeCrit Whether the conditions for a melee crit are fulfilled
	 * @param isLuminous Whether Luminous Infusion is calculating this damage
	 * @return Damage to deal to the evildoer
	 */
	public double calculateDamage(final DamageEvent event, final double multiplier, final boolean isMeleeCrit, final boolean isLuminous) {
		final boolean weaponHasCumbersome = ItemStatUtils.hasEnchantment(mPlayer.getInventory().getItemInMainHand(), EnchantmentType.CUMBERSOME);
		return (isLuminous ? 0 : DAMAGE) + event.getFlatDamage() * (isMeleeCrit && !weaponHasCumbersome ? CritScaling.CRIT_BONUS : 1.0) *
			Math.max(multiplier, 0.0);
	}

	private void onDamageCosmeticEffects(final LivingEntity enemy) {
		mCosmetic.justiceOnDamage(mPlayer, enemy, mPlayer.getWorld(), enemy.getLocation(), PartialParticle.getWidthDelta(enemy) * 1.5, mComboNumber);

		if (mComboNumber == 0 || mComboRunnable != null) {
			if (mComboRunnable != null) {
				mComboRunnable.cancel();
			}
			mComboRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					mComboNumber = 0;
					mComboRunnable = null;
				}
			};
			mComboRunnable.runTaskLater(mPlugin, (long) ((1D / EntityUtils.getAttributeOrDefault(mPlayer, Attribute.GENERIC_ATTACK_SPEED, 4)) * 20) + 15);
		}
		mComboNumber++;

		if (mComboNumber >= 3) {
			mComboNumber = 0;
			if (mComboRunnable != null) {
				mComboRunnable.cancel();
				mComboRunnable = null;
			}
		}
	}

	private void spawnAsh(Location loc) {
		final Item item = AbilityUtils.spawnAbilityItem(loc.getWorld(), loc, mCosmetic.justiceAsh(), mCosmetic.justiceAshName(), true, 0.3, false, true);
		GlowingManager.startGlowing(item, mCosmetic.justiceAshColor(), -1, 0, DivineJustice::canPickUpAsh, null);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				for (Player player : new Hitbox.UprightCylinderHitbox(item.getLocation(), 0.7, 0.7).getHitPlayers(true)) {
					if (!canPickUpAsh(player)) {
						continue;
					}

					applyEnhancementEffect(player, false);
					mCosmetic.justiceAshPickUp(player, item.getLocation());
					item.remove();
					this.cancel();
					break;
				}

				if (mT >= ENHANCEMENT_ASH_DURATION || !item.isValid()) {
					this.cancel();
					item.remove();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private static boolean canPickUpAsh(Player player) {
		final DivineJustice divineJustice = Plugin.getInstance().mAbilityManager.getPlayerAbility(player, DivineJustice.class);
		return divineJustice != null && divineJustice.isEnhanced();
	}

	public void applyEnhancementEffect(Player player, boolean fromBoneShard) {
		int existingEffectDuration = 0;
		double existingEffectAmount = 0;
		final NavigableSet<Effect> existingEffects = mPlugin.mEffectManager.clearEffects(player, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME);
		if (existingEffects != null) {
			Effect existingEffect = existingEffects.stream().findFirst().get();
			existingEffectDuration = existingEffect.getDuration();
			existingEffectAmount = existingEffect.getMagnitude();
		}

		final int duration = fromBoneShard ? ENHANCEMENT_BONE_SHARD_BONUS_DAMAGE_DURATION : Math.max(existingEffectDuration, mEnhanceDuration + 10);
		final double bonusDamage = fromBoneShard ? ENHANCEMENT_BONUS_DAMAGE_MAX : Math.min(existingEffectAmount + mEnhanceDamage, ENHANCEMENT_BONUS_DAMAGE_MAX);

		addEnhancementEffect(player, duration, bonusDamage);
	}

	private void addEnhancementEffect(Player player, int duration, double bonusDamage) {
		mPlugin.mEffectManager.addEffect(player, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME,
			new PercentDamageDealt(duration, bonusDamage).priority(2)
				.predicate((attacker, enemy) -> Crusade.enemyTriggersAbilities(enemy)).deleteOnAbilityUpdate(true));
		ClientModHandler.updateAbility(player, this);
	}

	public static void remove(Player p) {
		final Plugin plugin = Plugin.getInstance();
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			final DivineJustice dj = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(p, DivineJustice.class);
			if (dj == null || !dj.isEnhanced()) {
				plugin.mEffectManager.clearEffects(p, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME);
			}
		}, 5);
	}

	@Override
	public int getCharges() {
		final Effect activeEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME);
		return activeEffect == null ? 0 : (int) Math.round(100 * activeEffect.getMagnitude());
	}

	@Override
	public int getMaxCharges() {
		return isEnhanced() ? (int) Math.round(100 * ENHANCEMENT_BONUS_DAMAGE_MAX) : 0;
	}

	private static Description<DivineJustice> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Critical melee or projectile attacks deal ")
			.add(a -> DAMAGE, DAMAGE)
			.add(" plus ")
			.addPercent(a -> a.mPercentDamage, DAMAGE_MULTIPLIER_1, false, Ability::isLevelOne)
			.add(" of your critical attack damage as magic damage to undead enemies.");
	}

	private static Description<DivineJustice> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage is increased to ")
			.add(a -> DAMAGE, DAMAGE)
			.add(" plus ")
			.addPercent(a -> a.mPercentDamage, DAMAGE_MULTIPLIER_2, false, Ability::isLevelTwo)
			.add(" of your critical attack damage. Additionally, killing an undead enemy heals you for ")
			.addPercent(a -> a.mSelfHeal, HEALING_MULTIPLIER_OWN)
			.add(" of your max health and heals players within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks of you for ")
			.addPercent(a -> a.mAllyHeal, HEALING_MULTIPLIER_OTHER)
			.add(" of their max health.");
	}

	private static Description<DivineJustice> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Undead killed have a ")
			.addPercent(ENHANCEMENT_ASH_CHANCE)
			.add(" chance to drop Purified Ash which lingers for ")
			.addDuration(ENHANCEMENT_ASH_DURATION)
			.add(" seconds. Clerics with this ability who pick it up get ")
			.addPercent(a -> a.mEnhanceDamage, ENHANCEMENT_ASH_BONUS_DAMAGE)
			.add(" increased undead damage for ")
			.addDuration(a -> a.mEnhanceDuration, ENHANCEMENT_ASH_BONUS_DAMAGE_DURATION)
			.add(" seconds. This effect stacks up to ")
			.addPercent(ENHANCEMENT_BONUS_DAMAGE_MAX)
			.add(" and the duration is refreshed on each pickup. Bone Shards can be consumed from the inventory by right clicking to get the max effect for ")
			.add(a -> ENHANCEMENT_BONE_SHARD_BONUS_DAMAGE_DURATION, ENHANCEMENT_BONE_SHARD_BONUS_DAMAGE_DURATION, false, StringUtils::ticksToMinutes)
			.add(" minutes.");
	}


	@Override
	public @Nullable Component getHotbarMessage() {
		if (isEnhanced()) {
			TextColor color = INFO.getActionBarColor();
			String name = INFO.getHotbarName();

			int charges = getCharges();
			int maxCharges = getMaxCharges();

			// String output.
			Component output = Component.text("[", NamedTextColor.YELLOW)
				.append(Component.text(name != null ? name : "Error", color))
				.append(Component.text("]", NamedTextColor.YELLOW))
				.append(Component.text(": ", NamedTextColor.WHITE));

			output = output.append(Component.text(charges + "/" + maxCharges, (charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));

			return output;
		} else {
			return Component.text("");
		}
	}
}
