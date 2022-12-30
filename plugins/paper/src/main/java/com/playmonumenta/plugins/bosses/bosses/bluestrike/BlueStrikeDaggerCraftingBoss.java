package com.playmonumenta.plugins.bosses.bosses.bluestrike;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.bluestrike.SpellCraftDaggerAnimation;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class BlueStrikeDaggerCraftingBoss extends BossAbilityGroup {

	private static final String COMMAND = "bluestrikedaggercraftnpc";
	private static final String PERMISSION = "monumenta.commands.bluestrikedaggercraftnpc";
	private static final String NPC_TAG = "blueStrikeBossNPC";

	private boolean mIsCrafter = false;

	public static void register() {
		new CommandAPICommand(COMMAND)
			// Syntax:
			// bluestrikedaggercraftnpc @S @N
			// @S is the player triggering the command (When holding shard)
			// @N is the UUID of the npc entity
			.withPermission(CommandPermission.fromString(PERMISSION))
			.withArguments(
				new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				new EntitySelectorArgument("npc", EntitySelectorArgument.EntitySelector.ONE_ENTITY)
			)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Entity npc = (Entity) args[1];

				BlueStrikeDaggerCraftingBoss daggerCraftingBoss = BossUtils.getBossOfClass(npc, BlueStrikeDaggerCraftingBoss.class);
				if (daggerCraftingBoss != null) {
					daggerCraftingBoss.onInteract(player);
				}
			})
			.register();
	}

	private final Location mSpawnLoc;
	public static String identityTag = "boss_bluestrikedaggercraft";
	public static @Nullable BukkitTask mRunnable;
	public List<Spell> mOnlinePassives = List.of();
	public List<Spell> mOfflinePassives = new ArrayList<>();
	private Samwell mSamwellAbility;
	private final SpellManager mSpellManager = SpellManager.EMPTY;

	public static @Nullable BlueStrikeDaggerCraftingBoss deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return construct(plugin, boss);
	}

	public static @Nullable BlueStrikeDaggerCraftingBoss construct(Plugin plugin, LivingEntity boss) {
		// Get nearest entity called Samwell.
		Samwell samwell = null;
		List<LivingEntity> witherSkeletons = EntityUtils.getNearbyMobs(boss.getLocation(), 100, EnumSet.of(EntityType.WITHER_SKELETON));
		for (LivingEntity mob : witherSkeletons) {
			if (mob.getScoreboardTags().contains(Samwell.identityTag)) {
				samwell = BossUtils.getBossOfClass(mob, Samwell.class);
				break;
			}
		}
		if (samwell == null) {
			MMLog.warning("DaggerCraftingBoss: Samwell wasn't found! (This is a bug)");
			return null;
		}
		return new BlueStrikeDaggerCraftingBoss(plugin, boss, samwell);
	}

	BlueStrikeDaggerCraftingBoss(Plugin plugin, LivingEntity boss, Samwell samwell) {
		super(plugin, identityTag, boss);
		mSpawnLoc = mBoss.getLocation();
		boss.getScoreboardTags().add(NPC_TAG);

		mSamwellAbility = samwell;

		Location daggerLoc = mSamwellAbility.mDaggerLoc;

		mOnlinePassives = List.of(
				new SpellCraftDaggerAnimation(mBoss, daggerLoc)
		);

		super.constructBoss(mSpellManager, mOfflinePassives, 100, null);
	}

	// @Override onHurtByEntityWithSource
	// Due to ScriptedQuests auto-cancelling the damageEvent, we need to use ScriptedQuests
	// interactables / quests to call this function (via a command). Fun!
	public void onInteract(Player player) {
		String npcName = mBoss.getName();

		if (mSamwellAbility.mDefeated) {
			switch (npcName.toLowerCase()) {
				case "bhairavi" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "Quick, grab the wool. Let’s get out of here...");
				}
				case "levyn" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "He be DEAD!! Go get us tha wool! The traitor be DEAD!");
				}
				case "izzy" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "Hurry, go grab the wool. I don’t want to be here anymore.");
				}
				default -> {

				}
			}
			return;
		}

		if (mSamwellAbility.mCraftPhase) {
			switch (npcName.toLowerCase()) {
				case "bhairavi" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "I’ve started crafting the dagger. Keep them off me!");
				}
				case "levyn" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "I’m puttin’ yer dagger togetha! Keep me safe, cap’n!");
				}
				case "izzy" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "I’ve started putting together the dagger. Keep me safe!");
				}
				default -> {

				}
			}
			return;
		}

		if (mSamwellAbility.mPhase >= 4) {
			switch (npcName.toLowerCase()) {
				case "bhairavi" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "He’s vulnerable now, don’t worry about the stupid daggers!");
				}
				case "levyn" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "It be no time fer another dagger! Go finish ‘im!");
				}
				case "izzy" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "Just focus on Samwell so we can get the wool!");
				}
				default -> {

				}
			}
			return;
		}

		if (mSamwellAbility.mDaggerPhase) {
			switch (npcName.toLowerCase()) {
				case "bhairavi" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "It's done! Collect the dagger and let's take this monster down.");
				}
				case "levyn" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "Ahoy cap'n, the dagger be done! Get Samwell, quick!");
				}
				case "izzy" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "It's done, Captain! Get the dagger and stab the traitor!");
				}
				default -> {

				}
			}
			return;
		}

		int shards = mSamwellAbility.getShards();
		int reqShards = mSamwellAbility.mShardsReq - mSamwellAbility.getShards();

		for (ItemStack itemStack : player.getInventory()) {
			if (itemStack != null && itemStack.isSimilar(mSamwellAbility.mShards)) {
				int amount = itemStack.getAmount();
				if (reqShards > amount) {
					mSamwellAbility.addShards(amount);
					itemStack.setAmount(0);
				} else {
					mSamwellAbility.addShards(reqShards);
					itemStack.setAmount(amount - reqShards);
				}
				break;
			}
		}

		if (shards == mSamwellAbility.getShards()) {
			switch (npcName.toLowerCase()) {
				case "bhairavi" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "If you want me to do anything, I need you to grab some Blackblood Shards.");
				}
				case "levyn" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "Yarr, get ye some of them shards and I can try an' help!");
				}
				case "izzy" -> {
					MessagingUtils.sendNPCMessage(player, npcName, "If you can get ahold of some Blackblood Shards, I might be able to help!");
				}
				default -> {

				}
			}
			return;
		}

		if (mSamwellAbility.getShards() >= mSamwellAbility.mShardsReq) {
			// Begin Crafting
			switch (npcName.toLowerCase()) {
				case "bhairavi" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "That's enough. We'll start conjuring the dagger!"));
				}
				case "levyn" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "Yarr, that be enough! Dagger ahoy!"));
				}
				case "izzy" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "That should do it. We'll start weaving the dagger together now."));
				}
				default -> {

				}
			}
			mIsCrafter = true;
			mSamwellAbility.startCraftPhase();
		} else {
			switch (npcName.toLowerCase()) {
				case "bhairavi" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "Thank you. We'll need " + (mSamwellAbility.mShardsReq - mSamwellAbility.getShards()) + " more to put together the dagger."));
				}
				case "levyn" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "Gonna need ye to get " + (mSamwellAbility.mShardsReq - mSamwellAbility.getShards()) + " more cap'n! Then it'll be dagger ahoy!"));
				}
				case "izzy" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "If you can grab " + (mSamwellAbility.mShardsReq - mSamwellAbility.getShards()) + " more, we should be able to get the dagger together. Thanks!"));
				}
				default -> {

				}
			}
		}
	}

	public void craft() {
		changePhase(SpellManager.EMPTY, mOnlinePassives, null);
	}

	public void complete() {
		changePhase(mSpellManager, mOfflinePassives, null);
		if (mIsCrafter) {
			String npcName = mBoss.getName();
			switch (npcName.toLowerCase()) {
				case "bhairavi" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "It's done! Collect the dagger and let's take this monster down."));
				}
				case "levyn" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "Ahoy cap'n, the dagger be done! Get Samwell, quick!"));
				}
				case "izzy" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "It's done, Captain! Get the dagger and stab the traitor!"));
				}
				default -> {

				}
			}

			mIsCrafter = false;
		}
	}

	public void takeDamage() {
		String npcName = mBoss.getName();
		if (mSamwellAbility.getFails() < 2) {
			mSamwellAbility.addFail();
			switch (npcName.toLowerCase()) {
				case "bhairavi" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "Help! The Masked are on me! I can't focus!"));
				}
				case "levyn" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "Yarr, help me! Can't be craftin' if them fools are on me!"));
				}
				case "izzy" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "Quick! Help! I'm under attack!"));
				}
				default -> {

				}
			}

			// KB all nearby melee targets for a bit of a "buffer", if players are losing badly.
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), 5);
			for (LivingEntity mob : mobs) {
				if (mob.getScoreboardTags().contains(BlueStrikeTargetNPCBoss.identityTag)) {
					MovementUtils.knockAway(mBoss, mob, 1, 0.5f, false);
				}
			}
		} else {
			// 3 Fails = Restart!
			switch (npcName.toLowerCase()) {
				case "bhairavi" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "There was too many of them... We'll need new shards - the Masked ruined it..."));
				}
				case "levyn" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "My spleen! I can't keep craftin' like this, cap'n! Gimme some new sharrrrds!"));
				}
				case "izzy" -> {
					mSpawnLoc.getNearbyPlayers(100).stream().forEach(p -> MessagingUtils.sendNPCMessage(p, npcName, "I'm sorry Captain, we'll need more shards. The Masked broke what we've made."));
				}
				default -> {

				}
			}
			failure();
		}
	}

	public void failure() {
		changePhase(mSpellManager, mOfflinePassives, null);
		mSamwellAbility.clearTargetMobs();
		mSamwellAbility.changePhaseNormal();
		mIsCrafter = false;
	}
}
