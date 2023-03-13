/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.nav.destination.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.coreMainActivity
import org.kiwix.kiwixmobile.core.extensions.setBottomMarginToFragmentContainerView
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SimpleRecyclerViewScrollListener
import org.kiwix.kiwixmobile.core.utils.SimpleRecyclerViewScrollListener.Companion.SCROLL_DOWN
import org.kiwix.kiwixmobile.core.utils.SimpleRecyclerViewScrollListener.Companion.SCROLL_UP
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.databinding.FragmentDestinationLibraryBinding
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import javax.inject.Inject

private const val WAS_IN_ACTION_MODE = "WAS_IN_ACTION_MODE"

class LocalLibraryFragment : BaseFragment() {

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var dialogShower: DialogShower

  private var actionMode: ActionMode? = null
  private val disposable = CompositeDisposable()
  private var fragmentDestinationLibraryBinding: FragmentDestinationLibraryBinding? = null

  private val zimManageViewModel by lazy {
    requireActivity().viewModel<ZimManageViewModel>(viewModelFactory)
  }

  private val bookDelegate: BookOnDiskDelegate.BookDelegate by lazy {
    BookOnDiskDelegate.BookDelegate(
      sharedPreferenceUtil,
      { offerAction(RequestNavigateTo(it)) },
      { offerAction(RequestSelect(it)) }
    )
  }
  private val booksOnDiskAdapter: BooksOnDiskAdapter by lazy {
    BooksOnDiskAdapter(bookDelegate, BookOnDiskDelegate.LanguageDelegate)
  }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    LanguageUtils(requireActivity())
      .changeFont(requireActivity(), sharedPreferenceUtil)
    fragmentDestinationLibraryBinding = FragmentDestinationLibraryBinding.inflate(
      inflater,
      container,
      false
    )
    val toolbar = fragmentDestinationLibraryBinding?.root?.findViewById<Toolbar>(R.id.toolbar)
    val activity = activity as CoreMainActivity
    activity.setSupportActionBar(toolbar)
    activity.supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setTitle(R.string.library)
    }
    if (toolbar != null) {
      activity.setupDrawerToggle(toolbar)
    }
    setHasOptionsMenu(true)

    return fragmentDestinationLibraryBinding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    fragmentDestinationLibraryBinding?.zimSwiperefresh?.setOnRefreshListener(
      ::requestFileSystemCheck
    )
    fragmentDestinationLibraryBinding?.zimfilelist?.run {
      adapter = booksOnDiskAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    zimManageViewModel.fileSelectListStates.observe(viewLifecycleOwner, Observer(::render))
      .also {
        coreMainActivity.navHostContainer
          .setBottomMarginToFragmentContainerView(0)

        getBottomNavigationView()?.let {
          setBottomMarginToSwipeRefreshLayout(it.measuredHeight)
        }
      }
    disposable.add(sideEffects())
    zimManageViewModel.deviceListIsRefreshing.observe(viewLifecycleOwner) {
      fragmentDestinationLibraryBinding?.zimSwiperefresh?.isRefreshing = it!!
    }

    fragmentDestinationLibraryBinding?.goToDownloadsButtonNoFiles?.setOnClickListener {
      offerAction(FileSelectActions.UserClickedDownloadBooksButton)
    }
    hideFilePickerButton()

    fragmentDestinationLibraryBinding?.zimfilelist?.addOnScrollListener(
      SimpleRecyclerViewScrollListener { _, newState ->
        when (newState) {
          SCROLL_DOWN -> {
            setBottomMarginToSwipeRefreshLayout(0)
          }
          SCROLL_UP -> {
            getBottomNavigationView()?.let {
              setBottomMarginToSwipeRefreshLayout(it.measuredHeight)
            }
          }
        }
      }
    )
  }

  private fun getBottomNavigationView() =
    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view)

  private fun setBottomMarginToSwipeRefreshLayout(marginBottom: Int) {
    fragmentDestinationLibraryBinding?.zimSwiperefresh?.apply {
      val params = layoutParams as CoordinatorLayout.LayoutParams?
      params?.bottomMargin = marginBottom
      requestLayout()
    }
  }

  private fun hideFilePickerButton() {
    if (sharedPreferenceUtil.isPlayStoreBuild) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        fragmentDestinationLibraryBinding?.selectFile?.visibility = View.GONE
      }
    }

    fragmentDestinationLibraryBinding?.selectFile?.setOnClickListener {
      showFileChooser()
    }
  }

  private val launcher = registerForActivityResult(object : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
      return super.createIntent(context, input).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
      }
    }
  }) {
    it ?: return@registerForActivityResult
    requireActivity().contentResolver.run {
      takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      navigateToReaderFragment(it)
    }
  }

  private fun showFileChooser() {
    launcher.launch(arrayOf("*/*"))
  }

  private fun navigateToReaderFragment(uri: Uri) {
    activity?.navigate(
      LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader()
        .apply { zimFileUri = uri.toString() }
    )
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.menu_zim_manager, menu)
    val searchItem = menu.findItem(R.id.action_search)
    val languageItem = menu.findItem(R.id.select_language)
    languageItem.isVisible = false
    searchItem.isVisible = false
    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onResume() {
    super.onResume()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    actionMode = null
    fragmentDestinationLibraryBinding?.zimfilelist?.adapter = null
    fragmentDestinationLibraryBinding = null
    disposable.clear()
  }

  private fun sideEffects() = zimManageViewModel.sideEffects
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
      {
        val effectResult = it.invokeWith(requireActivity() as AppCompatActivity)
        if (effectResult is ActionMode) {
          actionMode = effectResult
        }
      }, Throwable::printStackTrace
    )

  private fun render(state: FileSelectListState) {
    val items: List<BooksOnDiskListItem> = state.bookOnDiskListItems
    bookDelegate.selectionMode = state.selectionMode
    booksOnDiskAdapter.items = items
    if (items.none(BooksOnDiskListItem::isSelected)) {
      actionMode?.finish()
      actionMode = null
    }
    actionMode?.title = String.format("%d", state.selectedBooks.size)
    fragmentDestinationLibraryBinding?.apply {
      if (items.isEmpty()) {
        fileManagementNoFiles.visibility = View.VISIBLE
        goToDownloadsButtonNoFiles.visibility = View.VISIBLE
      } else {
        fileManagementNoFiles.visibility = View.GONE
        goToDownloadsButtonNoFiles.visibility = View.GONE
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(WAS_IN_ACTION_MODE, actionMode != null)
  }

  private fun requestFileSystemCheck() {
    zimManageViewModel.requestFileSystemCheck.onNext(Unit)
  }

  private fun offerAction(action: FileSelectActions) {
    zimManageViewModel.fileSelectActions.offer(action)
  }
}
