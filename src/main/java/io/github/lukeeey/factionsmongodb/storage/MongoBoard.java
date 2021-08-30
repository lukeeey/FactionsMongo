package io.github.lukeeey.factionsmongodb.storage;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.data.MemoryBoard;
import com.massivecraft.factions.data.json.JSONBoard;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.Map;

public class MongoBoard extends MemoryBoard {
    private static final ReplaceOptions options = new ReplaceOptions().upsert(true);
    private static JSONBoard jsonBoardInstance;
    private MongoCollection<Document> collection;

    public MongoBoard(MongoCollection<Document> collection) {
        this.collection = collection;
        jsonBoardInstance = (JSONBoard) Board.instance;
        Board.instance = this;
    }

    public void forceSave() {
        forceSave(true);
    }

    public void forceSave(boolean sync) {
        Map<String, Map<String, String>> map = new Object2ObjectOpenHashMap<>();

        for (Map.Entry<FLocation, String> entry : flocationIds.entrySet()) {
            String worldName = entry.getKey().getWorldName();
            String coords = entry.getKey().getCoordString();
            String factionId = entry.getValue();

            if (!map.containsKey(worldName)) {
                map.put(worldName, new Object2ObjectOpenHashMap<>());
            }

            map.get(worldName).put(coords, factionId);
        }

       saveToMongo(map);
    }

    private void saveToMongo(Map<String, Map<String, String>> map) {
        map.forEach((worldName, map2) -> {
            Document coordsDoc = new Document();
            map2.forEach(coordsDoc::append);

            Bukkit.getLogger().info(coordsDoc.toJson());

            Document document = new Document()
                    .append("worldName", worldName)
                    .append("coords", coordsDoc);

            collection.replaceOne(Filters.eq("worldName", worldName), document, options);
        });
    }

    public void convert() {
        JSONBoard jsonBoard = jsonBoardInstance;
        jsonBoard.load();
        Map<String, Map<String, String>> savedFormat = jsonBoard.dumpAsSaveFormat();
        saveToMongo(savedFormat);
    }

    public int load() {
        MongoCursor<Document> iterator = collection.find().iterator();
        while (iterator.hasNext()) {
            Document doc = iterator.next();
            String worldName = doc.getString("worldName");
            Document coordsDoc = doc.get("coords", Document.class);

            coordsDoc.forEach((coordsString, factionId) -> {
                String[] coords = coordsString.trim().split("[,\\s]+");
                int x = Integer.parseInt(coords[0]);
                int z = Integer.parseInt(coords[1]);

                flocationIds.put(new FLocation(worldName, x, z), factionId.toString());
            });
        }
        return flocationIds.size();
    }

    @Override
    public void convertFrom(MemoryBoard old) {
        this.flocationIds = old.flocationIds;
        forceSave();
        Board.instance = this;
    }
}
