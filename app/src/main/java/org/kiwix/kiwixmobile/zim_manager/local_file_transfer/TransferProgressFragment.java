package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.os.Bundle;

import androidx.annotation.Nullable;
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


public class TransferProgressFragment extends Fragment {

  private static final String FILE_ITEMS = "file_items";

  /*@BindView(R.id.recycler_view_transfer_files) RecyclerView filesRecyclerView;*/
  private Unbinder unbinder;

  /*private ArrayList<FileItem> fileItems;
  private FileListAdapter fileListAdapter;*/

  public TransferProgressFragment() {
    // Required empty public constructor
  }

  public static TransferProgressFragment newInstance(ArrayList<FileItem> fileItems) {
    TransferProgressFragment fragment = new TransferProgressFragment();
    Bundle args = new Bundle();
    args.putParcelableArrayList(FILE_ITEMS, fileItems);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle bundle = this.getArguments();
    /*this.fileItems = bundle.getParcelableArrayList(FILE_ITEMS);*/
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_transfer_progress, container, false);
    unbinder = ButterKnife.bind(this, view);

    /*fileListAdapter = new FileListAdapter(fileItems);
    filesRecyclerView.setAdapter(fileListAdapter);

    filesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));*/

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (unbinder != null) unbinder.unbind();
  }

  /*public void changeStatus(int itemIndex, @FileItem.FileStatus int status) {
    fileItems.get(itemIndex).setFileStatus(status);
    fileListAdapter.notifyItemChanged(itemIndex);
  }*/
}
