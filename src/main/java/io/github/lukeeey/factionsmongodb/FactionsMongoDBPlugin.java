package io.github.lukeeey.factionsmongodb;

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
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.Collections;

public class FactionsMongoDBPlugin extends JavaPlugin {
    private static final String PASSWORD = "LBf!>n6jnS&3>CM10#q8";

    private MongoClient client;
    private MongoDatabase database;

    private MongoBoard board;
    private MongoFactions factions;
    private MongoFPlayers fplayers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initMongo();

        factions = new MongoFactions(database.getCollection(getConfig().getString("mongodb.collections.factions")));
        board  = new MongoBoard(database.getCollection(getConfig().getString("mongodb.collections.board")));
        fplayers = new MongoFPlayers(database.getCollection(getConfig().getString("mongodb.collections.fplayers")));

        fplayers.load();
        factions.load();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fmongoconvert") && sender instanceof ConsoleCommandSender) {
            sender.sendMessage(ChatColor.GOLD + "Converting... (this could take a while!)");
            factions.convert();
            fplayers.convert();
            board.convert();
            sender.sendMessage(ChatColor.GREEN + "Converted! " + ChatColor.GRAY + "Please restart the server.");
        }
        return true;
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
