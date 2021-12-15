package com.playmonumenta.plugins.abilities.cleric;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;



public class DivineJustice extends Ability {
	public static class DivineJusticeAllyHealingEnchantment extends BaseAbilityEnchantment {
		public DivineJusticeAllyHealingEnchantment() {
			super("Divine Justice Ally Healing", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

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
				"Your critical attacks passively deal %s holy damage to undead enemies, ignoring iframes.",
				DAMAGE
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Killing an undead enemy now passively heals %s%% of your max health and heals players within %s blocks of you for %s%% of their max health. Damage is increased from %s, to %s and then %s%%.",
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

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Crusade.class);
			});
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent entityDamageByEntityEvent) {
		//TODO pass in casted entities for events like these
		LivingEntity enemy = (LivingEntity)entityDamageByEntityEvent.getEntity();

		if (
			entityDamageByEntityEvent.getCause() == DamageCause.ENTITY_ATTACK
			&& PlayerUtils.isFallingAttack(mPlayer)
			&& Crusade.enemyTriggersAbilities(enemy, mCrusade)
		) {
			double originalDamage = entityDamageByEntityEvent.getDamage();
			double damage = DAMAGE;
			if (mDoHealingAndMultiplier) {
				// Use the whole melee damage here
				damage += (originalDamage + damage) * DAMAGE_MULTIPLIER;
			}

			mLastPassiveDamage = damage;
			EntityUtils.damageEntity(
				Plugin.getInstance(),
				enemy,
				damage,
				mPlayer,
				MagicType.HOLY,
				true,
				ABILITY,
				true,
				true,
				true
			);

			double widerWidthDelta = PartialParticle.getWidthDelta(enemy) * 1.5;
			PartialParticle partialParticle = new PartialParticle(
				Particle.END_ROD,
				LocationUtils.getHalfHeightLocation(enemy),
				10,
				widerWidthDelta,
				PartialParticle.getHeightDelta(enemy),
				widerWidthDelta,
				0.05
			).spawnAsPlayer(mPlayer);
			partialParticle.mParticle = Particle.FLAME;
			partialParticle.spawnAsPlayer(mPlayer);

			// /playsound block.anvil.land master @p ~ ~ ~ 0.15 1.5
			mPlayer.getWorld().playSound(
				enemy.getLocation(),
				Sound.BLOCK_ANVIL_LAND,
				0.15f,
				1.5f
			);
		}

		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent entityDeathEvent, boolean dropsLoot) {
		if (
			mPlayer != null
				&& mDoHealingAndMultiplier
				&& Crusade.enemyTriggersAbilities(entityDeathEvent.getEntity(), mCrusade)
		) {
			PlayerUtils.healPlayer(
				mPlayer,
				EntityUtils.getMaxHealth(mPlayer) * HEALING_MULTIPLIER_OWN
			);
			List<Player> players = PlayerUtils.otherPlayersInRange(mPlayer, RADIUS, true);
			for (Player otherPlayer : players) {
				PlayerUtils.healPlayer(
					otherPlayer,
					EntityUtils.getMaxHealth(otherPlayer) * DivineJusticeAllyHealingEnchantment.getExtraPercentHealing(mPlayer, DivineJusticeAllyHealingEnchantment.class, (float) HEALING_MULTIPLIER_OTHER)
				);
			}

			players.add(mPlayer);
			// /playsound block.note_block.chime master @p ~ ~ ~ 0.5 1.41
			doHealingSounds(players, Constants.NotePitches.C18);
			new BukkitRunnable() {
				@Override
				public void run() {
					// /playsound block.note_block.chime master @p ~ ~ ~ 0.5 1.78
					doHealingSounds(players, Constants.NotePitches.E22);
				}
			}.runTaskLater(Plugin.getInstance(), 2);
		}
	}

	public static void doHealingSounds(List<Player> players, float pitch) {
		for (Player healedPlayer : players) {
			healedPlayer.playSound(
				healedPlayer.getLocation(),
				Sound.BLOCK_NOTE_BLOCK_CHIME,
				0.5f,
				pitch
			);
		}
	}
}
