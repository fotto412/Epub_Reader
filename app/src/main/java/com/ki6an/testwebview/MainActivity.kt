package com.ki6an.testwebview


import android.app.SearchManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.util.Base64
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.github.mertakdut.BookSection
import com.github.mertakdut.CssStatus
import com.github.mertakdut.Reader
import com.github.mertakdut.exception.OutOfPagesException
import com.github.mertakdut.exception.ReadingException
import com.ki6an.testwebview.PageFragment.OnFragmentReadyListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), OnFragmentReadyListener {

    private var reader: Reader? = null
    private var mViewPager: ViewPager? = null

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private var pageCount = Int.MAX_VALUE
    private var pxScreenWidth = 0
    private var isPickedWebView = false
    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private var isSkippedToPage = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        pxScreenWidth = resources.displayMetrics.widthPixels
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        mViewPager = findViewById<View>(R.id.container) as ViewPager
        mViewPager!!.offscreenPageLimit = 0
        mViewPager!!.adapter = mSectionsPagerAdapter

        if (intent != null && intent.extras != null) {

            val filePath = intent.extras!!.getString("filePath")
            isPickedWebView = intent.extras!!.getBoolean("isWebView")
            try {
                reader = Reader()

                // Setting optionals once per file is enough.
                reader!!.setMaxContentPerSection(1250)
                reader!!.setCssStatus(if (isPickedWebView) CssStatus.INCLUDE else CssStatus.OMIT)
                reader!!.setIsIncludingTextContent(true)
                reader!!.setIsOmittingTitleTag(true)

                // This method must be called before readSection.
                reader!!.setFullContent(filePath)

                if (reader!!.isSavedProgressFound) {
                    val lastSavedPage = reader!!.loadProgress()
                    mViewPager!!.currentItem = lastSavedPage
                }
            } catch (e: ReadingException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onFragmentReady(position: Int): View? {
        var bookSection: BookSection? = null
        try {
            bookSection = reader?.readSection(position)
        } catch (e: ReadingException) {
            e.printStackTrace()
            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
        } catch (e: OutOfPagesException) {
            e.printStackTrace()
            pageCount = e.pageCount
            if (isSkippedToPage) {
                Toast.makeText(
                    this@MainActivity,
                    "Max page number is: $pageCount",
                    Toast.LENGTH_LONG
                ).show()
            }
            mSectionsPagerAdapter!!.notifyDataSetChanged()
        }
        isSkippedToPage = false

        return if (bookSection != null) {
            setFragmentView(
                isPickedWebView,
                bookSection.sectionContent,
                "text/html",
                "UTF-8")
        }else null

    }

        override fun onCreateOptionsMenu(menu: Menu): Boolean {
            menuInflater.inflate(R.menu.menu_main, menu)

            val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
            searchMenuItem = menu.findItem(R.id.action_search)

            searchView = MenuItemCompat.getActionView(searchMenuItem) as SearchView
            searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (query != null && query != "") {
                        if (TextUtils.isDigitsOnly(query)) {
                            loseFocusOnSearchView()
                            val skippingPage = Integer.valueOf(query)
                            if (skippingPage >= 0) {
                                isSkippedToPage = true
                                mViewPager!!.currentItem = skippingPage
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Page number can't be less than 0",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            loseFocusOnSearchView()
                            Toast.makeText(
                                this@MainActivity,
                                "Only numbers are allowed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return true
                    }
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }
            })
            return true
        }

        override fun onBackPressed() {
            if (!searchView?.isIconified!!) {
                loseFocusOnSearchView()
            } else {
                super.onBackPressed()
            }
        }

        override fun onStop() {
            super.onStop()
            try {
                reader!!.saveProgress(mViewPager!!.currentItem)
                Toast.makeText(
                    this@MainActivity,
                    "Saved page: " + mViewPager!!.currentItem + "...",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: ReadingException) {
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Progress is not saved: " + e.message,
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: OutOfPagesException) {
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Progress is not saved. Out of Bounds. Page Count: " + e.pageCount,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            return if (id == R.id.action_search) {
                true
            } else super.onOptionsItemSelected(item)
        }

        private  fun setFragmentView(
            isContentStyled: Boolean,
            data: String,
            mimeType: String,
            encoding: String
        ): View {
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return if (isContentStyled) {
                val webView = WebView(this@MainActivity)
                webView.loadDataWithBaseURL(null, data, mimeType, encoding, null)

                webView.layoutParams = layoutParams
                webView
            } else {
                val scrollView = ScrollView(this@MainActivity)
                scrollView.layoutParams = layoutParams
                val textView = TextView(this@MainActivity)
                textView.layoutParams = layoutParams
                textView.text = Html.fromHtml(data, { source ->
                    val imageAsStr = source.substring(source.indexOf(";base64,") + 8)
                    val imageAsBytes = Base64.decode(imageAsStr, Base64.DEFAULT)
                    val imageAsBitmap =
                        BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.size)
                    val imageWidthStartPx = (pxScreenWidth - imageAsBitmap.width) / 2
                    val imageWidthEndPx = pxScreenWidth - imageWidthStartPx
                    val imageAsDrawable: Drawable = BitmapDrawable(resources, imageAsBitmap)
                    imageAsDrawable.setBounds(
                        imageWidthStartPx,
                        0,
                        imageWidthEndPx,
                        imageAsBitmap.height
                    )
                    imageAsDrawable
                }, null)
                val pxPadding = dpToPx(12)
                textView.setPadding(pxPadding, pxPadding, pxPadding, pxPadding)
                scrollView.addView(textView)
                scrollView
            }
        }

        private fun loseFocusOnSearchView() {
            searchView?.setQuery("", false)
            searchView?.clearFocus()
            searchView?.isIconified = true
            MenuItemCompat.collapseActionView(searchMenuItem)
        }

        private fun dpToPx(dp: Int): Int {
            val displayMetrics = resources.displayMetrics
            return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
        }

        inner class SectionsPagerAdapter internal constructor(fm: FragmentManager?) :
            FragmentStatePagerAdapter(fm!!) {
            override fun getCount(): Int {
                return pageCount
            }

            override fun getItem(position: Int): Fragment {
                // getItem is called to instantiate the fragment for the given page.
                return PageFragment.newInstance(position)
            }
        }
    }
