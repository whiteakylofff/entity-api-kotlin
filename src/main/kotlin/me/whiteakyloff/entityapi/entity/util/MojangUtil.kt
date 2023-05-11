package me.whiteakyloff.entityapi.entity.util

import com.google.gson.JsonParser

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object MojangUtil
{
    private val skinMap = HashMap<String, Skin>()

    data class Skin(
        val timestamp: Long,
        val skinName: String,
        val playerUUID: String,
        val value: String,
        val signature: String
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - this.timestamp > TimeUnit.HOURS.toMillis(12L)
        }
    }

    @Throws(IOException::class)
    fun getSkinTextures(name: String): Skin {
        val cachedSkin = skinMap[name]

        if (cachedSkin != null && !cachedSkin.isExpired()) {
            return cachedSkin
        }
        val playerUUID = JsonParser()
            .parse(this.readURL(UUID_URL_STRING + name)).asJsonObject
            .get("id").asString
        val skinURL = this.readURL("$SKIN_URL_STRING$playerUUID?unsigned=false")

        val textureProperty = JsonParser()
            .parse(skinURL).asJsonObject
            .get("properties").asJsonArray
            .get(0).asJsonObject
        val texture = textureProperty.get("value").asString
        val signature = textureProperty.get("signature").asString

        Skin(System.currentTimeMillis(), name, playerUUID, texture, signature).let {
            this.skinMap[name] = it
            return it
        }
    }

    @Throws(IOException::class)
    private fun readURL(url: String): String = (URL(url).openConnection() as HttpURLConnection)
        .apply {
            this.requestMethod = "GET"
            this.readTimeout = 5000
            this.connectTimeout = 5000
            this.doOutput = true
            this.setRequestProperty("User-Agent", "entity-api") }
        .run {
            val output = StringBuilder()
            val `in` = BufferedReader(InputStreamReader(this.inputStream))

            while (`in`.ready()) {
                output.append(`in`.readLine())
            }
            `in`.close()
            return output.toString() }

    private const val UUID_URL_STRING = "https://api.mojang.com/users/profiles/minecraft/"
    private const val SKIN_URL_STRING = "https://sessionserver.mojang.com/session/minecraft/profile/"
}