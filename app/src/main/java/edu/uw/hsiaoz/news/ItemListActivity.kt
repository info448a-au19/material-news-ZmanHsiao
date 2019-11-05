package edu.uw.hsiaoz.news

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.design.widget.Snackbar
import android.widget.TextView

import edu.uw.hsiaoz.news.dummy.DummyContent
import kotlinx.android.synthetic.main.activity_item_list.*
import kotlinx.android.synthetic.main.item_list_content.view.*
import kotlinx.android.synthetic.main.item_list.*

import android.util.Log
import kotlinx.android.parcel.Parcelize
import android.webkit.URLUtil
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_NEUTRAL
import android.graphics.Bitmap
import android.os.Parcelable
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.SearchView
import android.util.LruCache
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.net.URL


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [ItemDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class ItemListActivity : AppCompatActivity() {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false
    private var q: String = ""

    companion object {

        private lateinit var context: Context
        private lateinit var vQueue: RequestQueue

        fun setContext(con: Context) {
            context=con
        }
        fun setRequestQueue() {
            if (!::vQueue.isInitialized) {
                vQueue = Volley.newRequestQueue(context)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)
        setContext(this)
        setRequestQueue()
        setSupportActionBar(toolbar)
        toolbar.title = title
        handleIntent(intent)
        fab.setOnClickListener {
            if (q != "") {
                qnews(q)
            } else {
                topnews()
            }
        }

        if (item_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.item_detail_container, SimpleFragment())
                    .commit()
        }

    }

    fun handleJson(url: String): JsonObjectRequest {
        var jsonRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener<JSONObject> { response ->
                    setupRecyclerView(item_list, this, parseNewsAPI(response))
                },
                Response.ErrorListener { response ->
                    val alertDialog = AlertDialog.Builder(this).create()
                    alertDialog.setTitle("Alert")
                    alertDialog.setMessage(response.toString())
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
                    alertDialog.show()  })
        return jsonRequest
    }

    private fun setupRecyclerView(recyclerView: RecyclerView, context: Context, newsList: List<NewsArticle>) {
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, newsList, twoPane)
        if (!twoPane) {
            recyclerView.apply {
                layoutManager = GridLayoutManager(context, 2)
            }
        }
    }

    class SimpleItemRecyclerViewAdapter(private val parentActivity: ItemListActivity,
                                        private val values: List<NewsArticle>,
                                        private val twoPane: Boolean) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as NewsArticle
                if (twoPane) {
                    val fragment = ItemDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(ItemDetailFragment.ARG_ITEM_ID, item.imageUrl)
                            putBoolean("two", true)
                            putParcelable("news", item)
                        }
                    }
                    parentActivity.supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .addToBackStack("asdf")
                            .commit()
                } else {
                    val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                        putExtra(ItemDetailFragment.ARG_ITEM_ID, item.imageUrl)
                        putExtra("news", item)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = item.headline
            Glide.with(context).load(item.imageUrl).apply(RequestOptions().error(R.drawable.brokenlink)).into(holder.imageView)
            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.id_text
            val imageView: ImageView = view.imageid
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        (menu.findItem(R.id.action_search).actionView as SearchView).apply {
            // Assumes current activity is the searchable activity
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            setIconifiedByDefault(false) // Do not iconify the widget; expand it by default
        }

        val expandListener = object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                var api = getString(R.string.apikey)
                var url = "https://newsapi.org/v2/top-headlines?country=us&apiKey=$api"
                val jsonRequest = handleJson(url)
                vQueue.add(jsonRequest)
                return true // Return true to collapse action view
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                // Do something when expanded
                return true // Return true to expand action view
            }
        }
        searchItem.setOnActionExpandListener(expandListener)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleIntent(intent)
    }
    private fun handleIntent(intent: Intent) {

        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                qnews(query)
            }
        } else {
            topnews()
        }
    }
    private fun qnews(query: String) {
        var api = getString(R.string.apikey)
        var url = "https://newsapi.org/v2/everything?q=$query&apiKey=$api"
        val jsonRequest = handleJson(url)
        vQueue.add(jsonRequest)
        q = query
    }
    private fun topnews() {
        var api = getString(R.string.apikey)
        var url = "https://newsapi.org/v2/top-headlines?country=us&apiKey=$api"
        val jsonRequest = handleJson(url)
        vQueue.add(jsonRequest)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState!!.putString("query", q)
        super.onSaveInstanceState(outState)
    }
}

/**
 * A class representing a single news item (article). Can be parsed from
 * the News API aggregator
 * @author Joel Ross
 */
@Parcelize
data class NewsArticle (
        val headline:String,
        val description:String,
        val publishedTime:Long,
        val webUrl:String,
        val imageUrl:String?,
        val sourceId:String,
        val sourceName: String
) : Parcelable


const val NEWS_ARTICLE_TAG = "NewsArticle"

/**
 * Parses the query response from the News API aggregator
 * https://newsapi.org/
 */
fun parseNewsAPI(response: JSONObject):List<NewsArticle> {

    val stories = mutableListOf<NewsArticle>()

    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    try {
        val jsonArticles = response.getJSONArray("articles") //response.articles

        for (i in 0 until Math.min(jsonArticles.length(), 20)) { //stop at 20
            val articleItemObj = jsonArticles.getJSONObject(i)

            //handle image url
            var imageUrl:String? = articleItemObj.getString("urlToImage")
            if (imageUrl == "null" || !URLUtil.isValidUrl(imageUrl)) {
                imageUrl = null //make actual null value
            }

            //handle date
            val publishedTime = try {
                val pubDateString = articleItemObj.getString("publishedAt")
                if(pubDateString != "null")
                    formatter.parse(pubDateString).time
                else
                    0L //return 0
            } catch (e: ParseException) {
                Log.e(NEWS_ARTICLE_TAG, "Error parsing date", e) //Android log the error
                0L //return 0
            }

            //access source
            val sourceObj = articleItemObj.getJSONObject("source")

            val story = NewsArticle(
                    headline = articleItemObj.getString("title"),
                    webUrl = articleItemObj.getString("url"),
                    description = articleItemObj.getString("description"),
                    imageUrl = imageUrl,
                    publishedTime = publishedTime,
                    sourceId = sourceObj.getString("id"),
                    sourceName = sourceObj.getString("name")
            )

            stories.add(story)
        } //end for loop
    } catch (e: JSONException) {
        Log.e(NEWS_ARTICLE_TAG, "Error parsing json", e) //Android log the error
    }

    return stories
}