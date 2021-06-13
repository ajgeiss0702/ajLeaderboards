package us.ajg0702.leaderboards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.signs.BoardSign;
import us.ajg0702.leaderboards.signs.SignManager;
import us.ajg0702.utils.spigot.LocUtils;
import us.ajg0702.utils.spigot.Messages;

public class Commands implements CommandExecutor, TabCompleter {
	
	Main pl;
	public Commands(Main pl) {
		this.pl = pl;
	}
	
	HashMap<CommandSender, String> confirmDeletes = new HashMap<>();

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.hasPermission("ajleaderboards.use")) {
			sender.sendMessage(color("&cYou do not have permission to use this!"));
			return true;
		}
		Player sply = sender instanceof Player ? (Player) sender : null;
		if(args.length == 0) {
			sender.sendMessage(color(
					"&6ajLeaderboards v"+pl.getDescription().getVersion()+" by ajgeiss0702\n"
					+ "  &e/"+label+" add &7- &eStart tracking a placeholder for all players.\n"
					+ "  &e/"+label+" list [board] &7- &eList all boards or list the top 10 for a certain board.\n"
					+ "  &e/"+label+" reload &7- &eReload the config.\n"
					+ "&7See the wiki for more info."
					));
			return true;
		}
		Cache cache = Cache.getInstance();
		switch(args[0].toLowerCase()) {
		case "add":
			if(args.length <= 1) {
				sender.sendMessage(color("&cPlease provide a placeholder to track.\n&7Usage: /"+label+" add <placeholder>"));
				return true;
			}
			String placeholder = args[1];
			placeholder = placeholder.replaceAll(Matcher.quoteReplacement("%"), "");
			if(!validatePlaceholder(placeholder, sply)) {
				sender.sendMessage(color("&cThe placeholder '"+placeholder+"' does not give a numerical value. Make sure that the placeholder returns a number that is not formatted."));
				return true;
			}
			boolean r = cache.createBoard(placeholder);
			if(r) {
				sender.sendMessage(color("&aBoard '"+placeholder+"' successfully created!"));
			} else {
				sender.sendMessage(color("&cBoard '"+placeholder+"' creation failed! See console for more info."));
			}
			return true;
		case "reload":
			pl.config.reload();
			pl.reloadInterval();
			sender.sendMessage(color("&aConfig reloaded!"));
			return true;
		case "update":
			if(args.length <= 2) {
				sender.sendMessage(color("&cPlease provide a board and player to update\n&7Usage: /"+label+" update <board> <player>"));
				return true;
			}
			Bukkit.getScheduler().runTaskAsynchronously(pl, new Runnable() {
				public void run() {
					String board = args[1];
					if(!cache.getBoards().contains(board)) {
						sender.sendMessage(color("&cThe board '"+board+"' does not exist."));
						return;
					}
					@SuppressWarnings("deprecation")
					OfflinePlayer p = Bukkit.getOfflinePlayer(args[2]);
					cache.updateStat(args[1], p);
					if(!p.isOnline()) {
						sender.sendMessage(color("&6Warning: &7The player you requested to update appears to be offline. Not all placeholders support this. I'll still try, but if there is an error or nothing is updated, the placeholder probably doesn't support it."));
					}
					sender.sendMessage(color("&aAttempted to update stat for "+p.getName()+" on board "+args[1]));
				}
			});
			return true;
		case "removeplayer":
			if(args.length <= 1) {
				sender.sendMessage(color("&cPlease provide a board and a player.\n&7Usage: /"+label+" removeplayer <player> <board>"));
				return true;
			}
			if(args.length <= 2) {
				sender.sendMessage(color("&cPlease provide a board.\n&7Usage: /"+label+" removeplayer <player> <board>"));
				return true;
			}
			String playername = args[1];
			String board0 = args[2];
			if(!cache.getBoards().contains(board0)) {
				sender.sendMessage(color("&cThe board '"+board0+"' does not exist."));
				return true;
			}
			Bukkit.getScheduler().runTaskAsynchronously(pl, () -> {
				Cache.getInstance().removePlayer(board0, Bukkit.getOfflinePlayer(playername).getUniqueId());
				sender.sendMessage(Messages.getInstance().color("&aRemoved "+playername+" from "+board0+"!"));
			});
			break;
		case "remove":
			if(args.length <= 1) {
				sender.sendMessage(color("&cPlease provide a placeholder to remove.\n&7Usage: /"+label+" remove <board>"));
				return true;
			}
			String board1 = args[1];
			if(!cache.getBoards().contains(board1)) {
				sender.sendMessage(color("&cThe board '"+board1+"' does not exist."));
				return true;
			}
			if(!confirmDeletes.containsKey(sender) || (confirmDeletes.containsKey(sender) && !confirmDeletes.get(sender).equals(board1))) {
				sender.sendMessage(color("&cThis action will delete data! If you add back the board, the top players will have to join again to show up.\n"
						+ "&7Repeat the command within 30 seconds to confirm this action"));
				confirmDeletes.put(sender, board1);
				Bukkit.getScheduler().runTaskLater(pl, new Runnable() {
					public void run() {
						if(confirmDeletes.containsKey(sender) && confirmDeletes.get(sender).equals(board1)) {
							confirmDeletes.remove(sender);
						}
					}
				}, 30*20);
				return true;
			} else {
				confirmDeletes.remove(sender);
				cache.removeBoard(board1);
				sender.sendMessage(color("&aThe board has been removed!"));
				return true;
			}
		case "list":
			if(args.length <= 1) {
				String list = "&6Boards";
				for(String boardn : cache.getBoards()) {
					list += "\n&7- &e"+boardn;
				}
				sender.sendMessage(color(list));
				return true;
			}
			String boardn = args[1];
			if(!cache.getBoards().contains(boardn)) {
				sender.sendMessage(color("&cthe board '"+boardn+"' does not exist."));
				return true;
			}
			String list = "&6Top for "+boardn;
			for(int i = 1;i<=10;i++) {
				StatEntry e = cache.getStat(i, boardn);
				list += "\n&6"+i+". &e"+e.getPlayer()+" &7- &e"+e.getScorePretty();
			}
			sender.sendMessage(color(list));
			return true;
		case "signs":
			if(args.length == 1) {
				sender.sendMessage(color(
						"&6ajLeaderboards Signs\n"
						+ "  &e/"+label+" signs add <board> <position>&7- &eSet the sign you are looking at to be a sign\n"
						+ "  &e/"+label+" signs list&7- &eList all signs.\n"
						+ "  &e/"+label+" signs remove &7- &eDeactivate the sign you are looking at\n"
						));
				return true;
			}
			switch(args[1].toLowerCase()) {
			case "add":
				if(args.length < 3) {
					sender.sendMessage(color("&cPlease provide a board and a position for the sign!\n&7Usage: /"+label+" signs add <board> <position>"));
					return true;
				}
				if(args.length < 4) {
					sender.sendMessage(color("&cPlease provide a position for the sign\n&7Usage: /"+label+" signs add <board> <position>"));
					return true;
				}
				if(!(sender instanceof Player)) {
					sender.sendMessage(color("&cYou must be in-game to do this!"));
					return true;
				}
				Player p = (Player) sender;
				int pos;
				try {
					pos = Integer.valueOf(args[3]);
				} catch(NumberFormatException e) {
					sender.sendMessage(color("&cInvalid number! Please enter a real number for the position.\n&7Usage: /"+label+" signs add <board> <position>"));
					return true;
				}
				Block target = p.getTargetBlock(null, 10);
				if(!target.getType().toString().contains("SIGN")) {
					sender.sendMessage(color("&cThe block you are looking at is not a sign! Please look at a sign to set."));
					return true;
				}
				SignManager.getInstance().addSign(target.getLocation(), args[2], pos);
				sender.sendMessage(color("&aSign created!"));
				return true;
			case "list":
				List<BoardSign> signs = SignManager.getInstance().getSigns();
				String s = "&6Signs";
				for(BoardSign sign : signs) {
					s += "\n&7- &e"+LocUtils.locToString(sign.getLocation())+" "+sign.getBoard();
				}
				if(signs.size() == 0) {
					s += "\n&7None";
				}
				sender.sendMessage(color(s));
				return true;
			case "remove":
				if(!(sender instanceof Player)) {
					sender.sendMessage(color("&cYou must be in-game to do this!"));
					return true;
				}
				Player p1 = (Player) sender;
				Block target1 = p1.getTargetBlock(null, 10);
				if(!target1.getType().toString().contains("SIGN")) {
					sender.sendMessage(color("&cThe block you are looking at is not a sign! Please look at a sign to remove."));
					return true;
				}
				if(SignManager.getInstance().removeSign(target1.getLocation())) {
					sender.sendMessage(color("&aSign removed!"));
				} else {
					sender.sendMessage(color("&cThat is not an ajLeaderboards sign!"));
				}
				
				return true;
			}
			return true;
		default:
			sender.sendMessage(color("&6Unknown command. Do /"+label+" for help."));
		}
		
			
		return true;
	}
	
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if(!sender.hasPermission("ajleaderboards.use")) {
			return new ArrayList<>();
		}
		if(args.length <= 1) {
			return Arrays.asList("add", "list", "reload", "remove", "signs", "update", "removeplayer");
		} else if(args.length == 2) {
			switch(args[0]) {
			case "update":
			case "list":
			case "remove":
				return Cache.getInstance().getBoards();
			case "signs":
				return Arrays.asList("add", "list", "remove");
			case "removeplayer":
				return null;
			default:
				return new ArrayList<>();
			}
		} else if(args.length == 3) {
			switch(args[0]) {
			case "update":
				return null;
			case "signs":
				switch(args[1]) {
				case "add":
					return Cache.getInstance().getBoards();
				case "list":
				case "remove":
					return new ArrayList<>();
				}
			case "removeplayer":
				return Cache.getInstance().getBoards();
			default:
				return new ArrayList<>();
			}
			
		}
		return new ArrayList<>();
	}
	
	
	public String color(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
	
	public boolean validatePlaceholder(String placeholder, Player sayOutput) {
		if(Bukkit.getOnlinePlayers().size() == 0) {
			pl.getLogger().warning("Unable to validate placeholder because no players are online.");
			return true;
		}
		Player vp = Bukkit.getOnlinePlayers().iterator().next();
		String out = PlaceholderAPI.setPlaceholders(vp, "%"+Cache.alternatePlaceholders(placeholder)+"%").replaceAll(",", "");
		try {
			Double.valueOf(out);
		} catch(NumberFormatException e) {
			if(sayOutput != null) {
				sayOutput.sendMessage(Messages.getInstance().color("&7Returned: "+out));
			}
			return false;
		}
		return true;
	}
	

	
	
	
	
}
