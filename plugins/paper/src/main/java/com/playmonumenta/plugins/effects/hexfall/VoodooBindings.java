package com.playmonumenta.plugins.effects.hexfall;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class VoodooBindings extends Effect {

	// White = No Requirements
	// Green = Solo
	// Yellow = Partners
	// Red = Party Stack
	public static final String GENERIC_NAME = "VoodooBindings";
	public static final String effectId = "voodooBindings";
	private final BossBar mBossBar;
	private @Nullable TextDisplay mDisplay;
	public final Queue<VoodooBinding> mBindings;

	public enum VoodooBinding {
		WHITE_CIRCLE(0, Component.text("⦿", NamedTextColor.WHITE), Component.text("⦿, conform.", Style.style(NamedTextColor.WHITE, TextDecoration.BOLD)), "white", "WC"),
		WHITE_DONUT(0, Component.text("⦾", NamedTextColor.WHITE), Component.text("⦾, conform.", Style.style(NamedTextColor.WHITE, TextDecoration.BOLD)), "white", "WD"),
		GREEN_CIRCLE(1, Component.text("⦿", NamedTextColor.GREEN), Component.text("⦿, in solitude.", Style.style(NamedTextColor.GREEN, TextDecoration.BOLD)), "green", "GC"),
		GREEN_DONUT(1, Component.text("⦾", NamedTextColor.GREEN), Component.text("⦾, in solitude.", Style.style(NamedTextColor.GREEN, TextDecoration.BOLD)), "green", "GD"),
		YELLOW_CIRCLE(2, Component.text("⦿", NamedTextColor.YELLOW), Component.text("⦿, in tandem.", Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)), "yellow", "YC"),
		YELLOW_DONUT(2, Component.text("⦾", NamedTextColor.YELLOW), Component.text("⦾, in tandem.", Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)), "yellow", "YD"),
		RED_CIRCLE(4, Component.text("⦿", NamedTextColor.RED), Component.text("⦿, unified.", Style.style(NamedTextColor.RED, TextDecoration.BOLD)), "red", "RC"),
		RED_DONUT(4, Component.text("⦾", NamedTextColor.RED), Component.text("⦾, unified.", Style.style(NamedTextColor.RED, TextDecoration.BOLD)), "red", "RD");

		private final int mPlayerCount;
		private final Component mColoredDot;
		private final Component mDirective;
		private final String mScoreboardTeam;
		private final String mStringDescriptor;

		VoodooBinding(int playerCount, Component coloredDot, Component chatMessage, String scoreboardTeam, String stringDescriptor) {
			mPlayerCount = playerCount;
			mColoredDot = coloredDot;
			mDirective = chatMessage;
			mScoreboardTeam = scoreboardTeam;
			mStringDescriptor = stringDescriptor;
		}

		public int playerCount() {
			return mPlayerCount;
		}

		public Component toColoredDot() {
			return mColoredDot;
		}

		public Component toDirective() {
			return mDirective;
		}

		public String glowingTeamName() {
			return mScoreboardTeam;
		}

		@Override
		public String toString() {
			return mStringDescriptor;
		}

		public static @Nullable VoodooBinding stringToEnum(String string) {
			return switch (string) {
				case "WC" -> WHITE_CIRCLE;
				case "WD" -> WHITE_DONUT;
				case "GC" -> GREEN_CIRCLE;
				case "GD" -> GREEN_DONUT;
				case "YC" -> YELLOW_CIRCLE;
				case "YD" -> YELLOW_DONUT;
				case "RC" -> RED_CIRCLE;
				case "RD" -> RED_DONUT;
				default -> null;
			};
		}
	}

	public VoodooBindings(int duration) {
		this(duration, new ArrayBlockingQueue<>(10));
	}

	public VoodooBindings(int duration, Queue<VoodooBinding> bindings) {
		super(duration, effectId);
		mBossBar = BossBar.bossBar(Component.text(""), 0, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
		mBindings = bindings;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (mBindings.isEmpty()) {
			this.clearEffect();
		}

		VoodooBinding binding = mBindings.peek();

		if (!(entity instanceof Player player) || binding == null) {
			return;
		}

		ScoreboardUtils.getExistingTeamOrCreate("red").removePlayer(player);
		ScoreboardUtils.getExistingTeamOrCreate("yellow").removePlayer(player);
		ScoreboardUtils.getExistingTeamOrCreate("green").removePlayer(player);
		ScoreboardUtils.getExistingTeamOrCreate("white").removePlayer(player);

		ScoreboardUtils.getExistingTeamOrCreate(binding.glowingTeamName()).addPlayer(player);
		entity.setGlowing(true);

		Component title = Component.empty();
		for (VoodooBinding voodooBinding : mBindings) {
			title = title.append(voodooBinding.toColoredDot());
		}

		mBossBar.name(Component.text("-== ").append(title).append(Component.text(" ==-")));

		if (mDisplay != null) {
			mDisplay.text(Component.text("Voodoo Bindings\n", Style.style(NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
				.append(Component.text("-== ", NamedTextColor.WHITE))
				.append(title)
				.append(Component.text(" ==-", NamedTextColor.WHITE)));
			mDisplay.setTransformation(new Transformation(new Vector3f(0, 1.5f, 0), new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f()));
			mDisplay.setInterpolationDelay(-1);
			mDisplay.setInterpolationDuration(-1);
		}
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.showBossBar(mBossBar);
			player.sendMessage(Component.text("A vile incantation commands you to follow the will of Hycenea.", NamedTextColor.GRAY, TextDecoration.ITALIC));
			player.playSound(entity.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1f, 1f);
			mDisplay = player.getWorld().spawn(player.getLocation().add(0, 3, 0), TextDisplay.class);
			mDisplay.setBillboard(Display.Billboard.CENTER);
			mDisplay.addScoreboardTag("HexfallDisplay");
			player.addPassenger(mDisplay);
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		if (event.isCancelled() || !(event.getEntity() instanceof Player player)) {
			return;
		}
		ScoreboardUtils.getExistingTeamOrCreate("white").removePlayer(player);
		ScoreboardUtils.getExistingTeamOrCreate("red").removePlayer(player);
		ScoreboardUtils.getExistingTeamOrCreate("yellow").removePlayer(player);
		ScoreboardUtils.getExistingTeamOrCreate("green").removePlayer(player);
		player.setGlowing(false);
		player.getWorld().hideBossBar(mBossBar);
		if (mDisplay != null) {
			mDisplay.remove();
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (!(entity instanceof Player player)) {
			return;
		}
		ScoreboardUtils.getExistingTeamOrCreate("white").removePlayer(player);
		ScoreboardUtils.getExistingTeamOrCreate("red").removePlayer(player);
		ScoreboardUtils.getExistingTeamOrCreate("yellow").removePlayer(player);
		ScoreboardUtils.getExistingTeamOrCreate("green").removePlayer(player);
		entity.setGlowing(false);
		player.getWorld().hideBossBar(mBossBar);
		if (mDisplay != null) {
			mDisplay.remove();
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("duration", getDuration());
		StringBuilder builder = new StringBuilder();
		for (VoodooBinding binding : mBindings) {
			builder.append(binding.toString());
		}
		jsonObject.addProperty("bindings", builder.toString());
		return jsonObject;
	}

	public static VoodooBindings deserialize(JsonObject object) {
		int duration = object.get("duration").getAsInt();
		String bindings = object.get("bindings").getAsString();
		Queue<VoodooBinding> voodooBindingsQueue = new ArrayBlockingQueue<>(10);

		for (int i = 0; i < bindings.length() - 1; i += 2) {
			voodooBindingsQueue.add(VoodooBinding.stringToEnum(bindings.substring(i, i + 2)));
		}

		return new VoodooBindings(duration, voodooBindingsQueue);
	}

	@Override
	public boolean shouldDeleteOnLogout() {
		return true;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text("Voodoo Bindings", NamedTextColor.RED);
	}

	@Override
	public String toString() {
		return String.format(GENERIC_NAME + ":%d", this.getDuration());
	}

	public VoodooBinding getCurrentBinding() {
		return mBindings.peek();
	}

	public void popCurrentBinding() {
		mBindings.poll();
	}
}
