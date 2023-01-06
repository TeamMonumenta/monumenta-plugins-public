package com.playmonumenta.plugins.abilities.mage.arcanist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.arcanist.CosmicMoonbladeCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CosmicMoonblade extends Ability {

	public static final String NAME = "Cosmic Moonblade";
	private static final double DAMAGE_1 = 5;
	private static final double DAMAGE_2 = 7;
	private static final int SWINGS = 2;
	private static final int RADIUS = 5;
	private static final int COOLDOWN = 20 * 8;
	private static final double ANGLE = 55;
	public static final double REDUCTION_MULTIPLIER_1 = 0.05;
	public static final double REDUCTION_MULTIPLIER_2 = 0.1;
	public static final int CAP_TICKS_1 = (int) (0.5 * Constants.TICKS_PER_SECOND);
	public static final int CAP_TICKS_2 = 1 * Constants.TICKS_PER_SECOND;

	public static final String CHARM_DAMAGE = "Cosmic Moonblade Damage";
	public static final String CHARM_SPELL_COOLDOWN = "Cosmic Moonblade Cooldown Reduction";
	public static final String CHARM_COOLDOWN = "Cosmic Moonblade Cooldown";
	public static final String CHARM_CAP = "Cosmic Moonblade Cooldown Cap";
	public static final String CHARM_RANGE = "Cosmic Moonblade Range";
	public static final String CHARM_SLASH = "Cosmic Moonblade Slashes";

	public static final AbilityInfo<CosmicMoonblade> INFO =
		new AbilityInfo<>(CosmicMoonblade.class, "Cosmic Moonblade", CosmicMoonblade::new)
			.linkedSpell(ClassAbility.COSMIC_MOONBLADE)
			.scoreboardId("CosmicMoonblade")
			.shorthandName("CM")
			.descriptions(
				String.format("Swap while holding a wand to cause a wave of arcane blades to hit every enemy within a %s block cone %s times in rapid succession. " +
					              "Each slash deals %s arcane magic damage and reduces all your other skill cooldowns by %s%% (Max %ss) if it hits at least one mob. Cooldown: %ss.",
					RADIUS,
					SWINGS,
					(int) DAMAGE_1,
					(int) (REDUCTION_MULTIPLIER_1 * 100),
					CAP_TICKS_1 / 20.0,
					COOLDOWN / 20),
				String.format("Cooldown reduction is increased to %s%% (Max %ss) per blade and damage is increased to %s.",
					(int) (REDUCTION_MULTIPLIER_2 * 100),
					CAP_TICKS_2 / 20,
					(int) DAMAGE_2))
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CosmicMoonblade::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(new ItemStack(Material.DIAMOND_SWORD, 1));

	private final double mDamage;
	private final double mLevelReduction;
	private final int mLevelCap;
	private final CosmicMoonbladeCS mCosmetic;

	public CosmicMoonblade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mLevelReduction = (isLevelOne() ? REDUCTION_MULTIPLIER_1 : REDUCTION_MULTIPLIER_2) + CharmManager.getLevelPercentDecimal(player, CHARM_SPELL_COOLDOWN);
		mLevelCap = CharmManager.getDuration(player, CHARM_CAP, (isLevelOne() ? CAP_TICKS_1 : CAP_TICKS_2));
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CosmicMoonbladeCS(), CosmicMoonbladeCS.SKIN_LIST);
	}


	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();
		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, (float) mDamage);
		double range = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RADIUS);
		int swings = (int) CharmManager.getLevel(mPlayer, CHARM_SLASH) + SWINGS;
		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		cancelOnDeath(new BukkitRunnable() {
			int mSwings = 0;

			@Override
			public void run() {
				mSwings++;
				Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), range, Math.toRadians(ANGLE));
				List<LivingEntity> hitMobs = hitbox.getHitMobs();
				if (!hitMobs.isEmpty()) {
					updateCooldowns(mLevelReduction);
					for (LivingEntity mob : hitMobs) {
						DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, false, false);
					}
				}

				World world = mPlayer.getWorld();
				Location origin = mPlayer.getLocation();
				mCosmetic.moonbladeSwingEffect(world, mPlayer, origin, range, mSwings, swings);

				if (mSwings >= swings) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 7));
	}

	public void updateCooldowns(double percent) {
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
			if (abil == this || linkedSpell == null) {
				continue;
			}
			int totalCD = abil.getModifiedCooldown();
			int reducedCD = Math.min((int) (totalCD * percent), mLevelCap);
			mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, reducedCD);
		}
	}
}
