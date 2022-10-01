package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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
	private static final int EAGLE_EYE_RADIUS = 20;
	private static final double ENHANCEMENT_DAMAGE_PERCENT = 0.15;

	public static final String CHARM_DURATION = "Eagle Eye Duration";
	public static final String CHARM_COOLDOWN = "Eagle Eye Cooldown";
	public static final String CHARM_VULN = "Eagle Eye Vulnerability Amplifier";
	public static final String CHARM_RADIUS = "Eagle Eye Radius";
	public static final String CHARM_REFRESH = "Eagle Eye Refresh";

	private final double mVulnLevel;
	private Team mEagleEyeTeam = null;
	private List<LivingEntity> mEntitiesAffected = new ArrayList<>(); // Used for tracking Entities on a first hit.

	public EagleEye(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Eagle Eye");
		mInfo.mLinkedSpell = ClassAbility.EAGLE_EYE;
		mInfo.mScoreboardId = "Tinkering"; // lmao
		mInfo.mShorthandName = "EE";
		mInfo.mDescriptions.add(String.format("When you left-click while sneaking you reveal all enemies in a %d block radius, giving them the glowing effect for %d seconds. Affected enemies have %d%% Vulnerability. If a mob under the effect of Eagle Eye dies the cooldown of Eagle Eye is reduced by %d seconds. This skill can not be activated if you have a pickaxe in your mainhand. Cooldown: %ds.",
			EAGLE_EYE_RADIUS, EAGLE_EYE_DURATION / 20, (int)(EAGLE_EYE_1_VULN_LEVEL * 100), EAGLE_EYE_REFRESH / 20, EAGLE_EYE_COOLDOWN / 20));
		mInfo.mDescriptions.add(String.format("The effect is increased to %d%% Vulnerability.", (int)(EAGLE_EYE_2_VULN_LEVEL * 100)));
		mInfo.mDescriptions.add("Your first attack against every enemy affected by this ability will deal " + (int)(ENHANCEMENT_DAMAGE_PERCENT * 100) + "% extra damage.");
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, EAGLE_EYE_COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.ENDER_EYE, 1);
		mInfo.mIgnoreCooldown = true;

		mVulnLevel = (isLevelOne() ? EAGLE_EYE_1_VULN_LEVEL : EAGLE_EYE_2_VULN_LEVEL) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULN);
		mEagleEyeTeam = ScoreboardUtils.getExistingTeamOrCreate("eagleEyeColor", NamedTextColor.YELLOW);
	}


	@Override
	public void cast(Action action) {
		Player player = mPlayer;
		if (player == null || isTimerActive() || !mPlayer.isSneaking()) {
			return;
		}

		World world = player.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.25f);

		mEntitiesAffected = EntityUtils.getNearbyMobs(player.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, EAGLE_EYE_RADIUS), mPlayer);

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
			world.playSound(mob.getLocation(), Sound.ENTITY_PARROT_IMITATE_SHULKER, 0.4f, 0.7f);
			new PartialParticle(Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
		}

		putOnCooldown();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && mPlayer.isSneaking()) {
			cast(Action.LEFT_CLICK_AIR);
		}

		if (isEnhanced() && mEntitiesAffected.contains(enemy)) {
			event.setDamage(event.getDamage() * (1 + ENHANCEMENT_DAMAGE_PERCENT));
			mEntitiesAffected.remove(enemy);

			// Revert glowing color to normal white
			if (mEagleEyeTeam.hasEntry(enemy.getUniqueId().toString())) {
				mEagleEyeTeam.removeEntry(enemy.getUniqueId().toString());
			}
		}

		return false;
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		return !ItemUtils.isPickaxe(inMainHand) && inMainHand.getType() != Material.HEART_OF_THE_SEA;
	}
}
