package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import static com.playmonumenta.plugins.Constants.Tags.REMOVE_ON_UNLOAD;

public class GuildDisplayBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_guild_display";
	private static final String scoreboardTag = "guild_display";
	private final List<Pair> mPairs;


	public static class Parameters extends BossParameters {
		@BossParam(help = "Y Offset")
		public double Y_OFFSET = 0;
		@BossParam(help = "X Offset")
		public double X_OFFSET = 0;
		@BossParam(help = "Z Offset")
		public double Z_OFFSET = 0;
		@BossParam(help = "Width")
		public double WIDTH = 0.0;
		@BossParam(help = "Height")
		public double HEIGHT = 0.0;
		@BossParam(help = "Rotation in Degrees")
		public double ROTATION = 0.0;
		@BossParam(help = "Ticks Before Reaching Full Size")
		public int SCALE_TICKS = 0;
	}

	private final List<Group> mAllGuilds = new ArrayList<>();
	private int mCurrentPos = 0;
	private static final int EXTRA = 5;

	public GuildDisplayBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mPairs = new ArrayList<>();
		Parameters p = BossParameters.getParameters(boss, identityTag, new GuildDisplayBoss.Parameters());
		int period = (int) (p.WIDTH * 20) + 2;
		for (Entity e : boss.getLocation().getWorld().getNearbyEntities(
			new BoundingBox(p.X_OFFSET + p.WIDTH + EXTRA, p.Y_OFFSET - EXTRA, p.Z_OFFSET + p.WIDTH + EXTRA,
				p.X_OFFSET - p.WIDTH - EXTRA, p.Y_OFFSET + p.HEIGHT + EXTRA, p.Z_OFFSET - p.WIDTH - EXTRA))) {
			if ((e instanceof ItemDisplay || e instanceof TextDisplay) && e.getScoreboardTags().contains(scoreboardTag)) {
				e.remove();
			}
		}
		new BukkitRunnable() {
			int mTicks = 0;
			int mLastHeight = 0;

			@Override
			public void run() {
				//cycle count
				if (mTicks % 50000 == 0) {
					updateGuilds();
				}
				if (mTicks % 100 == 0 && !mAllGuilds.isEmpty()) {
					for (int i = 0; i < p.HEIGHT / 3; i++) {
						if (mCurrentPos >= mAllGuilds.size()) {
							mCurrentPos = 0;
						}
						int pos = mCurrentPos;
						mCurrentPos++;
						//create a little randomized spacing to not make perfect columns
						int delay = FastUtils.randomIntInRange(0, 2 * 20);
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
							int height = mLastHeight + 3;
							if (height > p.HEIGHT) {
								height = 0;
							}
							mLastHeight = height;
							if (!mAllGuilds.isEmpty()) {
								if (!mBoss.isDead()) {
									//I HATE DISPLAY ENTITIES
									Location loc = mBoss.getLocation().clone().add(p.X_OFFSET, p.Y_OFFSET, p.Z_OFFSET);
									ItemDisplay banner = mBoss.getWorld().spawn(loc.clone().add((p.WIDTH / 2) * FastUtils.sinDeg(p.ROTATION), height, (p.WIDTH / 2) * FastUtils.cosDeg(p.ROTATION)), ItemDisplay.class);
									AxisAngle4f rotationForward = new AxisAngle4f((float) Math.toRadians(p.ROTATION - 90), 0, 1, 0);
									AxisAngle4f rotationReverse = new AxisAngle4f((float) Math.toRadians(p.ROTATION + 90), 0, 1, 0);
									banner.setTransformation(new Transformation(banner.getTransformation().getTranslation(), rotationReverse, new Vector3f(), new AxisAngle4f()));
									banner.setInterpolationDelay(0);
									banner.setInterpolationDuration(1);
									banner.addScoreboardTag(scoreboardTag);
									banner.addScoreboardTag(REMOVE_ON_UNLOAD);
									banner.setBrightness(new Display.Brightness(15, 15));
									banner.setItemStack(LuckPermsIntegration.getGuildBanner(mAllGuilds.get(pos)));
									TextDisplay text = mBoss.getWorld().spawn(loc.clone().add((p.WIDTH / 2) * FastUtils.sinDeg(p.ROTATION), 1.5 + height, (p.WIDTH / 2) * FastUtils.cosDeg(p.ROTATION)), TextDisplay.class);
									text.setAlignment(TextDisplay.TextAlignment.CENTER);
									text.text(LuckPermsIntegration.getGuildFullComponent(mAllGuilds.get(pos)));
									text.setTransformation(new Transformation(text.getTransformation().getTranslation(), rotationForward, new Vector3f(), new AxisAngle4f()));
									text.setInterpolationDelay(0);
									text.setInterpolationDuration(1);
									text.setBrightness(new Display.Brightness(15, 15));
									text.addScoreboardTag(scoreboardTag);
									text.addScoreboardTag(REMOVE_ON_UNLOAD);
									mPairs.add(new Pair(banner, text));
								}
							}
						}, delay);
					}
				}
				List<Pair> toRemove = new ArrayList<>();
				for (Pair pa : mPairs) {
					int result = pa.advance(-(p.WIDTH / period) * FastUtils.sinDeg(p.ROTATION),
						-(p.WIDTH / period) * FastUtils.cosDeg(p.ROTATION),
						period,
						p.SCALE_TICKS);
					if (result < 0) {
						toRemove.add(pa);
					}
				}
				mPairs.removeAll(toRemove);
				if (!mBoss.isValid()) {
					for (Pair pa : mPairs) {
						pa.delete();
					}
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	private static class Pair {
		private final ItemDisplay mBanner;
		private final TextDisplay mText;
		private int mTeleportCount;

		private Pair(ItemDisplay banner, TextDisplay text) {
			mBanner = banner;
			mText = text;
			mTeleportCount = 0;
		}

		private int advance(double x, double z, int period, int scaleTicks) {
			float size;
			if (scaleTicks <= 0) {
				size = 1.0f;
			} else {
				size = Integer.min(Integer.min(mTeleportCount, period - mTeleportCount), scaleTicks) / (float) scaleTicks;
			}
			Vector3f scale = new Vector3f(size, size, size);
			Transformation transformation;

			transformation = mBanner.getTransformation();
			mBanner.setTransformation(new Transformation(transformation.getTranslation(),
				transformation.getLeftRotation(),
				scale,
				transformation.getRightRotation()));
			mBanner.teleport(mBanner.getLocation().add(x, 0, z));

			transformation = mText.getTransformation();
			mText.setTransformation(new Transformation(transformation.getTranslation(),
				transformation.getLeftRotation(),
				scale,
				transformation.getRightRotation()));
			mText.teleport(mText.getLocation().add(x, 0, z));

			if (mTeleportCount > period) {
				delete();
				return -1;
			}
			mTeleportCount++;
			return 0;
		}

		private void delete() {
			if (mBanner.getLocation().isChunkLoaded()) {
				mBanner.remove();
			}
			if (mText.getLocation().isChunkLoaded()) {
				mText.remove();
			}
		}
	}

	private void updateGuilds() {
		Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {
			try {
				TreeMap<String, Group> sortMap = new TreeMap<>();
				for (Group guild : LuckPermsIntegration.getGuilds().join()) {
					String guildName = LuckPermsIntegration.getNonNullGuildName(guild);
					String sortKey = StringUtils.getNaturalSortKey(guildName);
					sortMap.put(sortKey, guild);
				}
				//set as sortKey seems to have duplicates
				Set<Group> guilds = new HashSet<>(sortMap.values());

				Bukkit.getScheduler().runTask(mPlugin, () -> {
					// Handle this list sync so that it can't be modified during reads
					mAllGuilds.clear();
					mAllGuilds.addAll(guilds);
					Collections.shuffle(mAllGuilds);
				});
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(mPlugin, () -> mPlugin.getLogger().log(Level.FINER, "An error occurred fetching all guilds:" + ex));
			}
		});
	}

	@Override
	public void unload() {
		// Would really like to remove the entities here, but removal isn't allowed during chunk unloading
		// Instead just clear the list, the entities have REMOVE_ON_UNLOAD tag so they'll be removed when the chunk loads next
		mPairs.clear();
	}
}
