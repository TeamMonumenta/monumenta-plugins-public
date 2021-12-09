package com.playmonumenta.plugins.abilities.warlock.reaper;

import java.util.EnumSet;
import java.util.NavigableSet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;


public class DarkPact extends Ability {

	public static final String PERCENT_HEAL_EFFECT_NAME = "DarkPactPercentHealEffect";
	private static final int PERCENT_HEAL = -1;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "DarkPactPercentDamageResistEffect";
	private static final double PERCENT_DAMAGE_RESIST = -0.1;
	private static final String AESTHETICS_EFFECT_NAME = "DarkPactAestheticsEffect";
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "DarkPactPercentDamageDealtEffect";
	private static final String PERCENT_ATKS_EFFECT_NAME = "DarkPactPercentAtksEffect";
	private static final int DURATION = 20 * 7;
	private static final int DURATION_INCREASE_ON_KILL = 20 * 1;
	private static final double PERCENT_DAMAGE_DEALT_1 = 0.4;
	private static final double PERCENT_DAMAGE_DEALT_2 = 1.0;
	private static final EnumSet<DamageCause> ALLOWED_DAMAGE_CAUSES = EnumSet.of(DamageCause.ENTITY_ATTACK);
	private static final double PERCENT_ATKS_1 = 0.1;
	private static final double PERCENT_ATKS_2 = 0.2;
	private static final int ABSORPTION_ON_KILL = 1;
	private static final int MAX_ABSORPTION = 6;
	private static final int COOLDOWN = 20 * 14;

	private final double mPercentDamageDealt;
	private final double mPercentAtks;
	private int mTicks = 0;
	private @Nullable JudgementChain mJudgementChain;

	public DarkPact(Plugin plugin, Player player) {
		super(plugin, player, "Dark Pact");
		mInfo.mScoreboardId = "DarkPact";
		mInfo.mShorthandName = "DaP";
		mInfo.mDescriptions.add("Swapping while airborne and not sneaking and holding a scythe causes a dark aura to form around you. For the next 7 seconds, you gain 10% damage reduction, +10% attack speed, and deal +40% melee damage on your scythe attacks. Each kill during this time increases the duration of your aura by 1 second and gives 1 absorption health (capped at 6) for the duration of the aura. However, the player cannot heal for 10 seconds. Cooldown: 14s.");
		mInfo.mDescriptions.add("You gain +20% attack speed and attacks with a scythe deal +100% melee damage, and Soul Rend bypasses the healing prevention, healing the player by +2/+4 HP, depending on the level of Soul Rend. Nearby players are still healed as normal.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.DARK_PACT;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.SOUL_SAND, 1);
		mPercentDamageDealt = getAbilityScore() == 1 ? PERCENT_DAMAGE_DEALT_1 : PERCENT_DAMAGE_DEALT_2;
		mPercentAtks = getAbilityScore() == 1 ? PERCENT_ATKS_1 : PERCENT_ATKS_2;

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mJudgementChain = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, JudgementChain.class);
			});
		}
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			event.setCancelled(true);
			// *TO DO* - Turn into boolean in constructor -or- look at changing trigger entirely
			if (mPlayer.isOnGround() || mPlayer.isSneaking() || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell) || (mPlayer.isSneaking() && mJudgementChain != null && mPlayer.getLocation().getPitch() < -50.0)) {
				return;
			}

			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 50, 0.2, 0.1, 0.2, 1);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.5f, 1.25f);
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1, 0.5f);

			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(DURATION, mPercentDamageDealt, ALLOWED_DAMAGE_CAUSES));
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_ATKS_EFFECT_NAME, new PercentAttackSpeed(DURATION, mPercentAtks, PERCENT_ATKS_EFFECT_NAME));
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_HEAL_EFFECT_NAME, new PercentHeal(DURATION, PERCENT_HEAL));
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(DURATION, PERCENT_DAMAGE_RESIST));
			mPlugin.mEffectManager.addEffect(mPlayer, AESTHETICS_EFFECT_NAME, new Aesthetics(DURATION,
					(entity, fourHertz, twoHertz, oneHertz) -> {
						world.spawnParticle(Particle.SPELL_WITCH, entity.getLocation(), 3, 0.2, 0.2, 0.2, 0.2);
					},
					(entity) -> {
						world.playSound(entity.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.3f, 0.75f);
					}));

			putOnCooldown();

			mTicks = DURATION;
		}
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (mTicks != 0) {
			mTicks += DURATION_INCREASE_ON_KILL;
		}
		NavigableSet<Effect> aestheticsEffects = mPlugin.mEffectManager.getEffects(mPlayer, AESTHETICS_EFFECT_NAME);
		if (aestheticsEffects != null) {
			AbsorptionUtils.addAbsorption(mPlayer, ABSORPTION_ON_KILL, MAX_ABSORPTION, aestheticsEffects.last().getDuration());
			for (Effect effect : aestheticsEffects) {
				effect.setDuration(effect.getDuration() + DURATION_INCREASE_ON_KILL);
			}
		}
		NavigableSet<Effect> percentDamageEffects = mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME);
		if (percentDamageEffects != null) {
			for (Effect effect : percentDamageEffects) {
				effect.setDuration(effect.getDuration() + DURATION_INCREASE_ON_KILL);
			}
		}
		NavigableSet<Effect> percentAtksEffects = mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_ATKS_EFFECT_NAME);
		if (percentAtksEffects != null) {
			for (Effect effect : percentAtksEffects) {
				effect.setDuration(effect.getDuration() + DURATION_INCREASE_ON_KILL);
			}
		}
		NavigableSet<Effect> percentDefense = mPlugin.mEffectManager.getEffects(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME);
		if (percentDefense != null) {
			for (Effect effect : percentDefense) {
				effect.setDuration(effect.getDuration() + DURATION_INCREASE_ON_KILL);
			}
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			if (!ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
				event.setDamage(event.getDamage() / (1 + mPercentDamageDealt));
			}
		}
		return true;
	}
}
