package tn.esprit.fithnity.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "http://72.61.145.239:9090/" // Emulator localhost - TODO: Change for VPS deployment
    private const val TAG = "NetworkModule"

    // Create logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Create OkHttp client with logging, timeouts, and retry
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)  // Increased for large uploads
        .readTimeout(60, TimeUnit.SECONDS)     // Increased for large uploads
        .writeTimeout(60, TimeUnit.SECONDS)    // Increased for large uploads
        .retryOnConnectionFailure(true)         // Enable retry on connection failure
        .build()

    // Create Gson with custom deserializer for UserInfo
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(UserInfo::class.java, UserInfoDeserializer())
        .create()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val authApi: AuthApiService get() = retrofit.create(AuthApiService::class.java)
    val userApi: UserApiService get() = retrofit.create(UserApiService::class.java)
    val rideApi: RideApiService get() = retrofit.create(RideApiService::class.java)
    val communityApi: CommunityApiService get() = retrofit.create(CommunityApiService::class.java)
    val chatApi: ChatApiService get() = retrofit.create(ChatApiService::class.java)
    val notificationApi: NotificationApiService get() = retrofit.create(NotificationApiService::class.java)
}






