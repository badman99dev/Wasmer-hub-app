package com.movie.app.best.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class Zee5SearchContentDeserializer : JsonDeserializer<Zee5SearchContent> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Zee5SearchContent {
        val obj = json.asJsonObject
        val typeName = obj.get("__typename")?.takeIf { it.isJsonPrimitive }?.asString
        val item = context.deserialize<Zee5Item>(json, Zee5Item::class.java)
        return when (typeName) {
            "Episode" -> Zee5SearchContent(episode = item)
            "TVShow" -> Zee5SearchContent(tvShowDetails = item)
            else -> Zee5SearchContent(movie = item)
        }
    }
}
