package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
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
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
	private static final int PHILOSOPHER_STONE_ABSORPTION_HEALTH = 24;
	private static final int PHILOSOPHER_STONE_ABSORPTION_DURATION = 20 * 3;
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

	private int mKills = 0;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private final int mLingerTime;

	private final BezoarCS mCosmetic;

	public Bezoar(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Bezoar");
		mInfo.mLinkedSpell = ClassAbility.BEZOAR;
		mInfo.mScoreboardId = "Bezoar";
		mInfo.mShorthandName = "BZ";
		mInfo.mDescriptions.add("Every 5th mob killed within 16 blocks of the Alchemist spawns a Bezoar that lingers for 10s. Picking up a Bezoar will grant the Alchemist an additional Alchemist Potion, and will grant both the player who picks it up and the Alchemist a custom healing effect that regenerates 5% of max health every second for 2 seconds and reduces the duration of all current potion debuffs by 10s.");
		mInfo.mDescriptions.add("The Bezoar now additionally grants +15% damage from all sources for 8s.");
		mInfo.mDescriptions.add("When a Bezoar would spawn, 10% of the time it will summon a Philosopher's Stone instead. When the Philosopher's Stone is picked up, the player and the Alchemist gain 12 absorption health for 3s and the Alchemist gains 3 potions.");
		mDisplayItem = new ItemStack(Material.LIME_CONCRETE, 1);

		mLingerTime = LINGER_TIME + CharmManager.getExtraDuration(mPlayer, CHARM_LINGER_TIME);

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
			final BlockData mFallingDustData = Material.LIME_CONCRETE.createBlockData();
			@Override
			public void run() {
				if (mPlayer == null) {
					return;
				}
				mT++;
				Location itemLoc = item.getLocation();
				new PartialParticle(Particle.FALLING_DUST, itemLoc, 1, 0.2, 0.2, 0.2, mFallingDustData).spawnAsPlayerActive(mPlayer);
				for (Player p : PlayerUtils.playersInRange(itemLoc, 1, true)) {
					if (p != mPlayer) {
						applyEffects(p);
					}
					applyEffects(mPlayer);

					if (mAlchemistPotions != null) {
						mAlchemistPotions.incrementCharges(POTIONS + (int) CharmManager.getLevel(mPlayer, CHARM_POTIONS));
					}

					item.remove();

					world.playSound(itemLoc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					world.playSound(itemLoc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					world.playSound(itemLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1f);
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

	private void applyEffects(Player player) {
		if (mPlayer == null) {
			return;
		}

		int debuffReduction = DEBUFF_REDUCTION + CharmManager.getExtraDuration(mPlayer, CHARM_DEBUFF_REDUCTION);
		for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
			PotionEffect effect = player.getPotionEffect(effectType);
			if (effect != null) {
				player.removePotionEffect(effectType);
				// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
				player.addPotionEffect(new PotionEffect(effectType, Math.max(effect.getDuration() - debuffReduction, 0), effect.getAmplifier()));
			}
		}

		double maxHealth = EntityUtils.getMaxHealth(player);
		mPlugin.mEffectManager.addEffect(player, "BezoarHealing", new CustomRegeneration(HEAL_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_HEAL_DURATION), CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, maxHealth * HEAL_PERCENT), mPlayer, mPlugin));

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(player, "BezoarPercentDamageDealtEffect", new PercentDamageDealt(DAMAGE_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DAMAGE_DURATION), DAMAGE_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE)));
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
				if (mPlayer == null) {
					return;
				}
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

					world.playSound(itemLoc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					world.playSound(itemLoc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					world.playSound(itemLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1f);
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
		if (mPlayer == null) {
			return;
		}

		double absorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, PHILOSOPHER_STONE_ABSORPTION_HEALTH);
		AbsorptionUtils.addAbsorption(player, absorption, absorption, PHILOSOPHER_STONE_ABSORPTION_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_ABSORPTION_DURATION));
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
