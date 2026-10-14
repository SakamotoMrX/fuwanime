package com.example.data.api

import com.example.data.model.AnimeApiResponse
import com.example.data.model.AnimeDetailData
import com.example.data.model.EpisodeListContainer
import com.example.data.model.MovieListContainer
import com.example.data.model.StreamNewData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface AnimeApiService {

    @GET("3/2/explore/movie")
    suspend fun exploreMovie(
        @Query("page") page: Int = 0,
        @Query("sort") sort: String = "views",
        @Query("keyword") keyword: String = "",
        @Query("genre_in") genreIn: String? = null,
        @Query("studio_in") studioIn: String? = null
    ): AnimeApiResponse<MovieListContainer>

    @GET("3/2/schedule/data")
    suspend fun getScheduleData(
        @Query("day") day: String
    ): AnimeApiResponse<MovieListContainer>

    @GET("3/2/movie/detail/{id}")
    suspend fun getMovieDetail(
        @Path("id") animeId: String,
        @Query("id_user") idUser: String = DEFAULT_USER_ID,
        @Query("key_client") keyClient: String = DEFAULT_CLIENT_KEY
    ): AnimeApiResponse<AnimeDetailData>

    @GET("3/2/movie/episode/{id}")
    suspend fun getMovieEpisodes(
        @Path("id") animeId: String,
        @Query("id_user") idUser: String = DEFAULT_USER_ID,
        @Query("key_client") keyClient: String = DEFAULT_CLIENT_KEY,
        @Query("page") page: Int = 0,
        @Query("search") search: String = ""
    ): AnimeApiResponse<EpisodeListContainer>

    @GET("3/2/episode/streamnew/{id}")
    suspend fun getEpisodeStreamNew(
        @Path("id") episodeId: String
    ): AnimeApiResponse<StreamNewData>

    companion object {
        const val BASE_URL = "https://xyz-api.animein.net/"
        const val DEFAULT_USER_ID = "396454"
        const val DEFAULT_CLIENT_KEY = "Di63beJZx6l7uPYKXZqzEdANGhsi2e77AFbUB8rgqSzXiRh2pi"
        const val USER_AGENT = "Dalvik/2.1.0 (Linux; U; Android 10; Infinix X680 Build/QP1A.190711.020)"

        fun create(): AnimeApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            // Do NOT add manual Accept-Encoding header, so OkHttp handles transparent GZIP decompression
            val client = OkHttpClient.Builder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Connection", "Keep-Alive")
                        .header("Accept", "application/json, text/plain, */*")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
                .build()
                .create(AnimeApiService::class.java)
        }
    }
}
