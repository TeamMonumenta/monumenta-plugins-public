package com.playmonumenta.plugins.depths.guis.gifts;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.guis.DepthsGUICommands;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import com.playmonumenta.plugins.guis.Gui;
import java.util.List;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDepthsSelectionGUI<T> extends Gui {
	private static final List<List<Integer>> SLOTS_MAP = List.of(
		List.of(13),
		List.of(11, 15),
		List.of(10, 13, 16),
		List.of(10, 12, 14, 16),
		List.of(9, 11, 13, 15, 17),
		List.of(10, 11, 12, 14, 15, 16),
		List.of(10, 11, 12, 13, 14, 15, 16),
		List.of(9, 10, 11, 12, 14, 15, 16, 17)
	);

	protected final @Nullable DepthsPlayer mDepthsPlayer;
	protected final String mGiftAbility;
	protected final List<T> mSelections;
	protected final Function<T, ItemStack> mItemMapper;
	protected final boolean mShouldReturn;

	public AbstractDepthsSelectionGUI(Player player, String title, String giftAbility, List<T> selections, Function<T, ItemStack> itemMapper, boolean shouldReturn) {
		super(player, 27, title);
		mDepthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
		mGiftAbility = giftAbility;
		mSelections = selections;
		mItemMapper = itemMapper;
		mShouldReturn = shouldReturn;
	}

	@Override
	protected void setup() {
		if (mDepthsPlayer == null) {
			return;
		}
		if (mSelections.isEmpty() || mSelections.size() > 8) {
			// abort if selections are invalid, this should never happen
			mDepthsPlayer.sendMessage("You do not meet the requirements for " + mGiftAbility + ", removing gift...");
			if (DepthsRoomType.DepthsRewardType.GIFT_REWARDS.contains(mDepthsPlayer.mEarnedRewards.peek())) {
				mDepthsPlayer.mEarnedRewards.poll();
			}
			DepthsManager.getInstance().setPlayerLevelInAbility(mGiftAbility, mPlayer, mDepthsPlayer, 0, false, false);
			return;
		}
		int n = mSelections.size();
		List<Integer> slots = SLOTS_MAP.get(n - 1);
		for (int i = 0; i < n; i++) {
			T selection = mSelections.get(i);
			setItem(slots.get(i), mItemMapper.apply(selection)).onLeftClick(() -> {
				close();
				selected(selection);
				// if not chaining guis
				if (mShouldReturn) {
					// should always be a gift reward but just in case
					if (DepthsRoomType.DepthsRewardType.GIFT_REWARDS.contains(mDepthsPlayer.mEarnedRewards.peek())) {
						mDepthsPlayer.mEarnedRewards.poll();
					}
					if (!mDepthsPlayer.mEarnedRewards.isEmpty()) {
						DepthsManager.getInstance().getRoomReward(mPlayer, null, true);
					} else {
						DepthsGUICommands.summary(mPlayer);
					}
				}
			});
		}
	}

	protected abstract void selected(T selection);
}
