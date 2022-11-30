package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.bosses.bosses.abilities.AlchemicalAberrationBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.EsotericEnhancementsCS;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class EsotericEnhancements extends PotionAbility {
	private static final double ABERRATION_POTION_DAMAGE_MULTIPLIER_1 = 0.6;
	private static final double ABERRATION_POTION_DAMAGE_MULTIPLIER_2 = 1;
	private static final double ABERRATION_DAMAGE_RADIUS = 3;
	private static final int ABERRATION_SUMMON_DURATION = 30;
	private static final double ABERRATION_BLEED_AMOUNT = 0.2;
	private static final int ABERRATION_BLEED_DURATION = 4 * 20;
	private static final int ABERRATION_COOLDOWN = 5 * 20;
	private static final double ABERRATION_TARGET_RADIUS = 8;
	private static final int ABERRATION_LIFETIME = 15 * 20;
	private static final int TICK_INTERVAL = 5;
	private static final double MAX_TARGET_Y = 4;

	public static final String CHARM_DAMAGE = "Esoteric Enhancements Damage";
	public static final String CHARM_RADIUS = "Esoteric Enhancements Radius";
	public static final String CHARM_BLEED = "Esoteric Enhancements Bleed Amplifier";
	public static final String CHARM_DURATION = "Esoteric Enhancements Bleed Duration";
	public static final String CHARM_COOLDOWN = "Esoteric Enhancements Cooldown";
	public static final String CHARM_CREEPER = "Esoteric Enhancements Creeper";
	public static final String CHARM_REACTION_TIME = "Esoteric Enhancements Reaction Time";
	public static final String CHARM_FUSE = "Esoteric Enhancements Fuse Time";
	public static final String CHARM_SPEED = "Esoteric Enhancements Speed";

	public static final AbilityInfo<EsotericEnhancements> INFO =
		new AbilityInfo<>(EsotericEnhancements.class, "Esoteric Enhancements", EsotericEnhancements::new)
			.linkedSpell(ClassAbility.ESOTERIC_ENHANCEMENTS)
			.scoreboardId("Esoteric")
			.shorthandName("Es")
			.descriptions(
				"When afflicting a mob with a Brutal potion within 1.5s of afflicting that mob with a Gruesome potion, summon an Alchemical Aberration. " +
					"The Aberration targets the mob with the highest health within 8 blocks and explodes on that mob, " +
					"dealing 60% of your potion damage and applying 20% Bleed for 4s to all mobs in a 3 block radius. Cooldown: 5s.",
				"Damage is increased to 100% of your potion damage.")
			.cooldown(ABERRATION_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(new ItemStack(Material.CREEPER_HEAD, 1));

	private @Nullable AlchemistPotions mAlchemistPotions;
	private final double mDamageMultiplier;

	private final HashMap<LivingEntity, Integer> mAppliedMobs;
	private final EsotericEnhancementsCS mCosmetic;

	public EsotericEnhancements(Plugin plugin, Player player) {
		super(plugin, player, INFO, 0, 0);

		mAppliedMobs = new HashMap<>();

		mDamageMultiplier = isLevelOne() ? ABERRATION_POTION_DAMAGE_MULTIPLIER_1 : ABERRATION_POTION_DAMAGE_MULTIPLIER_2;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EsotericEnhancementsCS(), EsotericEnhancementsCS.SKIN_LIST);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (isGruesome) {
			mAppliedMobs.put(mob, mob.getTicksLived());
		} else if (!isOnCooldown()) {
			// Clear out list so it doesn't build up
			int reactionTime = CharmManager.getDuration(mPlayer, CHARM_REACTION_TIME, ABERRATION_SUMMON_DURATION);
			mAppliedMobs.keySet().removeIf((entity) -> (entity.getTicksLived() - mAppliedMobs.get(entity) > reactionTime));

			// If it's still in the list, it was applied recently enough
			if (mAppliedMobs.containsKey(mob)) {
				int num = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_CREEPER);
				for (int i = 0; i < num; i++) {
					summonAberration(mob.getLocation());
					mCosmetic.esotericSummonEffect(mob.getWorld(), mPlayer, mob.getLocation());
				}
				putOnCooldown();
			}
		}
	}

	private void summonAberration(Location loc) {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Creeper aberration = (Creeper) LibraryOfSoulsIntegration.summon(loc, mCosmetic.getLos());
			if (aberration == null) {
				MMLog.warning("Failed to spawn Alchemical Aberration from Library of Souls");
				return;
			}

			AlchemicalAberrationBoss alchemicalAberrationBoss = BossUtils.getBossOfClass(aberration, AlchemicalAberrationBoss.class);
			if (alchemicalAberrationBoss == null) {
				MMLog.warning("Failed to get AlchemicalAberrationBoss for AlchemicalAberration");
				return;
			}

			double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ABERRATION_DAMAGE_RADIUS);
			alchemicalAberrationBoss.spawn(mPlayer, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mAlchemistPotions.getDamage() * mDamageMultiplier), radius, CharmManager.getDuration(mPlayer, CHARM_DURATION, ABERRATION_BLEED_DURATION), ABERRATION_BLEED_AMOUNT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BLEED), mPlugin.mItemStatManager.getPlayerItemStats(mPlayer));

			aberration.setMaxFuseTicks(CharmManager.getDuration(mPlayer, CHARM_FUSE, aberration.getMaxFuseTicks()));
			aberration.setExplosionRadius((int) radius);
			EntityUtils.setAttributeBase(aberration, Attribute.GENERIC_MOVEMENT_SPEED, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SPEED, EntityUtils.getAttributeBaseOrDefault(aberration, Attribute.GENERIC_MOVEMENT_SPEED, 0)));
			if (isLevelTwo()) {
				aberration.setPowered(true);
			}

			new BukkitRunnable() {
				int mTicks = 0;
				LivingEntity mTarget = null;
				@Override
				public void run() {
					if (mTicks >= ABERRATION_LIFETIME || !mPlayer.isOnline() || mPlayer.isDead() || aberration.isDead()) {
						aberration.remove();
						this.cancel();
						return;
					}

					if (mTarget == null || mTarget.isDead()) {
						mTarget = getHealthiestMob(aberration);
					}

					if (mTarget != null) {
						aberration.setTarget(mTarget);
					}

					mTicks += TICK_INTERVAL;
				}
			}.runTaskTimer(mPlugin, 0, TICK_INTERVAL);

		}, 1);
	}

	private @Nullable LivingEntity getHealthiestMob(LivingEntity aberration) {
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(aberration.getLocation(), ABERRATION_TARGET_RADIUS, aberration);
		nearbyMobs.removeIf(Entity::isInvulnerable);
		nearbyMobs.removeIf((mob) -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
		nearbyMobs.removeIf((mob) -> Math.abs(mob.getLocation().getY() - aberration.getLocation().getY()) > MAX_TARGET_Y);
		nearbyMobs.sort(Comparator.comparingDouble(Damageable::getHealth));
		if (!nearbyMobs.isEmpty()) {
			return nearbyMobs.get(nearbyMobs.size() - 1);
		}
		return null;
	}

}
