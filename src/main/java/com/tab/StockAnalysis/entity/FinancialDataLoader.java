/*
package com.tab.StockAnalysis.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bson.Document;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

public class FinancialDataLoader {

    private static final String API_URL = "https://financialmodelingprep.com/api/v3/quote/SPY,QQQ,IWM,VXX,EEM,XLF,TQQQ,EFA,HYG,LQD?apikey=25HLtnCgiRCFX9fcryDyWJzOxPEeXfKx"; // REPLACE WITH YOUR ACTUAL API KEY
    private static final String MONGO_CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "financial_data";
    private static final String COLLECTION_NAME = "etf_quotes";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MongoClient mongoClient;
    private final MongoCollection<Document> collection;

    public FinancialDataLoader() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        objectMapper = new ObjectMapper();
        mongoClient = MongoClients.create(MONGO_CONNECTION_STRING);
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        collection = database.getCollection(COLLECTION_NAME);
    }

    public static void main(String[] args) {
        FinancialDataLoader loader = new FinancialDataLoader();
        // You can run this in a loop or schedule it
        // For demonstration, we'll run it once.
        // In a real application, you might use ScheduledExecutorService.
        loader.loadAndStoreData();
        loader.closeConnection();
    }

    public void loadAndStoreData() {
        try {
            List<Quote> apiQuotes = fetchQuotesFromApi();
            if (apiQuotes != null && !apiQuotes.isEmpty()) {
                System.out.println("Fetched " + apiQuotes.size() + " quotes from API.");
                for (Quote apiQuote : apiQuotes) {
                    processQuote(apiQuote);
                }
            } else {
                System.out.println("No data fetched from API or API returned empty response.");
            }
        } catch (IOException e) {
            System.err.println("Error fetching or processing data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Quote> fetchQuotesFromApi() throws IOException {
        Request request = new Request.Builder()
                .url(API_URL)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String jsonResponse = response.body().string();
            // System.out.println("API Raw Response: " + jsonResponse); // For debugging
            return objectMapper.readValue(jsonResponse, new TypeReference<List<Quote>>() {});
        }
    }

    private void processQuote(Quote apiQuote) {
        Document existingDocument = collection.find(eq("symbol", apiQuote.getSymbol())).first();
        Document newDocument = convertQuoteToDocument(apiQuote);

        if (existingDocument == null) {
            // New data, insert it
            collection.insertOne(newDocument);
            System.out.println("Inserted new document for: " + apiQuote.getSymbol());
        } else {
            // Data exists, check if it has changed
            // A simple comparison by checking if the new document is different from the old one
            // For complex objects, you might need a deeper comparison logic
            // Here, we convert existingDocument to Quote object for easier comparison if needed,
            // but a direct Document comparison for equality on relevant fields is also possible.
            // For simplicity, we'll compare the new document's JSON string representation with the old one's.
            // A more robust solution might involve comparing specific fields.

            // To avoid comparing _id and other MongoDB internal fields, let's compare relevant fields.
            // Or, more simply, just replace if you're ok with overwriting.
            // For this example, we'll replace the document if there's any change detected by `replaceOne`
            // and `upsert` is set to true (which handles both insert and update if filter matches).
            // A more exact "only update if changed" would involve fetching the old document,
            // converting it to a Quote object, comparing it to apiQuote, and then updating if different.

            // Convert existingDocument back to Quote for comparison
            Quote existingQuote = objectMapper.convertValue(existingDocument, Quote.class);

            if (!quotesAreEqual(existingQuote, apiQuote)) {
                // Data has changed, update it
                collection.replaceOne(eq("symbol", apiQuote.getSymbol()), newDocument, new ReplaceOptions().upsert(true));
                System.out.println("Updated document for: " + apiQuote.getSymbol());
            } else {
                System.out.println("No change detected for: " + apiQuote.getSymbol() + ", skipping update.");
            }
        }
    }

    // A simple equality check for relevant fields. You might want to make this more comprehensive.
    private boolean quotesAreEqual(Quote q1, Quote q2) {
        if (q1 == null || q2 == null) return false; // Or throw an exception
        return q1.getPrice() == q2.getPrice() &&
                q1.getChangesPercentage() == q2.getChangesPercentage() &&
                q1.getChange() == q2.getChange() &&
                q1.getDayLow() == q2.getDayLow() &&
                q1.getDayHigh() == q2.getDayHigh() &&
                q1.getYearHigh() == q2.getYearHigh() &&
                q1.getYearLow() == q2.getYearLow() &&
                q1.getMarketCap() == q2.getMarketCap() &&
                q1.getPriceAvg50() == q2.getPriceAvg50() &&
                q1.getPriceAvg200() == q2.getPriceAvg200() &&
                q1.getVolume() == q2.getVolume() &&
                q1.getAvgVolume() == q2.getAvgVolume() &&
                q1.getOpen() == q2.getOpen() &&
                q1.getPreviousClose() == q2.getPreviousClose() &&
                // Handle nulls for eps, pe, earningsAnnouncement, sharesOutstanding
                java.util.Objects.equals(q1.getEps(), q2.getEps()) &&
                java.util.Objects.equals(q1.getPe(), q2.getPe()) &&
                java.util.Objects.equals(q1.getEarningsAnnouncement(), q2.getEarningsAnnouncement()) &&
                java.util.Objects.equals(q1.getSharesOutstanding(), q2.getSharesOutstanding()) &&
                q1.getTimestamp() == q2.getTimestamp(); // Timestamp is also part of the data that can change
        // Note: 'name' and 'exchange' are typically static for ETFs, but you could include them if they might change.
    }

    private Document convertQuoteToDocument(Quote quote) {
        Document doc = new Document("symbol", quote.getSymbol())
                .append("name", quote.getName())
                .append("price", quote.getPrice())
                .append("changesPercentage", quote.getChangesPercentage())
                .append("change", quote.getChange())
                .append("dayLow", quote.getDayLow())
                .append("dayHigh", quote.getDayHigh())
                .append("yearHigh", quote.getYearHigh())
                .append("yearLow", quote.getYearLow())
                .append("marketCap", quote.getMarketCap())
                .append("priceAvg50", quote.getPriceAvg50())
                .append("priceAvg200", quote.getPriceAvg200())
                .append("exchange", quote.getExchange())
                .append("volume", quote.getVolume())
                .append("avgVolume", quote.getAvgVolume())
                .append("open", quote.getOpen())
                .append("previousClose", quote.getPreviousClose())
                .append("timestamp", quote.getTimestamp());

        // Append nullable fields only if they are not null
        if (quote.getEps() != null) {
            doc.append("eps", quote.getEps());
        }
        if (quote.getPe() != null) {
            doc.append("pe", quote.getPe());
        }
        if (quote.getEarningsAnnouncement() != null) {
            doc.append("earningsAnnouncement", quote.getEarningsAnnouncement());
        }
        if (quote.getSharesOutstanding() != null) {
            doc.append("sharesOutstanding", quote.getSharesOutstanding());
        }

        return doc;
    }

    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed.");
        }
    }
}*/
