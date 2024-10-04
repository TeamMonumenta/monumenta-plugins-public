package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
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
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import java.util.NavigableSet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class DarkPact extends Ability {
	public static final String PERCENT_HEAL_EFFECT_NAME = "DarkPactPercentHealEffect";
	private static final int PERCENT_HEAL = -1;
	private static final String AESTHETICS_EFFECT_NAME = "DarkPactAestheticsEffect";
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "DarkPactPercentDamageDealtEffect";
	private static final int DURATION = Constants.TICKS_PER_SECOND * 7;
	private static final int DURATION_INCREASE_ON_KILL = Constants.TICKS_PER_SECOND;
	private static final double PERCENT_DAMAGE_DEALT_1 = 0.50;
	private static final double PERCENT_DAMAGE_DEALT_2 = 0.75;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(DamageType.MELEE);
	private static final int ABSORPTION_ON_KILL = 1;
	private static final int MAX_ABSORPTION = 6;
	private static final int COOLDOWN = Constants.TICKS_PER_SECOND * 14;
	private static final double EXTENDED_ANTIHEAL = -2.0 / 3.0;
	private static final int CANCEL_WINDOW = Constants.TICKS_PER_SECOND * 7;

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
			.descriptions(
				("Pressing the drop key while not sneaking and holding a scythe causes a dark aura to form around you. " +
					 "For the next %s seconds, your scythe attacks deal +%s%% melee damage. " +
					 "Each kill during this time increases the duration of your aura by %s second and gives %s absorption health (capped at %s) for the duration of the aura. " +
					 "However, you cannot heal for %s seconds, and healing is reduced by %s%% until the aura ends. " +
					 "You may retrigger this ability again after %s seconds to cancel your pact. Cooldown: %ss.")
					.formatted(StringUtils.ticksToSeconds(DURATION), StringUtils.multiplierToPercentage(PERCENT_DAMAGE_DEALT_1), StringUtils.ticksToSeconds(DURATION_INCREASE_ON_KILL),
						ABSORPTION_ON_KILL, MAX_ABSORPTION, StringUtils.ticksToSeconds(DURATION), StringUtils.multiplierToPercentage(-EXTENDED_ANTIHEAL), StringUtils.ticksToSeconds(CANCEL_WINDOW), StringUtils.ticksToSeconds(COOLDOWN)),
				("Attacks with a scythe deal +%s%% melee damage, and your Soul Rend bypasses the healing prevention, healing you by +%s/+%s HP, depending on the level of Soul Rend. " +
					 "Nearby players are still healed as normal.")
					.formatted(StringUtils.multiplierToPercentage(PERCENT_DAMAGE_DEALT_2), SoulRend.DARK_PACT_HEAL_1, SoulRend.DARK_PACT_HEAL_2))
			.simpleDescription("Deal extra melee damage and gain absorption per kill at the cost of not being able to heal for a short period of time.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DarkPact::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.SOUL_SAND)
			.priorityAmount(950); // multiplicative damage before additive


	private final double mPercentDamageDealt;
	private boolean mActive = false;
	private int mStartingTick = 0;

	private final DarkPactCS mCosmetic;

	public DarkPact(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentDamageDealt = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE) + (isLevelOne() ? PERCENT_DAMAGE_DEALT_1 : PERCENT_DAMAGE_DEALT_2);
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

		int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(duration, mPercentDamageDealt, AFFECTED_DAMAGE_TYPES, 0, (entity, enemy) -> entity instanceof Player player && ItemUtils.isHoe(player.getInventory().getItemInMainHand())));
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_HEAL_EFFECT_NAME, new PercentHeal(duration, PERCENT_HEAL));
		mPlugin.mEffectManager.addEffect(mPlayer, AESTHETICS_EFFECT_NAME, new Aesthetics(duration,
			(entity, fourHertz, twoHertz, oneHertz) -> mCosmetic.tick(mPlayer, fourHertz, twoHertz, oneHertz),
			(entity) -> mCosmetic.loseEffect(mPlayer)));

		putOnCooldown();
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (event.getEntity().getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			return;
		}

		int duration = CharmManager.getDuration(mPlayer, CHARM_REFRESH, DURATION_INCREASE_ON_KILL);

		NavigableSet<Effect> aestheticsEffects = mPlugin.mEffectManager.getEffects(mPlayer, AESTHETICS_EFFECT_NAME);
		if (aestheticsEffects != null) {
			AbsorptionUtils.addAbsorption(mPlayer, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, ABSORPTION_ON_KILL), CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CAP, MAX_ABSORPTION), aestheticsEffects.last().getDuration());
			for (Effect effect : aestheticsEffects) {
				effect.setDuration(effect.getDuration() + duration);
			}
			mCosmetic.onKill(mPlayer, event.getEntity());
		}
		NavigableSet<Effect> percentDamageEffects = mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME);
		if (percentDamageEffects != null) {
			for (Effect effect : percentDamageEffects) {
				effect.setDuration(effect.getDuration() + duration);
			}
		}
		NavigableSet<Effect> antiHealEffects = mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_HEAL_EFFECT_NAME);
		if (antiHealEffects != null) {
			int totalDuration = 0;
			for (Effect effect : antiHealEffects) {
				totalDuration = Math.max(effect.getDuration() + duration, totalDuration);
			}
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_HEAL_EFFECT_NAME, new PercentHeal(totalDuration, EXTENDED_ANTIHEAL));
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
}
