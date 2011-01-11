package com.shade.bukkit.towny;

import java.util.ArrayList;

import org.bukkit.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
/**
 * Handle events for all Player related events
 * @author Shade
 */
public class TownyPlayerListener extends PlayerListener {
    private final Towny plugin;

    public TownyPlayerListener(Towny instance) {
        plugin = instance;
    }

    @Override
    public void onPlayerJoin(PlayerEvent event) {
    	Player player = event.getPlayer();
    	try {
    		plugin.getTownyUniverse().onLogin(player);
    	} catch (TownyException x) {
    		plugin.sendErrorMsg(player, x.getError());
    	}
    }

    @Override
    public void onPlayerQuit(PlayerEvent event) {
    	plugin.getTownyUniverse().onLogout(event.getPlayer());
    }

    @Override
    public void onPlayerCommand(PlayerChatEvent event) {
        if (event.isCancelled())
        	return;
        
        String[] split = event.getMessage().split(" ");
        Player player = event.getPlayer();
        if (split[0].equalsIgnoreCase("/resident") || split[0].equalsIgnoreCase("/player")) {
        	String[] newSplit = new String[0];
        	System.arraycopy(split, 1, newSplit, 0, split.length-1);
        	parseResidentCommand(player, newSplit);
        	event.setCancelled(true);
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
    	
    }
    
    public void parseResidentCommand(Player player, String[] split) {
    	if (split.length == 0) {
    		try {
	    		Resident resident = plugin.getTownyUniverse().getResident(player.getName());
	    		plugin.getTownyUniverse().sendMessage(player, resident.getStatus());
    		} catch (NotRegisteredException x) {
    			plugin.sendErrorMsg(player, "You are not registered");
    		}
    	}
    	if (split[0].equalsIgnoreCase("list")) {
    		listResidents(player);
        }
    }

    public void parseTownCommand(Player player, String[] split) {
    	if (split.length == 0) {
    		try {
	    		Resident resident = plugin.getTownyUniverse().getResident(player.getName());
	    		Town town = resident.getTown();
	    		plugin.getTownyUniverse().sendMessage(player, town.getStatus());
    		} catch (NotRegisteredException x) {
    			plugin.sendErrorMsg(player, "You are not registered");
    		} catch (TownyException x) {
    			plugin.sendErrorMsg(player, x.getError());
    		}
    	}
    	if (split[0].equalsIgnoreCase("list")) {
    		listResidents(player);
        }
    }
    
    public void listResidents(Player player) {
    	player.sendMessage(ChatTools.formatTitle("Residents"));
		String colour;
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Resident resident : plugin.getTownyUniverse().getActiveResidents()) {
			if (resident.isMayor())
				colour = Colors.LightBlue;
			else if (resident.isKing())
				colour = Colors.Gold;
			else
				colour = Colors.White;
			formatedList.add(colour + resident.getName() + Colors.White);
		}
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
    }
    
    public void listTowns(Player player) {
    	player.sendMessage(ChatTools.formatTitle("Towns"));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Town town : plugin.getTownyUniverse().getTowns())
			formatedList.add(Colors.LightBlue + town.getName() + Colors.Blue + " [" + town.getNumResidents() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList.toArray()))
			player.sendMessage(line);
    }
}