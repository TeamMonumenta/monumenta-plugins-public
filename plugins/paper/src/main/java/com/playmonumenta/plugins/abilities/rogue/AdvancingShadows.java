package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.AdvancingShadowsCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class AdvancingShadows extends Ability {

	private static final int ADVANCING_SHADOWS_RANGE_1 = 11;
	private static final int ADVANCING_SHADOWS_RANGE_2 = 16;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED = 0.5f;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE = 4;
	private static final double ADVANCING_SHADOWS_OFFSET = 2.7;
	private static final int DURATION = 5 * 20;
	private static final double DAMAGE_BONUS_1 = 0.3;
	private static final double DAMAGE_BONUS_2 = 0.4;
	private static final int ADVANCING_SHADOWS_COOLDOWN = 20 * 20;
	private static final int ENHANCEMENT_KILL_REQUIREMENT_TIME = 20;
	private static final int ENHANCEMENT_CHAIN_DURATION = 20 * 4;

	public static final String CHARM_DAMAGE = "Advancing Shadows Damage Multiplier";
	public static final String CHARM_COOLDOWN = "Advancing Shadows Cooldown";
	public static final String CHARM_RANGE = "Advancing Shadows Range";
	public static final String CHARM_KNOCKBACK = "Advancing Shadows Knockback";
	public static final String CHARM_ENHANCE_TIMER = "Advancing Shadows Recast Timer";

	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "AdvancingShadowsPercentDamageDealtEffect";
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = DamageEvent.DamageType.getAllMeleeTypes();

	public static final AbilityInfo<AdvancingShadows> INFO =
		new AbilityInfo<>(AdvancingShadows.class, "Advancing Shadows", AdvancingShadows::new)
			.linkedSpell(ClassAbility.ADVANCING_SHADOWS)
			.scoreboardId("AdvancingShadows")
			.shorthandName("AS")
			.descriptions(
				String.format("While holding two swords and not sneaking, right click to teleport to the target hostile enemy within %s blocks and gain +%s%% Melee Damage for %s seconds. Cooldown: %ss.",
					ADVANCING_SHADOWS_RANGE_1 - 1,
					(int) (DAMAGE_BONUS_1 * 100),
					DURATION / 20,
					ADVANCING_SHADOWS_COOLDOWN / 20),
				String.format("Damage increased to +%s%% Melee Damage for %ss, teleport range is increased to %s blocks and all hostile non-target mobs within %s blocks are knocked away from the target.",
					(int) (DAMAGE_BONUS_2 * 100),
					DURATION / 20,
					ADVANCING_SHADOWS_RANGE_2 - 1,
					(int) ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE),
				String.format("If the mob you teleported to dies within %ss, you can recast Advancing Shadows again in the next %ss. Recasts provide 50%% of the damage bonus and do not provide Deadly Ronde stacks.",
					ENHANCEMENT_KILL_REQUIREMENT_TIME / 20,
					ENHANCEMENT_CHAIN_DURATION / 20))
			.simpleDescription("Teleport to a mob and gain a damage bonus.")
			.cooldown(ADVANCING_SHADOWS_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", AdvancingShadows::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.ENDER_EYE);

	private final double mPercentDamageDealt;
	private final double mActivationRange;
	private final int mRecastTimer;
	private final Team mColorTeam;

	private int mEnhancementKillTick = -999;
	private int mEnhancementChain;
	private boolean mCanRecast = false;

	private final AdvancingShadowsCS mCosmetic;

	public AdvancingShadows(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentDamageDealt = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE) + (isLevelOne() ? DAMAGE_BONUS_1 : DAMAGE_BONUS_2);
		mActivationRange = CharmManager.calculateFlatAndPercentValue(player, CHARM_RANGE, (isLevelOne() ? ADVANCING_SHADOWS_RANGE_1 : ADVANCING_SHADOWS_RANGE_2));
		mRecastTimer = CharmManager.getDuration(player, CHARM_ENHANCE_TIMER, ENHANCEMENT_KILL_REQUIREMENT_TIME);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new AdvancingShadowsCS());
		mColorTeam = ScoreboardUtils.getExistingTeamOrCreate("advancingShadowsColor", NamedTextColor.BLACK);
		mEnhancementChain = 0;
	}

	public boolean cast() {
		// Enhancement: If mCanRecast is true (which shows that targeted mob died in 1s), allow recast of AS for next 3 seconds.
		if (isOnCooldown() && !(isEnhanced() && mCanRecast && mEnhancementKillTick + ENHANCEMENT_CHAIN_DURATION >= Bukkit.getCurrentTick())) {
			return false;
		}

		LivingEntity entity = EntityUtils.getHostileEntityAtCursor(mPlayer, mActivationRange,
			(Entity e) -> mPlayer.getLocation().getDirection().dot(e.getLocation().subtract(mPlayer.getLocation()).toVector()) > 0);

		if (entity == null) {
			return false;
		}

		if (isEnhanced() && (mEnhancementKillTick + ENHANCEMENT_CHAIN_DURATION < Bukkit.getCurrentTick())) {
			// Lose Kill chain if last kill tick was over 80 ticks ago.
			mEnhancementChain = 0;
		}

		mCanRecast = false;

		double origDistance = mPlayer.getLocation().distance(entity.getLocation());
		if (origDistance > mActivationRange) {
			return false;
		}
		Vector dir = LocationUtils.getDirectionTo(entity.getLocation(), mPlayer.getLocation());
		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		int i = 0;
		while (loc.distance(entity.getLocation()) > ADVANCING_SHADOWS_OFFSET) {
			i++;
			loc.add(dir.clone().multiply(0.3333));
			mCosmetic.tpTrail(mPlayer, loc, i);
			if (loc.distance(entity.getLocation()) < ADVANCING_SHADOWS_OFFSET) {
				double multiplier = ADVANCING_SHADOWS_OFFSET - loc.distance(entity.getLocation());
				loc.subtract(dir.clone().multiply(multiplier));
				break;
			}
		}
		loc.add(0, 1, 0);

		// Just in case the player's teleportation loc is in a block.
		int count = 0;
		while (count < 5 && (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid())) {
			count++;
			loc.subtract(dir.clone().multiply(1.15));
		}

		// If still solid, something is wrong.
		if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid()) {
			mCosmetic.tpSoundFail(world, mPlayer);
			return false;
		}

		// Prevent the player from teleporting over void
		if (loc.getY() < 8) {
			boolean safe = false;
			for (int y = 0; y < loc.getY() - 1; y++) {
				Location tempLoc = loc.clone();
				tempLoc.setY(y);
				if (!tempLoc.isChunkLoaded()) {
					continue;
				}
				if (!tempLoc.getBlock().isPassable()) {
					safe = true;
					break;
				}
			}

			// Maybe void - not worth it
			if (!safe) {
				mCosmetic.tpSoundFail(world, mPlayer);
				return false;
			}

			// Don't teleport players below y = 1.1 to avoid clipping into oblivion
			loc.setY(Math.max(1.1, loc.getY()));
		}

		// Extra safeguard to prevent bizarro teleports
		if (mPlayer.getLocation().distance(loc) > mActivationRange) {
			mCosmetic.tpSoundFail(world, mPlayer);
			return false;
		}

		if (loc.distance(entity.getLocation()) <= origDistance && !ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES) && !ZoneUtils.hasZoneProperty(mPlayer.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			mPlayer.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
		}

		if (mEnhancementChain == 0) {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(DURATION, mPercentDamageDealt, AFFECTED_DAMAGE_TYPES));
		} else {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(DURATION, mPercentDamageDealt / 2.0, AFFECTED_DAMAGE_TYPES));
		}
		if (isLevelTwo()) {
			for (LivingEntity mob : EntityUtils.getNearbyMobs(entity.getLocation(),
				ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE, mPlayer)) {
				if (mob != entity) {
					MovementUtils.knockAway(entity, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED), true);
				}
			}
		}

		if (isEnhanced()) {
			// Create a Timer which checks every tick for the next second if Advancing Shadows is still up.
			if (Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(entity.getUniqueId().toString()) == null) {
				mColorTeam.addEntry(entity.getUniqueId().toString());
			}
			entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, mRecastTimer, 0));
			cancelOnDeath(new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					if (mT > mRecastTimer) {
						mEnhancementChain = 0;
						// Revert glowing color to normal white
						if (mColorTeam.hasEntry(entity.getUniqueId().toString())) {
							mColorTeam.removeEntry(entity.getUniqueId().toString());
						}

						cancel();
						return;
					} else if (entity.isDead() || !entity.isValid()) {
						mCosmetic.tpChain(world, mPlayer);

						mCanRecast = true;
						mEnhancementKillTick = Bukkit.getCurrentTick();
						mEnhancementChain++;

						MessagingUtils.sendActionBarMessage(mPlayer, "Advancing Shadows Chain: " + mEnhancementChain);
						cancel();
						return;
					}

					mT++;
				}
			}.runTaskTimer(mPlugin, 0, 1));
		}

		mCosmetic.tpParticle(mPlayer, entity);
		mCosmetic.tpSound(world, mPlayer);

		if (mEnhancementChain == 0) {
			putOnCooldown();
		}
		return true;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		ClassAbility classAbility = INFO.getLinkedSpell();
		int remainingCooldown = classAbility == null ? 0 : mPlugin.mTimers.getCooldown(mPlayer.getUniqueId(), classAbility);
		TextColor color = INFO.getActionBarColor();
		String name = INFO.getHotbarName();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		if (isEnhanced() && mCanRecast && mEnhancementKillTick + ENHANCEMENT_CHAIN_DURATION >= Bukkit.getCurrentTick()) {
			output = output.append(Component.text("✓", NamedTextColor.GOLD, TextDecoration.BOLD));
		} else if (remainingCooldown > 0) {
			output = output.append(Component.text(((int) Math.ceil(remainingCooldown / 20.0)) + "s", NamedTextColor.GRAY));
		} else {
			output = output.append(Component.text("✓", NamedTextColor.GREEN, TextDecoration.BOLD));
		}
		return output;
	}
}
