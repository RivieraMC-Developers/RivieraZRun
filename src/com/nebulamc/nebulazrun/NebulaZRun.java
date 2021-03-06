package com.nebulamc.nebulazrun;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.nebulamc.core.util.Change;
import com.nebulamc.core.util.ChangeQueue;
import com.nebulamc.core.util.ChangeType;
import com.nebulamc.nebulazrun.event.ChatExpectation;
import com.nebulamc.nebulazrun.event.ChatHandler;
import com.nebulamc.nebulazrun.event.EventHandler;
import com.nebulamc.nebulazrun.event.MinigameRemoveConfirm;
import com.nebulamc.nebulazrun.minigame.MinigameCreationMode;
import com.nebulamc.nebulazrun.minigame.MinigameState;
import com.nebulamc.nebulazrun.minigame.ZRunMinigame;

public class NebulaZRun extends JavaPlugin  {
	
	public Configuration Config;
	public ArrayList<ZRunMinigame> minigames;
	public EventHandler Events;
	public ChatHandler Chat;
	public ChangeQueue Changes;
	public HashMap<String, MinigameCreationMode> CreationModes;
	
	public NebulaZRun() {
		Config = new Configuration();
		minigames = new ArrayList<ZRunMinigame>();
		Events = new EventHandler(this);
		Changes = new ChangeQueue();
		Chat = new ChatHandler();
		CreationModes = new HashMap<String, MinigameCreationMode>();
	}

	@Override
	public void onEnable() {
		Config.loadFromFile(this);
		Events.registerEvents(this);
	}
	
	@Override
	public void onDisable() {
		Config.saveToFile(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String text, String[] args) { 
		if(text.equalsIgnoreCase("zrun")) {
			parseCentralCommand(sender, args);
			return true;
		}
		return false;
	}
	
	public void parseCentralCommand(CommandSender sender, String[] args) {
		if(args.length <= 0) {
			printAllCommands(sender);
		}
		else {
			if(args[0].equalsIgnoreCase("list") && hasPerms(sender, args)) {
				if(args.length == 1) {
					displayZRunList(sender);
				} 
				else displayError(sender, "Invalid usage. Try /zrun list");
			}
			else if(args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("help")) {
				printAllCommands(sender);
			}
			else if(args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("-")) {
				if(args.length >= 2) {
					String arg = args[1];
					ZRunMinigame minigame = null;
					if(isNumeric(arg)) {
						if(minigames.size() > Integer.parseInt(arg))  {
							minigame = minigames.get(Integer.parseInt(arg));
						}
						else sender.sendMessage("Invalid index '" + arg + "'!");
					}
					else if(containsName(minigames, arg)) {
						minigame = getFromName(minigames, arg);
					}
					else {
						sender.sendMessage(ChatColor.RED + "Could not find minigame with name or index '" + arg + "'");
					}
					if(minigame != null) {
						if(sender instanceof Player) {
							removeMinigameWithConfirm((Player)sender, minigame);
						}
						else removeMinigame(sender, minigame);
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "Invalid usage. Try /zrun remove <[index]:[name]>"); 
				}
			}
			else if(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("new") || args[0].equalsIgnoreCase("+")) {
				if(args.length == 1) {
					if(sender instanceof Player) {
						// Enable minigame creation mode.
						MinigameCreationMode mode = new MinigameCreationMode(this, (Player)sender);
						CreationModes.put(((Player)sender).getName(), mode);
						mode.enable();
					}
					else {
						sender.sendMessage(ChatColor.RED + "The console does not support minigame creation mode. Try /zrun add <name>");
						return;
					}
				}
				else {
					if(args.length == 2) {
						// Parse the arguments and then create a new minigame with those properties.
						try {
							String name = args[1];
							
							ZRunMinigame m = new ZRunMinigame(name);
							addMinigame(m);
							sender.sendMessage(ChatColor.GREEN + "Successfully added minigame called '" + args[1] + ".'");
						}
						catch(Exception ex) {
							sender.sendMessage(ChatColor.RED + "An error ocurred while trying to add minigame called '" + args[2] + ".'");
						}
					}
					else {
						sender.sendMessage(ChatColor.RED + "Invalid usage. Try /zrun add <name>");
					}
				}
			}
			else if(args[0].equalsIgnoreCase("enable")) {
				if(args.length >= 2) {
					String arg = args[1];
					ZRunMinigame minigame = null;
					if(isNumeric(arg)) {
						if(minigames.size() > Integer.parseInt(arg))  {
							minigame = minigames.get(Integer.parseInt(arg));
						}
						else sender.sendMessage("Invalid index '" + arg + "'!");
					}
					else if(containsName(minigames, arg)) {
						minigame = getFromName(minigames, arg);
					}
					else {
						sender.sendMessage(ChatColor.RED + "Could not find minigame with name or index '" + arg + "'");
					}
					if(minigame != null) {
						enableMinigame(minigame, sender);
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "Invalid usage. Correct usage is /zrun enable <[index]:[name]>"); 
				}
			}
			else if(args[0].equalsIgnoreCase("disable")) {
				if(args.length >= 2) {
					String arg = args[1];
					ZRunMinigame minigame = null;
					if(isNumeric(arg)) {
						if(minigames.size() > Integer.parseInt(arg))  {
							minigame = minigames.get(Integer.parseInt(arg));
						}
						else sender.sendMessage("Invalid index '" + arg + "'!");
					}
					else if(containsName(minigames, arg)) {
						minigame = getFromName(minigames, arg);
					}
					else {
						sender.sendMessage(ChatColor.RED + "Could not find minigame with name or index '" + arg + "'");
					}
					if(minigame != null) {
						disableMinigame(minigame, sender);
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "Invalid usage. Correct usage is /zrun disable <[index]:[name]>"); 
				}
			}
			else if(args[0].equalsIgnoreCase("reload")) {
				try {
					Config = new Configuration();
					minigames = new ArrayList<ZRunMinigame>();
					Events = new EventHandler(this);
					Changes = new ChangeQueue();
					Chat = new ChatHandler();
					CreationModes = new HashMap<String, MinigameCreationMode>();
					Config.loadFromFile(this);
					Events.registerEvents(this);
					sender.sendMessage(ChatColor.GREEN + "Successfully reinitialized the plugin and reloaded everything from file.");
				}
				catch(Exception ex) {
					sender.sendMessage(ChatColor.RED + "Something went wrong when attempting to reload Z-Run.");
				}
			}
			else if(args[0].equalsIgnoreCase("save")) {
				Config.saveToFile(this);
				sender.sendMessage(ChatColor.GREEN + "Successfully saved all objects in memory to the configuraton file.");
			}
			else if(args[0].equalsIgnoreCase("wand")) {
				if(sender instanceof Player) {
					Player player = (Player) sender;
					ItemStack wand = new ItemStack(Material.BLAZE_ROD, 1);
					ItemMeta wandMeta = wand.getItemMeta();
					wandMeta.setDisplayName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Z-Run " + ChatColor.GRAY + "" + ChatColor.BOLD + "Wand");
					ArrayList<String> wandLore = new ArrayList<String>();
					wandLore.add(ChatColor.GRAY + "The wand for making both region and single-block");
					wandLore.add(ChatColor.GRAY + "selections for use with Nebula Z-Run.");
					wandMeta.setLore(wandLore);
					wand.setItemMeta(wandMeta);
					
					player.getInventory().addItem(wand);
				}
				else {
					msg(sender, ChatColor.RED + "Error: The console cannot recieve a Z-Run wand.");
				}
			}
			else {
				sender.sendMessage(ChatColor.RED + "Unknown sub-command '" + args[0] + "'! Type /zrun to see a list of all of the sub-commands.");
			}
		}
	}
	
	public boolean containsName(ArrayList<ZRunMinigame> minigames2, String arg) {
		for(int i = 0; i < minigames2.size(); i++) {
			if(minigames2.get(i).name.equalsIgnoreCase(arg)) return true;
		}
		return false;
	}
	
	public void addMinigame(ZRunMinigame minigame) {
		this.minigames.add(minigame);
		minigame.state = MinigameState.IDLE;
	}

	public ZRunMinigame getFromName(ArrayList<ZRunMinigame> minigames2, String arg) {
		for(int i = 0; i < minigames2.size(); i++) {
			if(minigames2.get(i).name.equalsIgnoreCase(arg)) return minigames2.get(i);
		}
		return null;
	}

	public void removeMinigameWithConfirm(CommandSender sender, ZRunMinigame minigame) {
		sender.sendMessage(ChatColor.AQUA + "Are you sure you want to remove this minigame? Type yes, no, or cancel.");
		Change change = (new Change()).setType(ChangeType.REMOVE).setNewObject(minigame).setChangeID("minigame_remove").setSender(sender);
		Changes.enqueue(change);
		Chat.addChatExpectation(new MinigameRemoveConfirm((((Player)sender).getName()), this, change), (Player)sender);
	}
	
	public void removeMinigame(CommandSender sender, ZRunMinigame minigame) {
		minigames.remove(minigame);
		sender.sendMessage(ChatColor.GREEN + "Successfully removed minigame '" + minigame.name + ".'");
	}
	
	public void printAllCommands(CommandSender s) {
		msg(s, ChatColor.GRAY + repeat('-', 19) + "[" + ChatColor.DARK_PURPLE + "Nebula " + ChatColor.DARK_AQUA + "Z-Run" + ChatColor.GRAY + "]" + repeat('-', 20));
		msg(s, formatHelp("list", "Displays a list of all minigame instances."));
		msg(s, formatHelp("remove <[index]:[name]>", "Removes the specified minigame.", "del, delete, -"));
		msg(s, formatHelp("add", "Starts minigame creation mode.", "create, new, +"));
		msg(s, formatHelp("add <name>", "Creates a minigame with the specified properties.", "create, new, +"));
		msg(s, formatHelp("enable <[index]:[name]>", "Enables the specified minigame if disabled."));
		msg(s, formatHelp("disable <[index]:[name]>", "Disabled the specified minigame if not already disabled.."));
		msg(s, formatHelp("reload", "Stops all active games, removes all loaded minigames from memory, and reloads all minigames from file."));
		msg(s, formatHelp("save", "Saves all memory items to the configuration file."));
		msg(s, formatHelp("wand", "Gives the player sender a z-run wand."));
	}
	
	public String repeat(char character, int times) {
		String str = "";
		for(int i = 1; i <= times; i++) {
			str += character;
		}
		return str;
	}
	
	public String formatHelp(String expectedArgs, String description) {
		return ChatColor.GOLD + " - " + ChatColor.GREEN + "/zrun " + expectedArgs + ChatColor.GOLD + " - " + description;
	}
	
	public String formatHelp(String expectedArgs, String description, String aliases) {
		return ChatColor.GOLD + " - " + ChatColor.GREEN + "/zrun " + expectedArgs + ChatColor.GOLD + " - " + ChatColor.GRAY + "Aliases: " + aliases + ChatColor.GOLD + " - " + description;
	}
	
	public boolean hasPerms(CommandSender sender, String[] args) {
		if(!(sender instanceof Player)) return true;
		else {
			if(((Player)sender).isOp()) return true;
			else return false;
		}
	}
	
	public void msg(CommandSender sender, String message) {
		sender.sendMessage(message);
	}
	
	public void displayZRunList(CommandSender sender) {
		msg(sender, ChatColor.AQUA + "Currently registered zrun minigame instances:");
		for(int i = 0; i < minigames.size(); i++) {
			ChatColor state = minigames.get(i).state.toColor();
			String currentLine = "" + ChatColor.GOLD;
			currentLine += " [" + i + "] ";
			currentLine += state + "";
			currentLine += minigames.get(i).name;
			msg(sender, currentLine);
			
		}
	}
	
	public void displayError(CommandSender sender, String text) {
		msg(sender, ChatColor.RED + "Error: " + text);
	}
	
	public boolean cancelChat(AsyncPlayerChatEvent e) {
		if(Chat.ExpectingUsernames.contains(e.getPlayer().getName())) {
			for(int i = 0; i < Chat.Expectations.size(); i++) {
				if(Chat.Expectations.get(i).username == e.getPlayer().getName()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void handleDirectedChat(AsyncPlayerChatEvent e) {
		ChatExpectation expect = null;
		for(int i = 0; i < Chat.Expectations.size(); i++) {
			if(Chat.Expectations.get(i).username == e.getPlayer().getName()) {
				expect = Chat.Expectations.get(i);
			}
		}
		if(expect == null) return;
		if(e.getMessage().equalsIgnoreCase("cancel")) {
			expect.handleCancel(e.getPlayer());
			Chat.Expectations.remove(expect);
			return;
		}
		else {
			if(!expect.overrideDefault) {
				boolean found = false;
				for(int i = 0; i < expect.Expectations.length; i++) {
					if(e.getMessage().equalsIgnoreCase(expect.Expectations[i])) {
						expect.handleChat(e.getMessage(), e.getPlayer());
						Chat.Expectations.remove(expect);
						found = true;
					}
				}
				if(!found) {
					if(expect.handleInvalid(e.getMessage(), e.getPlayer())) {
						expect.handleReExpect(e.getMessage(), e.getPlayer());
					}
					else {
						expect.handleCancel(e.getPlayer());
						Chat.Expectations.remove(expect);
						return;
					}
				}
			}
			else {
				if(expect.doHandle(e.getMessage(), e.getPlayer())) {
					expect.handleChat(e.getMessage(), e.getPlayer());
					Chat.Expectations.remove(expect);
				}
				else {
					expect.handleCancel(e.getPlayer());
					Chat.Expectations.remove(expect);
					return;
				}
			}
		}
	}
	
	public void handleChat(AsyncPlayerChatEvent e) {
		
	}
	
	public void consumeChange(Change change) {
		switch(change.changeID.toLowerCase()) {
			case "minigame_remove": {
				minigames.remove(change.newObject);
				((CommandSender)change.sender).sendMessage(ChatColor.GREEN + "Successfully removed minigame '" + ((ZRunMinigame)change.newObject).name + ".'");
			}
		}
	} 
	
	public boolean isNumeric(String text) {
		boolean end = true;
		for(int i = 0; i < text.length(); i++) {
			if(Character.isDigit(text.charAt(i))) {}
			else end = false;
		}
		return end;
	}
	
	public void enableMinigame(ZRunMinigame m, CommandSender s) {
		if(m != null) {
			if(m.state == MinigameState.DISABLED) {
				m.state = MinigameState.IDLE;
				msg(s, ChatColor.GREEN + "The specified minigame is now enabled and idle.");
			}
			else msg(s, ChatColor.RED + "The specified minigame was already enabled.");
		}
		else {
			throw new IllegalArgumentException();
		}
	}
	
	public void disableMinigame(ZRunMinigame m, CommandSender s) {
		if(m != null) {
			if(m.state != MinigameState.DISABLED) {
				m.state = MinigameState.DISABLED;
				msg(s, ChatColor.GREEN + "The specified minigame is now disabled.");
			}
			else msg(s, ChatColor.RED + "The specified minigame was already disabled.");
		}
		else {
			throw new IllegalArgumentException();
		}
	}
}
