package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.SoundArgument;
import java.util.Collection;
import java.util.HashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class TitleUtilsCommand {
	private static final HashMap<Player, ActiveScrollingTitle> RUNNABLE_MAP = new HashMap<>();

	private static final String COMMAND = "titleutils";
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.titleutils");

	public static class ActiveScrollingTitle {
		private static class TitleScroll {
			final Component mTitle;
			final int mTitleSpeed;
			@Nullable
			final Sound mSound;
			final float mVolume;
			final float mPitch;

			private final int mMaxIndex;
			private int mIndex = 0;

			private int mTicks = 0;
			private boolean mIsComplete = false;

			public TitleScroll(Component title, int speed, @Nullable Sound sound, float volume, float pitch) {
				mMaxIndex = MessagingUtils.componentLength(title);

				// To send a title if there is active scrolling
				if (speed == 0) {
					speed = 1;
					mIsComplete = true;
					mIndex = mMaxIndex;
				}

				mTitle = title;
				mTitleSpeed = speed;
				mSound = sound;
				mVolume = volume;
				mPitch = pitch;
			}

			public Component scroll(Player player) {
				int index = mIndex;

				if (mTicks % mTitleSpeed == 0) {
					index++;
				}

				if (!mIsComplete && mSound != null && index != mIndex) {
					player.playSound(player.getLocation(), mSound, SoundCategory.PLAYERS, mVolume, mPitch);
				}

				mIsComplete = index >= mMaxIndex;
				mIndex = index;
				mTicks++;
				return MessagingUtils.subComponent(mTitle, 0, Math.min(index, mMaxIndex));
			}

			public boolean isComplete() {
				return mIsComplete;
			}
		}

		final Player mPlayer;
		final BukkitRunnable mRunnable;
		final int mFade;
		@Nullable
		TitleScroll mScrollingTitle;
		@Nullable
		TitleScroll mScrollingSubtitle;

		int mStayTicks = 0;
		int mMaxStay = 0;
		boolean mStay = false;

		final World mWorld;

		public ActiveScrollingTitle(Player player, int fade) {
			mFade = fade;
			mPlayer = player;
			mWorld = player.getWorld();
			mRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (player.isDead()
						|| !player.isOnline()
						|| !mWorld.equals(player.getWorld())) {
						this.cancel();
						return;
					}

					Component scrolledTitle = mScrollingTitle == null ? Component.empty() : mScrollingTitle.scroll(player);
					Component scrolledSubtitle = mScrollingSubtitle == null ? Component.empty() : mScrollingSubtitle.scroll(player);

					MessagingUtils.sendTitle(player, scrolledTitle, scrolledSubtitle, 0, mMaxStay, mFade);

					if (!mStay) {
						if ((mScrollingTitle == null || mScrollingTitle.isComplete())
							&& (mScrollingSubtitle == null || mScrollingSubtitle.isComplete())) {
							mStay = true;
						}
					} else if (++mStayTicks >= mMaxStay) {
						this.cancel();
					}
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					RUNNABLE_MAP.remove(player);
				}
			};
		}

		public void createTitle(Component title, int titleSpeed, int stay, @Nullable Sound sound, float volume, float pitch) {
			restartStayTicks(stay);
			mScrollingTitle = new TitleScroll(title, titleSpeed, sound, volume, pitch);
		}

		public void createSubtitle(Component title, int titleSpeed, int stay, @Nullable Sound sound, float volume, float pitch) {
			restartStayTicks(stay);
			mScrollingSubtitle = new TitleScroll(title, titleSpeed, sound, volume, pitch);
		}

		private void restartStayTicks(int stay) {
			mStay = false;
			mMaxStay = stay;
			mStayTicks = 0;
		}

		public void run() {
			mRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withSubcommands(
				new CommandAPICommand("scroll")
					.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
					.withArguments(new MultiLiteralArgument("type", "title", "subtitle"))
					.withArguments(new IntegerArgument("ticksBetween"))
					.withArguments(new IntegerArgument("stay"))
					.withArguments(new IntegerArgument("fade"))
					.withArguments(new SoundArgument("sound"))
					.withArguments(new FloatArgument("volume"))
					.withArguments(new FloatArgument("pitch"))
					.withArguments(new GreedyStringArgument("titletext"))
					.executes((sender, args) -> {
						String type = args.getUnchecked("type");
						Integer timer = args.getUnchecked("ticksBetween");
						Collection<Player> players = args.getUnchecked("players");
						String text = args.getUnchecked("titletext");

						if (type == null || timer == null || players == null || text == null) {
							return;
						}
						boolean isTitle = type.equals("title");

						int stay = args.getOrDefaultUnchecked("stay", 20);
						int fade = args.getOrDefaultUnchecked("fade", 10);
						Sound sound = args.getUnchecked("sound");
						float volume = args.getOrDefaultUnchecked("volume", 1.0f);
						float pitch = args.getOrDefaultUnchecked("pitch", 1.0f);

						Component component = MessagingUtils.fromMiniMessage(text);

						createBukkitRunnable(players, isTitle, timer, component, stay, fade, sound, volume, pitch);
					})
			).register();
	}

	public static void createBukkitRunnable(Collection<Player> players, boolean isTitle, int ticks, Component component,
											int stay, int fade, @Nullable Sound sound, float volume, float pitch) {
		for (Player p : players) {
			ActiveScrollingTitle scroll = RUNNABLE_MAP.get(p);
			boolean hasActiveScrolling = true;

			if (scroll == null) {
				scroll = new ActiveScrollingTitle(p, fade);
				hasActiveScrolling = false;
			}

			if (isTitle) {
				scroll.createTitle(component, ticks, stay, sound, volume, pitch);
			} else {
				scroll.createSubtitle(component, ticks, stay, sound, volume, pitch);
			}

			if (!hasActiveScrolling) {
				RUNNABLE_MAP.put(p, scroll);
				scroll.run();
			}
		}
	}
}
