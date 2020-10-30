package com.ki6an.testwebview

import android.graphics.Bitmap

/**
 * Created by Mert on 08.09.2016.
 */
class BookInfo {
    var title: String? = null
    lateinit var coverImage: ByteArray
    var filePath: String? = null
    var isCoverImageNotExists = false
    var coverImageBitmap: Bitmap? = null
}