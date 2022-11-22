package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.EagleEyeCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

public class EagleEye extends Ability {

	private static final int EAGLE_EYE_EFFECT_LVL = 0;
	private static final int EAGLE_EYE_DURATION = 10 * 20;
	private static final int EAGLE_EYE_COOLDOWN = 24 * 20;
	private static final int EAGLE_EYE_REFRESH = 2 * 20;
	private static final double EAGLE_EYE_1_VULN_LEVEL = 0.2;
	private static final double EAGLE_EYE_2_VULN_LEVEL = 0.35;
	private static final int EAGLE_EYE_RADIUS = 24;
	private static final double ENHANCEMENT_DAMAGE_PERCENT = 0.15;

	public static final String CHARM_DURATION = "Eagle Eye Duration";
	public static final String CHARM_COOLDOWN = "Eagle Eye Cooldown";
	public static final String CHARM_VULN = "Eagle Eye Vulnerability Amplifier";
	public static final String CHARM_RADIUS = "Eagle Eye Radius";
	public static final String CHARM_REFRESH = "Eagle Eye Refresh";

	public static final AbilityInfo<EagleEye> INFO =
		new AbilityInfo<>(EagleEye.class, "Eagle Eye", EagleEye::new)
			.linkedSpell(ClassAbility.EAGLE_EYE)
			.scoreboardId("Tinkering")
			.shorthandName("EE")
			.descriptions(
				String.format("When you left-click while sneaking you reveal all enemies in a %d block radius, " +
					              "giving them the glowing effect for %d seconds. Affected enemies have %d%% Vulnerability. " +
					              "If a mob under the effect of Eagle Eye dies the cooldown of Eagle Eye is reduced by %d seconds. " +
					              "This skill can not be activated if you have a pickaxe in your mainhand. Cooldown: %ds.",
					EAGLE_EYE_RADIUS, EAGLE_EYE_DURATION / 20, (int) (EAGLE_EYE_1_VULN_LEVEL * 100), EAGLE_EYE_REFRESH / 20, EAGLE_EYE_COOLDOWN / 20),
				String.format("The effect is increased to %d%% Vulnerability.", (int) (EAGLE_EYE_2_VULN_LEVEL * 100)),
				"Your first attack against every enemy affected by this ability will deal " + (int) (ENHANCEMENT_DAMAGE_PERCENT * 100) + "% extra damage.")
			.cooldown(EAGLE_EYE_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EagleEye::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)
				                                                                     .keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(new ItemStack(Material.ENDER_EYE, 1));

	private final double mVulnLevel;
	private final Team mEagleEyeTeam;
	private List<LivingEntity> mEntitiesAffected = new ArrayList<>(); // Used for tracking Entities on a first hit.
	private final EagleEyeCS mCosmetic;

	public EagleEye(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		// lmao
		mVulnLevel = (isLevelOne() ? EAGLE_EYE_1_VULN_LEVEL : EAGLE_EYE_2_VULN_LEVEL) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULN);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EagleEyeCS(), EagleEyeCS.SKIN_LIST);
		mEagleEyeTeam = mCosmetic.createTeams();

	}


	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		World world = mPlayer.getWorld();
		mCosmetic.eyeStart(world, mPlayer);

		mEntitiesAffected = new Hitbox.SphereHitbox(mPlayer.getEyeLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, EAGLE_EYE_RADIUS)).getHitMobs();

		for (LivingEntity mob : mEntitiesAffected) {
			// Don't apply vulnerability to arena mobs
			if (mob.getScoreboardTags().contains("arena_mob")) {
				continue;
			}

			// Only change glowing color if:
			// isEnhanced
			// mob not in a team
			if (isEnhanced() && Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(mob.getUniqueId().toString()) == null) {
				mEagleEyeTeam.addEntry(mob.getUniqueId().toString());
			}

			int duration = EAGLE_EYE_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);
			PotionUtils.applyPotion(mPlayer, mob,
				new PotionEffect(PotionEffectType.GLOWING, duration, EAGLE_EYE_EFFECT_LVL, true, false));
			EntityUtils.applyVulnerability(mPlugin, duration, mVulnLevel, mob);

			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					if (mob.isDead() || !mob.isValid()) {
						mPlugin.mTimers.updateCooldown(mPlayer, ClassAbility.EAGLE_EYE, EAGLE_EYE_REFRESH + CharmManager.getExtraDuration(mPlayer, CHARM_REFRESH));
						this.cancel();
					}
					if (mTicks >= EAGLE_EYE_DURATION) {
						if (mEagleEyeTeam.hasEntry(mob.getUniqueId().toString())) {
							mEagleEyeTeam.removeEntry(mob.getUniqueId().toString());
						}

						this.cancel();
						mEntitiesAffected.clear();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
			mCosmetic.eyeOnTarget(world, mPlayer, mob);
		}

		putOnCooldown();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (isEnhanced() && mEntitiesAffected.contains(enemy)) {
			event.setDamage(event.getDamage() * (1 + ENHANCEMENT_DAMAGE_PERCENT));
			mEntitiesAffected.remove(enemy);
			mCosmetic.eyeFirstStrike(enemy.getWorld(), mPlayer, enemy);

			// Revert glowing color to normal white
			if (mEagleEyeTeam.hasEntry(enemy.getUniqueId().toString())) {
				mEagleEyeTeam.removeEntry(enemy.getUniqueId().toString());
			}
		}

		return false;
	}

}
