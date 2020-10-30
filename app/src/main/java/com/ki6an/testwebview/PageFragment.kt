package com.ki6an.testwebview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment


class PageFragment : Fragment() {
    private var onFragmentReadyListener: OnFragmentReadyListener? = null

    interface OnFragmentReadyListener {
        fun onFragmentReady(position: Int): View?
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onFragmentReadyListener =
            context as OnFragmentReadyListener? //Activity version is deprecated.
    }

    override fun onDestroy() {
        super.onDestroy()
        onFragmentReadyListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView: View = inflater.inflate(R.layout.fragment_display, container, false)
        val mainLayout = rootView.findViewById<View>(R.id.fragment_main_layout) as RelativeLayout
        val view = arguments?.getInt(ARG_TAB_POSITON)?.let {
            onFragmentReadyListener!!.onFragmentReady(
                it
            )
        }
        if (view != null) {
            mainLayout.addView(view)
        }
        return mainLayout
    }

    companion object {

        private const val ARG_TAB_POSITON = "tab_position"

        fun newInstance(tabPosition: Int): PageFragment {
            val fragment = PageFragment()
            val args = Bundle()
            args.putInt(ARG_TAB_POSITON, tabPosition)
            fragment.arguments = args
            return fragment
        }
    }
}