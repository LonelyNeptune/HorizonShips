package com.gmail.Rhisereld.HorizonShips;

import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.regions.RegionOperationException;

@SuppressWarnings("deprecation")
public class HorizonCommandParser implements CommandExecutor 
{
	ConfigAccessor data;
	ConfigAccessor config;
	ShipHandler shipHandler;
	JavaPlugin plugin;
	
	HashMap<String, String> confirmCreate = new HashMap<String, String>();	//Used to confirm commands
	HashMap<String, String> confirmDelete = new HashMap<String, String>();
	HashMap<String, String> confirmAddDestination = new HashMap<String, String>();
	HashMap<String, String> confirmRemoveDestination = new HashMap<String, String>();
	HashMap<String, String> confirmAdjust = new HashMap<String, String>();
	
    public HorizonCommandParser(ConfigAccessor data, ConfigAccessor config, JavaPlugin plugin) 
    {
		this.data = data;
		this.config = config;
		this.plugin = plugin;
		shipHandler = new ShipHandler(data, config, plugin);
	}

	/**
     * onCommand() is called when a player enters a command recognised by Bukkit to belong to this plugin.
     * After that it is up to the contents of this method to determine what the commands do.
     * 
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) 
	{
		String[] arguments;
		Player player = null;
		String name = sender.getName();
		
		//All commands that fall under /ship [additional arguments]
		if (commandLabel.equalsIgnoreCase("ship"))
		{
			//ship
			if (args.length <= 0)
			{
				showCommands(sender);
				return true;
			}
			
			//ship create [shipName] [destinationName]
			if (args[0].equalsIgnoreCase("create"))
			{				
				//Check that the sender is a player
				if (sender instanceof Player)
					player = Bukkit.getPlayer(sender.getName());
				else
				{
					sender.sendMessage(ChatColor.RED + "This command cannot be used by the console.");
					return false;
				}
				
				//Check the player has permission
				if (!player.hasPermission("horizonships.admin.ship.create"))
				{
					sender.sendMessage(ChatColor.RED + "You don't have permission to create a ship.");
					return false;
				}
				
				//Check for correct number of arguments.
				if (args.length != 3)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect number of arguments! Correct usage: /ship create [shipName] [destinationName]");
					return false;
				}

				sender.sendMessage(ChatColor.YELLOW + "A ship will be created using your current WorldEdit selection. Is this correct?"
						+ " Type '/ship confirm create' to confirm.");
				confirmCreate.put(name, args[1] + " " + args[2]);
				confirmCreateTimeout(sender);
				
				return true;
			}
			
			//ship delete [shipName]
			if (args[0].equalsIgnoreCase("delete"))
			{
				//Check the player has permission OR is the console
				if (!sender.hasPermission("horizonships.admin.ship.delete") && !(sender instanceof Player))
				{
					sender.sendMessage(ChatColor.RED + "You don't have permission to delete a ship.");
					return false;
				}
				
				//Check for correct number of arguments.
				if (args.length != 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect number of arguments! Correct usage: /ship delete [shipName]");
					return false;
				}
				
				sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to delete the ship " + args[1] + "?"
						+ " Type '/ship confirm delete' to confirm.");
				confirmDelete.put(name, args[1]);
				confirmDeleteTimeout(sender);
			}
			
			//ship add
			if (args[0].equalsIgnoreCase("add"))
			{
				if (args.length < 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect number of arguments!");
					return false;
				}
				
				//ship add destination [shipName] [destinationName]
				if (args[1].equalsIgnoreCase("destination"))
				{
					//Check that the sender is a player
					if (sender instanceof Player)
						player = Bukkit.getPlayer(sender.getName());
					else
					{
						sender.sendMessage(ChatColor.RED + "This command cannot be used by the console.");
						return false;
					}
					
					//Check that the player has permission
					if (!player.hasPermission("horizonships.admin.destination.add"))
					{
						sender.sendMessage("You don't have permission to create a destination.");
						return false;
					}
					
					//Check for correct number of arguments
					if (args.length != 4)
					{
						sender.sendMessage(ChatColor.RED + "Incorrect number of arguments! Correct usage: /ship add destination [shipName] "
								+ "[destinationName]");
						return false;
					}
					
					sender.sendMessage(ChatColor.YELLOW + "The ship will be pasted inside your current WorldEdit selection. Is this correct? "
							+ " Type '/ship confirm add destination' to confirm.");
					confirmAddDestination.put(name, args[2] + " " + args[3]);
					confirmAddDestinationTimeout(sender);
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "Incorrect format.");
					return false;
				}
			}
			
			//ship remove
			if (args[0].equalsIgnoreCase("remove"))
			{
				if (args.length < 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect number of arguments!");
					return false;
				}
				
				//ship remove destination [shipName] [destinationName]
				if (args[1].equalsIgnoreCase("destination"))
				{	
					//Check the player has permission OR is the console
					if (!sender.hasPermission("horizonships.admin.destination.remove") && !(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "You don't have permission to remove a destination.");
						return false;
					}
					
					//Check for correct number of arguments
					if (args.length != 4)
					{
						sender.sendMessage(ChatColor.RED + "Incorrect number of arguments! Correct usage: /ship remove destination [shipName] "
								+ "[destinationName]");
						return false;
					}
					
					sender.sendMessage(ChatColor.YELLOW + "Are you sure you wish to remove the destination " + args[3] + " from "
							+ args[2] + "? Type '/ship confirm remove destination' to confirm.");
					confirmRemoveDestination.put(name, args[2] + " " + args[3]);
					confirmRemoveDestinationTimeout(sender);
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "Incorrect format.");
					return false;
				}
			}
			
			//ship adjust [north/east/south/west/up/down]
			if (args[0].equalsIgnoreCase("adjust"))
			{
				//Check argument length
				if (args.length != 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect format number of arguments! Correct usage: /ship adjust "
							+ "[north/east/south/west/up/down]");
					return false;
				}
				
				//Check there's something to adjust
				if (!confirmAdjust.containsKey(name))
				{
					sender.sendMessage(ChatColor.RED + "You are not currently adjusting a destination!");
					return false;
				}
				
				//Adjust
				player = Bukkit.getPlayer(sender.getName());
				arguments = confirmAdjust.get(name).split(" ");
				
				try {
					shipHandler.adjustDestination(player, args[1], arguments[0]);
				} catch (MaxChangedBlocksException e) {
					sender.sendMessage(ChatColor.RED + "Ship too large!");
					return false;
				} catch (DataException | RegionOperationException | IncompleteRegionException | IOException e) {
					sender.sendMessage(ChatColor.RED + e.getMessage());
					sender.sendMessage(ChatColor.RED + "Unable to adjust destination. Please contact an administrator.");
					e.printStackTrace();
					return false;
				} catch (IllegalArgumentException e) {
					sender.sendMessage(ChatColor.RED + e.getMessage());
					return false;
				}
				
				return true;
			}
			
			//ship list
			if (args[0].equalsIgnoreCase("list"))
			{
				//Check that the player has permission OR is the console
				if (!sender.hasPermission("horizonships.list") && !(sender instanceof Player))
				{
					sender.sendMessage("You don't have permission to view this.");
					return false;
				}
				
				shipHandler.listShips(sender);
			}
			
			//ship pilot [destination]
			if (args[0].equalsIgnoreCase("pilot"))
			{
				//Check that the sender is a player
				if (sender instanceof Player)
					player = Bukkit.getPlayer(sender.getName());
				else
				{
					sender.sendMessage(ChatColor.RED + "This command cannot be used by the console.");
					return false;
				}
				
				//Check that the player has permission
				if (!sender.hasPermission("horizonships.pilot"))
				{
					sender.sendMessage(ChatColor.RED + "You don't have permission to pilot a ship.");
					return false;
				}
				
				//Check for correct number of args.
				if (args.length != 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect format.");
					return true;
				}
				
				try {
					shipHandler.moveShip(player, args[1]);
				} catch (DataException | IOException e) {
					player.sendMessage(ChatColor.RED + "Couldn't move ship. Please report this to an Adminstrator.");
					e.printStackTrace();
				} catch (MaxChangedBlocksException e) {
					player.sendMessage(ChatColor.RED + "Ship too large!");
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					player.sendMessage(ChatColor.RED + e.getMessage());
				}
				
			}
			
			//ship diagnose
			if (args[0].equalsIgnoreCase("diagnose"))
			{
				if (args.length != 1)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect format number of arguments! Correct usage: /ship diagnose");
					return false;
				}
				
				//Check that the sender is a player
				if (sender instanceof Player)
					player = Bukkit.getPlayer(sender.getName());
				else
				{
					sender.sendMessage(ChatColor.RED + "This command cannot be used by the console.");
					return false;
				}
				
				//Check that the player has permission
				if (!player.hasPermission("horizonships.diagnose"))
				{
					sender.sendMessage("You don't have permission to diagnose a ship.");
					return false;
				}
				
				try {
					shipHandler.diagnose(player);
				} catch (IllegalArgumentException e) {
					player.sendMessage(ChatColor.RED + e.getMessage());
				}
			}
			
			//ship repair
			if (args[0].equalsIgnoreCase("repair"))
			{
				if (args.length != 1)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect format number of arguments! Correct usage: /ship repair");
					return false;
				}
				
				//Check that the sender is a player
				if (sender instanceof Player)
					player = Bukkit.getPlayer(sender.getName());
				else
				{
					sender.sendMessage(ChatColor.RED + "This command cannot be used by the console.");
					return false;
				}
				
				//Check that the player has permission
				if (!player.hasPermission("horizonships.repair"))
				{
					sender.sendMessage("You don't have permission to repair a ship.");
					return false;
				}
				
				try {
					shipHandler.repair(player);
				} catch (IllegalArgumentException e) {
					player.sendMessage(ChatColor.RED + e.getMessage());
				}	
			}
			
			//ship confirm
			if (args[0].equalsIgnoreCase("confirm"))
			{
				if (args.length < 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect format.");
					return true;
				}
				
				//ship confirm create
				if (args[1].equalsIgnoreCase("create"))
				{					
					if (confirmCreate.get(name) == null)
					{
						sender.sendMessage(ChatColor.RED + "There is nothing for you to confirm.");
						return true;
					}
					
					arguments = confirmCreate.get(name).split(" ");
					confirmCreate.remove(name);
					player = Bukkit.getPlayer(sender.getName());

					try {
						shipHandler.createShip(arguments[0], player, arguments[1]);
						player.sendMessage(ChatColor.YELLOW + "Ship " + arguments[0] + " created!");
					} catch (DataException | IOException e) {
						sender.sendMessage(ChatColor.RED + e.getMessage());
						player.sendMessage(ChatColor.RED + "Couldn't create ship. Please report this to an Adminstrator.");
						e.printStackTrace();
						return false;
					} catch (NullPointerException e) {
						sender.sendMessage(ChatColor.RED + e.getMessage());
						e.printStackTrace();
						return false;
					} catch (IllegalArgumentException e) {
						sender.sendMessage(ChatColor.RED + e.getMessage());
						e.printStackTrace();
						return false;
					}
					return true;
				}

				//ship confirm delete
				if (args[1].equalsIgnoreCase("delete"))
				{
					if (confirmDelete.get(name) == null)
					{
						sender.sendMessage(ChatColor.RED + "There is nothing for you to confirm.");
						return true;
					}
					
					try {
					shipHandler.deleteShip(sender, confirmDelete.get(name));
					} catch (IllegalArgumentException e) {
						sender.sendMessage(ChatColor.RED + e.getMessage());
						return false;
					} catch (IOException e) {
						sender.sendMessage(ChatColor.RED + e.getMessage());
						player.sendMessage(ChatColor.RED + "Couldn't create ship. Please report this to an Adminstrator.");
						e.printStackTrace();
						return false;
					}
					
					sender.sendMessage(ChatColor.YELLOW + "Ship deleted.");
					confirmDelete.remove(name);
					return true;
				}
				
				//ship confirm add destination
				if (args[1].equalsIgnoreCase("add"))
				{
					if (args.length != 3)
					{
						sender.sendMessage(ChatColor.RED + "Incorrect format.");
						return true;
					}
					if (args[2].equalsIgnoreCase("destination"))
					{			
						if (confirmAddDestination.get(name) == null)
						{
							sender.sendMessage(ChatColor.RED + "There is nothing for you to confirm.");
							return true;
						}

						//Paste ship
						player = Bukkit.getPlayer(sender.getName());
						arguments = confirmAddDestination.get(name).split(" ");
						try {
							shipHandler.testDestination(player, arguments[0], arguments[1]);
						} catch (DataException | IOException e) {
							sender.sendMessage(ChatColor.RED + e.getMessage());
							player.sendMessage(ChatColor.RED + "Couldn't paste ship. Please report this to an Adminstrator.");
							e.printStackTrace();
							return false;
						} catch (MaxChangedBlocksException e) {
							player.sendMessage(ChatColor.RED + "Ship too large!");
							e.printStackTrace();
							return false;
						} catch (NullPointerException | IllegalArgumentException e) {
							player.sendMessage(ChatColor.RED + e.getMessage());
							e.printStackTrace();
						}
					
						sender.sendMessage(ChatColor.YELLOW + "Ship pasted for reference. Adjust the destination of the ship using "
							+ "'/ship adjust [north/east/south/west/up/down'. To confirm placement, type "
							+ "'/ship confirm adjust'.");
					
						//Remove confirmation for destination, add confirmation for tweaking
						confirmAdjust.put(name, confirmAddDestination.get(name));
						confirmAdjustTimeout(sender);
						confirmAddDestination.remove(name);
					}
				}

				
				//ship confirm remove destination
				if (args[1].equalsIgnoreCase("remove"))
				{
					if (args.length != 3)
					{
						sender.sendMessage(ChatColor.RED + "Incorrect format.");
						return true;
					}
					if (args[2].equalsIgnoreCase("destination"))
					{			
						if (confirmRemoveDestination.get(name) == null)
						{
							sender.sendMessage(ChatColor.RED + "There is nothing for you to confirm.");
							return true;
						}

						//Remove destination
						arguments = confirmRemoveDestination.get(name).split(" ");
						shipHandler.removeDestination(arguments[0], arguments[1]);

						sender.sendMessage(ChatColor.YELLOW + "Destination removed.");
					
						//Remove confirmation for destination.
						confirmRemoveDestination.remove(name);
					}
				}

				//ship confirm adjust
				if (args[1].equalsIgnoreCase("adjust"))
				{
					if (confirmAdjust.get(name) == null)
					{
						sender.sendMessage(ChatColor.RED + "There is nothing for you to confirm.");
						return true;
					}

					//Add destination
					player = Bukkit.getPlayer(sender.getName());
					arguments = confirmAdjust.get(name).split(" ");
					shipHandler.addDestination(player, arguments[0], arguments[1]);
					confirmAdjust.remove(name);

					sender.sendMessage(ChatColor.YELLOW + "Ship destination created.");
				}
			}
			
			//ship cancel
			if (args[0].equalsIgnoreCase("cancel"))
			{
				if (confirmCreate.containsKey(name))
				{
					confirmCreate.remove(name);
					sender.sendMessage(ChatColor.YELLOW + "Ship creation cancelled.");
					return true;
				}
				
				if (confirmDelete.containsKey(name))
				{
					confirmDelete.remove(name);
					sender.sendMessage(ChatColor.YELLOW + "Ship deletion cancelled.");
					return true;
				}
				
				if (confirmAddDestination.containsKey(name))
				{
					confirmAddDestination.remove(name);
					sender.sendMessage(ChatColor.YELLOW + "Ship destination cancelled.");
					return true;
				}
				
				if (confirmAdjust.containsKey(name))
				{
					confirmAdjust.remove(name);
					shipHandler.cancelDestination(name);
					sender.sendMessage(ChatColor.YELLOW + "Ship destination cancelled.");
					return true;
				}
				
				sender.sendMessage(ChatColor.RED + "There is nothing to cancel.");
				return false;
			}
		}
		return false;
	}
	
	/**
	 * confirmCreateTimeout() removes the player from the list of players who have a create command awaiting
	 * confirmation after 10 seconds.
	 * 
	 * @param sender
	 * @param key
	 */
	private void confirmCreateTimeout(final CommandSender sender)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable()
		{
			public void run()
			{
				if (confirmCreate.containsKey(sender.getName()))
				{
					confirmCreate.remove(sender.getName());
					sender.sendMessage(ChatColor.RED + "You timed out.");
				}
			}
		} , 200);
	}

	/**
	 * confirmDeleteTimeout() removes the player from the list of players who have a delete command awaiting
	 * confirmation after 10 seconds.
	 * 
	 * @param sender
	 * @param key
	 */
	private void confirmDeleteTimeout(final CommandSender sender)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable()
		{
			public void run()
			{
				if (confirmDelete.containsKey(sender.getName()))
				{
					confirmDelete.remove(sender.getName());
					sender.sendMessage(ChatColor.RED + "You timed out.");
				}
			}			
		} , 200);
	}
	
	/**
	 * confirmAddDestinationTimeout() removes the player from the list of players who are in the process 
	 * of adding a destination and removes the destination being made.
	 * 
	 * @param sender
	 */
	private void confirmAddDestinationTimeout(final CommandSender sender)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable()
		{
			public void run()
			{
				if (confirmAddDestination.containsKey(sender.getName()))
				{
					confirmAddDestination.remove(sender.getName());
					sender.sendMessage(ChatColor.RED + "You timed out.");
				}
			}			
		} , 200);
	}
	
	/**
	 * confirmRemoveDestinationTimeout() removes the player from the list of players who are in the process
	 * of removing a destination.
	 * 
	 * @param sender
	 */
	private void confirmRemoveDestinationTimeout(final CommandSender sender)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable()
		{
			public void run()
			{
				if (confirmRemoveDestination.containsKey(sender.getName()))
				{
					confirmRemoveDestination.remove(sender.getName());
					sender.sendMessage(ChatColor.RED + "You timed out.");
				}
			}			
		} , 200);
	}
	
	/**
	 * confirmAdjustTimeout() removes the player from the list of players who are in the process 
	 * of adding a destination and removes the destination being made.
	 * 
	 * @param sender
	 */
	private void confirmAdjustTimeout(final CommandSender sender)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable()
		{
			public void run()
			{
				if (confirmAdjust.containsKey(sender.getName()))
				{
					shipHandler.cancelDestination(sender.getName());
					confirmAdjust.remove(sender.getName());
					sender.sendMessage(ChatColor.RED + "You timed out.");
				}
			}			
		} , 6000);
	}
	
	private void showCommands(CommandSender sender)
	{
		if (!(sender instanceof Player))
			sender.sendMessage("----------------<" + ChatColor.GOLD + " Horizon Ships Commands " + ChatColor.WHITE + ">----------------");
		else
			sender.sendMessage("--------------<" + ChatColor.GOLD + " Horizon Ships Commands " + ChatColor.WHITE + ">--------------");
		sender.sendMessage(ChatColor.GOLD + "Horizon Ships allows you to maintain and travel in ships!");
		if (sender.hasPermission("horizonships.admin.ship.create"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/ship create [shipName] [destinationName]");
			sender.sendMessage("Create a new ship at a starter destination, using your current WorldEdit selection.");
		}
		if (sender.hasPermission("horizonships.admin.ship.delete"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/ship delete [shipName]");
			sender.sendMessage("Delete a ship.");
		}
		if (sender.hasPermission("horizonships.admin.destination.add"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/ship add destination [shipName] [destinationName]");
			sender.sendMessage("Add a destination to the given ship, using your current WorldEdit selection.");
		}
		if (sender.hasPermission("horizonships.admin.destination.remove"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/ship remove [shipName] [destinationName]");
			sender.sendMessage("Remove a destination from the given ship.");
		}
		if (sender.hasPermission("horizonships.list"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/ship list");
			sender.sendMessage("Provides a list of all current ships.");
		}
		if (sender.hasPermission("horizonships.pilot"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/ship pilot [destination]");
			sender.sendMessage("Pilot the ship that you are currently within to a destination of your choice.");
		}
		if (sender.hasPermission("horizonships.diagnose"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/ship diagnose");
			sender.sendMessage("Examine the ship you are currently within to discover any mechanical defects.");
		}
		if (sender.hasPermission("horizonships.repair"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/ship diagnose");
			sender.sendMessage("Use the item in your active hand to repair the ship.");
		}
		
		sender.sendMessage(ChatColor.YELLOW + "/ship cancel");
		sender.sendMessage("Cancel any actions that are currently awaiting confirmation.");
	}
}