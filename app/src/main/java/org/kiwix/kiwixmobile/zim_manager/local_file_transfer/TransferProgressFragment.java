package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import org.kiwix.kiwixmobile.R;

import java.util.ArrayList;

/**
 * Part of the local file sharing module, this fragment is used to display the progress of the
 * file transfer. It displays a list of files along with their current status (as defined in the
 * {@link FileItem} class.
 * */
public class TransferProgressFragment extends Fragment {

  @BindView(R.id.recycler_view_transfer_files) RecyclerView filesRecyclerView;
  private Unbinder unbinder;

  private ArrayList<FileItem> fileItems;
  private FileListAdapter fileListAdapter;

  public TransferProgressFragment() {
    // Required empty public constructor
  }

  public TransferProgressFragment(ArrayList<FileItem> fileItems) {
    this.fileItems = fileItems;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_transfer_progress, container, false);
    unbinder = ButterKnife.bind(this, view);

    fileListAdapter = new FileListAdapter(getActivity(), fileItems);
    filesRecyclerView.setAdapter(fileListAdapter);

    filesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if(unbinder != null) unbinder.unbind();
  }

  public void changeStatus(int itemIndex, @FileItem.FileStatus int status) {
    fileItems.get(itemIndex).setFileStatus(status);
    fileListAdapter.notifyItemChanged(itemIndex);
  }
}
