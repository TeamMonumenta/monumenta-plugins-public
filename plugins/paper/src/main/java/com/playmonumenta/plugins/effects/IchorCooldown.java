package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class IchorCooldown extends Effect {
	public static final String effectID = "IchorCooldown";
	private final String mIchorType;

	public IchorCooldown(int duration, String ichorType) {
		super(duration, effectID);
		mIchorType = ichorType;
	}

	public String getIchorType() {
		return mIchorType;
	}

	public static IchorCooldown deserialize(JsonObject object) {
		int duration = object.get("duration").getAsInt();
		String ichorType = object.get("ichorType").getAsString();

		return new IchorCooldown(duration, ichorType);
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.sendActionBar(Component.text(mIchorType + " is now off cooldown!", NamedTextColor.YELLOW));
		}
	}

	@Override
	public String toString() {
		return String.format("Ichor duration:%d", this.getDuration());
	}
}
