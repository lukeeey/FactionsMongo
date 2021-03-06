package io.github.lukeeey.factionsmongodb;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterSettings;
import io.github.lukeeey.factionsmongodb.storage.MongoBoard;
import io.github.lukeeey.factionsmongodb.storage.MongoFPlayers;
import io.github.lukeeey.factionsmongodb.storage.MongoFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.Collections;

public class FactionsMongoPlugin extends JavaPlugin implements Listener {
    private MongoClient client;
    private MongoDatabase database;

    private MongoBoard board;
    private MongoFactions factions;
    private MongoFPlayers fplayers;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("Factions") == null) {
            getLogger().severe("FactionsUUID is not found");
            getPluginLoader().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        initMongo();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("factionsmongo")) {
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "FactionsMongo Commands:");
            sender.sendMessage(ChatColor.GOLD + "/fmongo reload" + ChatColor.GRAY + " - Reload the config");
            sender.sendMessage(ChatColor.GOLD + "/fmongo import" + ChatColor.GRAY + " - Import JSON data into the database");
//            sender.sendMessage(ChatColor.GOLD + "/fmongo export" + ChatColor.GRAY + " - Export from the database to JSON data");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "FactionsMongo config has been reloaded!");
                break;
            case "import":
                sender.sendMessage(ChatColor.GOLD + "Importing... (this could take a while!)");
                factions.convert();
                fplayers.convert();
                board.convert();
                sender.sendMessage(ChatColor.GREEN + "Imported! " + ChatColor.GRAY + "Please restart the server.");
                break;
            case "export":
                sender.sendMessage(ChatColor.RED + "Sorry, this is not implemented yet!");
                break;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getDescription().getName().equalsIgnoreCase("Factions")) {
            getLogger().info("Factions found, initializing...");
            factions = new MongoFactions(database.getCollection(getConfig().getString("mongodb.collections.factions")));
            board  = new MongoBoard(database.getCollection(getConfig().getString("mongodb.collections.board")));
            fplayers = new MongoFPlayers(database.getCollection(getConfig().getString("mongodb.collections.fplayers")));

            // This code is from FactionsUUID
            int loadedPlayers = fplayers.load();
            int loadedFactions = factions.load();
            for (FPlayer fPlayer : fplayers.getAllFPlayers()) {
                Faction faction = factions.getFactionById(fPlayer.getFactionId());
                if (faction == null) {
                    getLogger().info("Invalid faction id on " + fPlayer.getName() + ":" + fPlayer.getFactionId());
                    fPlayer.resetFactionData(false);
                    continue;
                }
                faction.addFPlayer(fPlayer);
            }
            int loadedClaims = board.load();
            board.clean();
            getLogger().info("Loaded " + loadedPlayers + " players in " + loadedFactions + " factions with " + loadedClaims + " claims");
        }
    }

    private void initMongo() {
        String username = getConfig().getString("mongodb.username");
        String password = getConfig().getString("mongodb.password");
        String database = getConfig().getString("mongodb.database");
        String host = getConfig().getString("mongodb.host");
        int port = getConfig().getInt("mongodb.port");
        String connectionString = getConfig().getString("mongodb.connection-string");

        if (connectionString != null && !connectionString.isEmpty()) {
            this.client = MongoClients.create(connectionString);
        } else {
            if (username == null || password == null || database == null || host == null) {
                getLogger().severe("Invalid config. Please fill out the username, password, host, port and database fields or specify a connection string and database.");
                getPluginLoader().disablePlugin(this);
                return;
            }

            ClusterSettings clusterSettings = ClusterSettings.builder()
                    .hosts(Collections.singletonList(new ServerAddress(new InetSocketAddress(host, port))))
                    .build();

            this.client = MongoClients.create(MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.applySettings(clusterSettings))
                    .credential(MongoCredential.createCredential(username, database, password.toCharArray()))
                    .writeConcern(WriteConcern.MAJORITY)
                    .retryWrites(true)
                    .build());
        }
        this.database = client.getDatabase(database);
    }
}
