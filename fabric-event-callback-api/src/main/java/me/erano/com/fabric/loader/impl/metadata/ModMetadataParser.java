package me.erano.com.fabric.loader.impl.metadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// fabric.mod.json'in sadece "id" ve "entrypoints" alanlarini okur. Gercek parser (V1ModMetadataParser)
// tum semayi dogrular ve loader'in kendi gomulu JSON okuyucusunu kullanir; burada Gson.
// Bir entrypoint ya duz metin ("com.example.Mod") ya da {"adapter": "...", "value": "..."} olabilir.
public final class ModMetadataParser {
    private ModMetadataParser() {
    }

    public static LoaderModMetadata parse(InputStream in) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String id = root.get("id").getAsString();
            Map<String, List<EntrypointMetadata>> entrypoints = new LinkedHashMap<>();
            if (root.has("entrypoints")) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("entrypoints").entrySet()) {
                    List<EntrypointMetadata> list = new ArrayList<>();
                    for (JsonElement element : entry.getValue().getAsJsonArray()) {
                        if (element.isJsonPrimitive()) {
                            list.add(new EntrypointMetadata("default", element.getAsString()));
                        } else {
                            JsonObject obj = element.getAsJsonObject();
                            String adapter = obj.has("adapter") ? obj.get("adapter").getAsString() : "default";
                            list.add(new EntrypointMetadata(adapter, obj.get("value").getAsString()));
                        }
                    }
                    entrypoints.put(entry.getKey(), list);
                }
            }
            return new LoaderModMetadata(id, entrypoints);
        }
    }
}
