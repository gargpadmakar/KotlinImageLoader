package com.padmakar.imageloader.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.padmakar.imageloader.R
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.data.TestImages
import com.padmakar.imageloader.view.loadImage

class ViewDemoActivity : ComponentActivity() {

    private var rapidIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_demo)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootViewDemo)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupSingleDemo()
        setupDownsampleDemo()
        setupRapidDemo()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)
        btnBack?.setOnClickListener { finish() }
    }

    private fun setupSingleDemo() {
        val ivSingle = findViewById<ImageView>(R.id.ivSingleDemo)
        val btnSuccess = findViewById<Button>(R.id.btnLoadSuccess)
        val btnError = findViewById<Button>(R.id.btnLoad404)

        fun load(url: String) {
            val request = ImageRequest.Builder(url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .build()
            ivSingle.loadImage(request)
        }

        load(TestImages.SINGLE_SUCCESS_URL)

        btnSuccess.setOnClickListener { load(TestImages.SINGLE_SUCCESS_URL) }
        btnError.setOnClickListener { load(TestImages.INVALID_ERROR_URL) }
    }

    private fun setupDownsampleDemo() {
        val ivDownsample = findViewById<ImageView>(R.id.ivDownsampleDemo)
        val tvStatus = findViewById<TextView>(R.id.tvDownsampleStatus)

        val request = ImageRequest.Builder(TestImages.LARGE_IMAGE_URL)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_error)
            .build()

        ivDownsample.loadImage(request)
        tvStatus.text = "Downsampling from 2500x1667 to measured 120x80 dp"
    }

    private fun setupRapidDemo() {
        val ivRapid = findViewById<ImageView>(R.id.ivRapidDemo)
        val tvStatus = findViewById<TextView>(R.id.tvRapidStatus)
        val btnToggle = findViewById<Button>(R.id.btnRapidToggle)

        fun loadCurrent() {
            val (id, url) = TestImages.RAPID_TEST_IMAGES[rapidIndex]
            tvStatus.text = "Requested Target ID: $id"

            val request = ImageRequest.Builder(url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .build()

            ivRapid.loadImage(request)
        }

        loadCurrent()

        btnToggle.setOnClickListener {
            rapidIndex = (rapidIndex + 1) % TestImages.RAPID_TEST_IMAGES.size
            loadCurrent()
        }
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvImageList)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ImageRecyclerAdapter(TestImages.LIST_IMAGES)
    }

    private class ImageRecyclerAdapter(
        private val items: List<Pair<Int, String>>
    ) : RecyclerView.Adapter<ImageRecyclerAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage: ImageView = view.findViewById(R.id.ivRecyclerImage)
            val tvIndex: TextView = view.findViewById(R.id.tvRecyclerIndex)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recycler_image, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (index, url) = items[position]
            holder.tvIndex.text = "Item #$index"

            val request = ImageRequest.Builder(url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .build()

            holder.ivImage.loadImage(request)
        }

        override fun getItemCount(): Int = items.size
    }
}
