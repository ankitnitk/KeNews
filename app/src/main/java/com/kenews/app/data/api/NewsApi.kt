package com.kenews.app.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

data class Article(
    val id: Int,
    val url: String,
    val title: String,
    val summary: String,
    val category: String,
    val source: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("published_at") val publishedAt: String,
)

data class ArticlesResponse(val articles: List<Article>, val count: Int)
data class CategoriesResponse(val categories: List<String>)

interface NewsApi {
    @GET("articles")
    suspend fun getArticles(
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
    ): ArticlesResponse

    @GET("categories")
    suspend fun getCategories(): CategoriesResponse
}
