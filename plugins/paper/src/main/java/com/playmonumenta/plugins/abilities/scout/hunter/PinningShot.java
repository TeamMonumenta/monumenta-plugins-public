package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.scout.Quickdraw;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.PinningShotCS;
import com.playmonumenta.plugins.effects.ProjectileIframe;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

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

	public static final Style PIN_COLOR = Style.style(TextColor.color(0x818C3F));

	public static final AbilityInfo<PinningShot> INFO =
		new AbilityInfo<>(PinningShot.class, "Pinning Shot", PinningShot::new)
			.linkedSpell(ClassAbility.PINNING_SHOT)
			.scoreboardId("PinningShot")
			.shorthandName("PSh")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Shooting a mob for the first time roots and weakens it. Shooting it again damages it for a percentage of its maximum health.")
			.displayItem(Material.CROSSBOW);

	private final double mDamageMultiplier;
	private final double mWeaken;
	private final Map<LivingEntity, Boolean> mPinnedMobs = new HashMap<>();
	private final PinningShotCS mCosmetic;

	public PinningShot(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageMultiplier = (isLevelOne() ? PINNING_SHOT_1_DAMAGE_MULTIPLIER : PINNING_SHOT_2_DAMAGE_MULTIPLIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mWeaken = (isLevelOne() ? PINNING_WEAKEN_1 : PINNING_WEAKEN_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new PinningShotCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!(event.getDamager() instanceof Projectile proj) ||
			!EntityUtils.isAbilityTriggeringProjectile(proj, false) ||
			proj.getScoreboardTags().contains(Quickdraw.SOURCE_QUICKDRAW_VOLLEY_TAG)) {
			return false;
		}

		ProjectileIframe projectileIframe = mPlugin.mEffectManager.getActiveEffect(enemy, ProjectileIframe.class);
		if (projectileIframe != null) {
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

	private static Description<PinningShot> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Your first projectile against a mob *Pins*").styles(PIN_COLOR)
			.addLine("them for %t, rooting and weakening them.")
				.statValues(stat(PINNING_SHOT_DURATION))
			.addLine("(Bosses are excluded)")
			.addLine()
			.addStat("Effect: %p1 Weakness (while Pinned)")
				.statValues(stat(a -> a.mWeaken, PINNING_WEAKEN_1))
			.addLine()
			.addLine("Shooting the mob again removes the *Pin*").styles(PIN_COLOR)
			.addLine("and deals bonus damage based on the mob's")
			.addLine("maximum health.")
			.addLine()
			.addStat("Bonus Damage: %p1 of max HP")
				.statValues(stat(a -> a.mDamageMultiplier, PINNING_SHOT_1_DAMAGE_MULTIPLIER))
			.addDashedLine();
	}

	private static Description<PinningShot> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Pinning Shot*'s weakness").styles(UNDERLINED)
			.addLine("and bonus damage.").styles(PIN_COLOR)
			.addLine()
			.addStatComparison("Effect: %p1 -> %p2 Weakness")
				.statValues(stat(PINNING_WEAKEN_1), stat(a -> a.mWeaken, PINNING_WEAKEN_2))
			.addStatComparison("Bonus Damage: %p1 -> %p2 of max HP")
				.statValues(stat(PINNING_SHOT_1_DAMAGE_MULTIPLIER), stat(a -> a.mDamageMultiplier, PINNING_SHOT_2_DAMAGE_MULTIPLIER))
			.addDashedLine();
	}
}
