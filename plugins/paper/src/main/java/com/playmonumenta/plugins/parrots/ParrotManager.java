package com.playmonumenta.plugins.parrots;

import java.util.HashSet;
import java.util.Set;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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
	}

	public enum PlayerShoulder {
		LEFT,
		RIGHT,
		NONE;
	}


	private static Plugin mPlugin;

	private static final String SCOREBOARD_PARROT_VISIBLE = "ParrotVisible";
	// 0 if invisible, 1 if visible.

	private static final String SCOREBOARD_PARROT_BOTH = "ParrotBoth";
	// 0 if player can only hold only one parrot

	private static final String SCOREBOARD_PARROT_LEFT = "ParrotLeft";
	// store the info about which parrot is on the left shoulder

	private static final String SCOREBOARD_PARROT_RIGHT = "ParrotRight";
	// store the info about which parrot is on the right shoulder

	private static final Set<Player> mPrideRight = new HashSet<>();
	private static final Set<Player> mPrideLeft = new HashSet<>();

	private static final int FREQUENCY = 3;

	private static BukkitRunnable mRunnable;


	public ParrotManager(Plugin plugin) {
		mPlugin = plugin;
		mRunnable = null;
	}

	public static void removeParrot(Player p) {
		p.setShoulderEntityLeft(null);
		p.setShoulderEntityRight(null);
	}

	public static ParrotVariant getParrotVariantByName(String name) {
		for (ParrotVariant pv : ParrotVariant.values()) {
			if (pv.getName().equals(name)) {
				return pv;
			}
		}
		return null;
	}

	public static void updateParrot(Player p, ParrotVariant variant, PlayerShoulder shoulder) {
		updateParrot(p, variant.mNumber, shoulder);
	}

	public static void updateParrot(Player p, int variantNum, PlayerShoulder shoulder) {
		if (shoulder == PlayerShoulder.NONE) {
			p.sendMessage(Component.text("[Parrot Manager] shoulder == none. how do we get here? please contact a Mod.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
			return;
		}

		ParrotVariant variant;

		try {
			variant = ParrotVariant.values()[variantNum - 1];
		} catch (Exception ex) {
			mPlugin.getLogger().warning("[Parrot Manager] Catch an IndexOutOfBoundException. Probably coming from a different version of the plugin?");
			p.sendMessage(Component.text("[Parrot Manager] Error converting parrot. Please contact a Mod", NamedTextColor.RED).decoration(TextDecoration.BOLD, true).hoverEvent(Component.text(ex.getMessage())));
			return;
		}

		int leftShoulderParrot = ScoreboardUtils.getScoreboardValue(p, SCOREBOARD_PARROT_LEFT).orElse(0);
		int rightShoulderParrot = ScoreboardUtils.getScoreboardValue(p, SCOREBOARD_PARROT_RIGHT).orElse(0);

		int bothShoulder = ScoreboardUtils.getScoreboardValue(p, SCOREBOARD_PARROT_BOTH).orElse(0);

		ScoreboardUtils.setScoreboardValue(p, SCOREBOARD_PARROT_VISIBLE, 1);

		if (bothShoulder == 0) { // only one parrot!
			if (shoulder == PlayerShoulder.RIGHT && leftShoulderParrot != 0) {
				p.setShoulderEntityLeft(null);
				ScoreboardUtils.setScoreboardValue(p, SCOREBOARD_PARROT_LEFT, 0);
				mPrideLeft.remove(p);
			}

			if (shoulder == PlayerShoulder.LEFT && rightShoulderParrot != 0) {
				p.setShoulderEntityRight(null);
				ScoreboardUtils.setScoreboardValue(p, SCOREBOARD_PARROT_RIGHT, 0);
				mPrideRight.remove(p);
			}
		}

		ParrotPet pp = new ParrotPet(variant, p);

		if (shoulder == PlayerShoulder.LEFT) {
			mPrideLeft.remove(p);
		} else {
			mPrideRight.remove(p);
		}

		if (variantNum == 12) {
			//pride parrot.

			if (shoulder == PlayerShoulder.LEFT) {
				mPrideLeft.add(p);
			} else {
				mPrideRight.add(p);
			}

			if (mRunnable == null) {
				//no task is running so create a new one
				mRunnable = new BukkitRunnable() {
					Parrot.Variant[] mVariants = Parrot.Variant.values();
					int mRandom = FastUtils.RANDOM.nextInt(5);

					public void run() {
						if (mPrideLeft.isEmpty() && mPrideRight.isEmpty()) {
							this.cancel();
						}

						if (this.isCancelled()) {
							mPrideLeft.clear();
							mPrideRight.clear();
							mRunnable = null;
							return;
						}

						mRandom = (mRandom + 1) % mVariants.length;
						Parrot.Variant variant = mVariants[mRandom];
						for (Player player : mPrideRight) {
							Parrot parrot = (Parrot) player.getShoulderEntityRight();
							parrot.setVariant(variant);
							player.setShoulderEntityRight(parrot);
						}

						for (Player player : mPrideLeft) {
							Parrot parrot = (Parrot) player.getShoulderEntityLeft();
							parrot.setVariant(variant);
							player.setShoulderEntityLeft(parrot);
						}

					}
				};
				mRunnable.runTaskTimer(mPlugin, 0, FREQUENCY);
			}
		}

		if (shoulder == PlayerShoulder.LEFT) {
			p.setShoulderEntityLeft(null);
			ScoreboardUtils.setScoreboardValue(p, SCOREBOARD_PARROT_LEFT, variantNum);
			p.setShoulderEntityLeft(pp.getParrot());
		}
		if (shoulder == PlayerShoulder.RIGHT) {
			p.setShoulderEntityRight(null);
			ScoreboardUtils.setScoreboardValue(p, SCOREBOARD_PARROT_RIGHT, variantNum);
			p.setShoulderEntityRight(pp.getParrot());
		}
	}

	public static void setParrotVisible(Player player, boolean visible) {
		if (visible) {
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE, 1);
			int parrotLeft = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_LEFT).orElse(0);
			int parrotRight = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_RIGHT).orElse(0);
			if (parrotLeft != 0) {
				updateParrot(player, parrotLeft, PlayerShoulder.LEFT);
			}
			if (parrotRight != 0) {
				updateParrot(player, parrotRight, PlayerShoulder.RIGHT);
			}
		} else {
			mPrideLeft.remove(player);
			mPrideRight.remove(player);
			player.setShoulderEntityLeft(null);
			player.setShoulderEntityRight(null);
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE, 0);
		}
	}

	public static void clearParrots(Player player) {
		setParrotVisible(player, false);
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_LEFT, 0);
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_RIGHT, 0);
	}

	public static boolean hasParrotOnShoulders(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_LEFT).orElse(0) != 0 || ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_RIGHT).orElse(0) != 0;
	}



	public static void updateAllParrot(Player player) {
		int parrotRight = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_RIGHT).orElse(0);
		int parrotLeft = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_LEFT).orElse(0);

		if (parrotLeft != 0) {
			ParrotManager.updateParrot(player, parrotLeft, PlayerShoulder.LEFT);
		}

		if (parrotRight != 0) {
			ParrotManager.updateParrot(player, parrotRight, PlayerShoulder.RIGHT);
		}
	}

	public static void updateAllIfVisible(Player player) {
		if (ParrotManager.isParrotsVisible(player)) {
			updateAllParrot(player);
		}
	}

	public static boolean isParrotsVisible(Player p) {
		return ScoreboardUtils.getScoreboardValue(p, SCOREBOARD_PARROT_VISIBLE).orElse(0) != 0;
	}

	public static boolean hasDoubleShoulders(Player p) {
		return ScoreboardUtils.getScoreboardValue(p, SCOREBOARD_PARROT_BOTH).orElse(0) > 0;
	}

	@EventHandler
	public void parrotSpawnEvent(EntitySpawnEvent e) {
		Entity entity = e.getEntity();

		if (entity instanceof Parrot) {
			if (entity.getScoreboardTags().contains(PARROT_TAG)) {
				e.setCancelled(true);
				return;
			}

			//parrot spawned with an old version will not have the tag, so we need to check the name
			Parrot parrot = (Parrot) entity;
			for (ParrotVariant variant : ParrotVariant.values()) {
				if (parrot.getCustomName() != null && parrot.getCustomName().contains(variant.mName)) {
					e.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler
	public void onCFlight(PlayerToggleFlightEvent e) {
		final Player player = e.getPlayer();
		int visible = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE).orElse(0);
		if (visible != 0 && player.isFlying()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					updateAllParrot(player);
				}
			}.runTaskLater(mPlugin, 5L);
		}
	}

	@EventHandler
	public void onJoinBed(PlayerBedEnterEvent e) {
		//we need to remove the parrots when someone go to the bed
		final Player player = e.getPlayer();
		int visible = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE).orElse(0);
		if (visible != 0) {
			mPrideLeft.remove(player);
			mPrideRight.remove(player);
		}
	}


	@EventHandler
	public void onLeaveBed(PlayerBedLeaveEvent e) {
		final Player player = e.getPlayer();
		int visible = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE).orElse(0);
		if (visible != 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					updateAllIfVisible(player);
				}
			}.runTaskLater(mPlugin, 5L);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent e) {
		//we need to remove the player from the set when quitting
		final Player player = e.getPlayer();
		if (isParrotsVisible(player)) {
			mPrideLeft.remove(player);
			mPrideRight.remove(player);
		}
	}

	//this two methods are for fixing a bug with optifine that doesn't show the skins
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent e) {
		//this function is called each time a player change shard or connect to the server
		final Player player = e.getPlayer();
		new BukkitRunnable() {
			public void run() {
				updateAllIfVisible(player);
			}
		}.runTaskLater(mPlugin, 40L);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		final Player player = e.getPlayer();
		new BukkitRunnable() {
			public void run() {
				updateAllIfVisible(player);
			}
		}.runTaskLater(mPlugin, 20L);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onGamemodeChange(PlayerGameModeChangeEvent e) {
		final GameMode gameMode = e.getNewGameMode();
		final Player player = e.getPlayer();
		final GameMode oldGameMode = player.getGameMode();
		if ((gameMode != GameMode.SPECTATOR && gameMode != GameMode.CREATIVE) && (oldGameMode.equals(GameMode.CREATIVE) || oldGameMode.equals(GameMode.SPECTATOR))) {
			new BukkitRunnable() {
				public void run() {
					updateAllIfVisible(player);
				}
			}.runTaskLater(mPlugin, 20L);
		}
	}

}
