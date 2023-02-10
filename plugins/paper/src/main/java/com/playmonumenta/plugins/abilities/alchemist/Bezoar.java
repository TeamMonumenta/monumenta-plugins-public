package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.BezoarCS;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Bezoar extends Ability {
	private static final int FREQUENCY = 5;
	private static final double RADIUS = 16;
	private static final int LINGER_TIME = 10 * 20;
	private static final int DEBUFF_REDUCTION = 10 * 20;
	private static final int HEAL_DURATION = 2 * 20;
	private static final double HEAL_PERCENT = 0.05;
	private static final int DAMAGE_DURATION = 8 * 20;
	private static final double DAMAGE_PERCENT = 0.15;
	private static final int POTIONS = 1;

	private static final double PHILOSOPHER_STONE_SPAWN_RATE = 0.1;
	private static final int PHILOSOPHER_STONE_ABSORPTION_HEALTH = 8;
	private static final int PHILOSOPHER_STONE_ABSORPTION_DURATION = 20 * 8;
	private static final int PHILOSOPHER_STONE_POTIONS = 3;

	public static final String CHARM_REQUIREMENT = "Bezoar Generation Requirement";
	public static final String CHARM_LINGER_TIME = "Bezoar Linger Duration";
	public static final String CHARM_DEBUFF_REDUCTION = "Bezoar Debuff Reduction";
	public static final String CHARM_HEAL_DURATION = "Bezoar Healing Duration";
	public static final String CHARM_HEALING = "Bezoar Healing";
	public static final String CHARM_DAMAGE_DURATION = "Bezoar Damage Duration";
	public static final String CHARM_DAMAGE = "Bezoar Damage Modifier";
	public static final String CHARM_PHILOSOPHER_STONE_RATE = "Bezoar Philosopher Stone Spawn Rate";
	public static final String CHARM_ABSORPTION = "Bezoar Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Bezoar Absorption Duration";
	public static final String CHARM_POTIONS = "Bezoar Potions";
	public static final String CHARM_PHILOSOPHER_STONE_POTIONS = "Bezoar Philosopher Stone Potions";
	public static final String CHARM_RADIUS = "Bezoar Radius";

	public static final AbilityInfo<Bezoar> INFO =
		new AbilityInfo<>(Bezoar.class, "Bezoar", Bezoar::new)
			.linkedSpell(ClassAbility.BEZOAR)
			.scoreboardId("Bezoar")
			.shorthandName("BZ")
			.descriptions(
				("Every %sth mob killed within %s blocks of the Alchemist spawns a Bezoar that lingers for %ss. " +
				"Picking up a Bezoar will grant the Alchemist an additional Alchemist Potion, " +
				"and will grant both the player who picks it up and the Alchemist a custom healing effect that " +
				"regenerates %s%% of max health every second for %ss and reduces the duration of all current " +
				"potion debuffs by %ss.")
					.formatted(
							FREQUENCY,
							StringUtils.to2DP(RADIUS),
							StringUtils.ticksToSeconds(LINGER_TIME),
							StringUtils.multiplierToPercentage(HEAL_PERCENT),
							StringUtils.ticksToSeconds(HEAL_DURATION),
							StringUtils.ticksToSeconds(DEBUFF_REDUCTION)
					),
				"The Bezoar now additionally grants +%s%% damage from all sources for %ss."
					.formatted(
							StringUtils.multiplierToPercentage(DAMAGE_PERCENT),
							StringUtils.ticksToSeconds(DAMAGE_DURATION)
					),
				("When a Bezoar would spawn, %s%% of the time it will summon a Philosopher's Stone instead. " +
				"When the Philosopher's Stone is picked up, the player and the Alchemist gain %s absorption hearts for " +
				"%ss and the Alchemist gains %s potions.")
					.formatted(
							StringUtils.multiplierToPercentage(PHILOSOPHER_STONE_SPAWN_RATE),
							PHILOSOPHER_STONE_ABSORPTION_HEALTH / 2,
							StringUtils.ticksToSeconds(PHILOSOPHER_STONE_ABSORPTION_DURATION),
							PHILOSOPHER_STONE_POTIONS
					)
			)
			.displayItem(new ItemStack(Material.LIME_CONCRETE, 1));

	private int mKills = 0;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private final int mLingerTime;

	private final BezoarCS mCosmetic;

	public Bezoar(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLingerTime = CharmManager.getDuration(mPlayer, CHARM_LINGER_TIME, LINGER_TIME);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new BezoarCS(), BezoarCS.SKIN_LIST);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	public void dropBezoar(EntityDeathEvent event) {
		mKills = 0;
		Location loc = event.getEntity().getLocation().add(0, 0.25, 0);
		if (isEnhanced() && FastUtils.RANDOM.nextDouble() <= (PHILOSOPHER_STONE_SPAWN_RATE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_PHILOSOPHER_STONE_RATE))) {
			spawnPhilosopherStone(loc);
		} else {
			spawnBezoar(loc);
		}
	}

	private void spawnBezoar(Location loc) {
		World world = loc.getWorld();
		ItemStack itemBezoar = mCosmetic.bezoarItem();
		Item item = world.dropItemNaturally(loc, itemBezoar);
		item.setGlowing(true);
		item.setPickupDelay(Integer.MAX_VALUE);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT++;
				Location itemLoc = item.getLocation();
				mCosmetic.bezoarTick(mPlayer, itemLoc, mT);
				for (Player p : PlayerUtils.playersInRange(itemLoc, 1, true)) {
					if (p != mPlayer) {
						applyEffects(p);
						mCosmetic.bezoarTarget(p, itemLoc);
					}
					applyEffects(mPlayer);
					mCosmetic.bezoarTarget(mPlayer, itemLoc);

					if (mAlchemistPotions != null) {
						mAlchemistPotions.incrementCharges(POTIONS + (int) CharmManager.getLevel(mPlayer, CHARM_POTIONS));
					}

					item.remove();
					mCosmetic.bezoarPickup(mPlayer, itemLoc);

					this.cancel();
					break;
				}

				if (mT >= mLingerTime || item.isDead()) {
					this.cancel();
					item.remove();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}


	private void applyEffects(Player player) {
		int debuffReduction = CharmManager.getDuration(mPlayer, CHARM_DEBUFF_REDUCTION, DEBUFF_REDUCTION);
		for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
			PotionEffect effect = player.getPotionEffect(effectType);
			if (effect != null) {
				player.removePotionEffect(effectType);
				// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
				player.addPotionEffect(new PotionEffect(effectType, Math.max(effect.getDuration() - debuffReduction, 0), effect.getAmplifier()));
			}
		}

		double maxHealth = EntityUtils.getMaxHealth(player);
		mPlugin.mEffectManager.addEffect(player, "BezoarHealing", new CustomRegeneration(CharmManager.getDuration(mPlayer, CHARM_HEAL_DURATION, HEAL_DURATION), CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, maxHealth * HEAL_PERCENT), mPlayer, mPlugin));

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(player, "BezoarPercentDamageDealtEffect", new PercentDamageDealt(CharmManager.getDuration(mPlayer, CHARM_DAMAGE_DURATION, DAMAGE_DURATION), DAMAGE_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE)));
		}
	}

	private void spawnPhilosopherStone(Location loc) {
		World world = loc.getWorld();
		ItemStack itemStone = new ItemStack(Material.RED_CONCRETE);
		ItemMeta stoneMeta = itemStone.getItemMeta();
		stoneMeta.displayName(Component.text("Philosopher's Stone", NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false));
		itemStone.setItemMeta(stoneMeta);
		ItemUtils.setPlainName(itemStone, "Philosopher's Stone");
		Item item = world.dropItemNaturally(loc, itemStone);
		item.setGlowing(true);
		item.setPickupDelay(Integer.MAX_VALUE);

		new BukkitRunnable() {
			int mT = 0;
			final BlockData mFallingDustData = Material.RED_CONCRETE.createBlockData();
			@Override
			public void run() {
				mT++;
				Location itemLoc = item.getLocation();
				new PartialParticle(Particle.FALLING_DUST, itemLoc, 1, 0.2, 0.2, 0.2, mFallingDustData).spawnAsPlayerActive(mPlayer);
				for (Player p : PlayerUtils.playersInRange(itemLoc, 1, true)) {
					if (p != mPlayer) {
						applyPhilosopherEffects(p);
					}
					applyPhilosopherEffects(mPlayer);

					if (mAlchemistPotions != null) {
						mAlchemistPotions.incrementCharges(PHILOSOPHER_STONE_POTIONS + (int) CharmManager.getLevel(mPlayer, CHARM_PHILOSOPHER_STONE_POTIONS));
					}

					item.remove();

					world.playSound(itemLoc, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
					world.playSound(itemLoc, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
					world.playSound(itemLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1, 1f);
					new PartialParticle(Particle.BLOCK_CRACK, itemLoc, 30, 0.15, 0.15, 0.15, 0.75F, Material.LIME_CONCRETE.createBlockData()).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.TOTEM, itemLoc, 20, 0, 0, 0, 0.35F).spawnAsPlayerActive(mPlayer);

					this.cancel();
					break;
				}

				if (mT >= mLingerTime || item.isDead()) {
					this.cancel();
					item.remove();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void applyPhilosopherEffects(Player player) {
		double absorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, PHILOSOPHER_STONE_ABSORPTION_HEALTH);
		AbsorptionUtils.addAbsorption(player, absorption, absorption, CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, PHILOSOPHER_STONE_ABSORPTION_DURATION));
	}

	public boolean shouldDrop() {
		return mKills >= FREQUENCY + (int) CharmManager.getLevel(mPlayer, CHARM_REQUIREMENT);
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (event.getEntity().getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			return;
		}

		mKills++;
		if (shouldDrop()) {
			dropBezoar(event);
		}
	}

	@Override
	public double entityDeathRadius() {
		return CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
	}

}
