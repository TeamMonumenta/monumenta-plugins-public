package com.playmonumenta.plugins.minigames;

import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.Hitbox;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class TestMinigame extends Minigame {
	public static final String ID = "Test";
	private static final double RADIUS = 15.0;
	private final Location mCenter;

	@SuppressWarnings("unused")
	// The test minigame isn't used anywhere
	public TestMinigame(Location center, Arguments arguments) {
		super(ID, new Hitbox.SphereHitbox(center, RADIUS));
		mCenter = center;
	}

	@Override
	public void startMinigame(@Nullable Player p) {
		/* For minigames where you want to hit multiple people, use alternative targeting methods (like this)
		and just null out the player argument, then select whoever you want in the command, it won't affect the players here.
		Note that this is pretty deranged. I don't expect anyone to use this to create multiplayer minigames,
		but the option is here for you, if you want.
		Then again, hopefully you're smart enough to rewrite this section of code in a better way. */
		mHitbox.getHitPlayers(true).forEach(player ->
			player.sendMessage("Test Minigame started!"));
	}

	@Override
	void tick(long tick) {
		if (tick % 20 == 0) {
			new PPCircle(Particle.END_ROD, mCenter, RADIUS)
				.countPerMeter(2)
				.spawnAsBoss();
		}
	}

	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		mHitbox.getHitPlayers(true).forEach(player ->
			player.sendMessage("This entity died: " + event.getEntity()));
	}

	@Override
	public void onEndMinigame() {
		mHitbox.getHitPlayers(true).forEach(player ->
			player.sendMessage("Test Minigame stopped! :c"));
	}
}
