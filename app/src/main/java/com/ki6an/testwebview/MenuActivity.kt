package com.ki6an.testwebview

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.github.mertakdut.Reader
import com.github.mertakdut.exception.ReadingException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*


class MenuActivity() : AppCompatActivity() {
    private var progressBar: ProgressBar? = null
    private var occurredException: Exception? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        val toolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        (findViewById<View>(R.id.grid_book_info) as GridView).onItemClickListener =
            OnItemClickListener { adapterView, view, i, l ->
                val clickedItemFilePath = (adapterView.adapter.getItem(i) as BookInfo).filePath
                askForWidgetToUse(clickedItemFilePath)
            }


        progressBar = findViewById<View>(R.id.progressbar) as ProgressBar
        progressBar!!.visibility = View.VISIBLE

        lifecycleScope.launch() {

            val bookInfoList = doOnBackgroundThread()

            Log.i("in menu FINAL ", bookInfoList.toString())

            progressBar!!.visibility = View.GONE

            if (bookInfoList != null) {
                val adapter = BookInfoGridAdapter(this@MenuActivity, bookInfoList)
                (findViewById<View>(R.id.grid_book_info) as GridView).adapter = adapter
            }
            if (occurredException != null) {
                Toast.makeText(this@MenuActivity, occurredException!!.message, Toast.LENGTH_LONG)
                    .show()
            }

        }

    }


    private fun searchForPdfFiles(): List<BookInfo>? {
        val isSDPresent = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        var bookInfoList: MutableList<BookInfo>? = null
        if (isSDPresent) {

            Log.i("in Main", "is sd present $isSDPresent")

            bookInfoList = ArrayList()
            val files = getListFiles(File(Environment.getExternalStorageDirectory().absolutePath))
            val sampleFile = getFileFromAssets("nn.epub")
            files.add(0, sampleFile)
            for (file in files) {
                val bookInfo = BookInfo()
                bookInfo.title = file.name
                bookInfo.filePath = file.path

                Log.i("in Main", "the title and file path are  ${file.name} and ${file.path}")

                bookInfoList.add(bookInfo)

            }
        }

        Log.i("in Main", "the books list is ${bookInfoList.toString()}")
        return bookInfoList
    }

    private fun getFileFromAssets(fileName: String): File {
//        val file = File("$cacheDir/$fileName")

        val file = File(cacheDir, fileName)
        if (!file.exists()) try {
            val `is` = assets.open(fileName)
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            val fos = FileOutputStream(file)
            fos.write(buffer)
            fos.close()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return file
    }

    private fun getListFiles(parentDir: File): MutableList<File> {
        val inFiles = ArrayList<File>()
        val files = parentDir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    inFiles.addAll(getListFiles(file))
                } else {
                    if (file.name.endsWith(".epub")) {
                        inFiles.add(file)
                    }
                }
            }
        }
        return inFiles
    }

    private fun askForWidgetToUse(filePath: String?) {

        val intent = Intent(this@MenuActivity, MainActivity::class.java)
        intent.putExtra("filePath", filePath)
        AlertDialog.Builder(this@MenuActivity)
            .setTitle("Pick your widget")
            .setMessage("TextView or WebView?")
            .setPositiveButton(
                "TextView"
            ) { _, _ ->
                intent.putExtra("isWebView", false)
                startActivity(intent)
            }
            .setNegativeButton(
                "WebView"
            ) { _, _ ->
                intent.putExtra("isWebView", true)
                startActivity(intent)
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }


    private suspend fun doOnBackgroundThread(): List<BookInfo>?  = withContext(IO){
        val bookInfoList = searchForPdfFiles()
        val reader = Reader()
        if (bookInfoList != null) {
            for (bookInfo in bookInfoList) {

                Log.i("in Main", "the bookinfo parameter $bookInfo")

                try {
                    reader.setInfoContent(bookInfo.filePath)
//
                    val title = reader.infoPackage.metadata.title

                    Log.i("in Main", "the title is XXXXXXXXX $title")

                    if (title != null && title != "") {
                        bookInfo.title = reader.infoPackage.metadata.title
                    } else { // If title doesn't exist, use fileName instead.
                        val dotIndex = bookInfo.title!!.lastIndexOf('.')
                        bookInfo.title = bookInfo.title!!.substring(0, dotIndex)
                    }
                    bookInfo.coverImage = reader.coverImage
                } catch (e: ReadingException) {
                    occurredException = e
                    e.printStackTrace()
                }
            }
        }

        Log.i("in Main", "at the end of the suspend function and the return value is $bookInfoList")

        return@withContext bookInfoList
    }


}