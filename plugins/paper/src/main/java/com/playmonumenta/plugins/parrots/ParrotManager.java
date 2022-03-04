package com.playmonumenta.plugins.parrots;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ParrotManager implements Listener {

	protected static final String PARROT_TAG = "ParrotPet";

	public enum ParrotVariant {
		//minecraft default
		RED("Scarlet Macaw", 1, Parrot.Variant.RED),
		BLUE("Hyacinth Macaw", 2, Parrot.Variant.BLUE),
		CYAN("Blue-Yellow Macaw", 3, Parrot.Variant.CYAN),
		GREEN("Green Parakeet", 4, Parrot.Variant.GREEN),
		GRAY("Gray Cockatiel", 5, Parrot.Variant.GRAY),

		//added with texture
		PATREON("Patreon Parakeet", 6, Parrot.Variant.RED),
		PULSATING_GOLD("Golden Conure", 7, Parrot.Variant.CYAN),
		PULSATING_EMERALD("Emerald Conure", 8, Parrot.Variant.GREEN),
		PIRATE("Scoundrel Macaw", 9, Parrot.Variant.BLUE),
		KAUL("Blackroot Kakapo", 10, Parrot.Variant.GREEN),
		ELDRASK("Permafrost Kea", 11, Parrot.Variant.CYAN),
		RAINBOW("Rainbow Parrot", 12, Parrot.Variant.CYAN),
		SNOWY("Snowy Cockatoo", 13, Parrot.Variant.GRAY),
		DEPTHS("Otherworldly Myiopsitta", 14, Parrot.Variant.RED),
		DEPTHS_UPGRADE("Otherworldly Myiopsitta (u)", 15, Parrot.Variant.BLUE),
		BEE("Bee Conure", 16, Parrot.Variant.CYAN),
		RADIANT("Radiant Conure", 17, Parrot.Variant.CYAN),
		HEKAWT("Veil Electus", 18, Parrot.Variant.BLUE);


		private String mName;
		private int mNumber;
		private Parrot.Variant mVariant;

		ParrotVariant(String name, int num, Parrot.Variant variant) {
			this.mName = name;
			this.mNumber = num;
			this.mVariant = variant;
		}

		public void setNumber(int num) {
			this.mNumber = num;
		}

		public int getNumber() {
			return this.mNumber;
		}

		public void setName(String name) {
			this.mName = name;
		}

		public String getName() {
			return this.mName;
		}

		public Parrot.Variant getVariant() {
			return this.mVariant;
		}

		public void setVariant(Parrot.Variant variant) {
			this.mVariant = variant;
		}

		public static @Nullable ParrotVariant getVariantByNumber(int number) {
			if (number == 0) {
				return null;
			}
			for (ParrotVariant variant : values()) {
				if (variant.mNumber == number) {
					return variant;
				}
			}
			return null;
		}
	}

	public enum PlayerShoulder {
		LEFT,
		RIGHT,
		NONE;
	}


	private static @Nullable Plugin mPlugin;

	private static final String SCOREBOARD_PARROT_VISIBLE = "ParrotVisible";
	// 0 if invisible, 1 if visible.

	private static final String SCOREBOARD_PARROT_BOTH = "ParrotBoth";
	// 0 if player can hold only one parrot

	private static final String SCOREBOARD_PARROT_LEFT = "ParrotLeft";
	// store the info about which parrot is on the left shoulder

	private static final String SCOREBOARD_PARROT_RIGHT = "ParrotRight";
	// store the info about which parrot is on the right shoulder

	private static final Set<Player> mPrideRight = new HashSet<>();
	private static final Set<Player> mPrideLeft = new HashSet<>();

	private static final Map<Player, ParrotVariant> mLeftShoulders = new HashMap<>();
	private static final Map<Player, ParrotVariant> mRightShoulders = new HashMap<>();

	private static final int PRIDE_FREQUENCY = 3;

	private static @Nullable BukkitRunnable mPrideRunnable;

	public ParrotManager(Plugin plugin) {
		mPlugin = plugin;
		mPrideRunnable = null;

		// Periodically updates all players' parrots.
		// Workaround for an Optifine bug that only shows custom parrot textures if the parrot has been a standalone entity before it was put on a shoulder.
		// Updates only a few players at a time to spread out server load, as this causes noticeable lag when done for many players at once.
		new BukkitRunnable() {
			Iterator<? extends Player> mPlayers = Collections.emptyIterator();

			@Override
			public void run() {
				if (!mPlayers.hasNext()) {
					mPlayers = ImmutableList.copyOf(Bukkit.getOnlinePlayers()).iterator();
				}
				for (int i = 0; i < 10 && mPlayers.hasNext(); i++) {
					Player player = mPlayers.next();
					// Flying players lose parrots almost instantly, causing flickering, so don't update parrots for them. They'll get their parrots back once they land.
					if (player.isValid() && !player.isFlying() && !PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
						respawnParrots(player);
					}
				}
			}
		}.runTaskTimer(plugin, 10 * 20L, 3 * 20L); // low priority task, so can start after a long delay
	}

	private static void respawnParrots(Player player) {
		ParrotVariant leftParrot = mLeftShoulders.get(player);
		if (leftParrot != null) {
			ParrotPet pp = new ParrotPet(leftParrot, player);
			player.setShoulderEntityLeft(pp.spawnParrot());
		}

		ParrotVariant rightParrot = mRightShoulders.get(player);
		if (rightParrot != null) {
			ParrotPet pp = new ParrotPet(rightParrot, player);
			player.setShoulderEntityRight(pp.spawnParrot());
		}
	}

	public static void updateParrots(Player player) {

		boolean visible = areParrotsVisible(player);
		int leftShoulderParrot = visible ? ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_LEFT).orElse(0) : 0;
		int rightShoulderParrot = visible ? ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_RIGHT).orElse(0) : 0;
		ParrotVariant leftVariant = ParrotVariant.getVariantByNumber(leftShoulderParrot);
		ParrotVariant rightVariant = ParrotVariant.getVariantByNumber(rightShoulderParrot);

		if (!hasDoubleShoulders(player) && leftShoulderParrot != 0 && rightShoulderParrot != 0) { // player somehow has two parrots but can only have one - remove one
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_RIGHT, 0);
			rightVariant = null;
		}

		if (leftVariant != null) {
			mLeftShoulders.put(player, leftVariant);
			if (leftVariant == ParrotVariant.RAINBOW) {
				mPrideLeft.add(player);
			} else {
				mPrideLeft.remove(player);
			}
		} else {
			mLeftShoulders.remove(player);
			mPrideLeft.remove(player);
		}
		if (rightVariant != null) {
			mRightShoulders.put(player, rightVariant);
			if (rightVariant == ParrotVariant.RAINBOW) {
				mPrideRight.add(player);
			} else {
				mPrideRight.remove(player);
			}
		} else {
			mRightShoulders.remove(player);
			mPrideRight.remove(player);
		}

		player.setShoulderEntityLeft(null);
		player.setShoulderEntityRight(null);
		respawnParrots(player);

		if (mPrideRunnable == null && (!mPrideLeft.isEmpty() || !mPrideRight.isEmpty())) {
			// no task is running so create a new one
			mPrideRunnable = new BukkitRunnable() {
				static final Parrot.Variant[] VARIANTS = Parrot.Variant.values();
				int mVariant = 0;

				@Override
				public void run() {
					if (mPrideLeft.isEmpty() && mPrideRight.isEmpty()) {
						this.cancel();
					}

					if (this.isCancelled()) {
						mPrideLeft.clear();
						mPrideRight.clear();
						mPrideRunnable = null;
						return;
					}

					mVariant = (mVariant + 1) % VARIANTS.length;
					Parrot.Variant variant = VARIANTS[mVariant];
					for (Player player : mPrideRight) {
						if (player.getShoulderEntityRight() instanceof Parrot parrot) {
							parrot.setVariant(variant);
							player.setShoulderEntityRight(parrot);
						}
					}

					for (Player player : mPrideLeft) {
						if (player.getShoulderEntityLeft() instanceof Parrot parrot) {
							parrot.setVariant(variant);
							player.setShoulderEntityLeft(parrot);
						}
					}
				}
			};
			mPrideRunnable.runTaskTimer(mPlugin, 0, PRIDE_FREQUENCY);
		}
	}

	public static void setParrotVisible(Player player, boolean visible) {
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE, visible ? 1 : 0);
		updateParrots(player);
	}

	public static void clearParrots(Player player) {
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE, 0);
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_LEFT, 0);
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_RIGHT, 0);
		mPrideLeft.remove(player);
		mPrideRight.remove(player);
		mLeftShoulders.remove(player);
		mRightShoulders.remove(player);
		updateParrots(player);
	}

	public static void selectParrot(Player player, ParrotVariant variant, PlayerShoulder shoulder) {
		ScoreboardUtils.setScoreboardValue(player, shoulder == PlayerShoulder.LEFT ? SCOREBOARD_PARROT_LEFT : SCOREBOARD_PARROT_RIGHT, variant.mNumber);
		if (!hasDoubleShoulders(player)) {
			// if the player only has a single shoulder available, remove the parrot from the other shoulder (if applicable)
			ScoreboardUtils.setScoreboardValue(player, shoulder == PlayerShoulder.LEFT ? SCOREBOARD_PARROT_RIGHT : SCOREBOARD_PARROT_LEFT, 0);
		}
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE, 1);
		updateParrots(player);
	}

	public static boolean hasParrotOnShoulders(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_LEFT).orElse(0) != 0 || ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_RIGHT).orElse(0) != 0;
	}

	public static boolean areParrotsVisible(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE).orElse(0) != 0;
	}

	public static boolean hasDoubleShoulders(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_BOTH).orElse(0) > 0;
	}

	@EventHandler(ignoreCancelled = true)
	public void parrotSpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();

		if (entity instanceof Parrot parrot) {
			if (entity.getScoreboardTags().contains(PARROT_TAG)) {
				event.setCancelled(true);
				return;
			}

			// parrot spawned with an old version will not have the tag, so we need to check the name
			for (ParrotVariant variant : ParrotVariant.values()) {
				if (parrot.getCustomName() != null && parrot.getCustomName().contains(variant.mName)) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onCFlight(PlayerToggleFlightEvent event) {
		final Player player = event.getPlayer();
		if (areParrotsVisible(player) && !event.isFlying()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					updateParrots(player);
				}
			}.runTaskLater(mPlugin, 5L);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onJoinBed(PlayerBedEnterEvent event) {
		// we need to remove the parrots when someone sleeps in a bed
		final Player player = event.getPlayer();
		mPrideLeft.remove(player);
		mPrideRight.remove(player);
		mLeftShoulders.remove(player);
		mRightShoulders.remove(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onLeaveBed(PlayerBedLeaveEvent event) {
		final Player player = event.getPlayer();
		if (areParrotsVisible(player)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					updateParrots(player);
				}
			}.runTaskLater(mPlugin, 5L);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (areParrotsVisible(player)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					updateParrots(player);
				}
			}.runTaskLater(mPlugin, 5L);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		// remove the player from any sets and maps when quitting
		final Player player = event.getPlayer();
		mPrideLeft.remove(player);
		mPrideRight.remove(player);
		mLeftShoulders.remove(player);
		mRightShoulders.remove(player);
	}

}
