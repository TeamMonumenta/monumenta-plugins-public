package com.playmonumenta.plugins.abilities.cleric;

import java.util.List;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class DivineJustice extends Ability {
	public static final String NAME = "Divine Justice";
	public static final ClassAbility ABILITY = ClassAbility.DIVINE_JUSTICE;

	public static final int DAMAGE_1 = 4;
	public static final double DAMAGE_MULTIPLIER_2 = 0.15;
	public static final double HEALING_MULTIPLIER_OWN = 0.1;
	public static final double HEALING_MULTIPLIER_OTHER = 0.05;
	public static final int RADIUS = 12;

	private final boolean mDoHealingAndMultiplier;

	// Passive damage to share with Holy Javelin
	public double mLastPassiveDamage = 0;

	private @Nullable Crusade mCrusade;

	public DivineJustice(
		@NotNull Plugin plugin,
		@NotNull Player player
	) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "DivineJustice";
		mInfo.mShorthandName = "DJ";
		mInfo.mDescriptions.add(
			String.format(
				"Your cooled down falling attacks passively deal %s holy damage to undead enemies, ignoring iframes.",
				DAMAGE_1
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Killing an undead enemy now passively heals %s%% of your max health and heals players within %s blocks of you for %s%% of their max health. Damage is increased from %s, to %s and then %s%%.",
				StringUtils.multiplierToPercentage(HEALING_MULTIPLIER_OWN),
				RADIUS,
				StringUtils.multiplierToPercentage(HEALING_MULTIPLIER_OTHER),
				DAMAGE_1,
				DAMAGE_1,
				DAMAGE_MULTIPLIER_2
			)
		);

		mDoHealingAndMultiplier = getAbilityScore() == 2;

		if (player != null) {
		Bukkit.getScheduler().runTask(plugin, () -> {
				mCrusade = AbilityManager.getManager().getPlayerAbility(mPlayer, Crusade.class);
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
			double damage = DAMAGE_1;
			if (mDoHealingAndMultiplier) {
				// Use the whole melee damage here
				damage += (originalDamage + damage) * DAMAGE_MULTIPLIER_2;
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
			@NotNull PartialParticle partialParticle = new PartialParticle(
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
			mDoHealingAndMultiplier
			&& Crusade.enemyTriggersAbilities(entityDeathEvent.getEntity(), mCrusade)
		) {
			PlayerUtils.healPlayer(
				mPlayer,
				mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * HEALING_MULTIPLIER_OWN
			);
			@NotNull List<@NotNull Player> players = PlayerUtils.playersInRange(mPlayer, RADIUS, false);
			for (@NotNull Player otherPlayer : players) {
				PlayerUtils.healPlayer(
					otherPlayer,
					otherPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * HEALING_MULTIPLIER_OTHER
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
		for (@NotNull Player healedPlayer : players) {
			healedPlayer.playSound(
				healedPlayer.getLocation(),
				Sound.BLOCK_NOTE_BLOCK_CHIME,
				0.5f,
				pitch
			);
		}
	}
}