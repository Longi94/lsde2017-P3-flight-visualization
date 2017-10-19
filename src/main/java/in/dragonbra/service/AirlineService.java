package in.dragonbra.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import in.dragonbra.model.Operator;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lngtr
 * @since 2017-10-17
 */
public class AirlineService {

    public static final String URL = "https://v4p4sz5ijk.execute-api.us-east-1.amazonaws.com/anbdata/airlines/designators/code-list";

    private final OkHttpClient client = new OkHttpClient();

    private final Gson gson = new Gson();

    private final Type type = new TypeToken<List<Operator>>() {
    }.getType();

    private final String apiKey;

    private Map<String, String> airlines = new HashMap<>();

    public AirlineService(String apiKey) {
        this.apiKey = apiKey;
    }

    private String downloadAirlineName(String icao) throws IOException {
        Request request = new Request.Builder()
                .url(URL + "?api_key=" + apiKey + "&operators=" + icao)
                .build();

        Response response = client.newCall(request).execute();

        List<Operator> operators = gson.fromJson(response.body().string(), type);

        if (operators.isEmpty()) {
            return null;
        }

        return operators.get(0).getOperatorName();
    }

    public String getAirlineName(String icao) throws IOException {
        if (airlines.containsKey(icao)) {
            return airlines.get(icao);
        }

        String name = downloadAirlineName(icao);

        airlines.put(icao, name);

        return name;
    }
}
