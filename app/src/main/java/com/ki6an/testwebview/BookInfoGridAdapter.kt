package com.ki6an.testwebview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView


class BookInfoGridAdapter(private val context: Context, private val bookInfoList: List<BookInfo>) :
    BaseAdapter() {
    private inner class ViewHolder {
        var title: TextView? = null
        var coverImage: ImageView? = null
    }

    override fun getCount(): Int {
        return bookInfoList.size
    }

    override fun getItem(i: Int): Any {
        return bookInfoList[i]
    }

    override fun getItemId(i: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView = convertView
        val viewHolder: ViewHolder
        if (convertView == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.book_item, parent, false)
            viewHolder = ViewHolder()
            viewHolder.title = convertView.findViewById<View>(R.id.txt_book_title) as TextView
            viewHolder.coverImage = convertView.findViewById<View>(R.id.img_cover) as ImageView
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }
        viewHolder.title!!.text = bookInfoList[position].title
        val isCoverImageNotExists = bookInfoList[position].isCoverImageNotExists
        if (!isCoverImageNotExists) { // Not searched for coverImage for this position yet. It may exist.
            val savedBitmap = bookInfoList[position].coverImageBitmap
            if (savedBitmap != null) {
                viewHolder.coverImage!!.setImageBitmap(savedBitmap)
            } else {
                val coverImageAsBytes = bookInfoList[position].coverImage
                val bitmap = decodeBitmapFromByteArray(coverImageAsBytes, 100, 200)
                bookInfoList[position].coverImageBitmap = bitmap
//                bookInfoList[position].coverImage = null     //<----  need to do some work here
                viewHolder.coverImage!!.setImageBitmap(bitmap)
            }
        } else { // Searched before and not found.
            viewHolder.coverImage!!.setImageResource(android.R.drawable.alert_light_frame)
        }
        return convertView
    }

    private fun decodeBitmapFromByteArray(
        coverImage: ByteArray,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(coverImage, 0, coverImage.size, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(coverImage, 0, coverImage.size, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize > reqHeight
                && halfWidth / inSampleSize > reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}