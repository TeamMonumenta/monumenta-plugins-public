package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class TuathanBlessing extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "TuathanBlessing";
	public static final String effectID = "TuathanBlessing";

	public static int RADIUS = 12;
	public static double AMOUNT = 0.05;
	public static String DEFENSE_EFFECT = "TuathanBlessingDefense";
	public static String SPEED_EFFECT = "TuathanBlessingSpeed";

	public TuathanBlessing(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && entity instanceof Player player) {
			List<Player> otherPlayers = PlayerUtils.otherPlayersInRange(player, RADIUS, false);
			for (Player p : otherPlayers) {
				Plugin.getInstance().mEffectManager.addEffect(p, DEFENSE_EFFECT, new PercentDamageReceived(25, -1 * AMOUNT).displaysTime(false));
			}
			if (!otherPlayers.isEmpty()) {
				Plugin.getInstance().mEffectManager.addEffect(entity, SPEED_EFFECT, new PercentSpeed(25, AMOUNT, SPEED_EFFECT).displaysTime(false));
			}
		}
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	public static TuathanBlessing deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new TuathanBlessing(duration);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return "Tuathan Blessing";
	}

	@Override
	public String toString() {
		return String.format("TuathanBlessing duration:%d", this.getDuration());
	}

}
