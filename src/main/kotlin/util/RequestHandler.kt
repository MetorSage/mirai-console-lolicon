/*
 * Copyright (c) 2020-2023 Samarium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package io.github.samarium150.mirai.plugin.lolicon.util

import io.github.samarium150.mirai.plugin.lolicon.MiraiConsoleLolicon
import io.github.samarium150.mirai.plugin.lolicon.config.PluginConfig
import io.github.samarium150.mirai.plugin.lolicon.data.RequestBody
import io.github.samarium150.mirai.plugin.lolicon.data.ResponseBody
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import javax.imageio.ImageIO


internal suspend fun getAPIResponse(body: RequestBody): ResponseBody {
    return MiraiConsoleLolicon.client.post("https://api.lolicon.app/setu/v2") {
        contentType(ContentType.Application.Json)
        setBody(body)
    }.body()
}

fun rotateImageByDegrees(img: BufferedImage, degree: Int): BufferedImage? {
    val rads = Math.toRadians(degree.toDouble())
    val w = img.width
    val h = img.height
    val rotatedImage = BufferedImage(w, h, img.type)
    val g2d = rotatedImage.createGraphics() as Graphics2D
    val at = AffineTransform()
    at.rotate(rads, (w / 2).toDouble(), (h / 2).toDouble())
    g2d.transform = at
    g2d.drawImage(img, 0, 0, null)
    return rotatedImage
}

internal suspend fun downloadImage(url: String): InputStream {
    val response: HttpResponse = MiraiConsoleLolicon.client.get(url)
    val imageBytes: ByteArray = response.body()
    val bufferedImage = ImageIO.read(ByteArrayInputStream(imageBytes))
    val rotatedImage = rotateImageByDegrees(bufferedImage, 180)
    val out = ByteArrayOutputStream()
    ImageIO.write(rotatedImage, "png", out)
    val result: ByteArray = out.toByteArray()
    if (PluginConfig.save) {
        val urlPaths = url.split("/")
        val file = cacheFolder.resolve(urlPaths[urlPaths.lastIndex])
        file.writeBytes(result)
    }
    return result.inputStream()
}

internal suspend fun getImageInputStream(url: String): InputStream {
    return if (PluginConfig.save && PluginConfig.cache) {
        val paths = url.split("/")
        val path = "$cacheFolder/${paths[paths.lastIndex]}"
        val cache = File(System.getProperty("user.dir") + path)
        if (cache.exists()) cache.inputStream()
        else downloadImage(url)
    } else downloadImage(url)
}
