package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.EnumSet;
import java.util.NavigableSet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DarkPact extends Ability {

	public static final String PERCENT_HEAL_EFFECT_NAME = "DarkPactPercentHealEffect";
	private static final int PERCENT_HEAL = -1;
	private static final String AESTHETICS_EFFECT_NAME = "DarkPactAestheticsEffect";
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "DarkPactPercentDamageDealtEffect";
	private static final int DURATION = 20 * 7;
	private static final int DURATION_INCREASE_ON_KILL = 20 * 1;
	private static final double PERCENT_DAMAGE_DEALT_1 = 0.4;
	private static final double PERCENT_DAMAGE_DEALT_2 = 0.7;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(DamageType.MELEE);
	private static final int ABSORPTION_ON_KILL = 1;
	private static final int MAX_ABSORPTION = 6;
	private static final int COOLDOWN = 20 * 14;

	public static final String CHARM_COOLDOWN = "Dark Pact Cooldown";
	public static final String CHARM_DAMAGE = "Dark Pact Melee Damage";
	public static final String CHARM_REFRESH = "Dark Pact Refresh";
	public static final String CHARM_ATTACK_SPEED = "Dark Pact Attack Speed Amplifier";
	public static final String CHARM_CAP = "Dark Pact Absorption Health Cap";
	public static final String CHARM_DURATION = "Dark Pact Buff Duration";
	public static final String CHARM_ABSORPTION = "Dark Pact Absorption Health Per Kill";

	private final double mPercentDamageDealt;
	private @Nullable JudgementChain mJudgementChain;
	private boolean mActive = false;

	public DarkPact(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Dark Pact");
		mInfo.mScoreboardId = "DarkPact";
		mInfo.mShorthandName = "DaP";
		mInfo.mDescriptions.add("Swapping while airborne and not sneaking and holding a scythe causes a dark aura to form around you. For the next 7 seconds, your scythe attacks deal +40% melee damage. Each kill during this time increases the duration of your aura by 1 second and gives 1 absorption health (capped at 6) for the duration of the aura. However, the player cannot heal for 7 seconds. Cooldown: 14s.");
		mInfo.mDescriptions.add("Attacks with a scythe deal +70% melee damage, and Soul Rend bypasses the healing prevention, healing the player by +2/+4 HP, depending on the level of Soul Rend. Nearby players are still healed as normal.");
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, COOLDOWN);
		mInfo.mLinkedSpell = ClassAbility.DARK_PACT;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.SOUL_SAND, 1);
		mPercentDamageDealt = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE) + (isLevelOne() ? PERCENT_DAMAGE_DEALT_1 : PERCENT_DAMAGE_DEALT_2);

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mJudgementChain = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, JudgementChain.class);
			});
		}
	}

	@Override
	public double getPriorityAmount() {
		return 950; // multiplicative damage before additive
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer != null && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			event.setCancelled(true);
			// *TO DO* - Turn into boolean in constructor -or- look at changing trigger entirely
			if (mPlayer.isOnGround() || mPlayer.isSneaking() || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell) || (mPlayer.isSneaking() && mJudgementChain != null && mPlayer.getLocation().getPitch() < -50.0)) {
				return;
			}

			World world = mPlayer.getWorld();
			new PartialParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 50, 0.2, 0.1, 0.2, 1).spawnAsPlayerActive(mPlayer);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.5f, 1.25f);
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1, 0.5f);
			int duration = DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);

			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(duration, mPercentDamageDealt, AFFECTED_DAMAGE_TYPES, 0, (entity, enemy) -> entity instanceof Player player && ItemUtils.isHoe(player.getInventory().getItemInMainHand())));
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_HEAL_EFFECT_NAME, new PercentHeal(duration, PERCENT_HEAL));
			mPlugin.mEffectManager.addEffect(mPlayer, AESTHETICS_EFFECT_NAME, new Aesthetics(duration,
					(entity, fourHertz, twoHertz, oneHertz) -> {
					new PartialParticle(Particle.SPELL_WITCH, entity.getLocation(), 3, 0.2, 0.2, 0.2, 0.2).spawnAsPlayerActive(mPlayer);
					},
					(entity) -> {
						world.playSound(entity.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.3f, 0.75f);
					}));

			putOnCooldown();

		}
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (mPlayer == null) {
			return;
		}
		int duration = DURATION_INCREASE_ON_KILL + CharmManager.getExtraDuration(mPlayer, CHARM_REFRESH);


		NavigableSet<Effect> aestheticsEffects = mPlugin.mEffectManager.getEffects(mPlayer, AESTHETICS_EFFECT_NAME);
		if (aestheticsEffects != null) {
			AbsorptionUtils.addAbsorption(mPlayer, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, ABSORPTION_ON_KILL), CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CAP, MAX_ABSORPTION), aestheticsEffects.last().getDuration());
			for (Effect effect : aestheticsEffects) {
				effect.setDuration(effect.getDuration() + duration);
			}
		}
		NavigableSet<Effect> percentDamageEffects = mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME);
		if (percentDamageEffects != null) {
			for (Effect effect : percentDamageEffects) {
				effect.setDuration(effect.getDuration() + duration);
			}
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
