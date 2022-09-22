package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.CholericFlamesCS;
import com.playmonumenta.plugins.effects.SpreadEffectOnDeath;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;



public class CholericFlames extends Ability {

	public static final int RADIUS = 8;
	private static final int DAMAGE_1 = 3;
	private static final int DAMAGE_2 = 5;
	private static final int DURATION = 7 * 20;
	private static final int COOLDOWN = 10 * 20;
	private static final int MAX_DEBUFFS = 3;
	private static final String SPREAD_EFFECT_ON_DEATH_EFFECT = "CholericFlamesSpreadEffectOnDeath";
	private static final int SPREAD_EFFECT_DURATION = 30 * 20;
	private static final int SPREAD_EFFECT_DURATION_APPLIED = 5 * 20;
	private static final double SPREAD_EFFECT_RADIUS = 3;
	public static final float KNOCKBACK = 0.5f;

	public static final String CHARM_DAMAGE = "Choleric Flames Damage";
	public static final String CHARM_RANGE = "Choleric Flames Range";
	public static final String CHARM_COOLDOWN = "Choleric Flames Cooldown";
	public static final String CHARM_FIRE = "Choleric Flames Fire Duration";
	public static final String CHARM_HUNGER = "Choleric Flames Hunger Duration";
	public static final String CHARM_KNOCKBACK = "Choleric Flames Knockback";

	private final double mDamage;

	private final CholericFlamesCS mCosmetic;

	public CholericFlames(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Choleric Flames");
		mInfo.mScoreboardId = "CholericFlames";
		mInfo.mShorthandName = "CF";
		mInfo.mDescriptions.add("Sneaking and right-clicking while not looking down while holding a scythe knocks back and ignites mobs within 8 blocks of you for 7s, additionally dealing 3 magic damage. Cooldown: 10s.");
		mInfo.mDescriptions.add("The damage is increased to 5, and also afflict mobs with Hunger I.");
		mInfo.mDescriptions.add("Mobs ignited by this ability are inflicted with an additional level of Inferno for each debuff they have prior to this ability, up to 3. Additionally, when these mobs die, they explode, applying all Inferno they have at the time of death to all mobs within a 3 block radius for 5s.");
		mInfo.mLinkedSpell = ClassAbility.CHOLERIC_FLAMES;
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.FIRE_CHARGE, 1);

		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CholericFlamesCS(), CholericFlamesCS.SKIN_LIST);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		Location loc = mPlayer.getLocation();

		World world = mPlayer.getWorld();
		new BukkitRunnable() {
			double mRadius = 0;
			final Location mLoc = mPlayer.getLocation().add(0, 0.15, 0);

			@Override
			public void run() {
				mRadius += 1.25;
				mCosmetic.flameParticle(mPlayer, mLoc, mRadius);
				if (mRadius >= CharmManager.getRadius(mPlayer, CHARM_RANGE, RADIUS) + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		mCosmetic.flameEffects(mPlayer, world, loc);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.mLinkedSpell, true);
			MovementUtils.knockAway(mPlayer, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK));

			// Gets a copy so modifying the inferno level does not have effect elsewhere
			ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
			if (isEnhanced()) {
				int debuffs = Math.min(AbilityUtils.getDebuffCount(mPlugin, mob), MAX_DEBUFFS);
				if (debuffs > 0) {
					playerItemStats.getItemStats().add(ItemStatUtils.EnchantmentType.INFERNO.getItemStat(), debuffs);
				}
				mPlugin.mEffectManager.addEffect(mob, SPREAD_EFFECT_ON_DEATH_EFFECT, new SpreadEffectOnDeath(SPREAD_EFFECT_DURATION, Inferno.INFERNO_EFFECT_NAME, SPREAD_EFFECT_RADIUS, SPREAD_EFFECT_DURATION_APPLIED, false));
			}
			EntityUtils.applyFire(mPlugin, DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_FIRE), mob, mPlayer, playerItemStats);

			if (isLevelTwo()) {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.HUNGER, DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_HUNGER), 0, false, true));
			}
		}

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())
		       && mPlayer.getLocation().getPitch() < 50;
	}
}
