package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.EagleEyeCS;
import com.playmonumenta.plugins.effects.EagleEyeEnhancement;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class EagleEye extends Ability {

	private static final int EAGLE_EYE_DURATION = 10 * 20;
	private static final int EAGLE_EYE_COOLDOWN = 24 * 20;
	private static final int EAGLE_EYE_REFRESH = 2 * 20;
	private static final double EAGLE_EYE_1_VULN_LEVEL = 0.2;
	private static final double EAGLE_EYE_2_VULN_LEVEL = 0.35;
	private static final int EAGLE_EYE_RADIUS = 24;
	private static final double ENHANCEMENT_DAMAGE_PERCENT = 0.35;
	private static final int ENHANCEMENT_HITS = 1;

	public static final String CHARM_DURATION = "Eagle Eye Duration";
	public static final String CHARM_COOLDOWN = "Eagle Eye Cooldown";
	public static final String CHARM_VULN = "Eagle Eye Vulnerability Amplifier";
	public static final String CHARM_RADIUS = "Eagle Eye Radius";
	public static final String CHARM_REFRESH = "Eagle Eye Refresh";
	public static final String CHARM_DAMAGE = "Eagle Eye Enhancement Damage";
	public static final String CHARM_HITS = "Eagle Eye Enhancement Hits";

	public static final String GLOWING_OPTION_SCOREBOARD_NAME = "EagleEyeGlowingOption";

	public enum GlowingOption {
		ALL("Mobs affected by any player's Eagle Eye will glow (default)"),
		OWN("Only mobs affected by your own Eagle Eye will glow"),
		NEVER("No mobs affected by Eagle Eye will glow");

		public final String mDescription;

		GlowingOption(String description) {
			this.mDescription = description;
		}
	}

	public static final AbilityInfo<EagleEye> INFO =
		new AbilityInfo<>(EagleEye.class, "Eagle Eye", EagleEye::new)
			.linkedSpell(ClassAbility.EAGLE_EYE)
			.scoreboardId("Tinkering")
			.shorthandName("EE")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Reveal nearby mobs, making them more vulnerable to attacks.")
			.cooldown(EAGLE_EYE_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EagleEye::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).fallThrough()
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.ENDER_EYE);

	private final double mVulnLevel;
	private final double mRadius;
	private final int mDuration;
	private final int mRefresh;
	private List<LivingEntity> mEntitiesAffected = new ArrayList<>(); // Used for tracking Entities on a first hit.
	private final double mDamage;
	private final int mHits;
	private final EagleEyeCS mCosmetic;

	public EagleEye(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mVulnLevel = (isLevelOne() ? EAGLE_EYE_1_VULN_LEVEL : EAGLE_EYE_2_VULN_LEVEL) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULN);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, EAGLE_EYE_RADIUS);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, EAGLE_EYE_DURATION);
		mRefresh = CharmManager.getDuration(mPlayer, CHARM_REFRESH, EAGLE_EYE_REFRESH);
		mDamage = ENHANCEMENT_DAMAGE_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mHits = ENHANCEMENT_HITS + (int) CharmManager.getLevel(mPlayer, CHARM_HITS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EagleEyeCS());
	}


	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		mCosmetic.eyeStart(world, mPlayer, mPlayer.getLocation());

		mEntitiesAffected = new Hitbox.SphereHitbox(mPlayer.getEyeLocation(), mRadius).getHitMobs();


		for (LivingEntity mob : mEntitiesAffected) {
			// Don't apply vulnerability to arena mobs
			if (mob.getScoreboardTags().contains("arena_mob")) {
				continue;
			}

			// If enhanced, add two glowing instances and remove the enhancement bonus glow later
			if (isEnhanced()) {
				mPlugin.mEffectManager.addEffect(mob, "EagleEye", new EagleEyeEnhancement(mDamage, mHits, mDuration, mPlayer, mCosmetic));
				GlowingManager.startGlowing(mob, mCosmetic.enhancementGlowColor(), mDuration, GlowingManager.PLAYER_ABILITY_PRIORITY + 1, p -> canSeeGlowing(p, mPlayer), "EagleEyeEnhancement-" + mPlayer.name());
			}
			GlowingManager.startGlowing(mob, NamedTextColor.WHITE, mDuration, GlowingManager.PLAYER_ABILITY_PRIORITY, p -> canSeeGlowing(p, mPlayer), null);

			EntityUtils.applyVulnerability(mPlugin, mDuration, mVulnLevel, mob);

			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					if (mob.isDead() || !mob.isValid()) {
						mPlugin.mTimers.updateCooldown(mPlayer, ClassAbility.EAGLE_EYE, mRefresh);
						this.cancel();
					}
					if (mTicks >= mDuration) {
						this.cancel();
						mEntitiesAffected.clear();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
			mCosmetic.eyeOnTarget(world, mPlayer, mob);
		}

		putOnCooldown();

		// Always return false - Eagle Eye is a special case where we want other abilities to trigger regardless of success
		return false;
	}

	private static boolean canSeeGlowing(Player player, Player sourcePlayer) {
		int value = Math.min(Math.max(0, ScoreboardUtils.getScoreboardValue(player, GLOWING_OPTION_SCOREBOARD_NAME).orElse(0)), GlowingOption.values().length);
		GlowingOption option = GlowingOption.values()[value];
		return option == GlowingOption.ALL || (option == GlowingOption.OWN && player == sourcePlayer);
	}

	private static Description<EagleEye> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to reveal all enemies within ")
			.add(a -> a.mRadius, EAGLE_EYE_RADIUS)
			.add(" blocks, giving them the glowing effect for ")
			.addDuration(a -> a.mDuration, EAGLE_EYE_DURATION)
			.add(" seconds. Affected enemies have ")
			.addPercent(a -> a.mVulnLevel, EAGLE_EYE_1_VULN_LEVEL, false, Ability::isLevelOne)
			.add(" vulnerability. If a mob under the effect of Eagle Eye dies the cooldown of Eagle Eye is reduced by ")
			.addDuration(a -> a.mRefresh, EAGLE_EYE_REFRESH)
			.add(" seconds.")
			.addCooldown(EAGLE_EYE_COOLDOWN);
	}

	private static Description<EagleEye> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The vulnerability is increased to ")
			.addPercent(a -> a.mVulnLevel, EAGLE_EYE_2_VULN_LEVEL, false, Ability::isLevelTwo)
			.add(".");
	}

	private static Description<EagleEye> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your first attack against every enemy affected by this ability will deal ")
			.addPercent(ENHANCEMENT_DAMAGE_PERCENT)
			.add(" extra damage.");
	}
}
