package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.AdvancingShadowsCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

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
	private static final int ENHANCEMENT_CHAIN_DURATION = 20 * 3;

	private static final float[] PITCHES = {1.6f, 1.8f, 1.6f, 1.8f, 2f};

	public static final String CHARM_DAMAGE = "Advancing Shadows Damage Multiplier";
	public static final String CHARM_COOLDOWN = "Advancing Shadows Cooldown";
	public static final String CHARM_RANGE = "Advancing Shadows Range";
	public static final String CHARM_KNOCKBACK = "Advancing Shadows Knockback";

	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "AdvancingShadowsPercentDamageDealtEffect";
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(DamageType.MELEE, DamageType.MELEE_ENCH, DamageType.MELEE_SKILL);

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
				String.format("If the mob you teleported to dies within %ss, you can recast Advancing Shadows again in the next %ss.",
					ENHANCEMENT_KILL_REQUIREMENT_TIME / 20,
					ENHANCEMENT_CHAIN_DURATION / 20))
			.cooldown(ADVANCING_SHADOWS_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", AdvancingShadows::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(new ItemStack(Material.ENDER_EYE, 1));

	private final double mPercentDamageDealt;
	private final double mActivationRange;
	private final Team mColorTeam;

	private int mEnhancementKillTick = -999;
	private int mEnhancementChain = 0;
	private boolean mCanRecast = false;

	private final AdvancingShadowsCS mCosmetic;

	public AdvancingShadows(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentDamageDealt = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE) + (isLevelOne() ? DAMAGE_BONUS_1 : DAMAGE_BONUS_2);
		mActivationRange = CharmManager.calculateFlatAndPercentValue(player, CHARM_RANGE, (isLevelOne() ? ADVANCING_SHADOWS_RANGE_1 : ADVANCING_SHADOWS_RANGE_2));

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new AdvancingShadowsCS(), AdvancingShadowsCS.SKIN_LIST);
		mColorTeam = ScoreboardUtils.getExistingTeamOrCreate("advancingShadowsColor", NamedTextColor.BLACK);
	}

	public void cast() {
		// Enhancement: If mCanRecast is true (which shows that targeted mob died in 1s), allow recast of AS for next 3 seconds.
		if (isOnCooldown() && !(isEnhanced() && mCanRecast && mEnhancementKillTick + ENHANCEMENT_CHAIN_DURATION >= Bukkit.getCurrentTick())) {
			return;
		}

		if (isEnhanced() && (mEnhancementKillTick + ENHANCEMENT_CHAIN_DURATION < Bukkit.getCurrentTick())) {
			// Lose Kill chain if last kill tick was over 60 ticks ago.
			mEnhancementChain = 0;
		}

		mCanRecast = false;

		// Basically makes sure if the target is in LoS and if there is a path.
		Location eyeLoc = mPlayer.getEyeLocation();
		Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), (int) Math.ceil(mActivationRange));
		ray.mThroughBlocks = false;
		ray.mThroughNonOccluding = false;
		ray.mTargetPlayers = AbilityManager.getManager().isPvPEnabled(mPlayer);

		RaycastData data = ray.shootRaycast();

		LivingEntity entity = data.getEntities().stream()
			                      .filter(t -> t != mPlayer && t.isValid() && EntityUtils.isHostileMob(t))
			                      .findFirst()
			                      .orElse(null);
		if (entity == null) {
			return;
		}

		double maxRange = mActivationRange;
		double origDistance = mPlayer.getLocation().distance(entity.getLocation());
		if (origDistance <= maxRange) {
			Vector dir = LocationUtils.getDirectionTo(entity.getLocation(), mPlayer.getLocation());
			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();

			mCosmetic.tpStart(mPlayer);
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
				return;
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
					return;
				}

				// Don't teleport players below y = 1.1 to avoid clipping into oblivion
				loc.setY(Math.max(1.1, loc.getY()));
			}

			// Extra safeguard to prevent bizarro teleports
			if (mPlayer.getLocation().distance(loc) > maxRange) {
				mCosmetic.tpSoundFail(world, mPlayer);
				return;
			}

			mCosmetic.tpParticle(mPlayer);
			mCosmetic.tpSound(world, mPlayer);

			if (loc.distance(entity.getLocation()) <= origDistance) {
				mPlayer.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
			}

			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(DURATION, mPercentDamageDealt, AFFECTED_DAMAGE_TYPES));
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
				entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, ENHANCEMENT_KILL_REQUIREMENT_TIME, 0));
				new BukkitRunnable() {
					int mT = 0;

					@Override public void run() {
						if (mT > ENHANCEMENT_KILL_REQUIREMENT_TIME) {
							mEnhancementChain = 0;
							// Revert glowing color to normal white
							if (mColorTeam.hasEntry(entity.getUniqueId().toString())) {
								mColorTeam.removeEntry(entity.getUniqueId().toString());
							}

							cancel();
							return;
						} else if (entity.isDead() || !entity.isValid()) {
							for (int i = 0; i < PITCHES.length; i++) {
								float pitch = PITCHES[i];
								new BukkitRunnable() {
									@Override
									public void run() {
										world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_RESONATE, 1, pitch);
									}
								}.runTaskLater(mPlugin, i);
							}
							mCanRecast = true;
							mEnhancementKillTick = Bukkit.getCurrentTick();
							mEnhancementChain++;

							MessagingUtils.sendActionBarMessage(mPlayer, "Advancing Shadows Chain: " + mEnhancementChain);
							cancel();
							return;
						}

						mT++;
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}

			mCosmetic.tpParticle(mPlayer);
			mCosmetic.tpSound(world, mPlayer);

			putOnCooldown();
		}
	}

}
