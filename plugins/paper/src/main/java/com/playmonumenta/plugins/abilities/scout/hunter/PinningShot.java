package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.PinningShotCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class PinningShot extends Ability {

	private static final double PINNING_SHOT_1_DAMAGE_MULTIPLIER = 0.1;
	private static final double PINNING_SHOT_2_DAMAGE_MULTIPLIER = 0.2;
	private static final int PINNING_SHOT_DURATION = (int) (20 * 2.5);
	private static final double PINNING_SLOW = 1.0;
	private static final double PINNING_SLOW_BOSS = 0.3;
	private static final double PINNING_WEAKEN_1 = 0.3;
	private static final double PINNING_WEAKEN_2 = 0.6;

	public static final String CHARM_DAMAGE = "Pinning Shot Max Health Damage";
	public static final String CHARM_WEAKEN = "Pinning Shot Weakness Amplifier";

	public static final AbilityInfo<PinningShot> INFO =
		new AbilityInfo<>(PinningShot.class, "Pinning Shot", PinningShot::new)
			.linkedSpell(ClassAbility.PINNING_SHOT)
			.scoreboardId("PinningShot")
			.shorthandName("PSh")
			.descriptions(
				String.format("The first time you shoot a non-boss enemy, pin it for %ss. Pinned enemies are afflicted with %d%% Slowness and %d%% Weaken (Bosses receive %d%% Slowness and no Weaken). Shooting a pinned non-boss enemy deals %d%% of its max health on top of regular damage and removes the pin. A mob cannot be pinned more than once.",
					PINNING_SHOT_DURATION / 20.0, (int) (PINNING_SLOW * 100), (int) (PINNING_WEAKEN_1 * 100), (int) (PINNING_SLOW_BOSS * 100), (int) (PINNING_SHOT_1_DAMAGE_MULTIPLIER * 100)),
				String.format("Weaken increased to %d%% and bonus damage increased to %d%% max health.", (int) (PINNING_WEAKEN_2 * 100), (int) (PINNING_SHOT_2_DAMAGE_MULTIPLIER * 100)))
			.displayItem(new ItemStack(Material.CROSSBOW, 1));

	private final double mDamageMultiplier;
	private final double mWeaken;
	private final Map<LivingEntity, Boolean> mPinnedMobs = new HashMap<LivingEntity, Boolean>();
	private final PinningShotCS mCosmetic;

	public PinningShot(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageMultiplier = (isLevelOne() ? PINNING_SHOT_1_DAMAGE_MULTIPLIER : PINNING_SHOT_2_DAMAGE_MULTIPLIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mWeaken = (isLevelOne() ? PINNING_WEAKEN_1 : PINNING_WEAKEN_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new PinningShotCS(), PinningShotCS.SKIN_LIST);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageType.PROJECTILE || !(event.getDamager() instanceof Projectile proj) || !EntityUtils.isAbilityTriggeringProjectile(proj, false)) {
			return false;
		}

		World world = mPlayer.getWorld();
		if (mPinnedMobs.containsKey(enemy)) { // pinned once already
			if (mPinnedMobs.get(enemy)) { // currently pinned
				mPinnedMobs.put(enemy, false);
				mCosmetic.pinEffect2(world, mPlayer, enemy);
				EntityUtils.setSlowTicks(mPlugin, enemy, 1);
				EntityUtils.setWeakenTicks(mPlugin, enemy, 1);
				if (!EntityUtils.isBoss(enemy)) {
					DamageUtils.damage(mPlayer, enemy, DamageType.TRUE, EntityUtils.getMaxHealth(enemy) * mDamageMultiplier, mInfo.getLinkedSpell(), true, false);
				}
			}
		} else {
			mCosmetic.pinEffect1(world, mPlayer, enemy);
			if (EntityUtils.isBoss(enemy)) {
				EntityUtils.applySlow(mPlugin, PINNING_SHOT_DURATION, PINNING_SLOW_BOSS, enemy);
			} else {
				EntityUtils.applySlow(mPlugin, PINNING_SHOT_DURATION, PINNING_SLOW, enemy);
				EntityUtils.applyWeaken(mPlugin, PINNING_SHOT_DURATION, mWeaken, enemy);
			}
			mPinnedMobs.put(enemy, true);
			cancelOnDeath(new BukkitRunnable() {
				@Override
				public void run() {
					mPinnedMobs.put(enemy, false);
				}
			}.runTaskLater(mPlugin, PINNING_SHOT_DURATION));
		}
		return false; // prevents multiple applications itself
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			mPinnedMobs.entrySet().removeIf((entry) -> !entry.getKey().isValid() || entry.getKey().isDead());
		}
	}

}
