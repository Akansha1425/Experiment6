package com.example.restapiapp  // Change to your actual package name

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

// ─── Data class ────────────────────────────────────────────────────────────
data class Product(
    val title: String,
    val price: Double,
    val category: String,
    val image: String,
    val rating: Double,
    val ratingCount: Int
)

// ─── Adapter ────────────────────────────────────────────────────────────────
class ProductAdapter(private val products: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProduct: ImageView = view.findViewById(R.id.ivProduct)
        val tvTitle: TextView    = view.findViewById(R.id.tvTitle)
        val tvPrice: TextView    = view.findViewById(R.id.tvPrice)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvRating: TextView   = view.findViewById(R.id.tvRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val p = products[position]
        holder.tvTitle.text    = p.title
        holder.tvPrice.text    = "Price: $${p.price}"
        holder.tvCategory.text = "Category: ${p.category}"
        holder.tvRating.text   = "★ ${p.rating}  (${p.ratingCount} reviews)"

        // Load image using Glide
        Glide.with(holder.itemView.context)
            .load(p.image)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(holder.ivProduct)
    }

    override fun getItemCount(): Int = products.size
}

// ─── MainActivity ───────────────────────────────────────────────────────────
class MainActivity : AppCompatActivity() {

    private val API_URL = "https://fakestoreapi.com/products"
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar  = findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchProducts()
    }

    private fun fetchProducts() {
        thread {
            try {
                val url        = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout    = 10000
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader   = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Parse JSON array
                    val jsonArray   = JSONArray(response.toString())
                    val productList = mutableListOf<Product>()

                    for (i in 0 until jsonArray.length()) {
                        val obj      = jsonArray.getJSONObject(i)
                        val ratingObj = obj.getJSONObject("rating")
                        productList.add(
                            Product(
                                title      = obj.getString("title"),
                                price      = obj.getDouble("price"),
                                category   = obj.getString("category"),
                                image      = obj.getString("image"),      // ← image URL
                                rating     = ratingObj.getDouble("rate"),
                                ratingCount = ratingObj.getInt("count")
                            )
                        )
                    }

                    runOnUiThread {
                        progressBar.visibility  = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.adapter    = ProductAdapter(productList)
                    }

                } else {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Server error: ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}