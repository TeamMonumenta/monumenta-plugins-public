package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class DarkesterestPact extends Ability {

	private static final int COOLDOWN = 90 * 20;
	public static final String HAS_BECOME_PUFFERFISH_SCOREBOARD = "DarkesterestPactUsed";
	// I'm using a scoreboard to mark that the player has used the ability instead of a tag,
	// as a scoreboard can easily be deleted, while the tag stays on the player. The scoreboard
	// exists to prevent players from escaping their pufferfish while in spectator mode;
	// This could be done by logging out or changing shards, which the tag prevents.

	public static final AbilityInfo<DarkesterestPact> INFO =
		new AbilityInfo<>(DarkesterestPact.class, "Darkesterest Pact", DarkesterestPact::new)
			.linkedSpell(ClassAbility.DARKESTEREST_PACT)
			.scoreboardId("DarkesterestPact")
			.shorthandName("DP2")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("This pact is even darker than Darkest Pact.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DarkesterestPact::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true).onGround(false)))
			.displayItem(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);

	private @Nullable PufferFish mPufferFish = null;

	public DarkesterestPact(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)
			|| ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_SPECTATOR_ON_DEATH)
			|| ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_SPECTATOR_ON_RESPAWN)) {
			return false;
		}
		putOnCooldown();
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1f);
		mPufferFish = (PufferFish) LibraryOfSoulsIntegration.summon(loc, "DDDGhoulofDeathDDD");
		if (mPufferFish == null) {
			return false;
		}
		mPlayer.setGameMode(GameMode.SPECTATOR);
		mPlayer.setSpectatorTarget(mPufferFish);
		ScoreboardUtils.setScoreboardValue(mPlayer, HAS_BECOME_PUFFERFISH_SCOREBOARD, 1);
		new BukkitRunnable() {
			int mT = 0;
			final World mWorld = mPlayer.getWorld();

			@Override
			public void run() {
				mT++;
				if (mPufferFish != null && mPufferFish.isValid() && mPlayer.getGameMode() == GameMode.SPECTATOR && mPlayer.isOnline()) {
					mPlayer.setSpectatorTarget(mPufferFish);
				} else {
					ZoneUtils.setExpectedGameMode(mPlayer);
					if (mPufferFish != null && mPufferFish.isDead()) {
						mPlayer.damage(9999);
						mPufferFish.remove();
					}
					cancel();
				}
				if (mT % 30 == 0 && mPufferFish != null) {
					LivingEntity nearest = EntityUtils.getNearestMob(mPufferFish.getLocation(), 10, mPufferFish);
					if (nearest != null) {
						mPufferFish.setVelocity(LocationUtils.getDirectionTo(nearest.getEyeLocation(), mPufferFish.getLocation()).multiply(0.5));
					}
				}
				if (mT > 200 || !mPlayer.isOnline() || mPlayer.getWorld() != mWorld) {
					ZoneUtils.setExpectedGameMode(mPlayer);
					if (mPufferFish != null) {
						mPufferFish.remove();
					}
					cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
		return true;
	}

	private static Description<DarkesterestPact> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<DarkesterestPact> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<DarkesterestPact> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addTrigger()
			.addDashedLine()
			.addLine("Turn yourself into a massive frightening dark and")
			.addLine("creepy scary Ghoul of Death with %d Health which").statValues(stat(1500))
			.addLine("you control. All your abilities deal +%p damage").statValues(stat(3))
			.addLine("and apply debuffs at +%p extra potency while in").statValues(stat(0.3))
			.addLine("Massive Frightening Dark Creepy Scary Ghoul Mode.")
			.addLine("Any mob you look at is silenced for %t because it's").statValues(stat(200))
			.addLine("just so scary.")
			.addDashedLine();
	}
}
