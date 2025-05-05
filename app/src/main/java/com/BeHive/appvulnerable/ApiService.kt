import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Define la interfaz de la API
interface ApiService {
    @GET("endpoint")
    suspend fun getData(): Response<DataModel>
}

// Configura Retrofit
object RetrofitClient {
    private const val BASE_URL = "https://api.example.com/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}