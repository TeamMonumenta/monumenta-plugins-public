package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.warlock.SoulRend;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.reaper.DarkPactCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.NavigableSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class DarkPact extends Ability {
	public static final String PERCENT_HEAL_EFFECT_NAME = "DarkPactPercentHealEffect";
	private static final int PERCENT_HEAL = -1;
	private static final String AESTHETICS_EFFECT_NAME = "DarkPactAestheticsEffect";
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "DarkPactPercentDamageDealtEffect";
	private static final int DURATION = TICKS_PER_SECOND * 7;
	private static final int DURATION_INCREASE_ON_KILL = TICKS_PER_SECOND;
	private static final double PERCENT_DAMAGE_DEALT_1 = 0.50;
	private static final double PERCENT_DAMAGE_DEALT_2 = 0.75;
	private static final int ABSORPTION_ON_KILL = 1;
	private static final int MAX_ABSORPTION = 6;
	private static final int COOLDOWN = TICKS_PER_SECOND * 14;
	private static final double EXTENDED_ANTIHEAL = -2.0 / 3.0;
	private static final int CANCEL_WINDOW = TICKS_PER_SECOND * 7;

	public static final String CHARM_COOLDOWN = "Dark Pact Cooldown";
	public static final String CHARM_DAMAGE = "Dark Pact Melee Damage";
	public static final String CHARM_REFRESH = "Dark Pact Refresh";
	public static final String CHARM_ATTACK_SPEED = "Dark Pact Attack Speed Amplifier";
	public static final String CHARM_CAP = "Dark Pact Absorption Health Cap";
	public static final String CHARM_DURATION = "Dark Pact Buff Duration";
	public static final String CHARM_ABSORPTION = "Dark Pact Absorption Health Per Kill";

	public static final AbilityInfo<DarkPact> INFO =
		new AbilityInfo<>(DarkPact.class, "Dark Pact", DarkPact::new)
			.linkedSpell(ClassAbility.DARK_PACT)
			.scoreboardId("DarkPact")
			.shorthandName("DaP")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Deal extra melee damage and gain absorption per kill at the cost of not being able to heal for a short period of time.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DarkPact::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.SOUL_SAND)
			.priorityAmount(950); // multiplicative damage before additive


	private final double mPercentDamageDealt;
	private final int mDuration;
	private final int mDurationIncreaseOnKill;
	private final double mAbsorption;
	private final double mMaxAbsorption;

	private boolean mActive = false;
	private int mStartingTick = 0;

	private final DarkPactCS mCosmetic;

	public DarkPact(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentDamageDealt = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE) + (isLevelOne() ? PERCENT_DAMAGE_DEALT_1 : PERCENT_DAMAGE_DEALT_2);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mDurationIncreaseOnKill = CharmManager.getDuration(mPlayer, CHARM_REFRESH, DURATION_INCREASE_ON_KILL);
		mAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, ABSORPTION_ON_KILL);
		mMaxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CAP, MAX_ABSORPTION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DarkPactCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			if (mPlugin.mEffectManager.hasEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME) && Bukkit.getServer().getCurrentTick() - mStartingTick >= CANCEL_WINDOW) {
				mActive = false;
				ClientModHandler.updateAbility(mPlayer, this);

				mPlugin.mEffectManager.clearEffects(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME);
				mPlugin.mEffectManager.clearEffects(mPlayer, PERCENT_HEAL_EFFECT_NAME);
				mPlugin.mEffectManager.clearEffects(mPlayer, AESTHETICS_EFFECT_NAME);

				return true;
			}

			return false;
		}

		World world = mPlayer.getWorld();
		mCosmetic.onCast(mPlayer, world, mPlayer.getLocation());

		mActive = true;
		ClientModHandler.updateAbility(mPlayer, this);
		mStartingTick = Bukkit.getServer().getCurrentTick();

		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME,
			new PercentDamageDealt(mDuration, mPercentDamageDealt)
				.predicate((entity, enemy) -> entity instanceof Player player && ItemUtils.isHoe(player.getInventory().getItemInMainHand()))
				.damageTypes(DamageType.getAllMeleeTypes()).deleteOnAbilityUpdate(true));
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_HEAL_EFFECT_NAME, new PercentHeal(mDuration, PERCENT_HEAL)
			.deleteOnAbilityUpdate(true));
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_HEAL_EFFECT_NAME, new PercentHeal(mDuration, EXTENDED_ANTIHEAL)
			.deleteOnAbilityUpdate(true));
		mPlugin.mEffectManager.addEffect(mPlayer, AESTHETICS_EFFECT_NAME, new Aesthetics(mDuration,
			(entity, fourHertz, twoHertz, oneHertz) -> mCosmetic.tick(mPlayer, fourHertz, twoHertz, oneHertz),
			(entity) -> mCosmetic.loseEffect(mPlayer)).deleteOnAbilityUpdate(true));

		putOnCooldown();
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (event.getEntity().getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			return;
		}

		Effect aestheticsEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, AESTHETICS_EFFECT_NAME);
		if (aestheticsEffect != null) {
			AbsorptionUtils.addAbsorption(mPlayer, mAbsorption, mMaxAbsorption, aestheticsEffect.getDuration());
			aestheticsEffect.setDuration(aestheticsEffect.getDuration() + mDurationIncreaseOnKill);
			mCosmetic.onKill(mPlayer, event.getEntity());
		}
		Effect percentDamageEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME);
		if (percentDamageEffect != null) {
			percentDamageEffect.setDuration(percentDamageEffect.getDuration() + mDurationIncreaseOnKill);
		}
		NavigableSet<Effect> antiHealEffects = mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_HEAL_EFFECT_NAME);
		if (antiHealEffects != null) {
			// extend the non -100% healing effect
			Effect extendedAntiheal = antiHealEffects.first();
			extendedAntiheal.setDuration(extendedAntiheal.getDuration() + mDurationIncreaseOnKill);
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		boolean wasActive = mActive;
		mActive = mPlugin.mEffectManager.hasEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME);
		if (wasActive != mActive) {
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}

	private static Description<DarkPact> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to cause a dark aura to form around you. For the next ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds, your melee scythe attacks deal ")
			.addPercent(a -> a.mPercentDamageDealt, PERCENT_DAMAGE_DEALT_1, false, Ability::isLevelOne)
			.add(" more damage. Each kill during this time increases the duration of your aura by ")
			.addDuration(a -> a.mDurationIncreaseOnKill, DURATION_INCREASE_ON_KILL)
			.add(" second and gives ")
			.add(a -> a.mAbsorption, ABSORPTION_ON_KILL)
			.add(" absorption health (up to ")
			.add(a -> a.mMaxAbsorption, MAX_ABSORPTION)
			.add(") for the duration of the aura. However, you cannot heal for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds, and healing is reduced by ")
			.addPercent(-EXTENDED_ANTIHEAL)
			.add(" until the aura ends. You may retrigger this ability again after ")
			.addDuration(CANCEL_WINDOW)
			.add(" seconds to cancel your pact.")
			.addCooldown(COOLDOWN);
	}

	private static Description<DarkPact> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage buff is increased to ")
			.addPercent(a -> a.mPercentDamageDealt, PERCENT_DAMAGE_DEALT_2, false, Ability::isLevelTwo)
			.add(", and your Soul Rend bypasses the healing prevention, healing you by ")
			.add((a, p) -> {
				Description<SoulRend> subDescription;
				SoulRend soulRend = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(p, SoulRend.class);
				if (soulRend == null) {
					subDescription = new DescriptionBuilder<>(() -> SoulRend.INFO)
						.add(aa -> SoulRend.DARK_PACT_HEAL_1, SoulRend.DARK_PACT_HEAL_1)
						.add("/")
						.add(aa -> SoulRend.DARK_PACT_HEAL_2, SoulRend.DARK_PACT_HEAL_2)
						.add(" health, depending on the level of Soul Rend.");
				} else {
					subDescription = new DescriptionBuilder<>(() -> SoulRend.INFO)
						.add(sr -> sr.mDarkPactHeal, soulRend.isLevelOne() ? SoulRend.DARK_PACT_HEAL_1 : SoulRend.DARK_PACT_HEAL_2)
						.add(" health.");
				}
				return subDescription.get(soulRend, p);
			})
			.add(" Nearby players are still healed as normal.");
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		ClassAbility classAbility = INFO.getLinkedSpell();
		int remainingCooldown = classAbility == null ? 0 : mPlugin.mTimers.getCooldown(mPlayer.getUniqueId(), classAbility);
		TextColor color = INFO.getActionBarColor();
		String name = INFO.getHotbarName();


		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", !mActive ? NamedTextColor.GRAY : color))
			.append(Component.text("]", NamedTextColor.YELLOW));

		output = output.append(Component.text(": ", NamedTextColor.WHITE));

		if (mActive && (CANCEL_WINDOW - (Bukkit.getServer().getCurrentTick() - mStartingTick)) > 0) {
			output = output.append(Component.text(((int) Math.ceil(Math.max(0, 20 + CANCEL_WINDOW - (Bukkit.getServer().getCurrentTick() - mStartingTick))) / TICKS_PER_SECOND) + "s", NamedTextColor.DARK_RED));
		} else if (remainingCooldown > 0) {
			output = output.append(Component.text(((int) Math.ceil(remainingCooldown / 20.0)) + "s", NamedTextColor.GRAY));
		} else {
			output = output.append(Component.text("✓", NamedTextColor.GREEN, TextDecoration.BOLD));
		}

		return output;
	}
}
