package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DivineJusticeCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;

import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;



public class DivineJustice extends Ability {

	public static final String NAME = "Divine Justice";
	public static final ClassAbility ABILITY = ClassAbility.DIVINE_JUSTICE;

	public static final int DAMAGE = 4;
	public static final double DAMAGE_MULTIPLIER = 0.15;
	public static final double HEALING_MULTIPLIER_OWN = 0.1;
	public static final double HEALING_MULTIPLIER_OTHER = 0.05;
	public static final int RADIUS = 12;

	private final boolean mDoHealingAndMultiplier;

	// Passive damage to share with Holy Javelin
	public double mLastPassiveDamage = 0;

	private @Nullable Crusade mCrusade;

	private final DivineJusticeCS mCosmetic;

	private int mComboNumber = 0;
	private BukkitRunnable mComboRunnable = null;

	public DivineJustice(
		Plugin plugin,
		@Nullable Player player
	) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "DivineJustice";
		mInfo.mShorthandName = "DJ";
		mInfo.mDescriptions.add(
			String.format(
				"Your critical attacks passively deal %s magic damage to undead enemies, ignoring iframes.",
				DAMAGE
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Killing an undead enemy now passively heals %s%% of your max health and heals players within %s blocks of you for %s%% of their max health. Damage is increased from %s, to %s and %s%% of your critical attack damage.",
				StringUtils.multiplierToPercentage(HEALING_MULTIPLIER_OWN),
				RADIUS,
				StringUtils.multiplierToPercentage(HEALING_MULTIPLIER_OTHER),
				DAMAGE,
				DAMAGE,
				StringUtils.multiplierToPercentage(DAMAGE_MULTIPLIER)
			)
		);
		mDisplayItem = new ItemStack(Material.IRON_SWORD, 1);

		mDoHealingAndMultiplier = getAbilityScore() == 2;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DivineJusticeCS(), DivineJusticeCS.SKIN_LIST);

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Crusade.class);
			});
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer != null && event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer) && Crusade.enemyTriggersAbilities(enemy, mCrusade)) {
			double originalDamage = event.getDamage();
			double damage = DAMAGE;
			if (mDoHealingAndMultiplier) {
				// Use the whole melee damage here
				damage += originalDamage * DAMAGE_MULTIPLIER;
			}

			mLastPassiveDamage = damage;
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, damage, mInfo.mLinkedSpell, true, false);

			double widerWidthDelta = PartialParticle.getWidthDelta(enemy) * 1.5;
			mCosmetic.justiceOnDamage(mPlayer, enemy, widerWidthDelta, mComboNumber);

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
				mComboRunnable.runTaskLater(mPlugin, (long) ((1D / mPlayer.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getValue()) * 20) + 15);
			}
			mComboNumber++;

			if (mComboNumber >= 3) {
				if (mComboRunnable != null) {
					mComboRunnable.cancel();
					mComboRunnable = null;
				}
				mComboNumber = 0;
			}

			return true;
		}
		return false;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent entityDeathEvent, boolean dropsLoot) {
		if (
			mPlayer != null
				&& mDoHealingAndMultiplier
				&& Crusade.enemyTriggersAbilities(entityDeathEvent.getEntity(), mCrusade)
		) {
			PlayerUtils.healPlayer(
				mPlugin,
				mPlayer,
				EntityUtils.getMaxHealth(mPlayer) * HEALING_MULTIPLIER_OWN
			);
			List<Player> players = PlayerUtils.otherPlayersInRange(mPlayer, RADIUS, true);
			for (Player otherPlayer : players) {
				PlayerUtils.healPlayer(
					mPlugin,
					otherPlayer,
					EntityUtils.getMaxHealth(mPlayer) * HEALING_MULTIPLIER_OTHER,
					mPlayer
				);
			}

			players.add(mPlayer);
			mCosmetic.justiceKill(mPlayer, entityDeathEvent.getEntity().getLocation());
			mCosmetic.justiceHealSound(players, mCosmetic.getHealPitchSelf());
			new BukkitRunnable() {
				@Override
				public void run() {
					mCosmetic.justiceHealSound(players, mCosmetic.getHealPitchOther());
				}
			}.runTaskLater(Plugin.getInstance(), 2);
		}
	}
}
