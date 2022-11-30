package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.TransmRingCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class TransmutationRing extends PotionAbility {
	private static final int TRANSMUTATION_RING_1_COOLDOWN = 25 * 20;
	private static final int TRANSMUTATION_RING_2_COOLDOWN = 20 * 20;
	private static final int TRANSMUTATION_RING_RADIUS = 5;
	private static final int TRANSMUTATION_RING_DURATION = 10 * 20;
	private static final String TRANSMUTATION_RING_DAMAGE_EFFECT_NAME = "TransmutationRingDamageEffect";
	private static final double DAMAGE_AMPLIFIER = 0.15;
	private static final double DAMAGE_PER_DEATH_AMPLIFIER = 0.01;
	private static final int MAX_KILLS = 15;
	private static final int DURATION_INCREASE = 7; // was 0.333333 seconds but minecraft tick system is bad
	private static final int MAX_DURATION_INCREASE = 5 * 20;

	public static final String TRANSMUTATION_POTION_METAKEY = "TransmutationRingPotion";

	public static final String CHARM_COOLDOWN = "Transmutation Ring Cooldown";
	public static final String CHARM_RADIUS = "Transmutation Ring Radius";
	public static final String CHARM_DURATION = "Transmutation Ring Duration";
	public static final String CHARM_DAMAGE_AMPLIFIER = "Transmutation Ring Damage Amplifier";
	public static final String CHARM_PER_KILL_AMPLIFIER = "Transmutation Ring Per Death Amplifier";
	public static final String CHARM_MAX_KILLS = "Transmutation Ring Max Kills";

	public static final AbilityInfo<TransmutationRing> INFO =
		new AbilityInfo<>(TransmutationRing.class, "Transmutation Ring", TransmutationRing::new)
			.linkedSpell(ClassAbility.TRANSMUTATION_RING)
			.scoreboardId("Transmutation")
			.shorthandName("TR")
			.descriptions(
				"Sneak while throwing an Alchemist's Potion to create a Transmutation Ring at the potion's landing location that lasts for 10 seconds. " +
					"The ring has a radius of 5 blocks. Other players within this ring deal 10% extra damage on all attacks. " +
					"Mobs that die within this ring increase the damage bonus by 1% per mob, up to 30% total extra damage. Cooldown: 25s.",
				"Mobs that die within this ring also increase the duration of the ring by 0.35 seconds per mob, up to 5 extra seconds. Cooldown: 20s.")
			.cooldown(TRANSMUTATION_RING_1_COOLDOWN, TRANSMUTATION_RING_2_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(new ItemStack(Material.GOLD_NUGGET, 1));

	private final double mRadius;

	private @Nullable Location mCenter;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private int mKills = 0;

	private final TransmRingCS mCosmetic;

	public TransmutationRing(Plugin plugin, Player player) {
		super(plugin, player, INFO, 0, 0);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, TRANSMUTATION_RING_RADIUS);

		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TransmRingCS(), TransmRingCS.SKIN_LIST);
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (!isOnCooldown()
			    && mPlayer.isSneaking()
			    && ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand())
			    && mAlchemistPotions.isAlchemistPotion(potion)) {
			putOnCooldown();
			potion.setMetadata(TRANSMUTATION_POTION_METAKEY, new FixedMetadataValue(mPlugin, null));
		}
		return true;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata(TRANSMUTATION_POTION_METAKEY)) {
			mCenter = potion.getLocation();
			World world = mPlayer.getWorld();

			mCosmetic.ringSoundStart(world, mCenter);

			int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, TRANSMUTATION_RING_DURATION);
			double amplifier = DAMAGE_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_AMPLIFIER);
			double perKillAmplifier = DAMAGE_PER_DEATH_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_PER_KILL_AMPLIFIER);
			int maxKills = MAX_KILLS + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_KILLS);

			PPCircle particles = mCosmetic.ringPPCircle(mCenter, mRadius);

			new BukkitRunnable() {
				int mTicks = 0;
				int mMaxTicks = duration;

				@Override
				public void run() {
					if (isLevelTwo()) {
						mMaxTicks = duration + Math.min(mKills * DURATION_INCREASE, MAX_DURATION_INCREASE);
					}

					if (mTicks >= mMaxTicks || mCenter == null) {
						mCenter = null;
						mKills = 0;
						this.cancel();
						return;
					}

					double damageBoost = amplifier + Math.min(mKills, maxKills) * perKillAmplifier;
					List<Player> players = PlayerUtils.playersInRange(mCenter, mRadius, true);
					players.remove(mPlayer);
					for (Player player : players) {
						mPlugin.mEffectManager.addEffect(player, TRANSMUTATION_RING_DAMAGE_EFFECT_NAME, new PercentDamageDealt(20, damageBoost).displaysTime(false));
					}

					mCosmetic.ringEffect(mPlayer, mCenter, particles, mRadius, TRANSMUTATION_RING_RADIUS, mTicks);

					mTicks += 5;
				}
			}.runTaskTimer(mPlugin, 0, 5);
		}

		return true;
	}

	@Override
	public @Nullable Location entityDeathRadiusCenterLocation() {
		return mCenter;
	}

	@Override
	public double entityDeathRadius() {
		return mRadius;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mKills++;
		mCosmetic.ringEffectOnKill(mPlayer, event.getEntity().getLocation());
	}

}
