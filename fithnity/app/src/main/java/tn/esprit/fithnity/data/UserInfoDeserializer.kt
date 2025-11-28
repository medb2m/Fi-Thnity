package tn.esprit.fithnity.data

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Custom deserializer for UserInfo that handles both:
 * - Object format: { "_id": "...", "name": "...", ... }
 * - String format: "userId" (when user is not populated)
 */
class UserInfoDeserializer : JsonDeserializer<UserInfo> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): UserInfo? {
        if (json == null || json.isJsonNull) {
            return null
        }

        // If it's a string (user ID), return a minimal UserInfo with just the ID
        if (json.isJsonPrimitive && json.asJsonPrimitive.isString) {
            return UserInfo(
                _id = json.asString,
                name = null,
                email = null,
                phoneNumber = null,
                photoUrl = null,
                isVerified = null,
                emailVerified = null,
                rating = null,
                totalRides = null
            )
        }

        // If it's an object, deserialize normally
        if (json.isJsonObject) {
            val gson = Gson()
            return gson.fromJson(json, UserInfo::class.java)
        }

        return null
    }
}

