package com.app.sample.model;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Json {

    private static GsonBuilder getGsonBuilder(TypeDeserializer... typeDeserializers) {
        GsonBuilder builder = new GsonBuilder();
        if (typeDeserializers != null) {
            for (TypeDeserializer typeDeserializer : typeDeserializers) {
                builder.registerTypeAdapter(typeDeserializer.getType(), typeDeserializer.getDeserializer());
            }
        }
        builder.setDateFormat("MMM dd yyyy HH:mm");
        return builder;
    }

    private static Gson getGson(TypeDeserializer... typeDeserializers) {
        return getGsonBuilder(typeDeserializers).create();
    }

    public static String stringify(Object src) {
        return getGson().toJson(src);
    }

    public static <T> T parse(String json, Class<T> type, TypeDeserializer... typeDeserializers) {
        return getGson(typeDeserializers).fromJson(json, type);
    }

    public static <T> List<T> parseList(String json, Type type, TypeDeserializer... typeDeserializers) {
        Type listType = getListType();
        ListTypeAdapter<T> listTypeAdapter = newListTypeAdapter(type);
        GsonBuilder gsonBuilder = getGsonBuilder(typeDeserializers);
        gsonBuilder.registerTypeAdapter(listType, listTypeAdapter);
        Gson gson = gsonBuilder.create();

        try {
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            Log.e("Error transforming list", e.getMessage());
            return new ArrayList<T>();
        }
    }

    /**
     * Returns a new list type.
     *
     * @return Type
     */
    private static Type getListType() {
        return getListTypeToken().getType();
    }

    /**
     * Returns a new list object token based on the containing class generic object attribute.
     *
     * @return TypeToken
     */
    private static <T> TypeToken<List<T>> getListTypeToken() {
        return new TypeToken<List<T>>() {
        };
    }

    /**
     * Builds a new list object com.foodly.adapter for the given target class.
     *
     * @param type Type
     * @return ListTypeAdapter
     */
    private static <T> ListTypeAdapter<T> newListTypeAdapter(Type type) {
        return new ListTypeAdapter<T>(type);
    }

    public abstract static class TypeDeserializer {
        private Type type;
        private JsonDeserializer<?> deserializer;

        public TypeDeserializer(Type type, JsonDeserializer<?> deserializer) {
            this.type = type;
            this.deserializer = deserializer;
        }

        public Type getType() {
            return type;
        }

        public JsonDeserializer<?> getDeserializer() {
            return deserializer;
        }
    }

    private static class ListTypeAdapter<T> implements JsonDeserializer<List<T>> {

        private Type type;

        public ListTypeAdapter(Type newType) {
            this.type = newType;
        }

        @Override
        public List<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) {
            List<T> list = new ArrayList<>();
            if (json.isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray()) {
                    list.add(this.deserialize(ctx, element));
                }
            } else if (json.isJsonObject()) {
                list.add(this.deserialize(ctx, json));
            } else {
                throw new RuntimeException("Unexpected JSON type: " + json.getClass());
            }
            return list;
        }

        private T deserialize(JsonDeserializationContext ctx, JsonElement element) {
            return ctx.deserialize(element, this.type);
        }
    }

}
