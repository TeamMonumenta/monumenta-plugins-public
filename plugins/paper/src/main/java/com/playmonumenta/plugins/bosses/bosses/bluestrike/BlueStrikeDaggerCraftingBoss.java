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
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
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
				new EntitySelectorArgument.OnePlayer("players"),
				new EntitySelectorArgument.OneEntity("npc")
			)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("players");
				Entity npc = args.getUnchecked("npc");

				BlueStrikeDaggerCraftingBoss daggerCraftingBoss = BossUtils.getBossOfClass(npc, BlueStrikeDaggerCraftingBoss.class);
				if (daggerCraftingBoss != null) {
					daggerCraftingBoss.onInteract(player);
				}
			})
			.register();
	}

	public static String identityTag = "boss_bluestrikedaggercraft";
	public List<Spell> mOnlinePassives;
	public List<Spell> mOfflinePassives = new ArrayList<>();
	private final Samwell mSamwellAbility;
	private final SpellManager mSpellManager = SpellManager.EMPTY;

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
		if (mSamwellAbility.mDefeated) {
			sendMessage(player,
				"Quick, grab the wool. Let’s get out of here...",
				"He be DEAD!! Go get us tha wool! The traitor be DEAD!",
				"Hurry, go grab the wool. I don’t want to be here anymore."
			);
			return;
		}

		if (mSamwellAbility.mCraftPhase) {
			sendMessage(player,
				"I’ve started crafting the dagger. Keep them off me!",
				"I’m puttin’ yer dagger togetha! Keep me safe, cap’n!",
				"I’ve started putting together the dagger. Keep me safe!"
			);
			return;
		}

		if (mSamwellAbility.mPhase >= 4) {
			sendMessage(player,
				"He’s vulnerable now, don’t worry about the stupid daggers!",
				"It be no time fer another dagger! Go finish ‘im!",
				"Just focus on Samwell so we can get the wool!"
			);
			return;
		}

		if (mSamwellAbility.mDaggerPhase) {
			sendMessage(player,
				"It's done! Collect the dagger and let's take this monster down.",
				"Ahoy cap'n, the dagger be done! Get Samwell, quick!",
				"It's done, Captain! Get the dagger and stab the traitor!"
			);
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
			sendMessage(player,
				"If you want me to do anything, I need you to grab some Blackblood Shards.",
				"Yarr, get ye some of them shards and I can try an' help!",
				"If you can get ahold of some Blackblood Shards, I might be able to help!"
			);
			return;
		}

		if (mSamwellAbility.getShards() >= mSamwellAbility.mShardsReq) {
			// Begin Crafting
			sendMessage(player,
				"That's enough. We'll start conjuring the dagger!",
				"Yarr, that be enough! Dagger ahoy!",
				"That should do it. We'll start weaving the dagger together now."
			);
			mIsCrafter = true;
			mSamwellAbility.startCraftPhase();
		} else {
			int remainingShards = mSamwellAbility.mShardsReq - mSamwellAbility.getShards();
			sendMessage(null,
				"Thank you. We'll need " + remainingShards + " more to put together the dagger.",
				"Gonna need ye to get " + remainingShards + " more cap'n! Then it'll be dagger ahoy!",
				"If you can grab " + remainingShards + " more, we should be able to get the dagger together. Thanks!"
			);
		}
	}

	public void craft() {
		changePhase(SpellManager.EMPTY, mOnlinePassives, null);
	}

	public void complete() {
		changePhase(mSpellManager, mOfflinePassives, null);
		if (mIsCrafter) {
			sendMessage(null,
				"It's done! Collect the dagger and let's take this monster down.",
				"Ahoy cap'n, the dagger be done! Get Samwell, quick!",
				"It's done, Captain! Get the dagger and stab the traitor!"
			);

			mIsCrafter = false;
		}
	}

	public void takeDamage() {
		if (mSamwellAbility.getFails() < 2) {
			mSamwellAbility.addFail();
			sendMessage(null,
				"Help! The Masked are on me! I can't focus!",
				"Yarr, help me! Can't be craftin' if them fools are on me!",
				"Quick! Help! I'm under attack!"
			);

			// KB all nearby melee targets for a bit of a "buffer", if players are losing badly.
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), 5);
			for (LivingEntity mob : mobs) {
				if (mob.getScoreboardTags().contains(BlueStrikeTargetNPCBoss.identityTag)) {
					MovementUtils.knockAway(mBoss, mob, 1, 0.5f, false);
				}
			}
		} else {
			// 3 Fails = Restart!
			sendMessage(null,
				"There was too many of them... We'll need new shards - the Masked ruined it...",
				"My spleen! I can't keep craftin' like this, cap'n! Gimme some new sharrrrds!",
				"I'm sorry Captain, we'll need more shards. The Masked broke what we've made."
			);
			failure();
		}
	}

	// Set player to null to send to all players in the arena
	private void sendMessage(@Nullable Player player, String bhairavi, String levyn, String izzy) {
		String npcName = mBoss.getName();
		String msg = switch (npcName.toLowerCase(Locale.getDefault())) {
			case "bhairavi" -> bhairavi;
			case "levyn" -> levyn;
			case "izzy" -> izzy;
			default -> null;
		};
		if (msg != null) {
			Collection<Player> players = player == null ? mSamwellAbility.getPlayers() : List.of(player);
			for (Player p : players) {
				MessagingUtils.sendNPCMessage(p, npcName, msg);
			}
		}
	}

	public void failure() {
		changePhase(mSpellManager, mOfflinePassives, null);
		mSamwellAbility.clearTargetMobs();
		mSamwellAbility.changePhaseNormal();
		mIsCrafter = false;
	}
}
