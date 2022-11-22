package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DivineJusticeCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import java.util.NavigableSet;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;


public class DivineJustice extends Ability {

	public static final String NAME = "Divine Justice";
	public static final ClassAbility ABILITY = ClassAbility.DIVINE_JUSTICE;

	public static final int DAMAGE = 4;
	public static final double DAMAGE_MULTIPLIER = 0.15;
	public static final double HEALING_MULTIPLIER_OWN = 0.1;
	public static final double HEALING_MULTIPLIER_OTHER = 0.05;
	public static final int RADIUS = 12;
	public static final double ENHANCEMENT_ASH_CHANCE = 0.33;
	public static final int ENHANCEMENT_ASH_DURATION = 10 * 20;
	public static final double ENHANCEMENT_ASH_BONUS_DAMAGE = 0.025;
	public static final double ENHANCEMENT_BONUS_DAMAGE_MAX = 0.15;
	public static final int ENHANCEMENT_ASH_BONUS_DAMAGE_DURATION = 20 * 20;
	public static final int ENHANCEMENT_BONE_SHARD_BONUS_DAMAGE_DURATION = 4 * 60 * 20;
	public static final String ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME = "DivineJusticeBonusDamageEffect";
	public static final Material ASH_MATERIAL = Material.GUNPOWDER;
	public static final String ASH_NAME = "Purified Ash";

	public static final String CHARM_DAMAGE = "Divine Justice Damage";
	public static final String CHARM_SELF = "Divine Justice Self Heal";
	public static final String CHARM_ALLY = "Divine Justice Ally Heal";

	public static final AbilityInfo<DivineJustice> INFO =
		new AbilityInfo<>(DivineJustice.class, NAME, DivineJustice::new)
			.linkedSpell(ABILITY)
			.scoreboardId("DivineJustice")
			.shorthandName("DJ")
			.descriptions(
				String.format(
					"Your critical attacks passively deal %s magic damage to undead enemies, ignoring iframes.",
					DAMAGE
				),
				String.format(
					"Killing an undead enemy now passively heals %s%% of your max health and heals players within %s blocks of you for %s%% of their max health." +
						" Damage is increased from %s, to %s and %s%% of your critical attack damage.",
					StringUtils.multiplierToPercentage(HEALING_MULTIPLIER_OWN),
					RADIUS,
					StringUtils.multiplierToPercentage(HEALING_MULTIPLIER_OTHER),
					DAMAGE,
					DAMAGE,
					StringUtils.multiplierToPercentage(DAMAGE_MULTIPLIER)
				),
				String.format(
					"Undead killed have a %s%% chance to drop Purified Ash which disappears after %ss." +
						" Clerics with this ability who pick it up get %s%% increased undead damage for %ss." +
						" This effect stacks up to %s%% and the duration is refreshed on each pickup." +
						" Bone Shards can be consumed from the inventory by right-clicking to get the max effect for %s minutes.",
					StringUtils.multiplierToPercentage(ENHANCEMENT_ASH_CHANCE),
					StringUtils.ticksToSeconds(ENHANCEMENT_ASH_DURATION),
					StringUtils.multiplierToPercentage(ENHANCEMENT_ASH_BONUS_DAMAGE),
					StringUtils.ticksToSeconds(ENHANCEMENT_ASH_BONUS_DAMAGE_DURATION),
					StringUtils.multiplierToPercentage(ENHANCEMENT_BONUS_DAMAGE_MAX),
					ENHANCEMENT_BONE_SHARD_BONUS_DAMAGE_DURATION / (60 * 20)
				)
			)
			.displayItem(new ItemStack(Material.IRON_SWORD, 1));

	private final boolean mDoHealingAndMultiplier;

	// Passive damage to share with Holy Javelin
	public double mLastPassiveDamage = 0;

	private @Nullable Crusade mCrusade;

	private final DivineJusticeCS mCosmetic;

	private int mComboNumber = 0;
	private BukkitRunnable mComboRunnable = null;

	private double mPriorAmount = 0;

	public DivineJustice(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDoHealingAndMultiplier = isLevelTwo();

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DivineJusticeCS(), DivineJusticeCS.SKIN_LIST);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class);
		});
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer) && Crusade.enemyTriggersAbilities(enemy, mCrusade)) {
			double originalDamage = event.getDamage();
			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE);
			if (mDoHealingAndMultiplier) {
				// Use the whole melee damage here
				damage += originalDamage * DAMAGE_MULTIPLIER;
			}

			mLastPassiveDamage = damage;
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, false);

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
		if (mDoHealingAndMultiplier && Crusade.enemyTriggersAbilities(entityDeathEvent.getEntity(), mCrusade)) {
			PlayerUtils.healPlayer(
				mPlugin,
				mPlayer,
				CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SELF, EntityUtils.getMaxHealth(mPlayer) * HEALING_MULTIPLIER_OWN)
			);
			List<Player> players = PlayerUtils.otherPlayersInRange(mPlayer, RADIUS, true);
			for (Player otherPlayer : players) {
				PlayerUtils.healPlayer(
					mPlugin,
					otherPlayer,
					CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ALLY, EntityUtils.getMaxHealth(otherPlayer) * HEALING_MULTIPLIER_OTHER),
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

		if (isEnhanced()
			    && Crusade.enemyTriggersAbilities(entityDeathEvent.getEntity(), mCrusade)
			    && FastUtils.RANDOM.nextDouble() <= ENHANCEMENT_ASH_CHANCE) {
			spawnAsh(entityDeathEvent.getEntity().getLocation());
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		// Keep track of prior tick's effect magnitude,
		// if effect duration runs out (becomes null) decrement stacks rather than clear effect entirely.
		Effect existingEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME);
		if (existingEffect != null && existingEffect.getMagnitude() > 0) {
			mPriorAmount = existingEffect.getMagnitude();
		} else if (existingEffect == null && mPriorAmount - ENHANCEMENT_ASH_BONUS_DAMAGE > 0) {
			mPlugin.mEffectManager.addEffect(mPlayer, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME,
				new PercentDamageDealt(ENHANCEMENT_ASH_BONUS_DAMAGE_DURATION, mPriorAmount - ENHANCEMENT_ASH_BONUS_DAMAGE, null, 2, (attacker, enemy) -> Crusade.enemyTriggersAbilities(enemy, mCrusade)));
			mPriorAmount -= ENHANCEMENT_ASH_BONUS_DAMAGE;
		}
	}

	private void spawnAsh(Location loc) {
		ItemStack itemStack = new ItemStack(ASH_MATERIAL);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.displayName(Component.text(ASH_NAME, NamedTextColor.GRAY)
			.decoration(TextDecoration.ITALIC, false));
		itemStack.setItemMeta(itemMeta);
		ItemUtils.setPlainName(itemStack);
		Item item = loc.getWorld().dropItemNaturally(loc, itemStack);
		item.setGlowing(true); // glowing is conditionally disabled for non-clerics in GlowingReplacer
		item.setPickupDelay(Integer.MAX_VALUE);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				for (Player player : PlayerUtils.playersInRange(item.getLocation(), 1, true)) {
					if (!canPickUpAsh(player)) {
						continue;
					}

					applyEnhancementEffect(player, false);

					player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_STEP, SoundCategory.PLAYERS, 0.75f, 0.5f);
					player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.2f, 0.2f);

					Location particleLocation = item.getLocation().add(0, 0.2, 0);
					new PartialParticle(Particle.ASH, particleLocation, 50)
						.delta(0.15, 0.1, 0.15)
						.spawnAsPlayerActive(player);
					new PartialParticle(Particle.REDSTONE, particleLocation, 7)
						.delta(0.1, 0.1, 0.1)
						.data(new Particle.DustOptions(Color.fromBGR(100, 100, 100), 1))
						.spawnAsPlayerActive(player);

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

	public static boolean isAsh(Item item) {
		return item.getItemStack().getType() == ASH_MATERIAL
			       && ASH_NAME.equals(ItemUtils.getPlainNameIfExists(item.getItemStack()));
	}

	public static boolean canPickUpAsh(Player player) {
		DivineJustice divineJustice = Plugin.getInstance().mAbilityManager.getPlayerAbility(player, DivineJustice.class);
		return divineJustice != null && divineJustice.isEnhanced();
	}

	public void applyEnhancementEffect(Player player, boolean fromBoneShard) {
		int existingEffectDuration = 0;
		double existingEffectAmount = 0;
		NavigableSet<Effect> existingEffects = mPlugin.mEffectManager.clearEffects(player, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME);
		if (existingEffects != null) {
			Effect existingEffect = existingEffects.stream().findFirst().get();
			existingEffectDuration = existingEffect.getDuration();
			existingEffectAmount = existingEffect.getMagnitude();
		}

		int duration = fromBoneShard ? ENHANCEMENT_BONE_SHARD_BONUS_DAMAGE_DURATION : Math.max(existingEffectDuration, ENHANCEMENT_ASH_BONUS_DAMAGE_DURATION);
		double bonusDamage = fromBoneShard ? ENHANCEMENT_BONUS_DAMAGE_MAX : Math.min(existingEffectAmount + ENHANCEMENT_ASH_BONUS_DAMAGE, ENHANCEMENT_BONUS_DAMAGE_MAX);

		mPlugin.mEffectManager.addEffect(player, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME,
			new PercentDamageDealt(duration, bonusDamage, null, 2, (attacker, enemy) -> Crusade.enemyTriggersAbilities(enemy, mCrusade)));
	}

	@Override
	public void remove(Player p) {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			DivineJustice dj = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(p, DivineJustice.class);
			if (dj == null || !dj.isEnhanced()) {
				mPlugin.mEffectManager.clearEffects(p, ENHANCEMENT_BONUS_DAMAGE_EFFECT_NAME);
			}
		}, 5);
	}

}
