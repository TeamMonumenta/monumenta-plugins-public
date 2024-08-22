package com.playmonumenta.velocity.integrations;

import com.velocitypowered.api.proxy.Player;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.platform.PlayerAdapter;

public class LuckPermsIntegration {
	public static final LuckPerms mLP = LuckPermsProvider.get();
	public static final PlayerAdapter<Player> mLuckPlayerAdapter = mLP.getPlayerAdapter(Player.class);
}
