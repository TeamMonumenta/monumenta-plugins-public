package pe.project.item.properties;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.utils.particlelib.ParticleEffect;

public class Hope implements ItemProperty {
	private static String PROPERTY_NAME = ChatColor.WHITE + "Hope";

	/* How much longer an item lasts per level */
	private static final int extraMinutesPerLevel = 5;
	private static final int tickPeriod = 6;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean hasOnSpawn() {
		return true;
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		item.setInvulnerable(true);

		new BukkitRunnable() {
			int count = 0;
			int resetCount = 0;

			@Override
			public void run() {
				count++;
				if ((resetCount < (level * extraMinutesPerLevel))
						&& ((count * tickPeriod) > Constants.TICKS_PER_MINUTE)) {
					resetCount++;
					count = 0;
					item.setTicksLived(1);
				}

				ParticleEffect.SPELL_INSTANT.display(0.2f, 0.2f, 0.2f, 0, 3, item.getLocation(), 40);
				if (item == null || item.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 10, tickPeriod);
	}
}
