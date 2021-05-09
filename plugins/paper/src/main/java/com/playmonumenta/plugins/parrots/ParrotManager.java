package com.playmonumenta.plugins.parrots;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ParrotManager implements Listener {

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
		PIRATE("Scoundrel Macaw", 9, Parrot.Variant.BLUE);

		private String mName;
		private int mNumber;
		private Parrot.Variant mVariant;

		ParrotVariant() {

		}

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


	public static Plugin mPlugin;

	private static final String SCOREBOARD_PARROT_VISIBLE = "ParrotVisible";
	// 0 if invisible, 1 if visible.

	private static final String SCOREBOARD_PARROT_BOTH = "ParrotBoth";
	// 0 if player can only hold only one parrot

	private static final String SCOREBOARD_PARROT_LEFT = "ParrotLeft";
	// store the info about which parrot is on the left shoulder

	private static final String SCOREBOARD_PARROT_RIGHT = "ParrotRight";
	// store the info about which parrot is on the right shoulder

	public ParrotManager(Plugin plugin) {
		mPlugin = plugin;
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

	public static void updateParrot(Player p, ParrotVariant variant, String shoulder) {
		int numVariant = variant.getNumber();

		int leftShoulderParrot = ScoreboardUtils.getScoreboardValue(p, SCOREBOARD_PARROT_LEFT);
		int rightShoulderParrot = ScoreboardUtils.getScoreboardValue(p, SCOREBOARD_PARROT_RIGHT);

		int bothShoulder = ScoreboardUtils.getScoreboardValue(p, SCOREBOARD_PARROT_BOTH);

		ScoreboardUtils.setScoreboardValue(p, SCOREBOARD_PARROT_VISIBLE, 1);

		if (bothShoulder == 0) { // only one parrot!
			if (shoulder == "RIGHT" && leftShoulderParrot != 0) {
				p.setShoulderEntityLeft(null);
				ScoreboardUtils.setScoreboardValue(p, SCOREBOARD_PARROT_LEFT, 0);
			}

			if (shoulder == "LEFT" && rightShoulderParrot != 0) {
				p.setShoulderEntityRight(null);
				ScoreboardUtils.setScoreboardValue(p, SCOREBOARD_PARROT_RIGHT, 0);

			}
		}


		ParrotPet pp = new ParrotPet(variant, p, shoulder);

		if (shoulder.toUpperCase().contains("LEFT")) {
			p.setShoulderEntityLeft(null);
			ScoreboardUtils.setScoreboardValue(p, SCOREBOARD_PARROT_LEFT, numVariant);
			p.setShoulderEntityLeft(pp.getParrot());
		}
		if (shoulder.toUpperCase().contains("RIGHT")) {
			p.setShoulderEntityRight(null);
			ScoreboardUtils.setScoreboardValue(p, SCOREBOARD_PARROT_RIGHT, numVariant);
			p.setShoulderEntityRight(pp.getParrot());
		}
	}

	public static void setParrotVisible(Player player, boolean visible) {
		if (visible) {
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE, 1);
			int parrotLeft = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_LEFT);
			int parrotRight = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_RIGHT);
			if (parrotLeft != 0) {
				updateParrot(player, ParrotVariant.values()[parrotLeft - 1], "LEFT");
			}
			if (parrotRight != 0) {
				updateParrot(player, ParrotVariant.values()[parrotRight - 1], "RIGHT");
			}
		} else {
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE, 0);
			player.setShoulderEntityLeft(null);
			player.setShoulderEntityRight(null);
		}
	}

	public static void clearParrots(Player player) {
		setParrotVisible(player, false);
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_LEFT, 0);
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_RIGHT, 0);
	}

	public static boolean hasParrotOnShoulders(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_LEFT) != 0 || ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_RIGHT) != 0;
	}



	public static void updateAllParrot(Player player) {
		int parrotRight = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_RIGHT);
		int parrotLeft = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_LEFT);

		if (parrotLeft != 0) {
			ParrotManager.updateParrot(player, ParrotVariant.values()[parrotLeft - 1], "LEFT");
		}

		if (parrotRight != 0) {
			ParrotManager.updateParrot(player, ParrotVariant.values()[parrotRight - 1], "RIGHT");
		}
	}

	public static void updateAllIfVisible(Player player) {
		if (ParrotManager.isParrotsVisible(player)) {
			updateAllParrot(player);
		}
	}

	public static boolean isParrotsVisible(Player p) {
		return ScoreboardUtils.getScoreboardValue(p, SCOREBOARD_PARROT_VISIBLE) != 0;
	}

	public static boolean hasDoubleShoulders(Player p) {
		return ScoreboardUtils.getScoreboardValue(p, SCOREBOARD_PARROT_BOTH) > 0;
	}

	@EventHandler
	public void parrotSpawnEvent(EntitySpawnEvent e) {
		Entity entity = e.getEntity();

		if (entity.getType() == EntityType.PARROT) {
			for (ParrotVariant pv : ParrotVariant.values()) {
				if (pv.getName().equals(entity.getName())) {
					e.setCancelled(true);
					break;
				}
			}
		}
	}

	@EventHandler
	public void onCFlight(PlayerToggleFlightEvent e) {
		Player player = e.getPlayer();
		int visible = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE);
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
	public void onLeaveBed(PlayerBedLeaveEvent e) {
		Player player = e.getPlayer();
		int visible = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE);
		if (visible != 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					updateAllParrot(player);
				}
			}.runTaskLater(mPlugin, 5);
		}

	}


}
