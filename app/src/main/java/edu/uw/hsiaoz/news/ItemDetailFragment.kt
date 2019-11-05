package edu.uw.hsiaoz.news

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import edu.uw.hsiaoz.news.dummy.DummyContent
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*
import com.bumptech.glide.RequestManager



/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [ItemListActivity]
 * in two-pane mode (on tablets) or a [ItemDetailActivity]
 * on handsets.
 */
class ItemDetailFragment : Fragment() {

    /**
     * The dummy content this fragment is presenting.
     */
    private var item: NewsArticle? = null
    private var twopane = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            item = it.getParcelable("news")
            activity?.toolbar_layout?.title = " "
            if (!it.getBoolean("two")) {
                twopane = false
                Glide.with(this).load(it.getString(ARG_ITEM_ID)).apply(RequestOptions().error(R.drawable.brokenlink)).into(activity!!.image)
            }
        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.item_detail, container, false)

        // Show the dummy content as text in a TextView.
        item?.let {
            rootView.headline.text = it.headline
            rootView.item_detail.text = it.description
            rootView.source.text = it.sourceName
            if (twopane) {
                Glide.with(this).load(it.imageUrl).apply(RequestOptions().error(R.drawable.brokenlink)).into(rootView.image)
                twopane = true
            }

        }

        return rootView
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_ITEM_ID = "item_id"
    }
}
