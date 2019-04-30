package org.kiwix.kiwixmobile.downloader

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.kiwix.kiwixmobile.R.id
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book

class OldDownloadAdapter(
  data: LinkedHashMap<Int, Book>,
  private val downloadFragment: DownloadFragment
) {

  private var mData = LinkedHashMap<Int, Book>()
  private var mKeys: Array<Int>? = null

  init {
    mData = data
    mKeys = mData.keys.toTypedArray()
  }

  fun complete(notificationID: Int) {
    /*   val fileName = getFileName(mDownloadFiles[mKeys!![position]])
       run {
           val completeSnack = Snackbar.make(view!!, resources.getString(R.string.download_complete_snackbar), Snackbar.LENGTH_LONG)
           completeSnack.setAction(resources.getString(R.string.open)) { v -> zimManageActivity!!.finishResult(fileName) }.setActionTextColor(resources.getColor(R.color.white)).show()
       }
       val zimFileSelectFragment = zimManageActivity!!.mSectionsPagerAdapter.getItem(0) as ZimFileSelectFragment
       zimFileSelectFragment.addBook(fileName)
  */
  }

  fun updateProgress(
    progress: Int,
    notificationID: Int
  ) {
    if (downloadFragment.isAdded) {
//                val position = Arrays.asList(*mKeys!!).indexOf(notificationID)
//                val viewGroup = listView.getChildAt(position - listView.firstVisiblePosition) as ViewGroup
//                        ?: return
//                val timeRemaining = viewGroup.findViewById<TextView>(R.id.time_remaining)
//                val secLeft = LibraryFragment.mService.timeRemaining.get(mKeys!![position], -1)
//                if (secLeft != -1)
//                    timeRemaining.text = toHumanReadableTime(secLeft)
    }
  }

  fun getView(
    position: Int,
    convertView: View?,
    parent: ViewGroup
  ): View {
    var convertView = convertView
    // Get the data item for this position
    // Check if an existing view is being reused, otherwise inflate the view
    if (convertView == null) {
//                convertView = LayoutInflater.from(faActivity).inflate(R.layout.download_item, parent, false)
    }
    mKeys = mData.keys.toTypedArray()
//
//            pause.setOnClickListener { v ->
//                val newPlayPauseState = if (LibraryFragment.mService.downloadStatus.get(mKeys!![position]) == DownloadService.PLAY) DownloadService.PAUSE else DownloadService.PLAY
//
//                if (newPlayPauseState == DownloadService.PLAY && KiwixMobileActivity.wifiOnly && !NetworkUtils.isWiFi(context!!)) {
//                    showNoWiFiWarning(context, Runnable { setPlayState(pause, position, newPlayPauseState) })
//                    return@setOnClickListener
//                }
//
//                timeRemaining.text = ""
//
//                setPlayState(pause, position, newPlayPauseState)
//            }


//            }

    // Return the completed view to render on screen
    return convertView!!
  }

}