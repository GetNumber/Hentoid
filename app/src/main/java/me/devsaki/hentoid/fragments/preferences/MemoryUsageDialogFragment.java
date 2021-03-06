package me.devsaki.hentoid.fragments.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.database.CollectionDAO;
import me.devsaki.hentoid.database.ObjectBoxDAO;
import me.devsaki.hentoid.enums.Site;
import me.devsaki.hentoid.util.FileHelper;
import me.devsaki.hentoid.util.Helper;
import me.devsaki.hentoid.util.Preferences;
import me.devsaki.hentoid.views.CircularProgressView;

import static androidx.core.view.ViewCompat.requireViewById;

/**
 * Created by Robb on 05/2020
 * Dialog for the library metadata export feature
 */
public class MemoryUsageDialogFragment extends DialogFragment {

    private int _4dp;

    private TableLayout table;
    private ImageView foldUnfoldArrow;


    public static void invoke(@NonNull final FragmentManager fragmentManager) {
        MemoryUsageDialogFragment fragment = new MemoryUsageDialogFragment();
        fragment.show(fragmentManager, null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        return inflater.inflate(R.layout.dialog_prefs_memory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);

        _4dp = Helper.dpToPixel(requireContext(), 4);

        double deviceFreeGb = -1;
        double deviceTotalGb = -1;

        DocumentFile rootFolder = FileHelper.getFolderFromTreeUriString(requireActivity(), Preferences.getStorageUri());
        if (rootFolder != null) {
            FileHelper.MemoryUsageFigures memUsage = new FileHelper.MemoryUsageFigures(requireContext(), rootFolder);
            deviceFreeGb = memUsage.getfreeUsageMb() / 1024;
            deviceTotalGb = memUsage.getTotalSpaceMb() / 1024;
        }

        CollectionDAO dao = new ObjectBoxDAO(requireContext());
        Map<Site, ImmutablePair<Integer, Long>> memUsage = dao.getMemoryUsagePerSource();
        double hentoidUsageGb = Stream.of(memUsage.values()).collect(Collectors.summingLong(ImmutablePair::getRight)) * 1.0 / (1024 * 1024 * 1024);

        CircularProgressView donut = requireViewById(rootView, R.id.memory_global_graph);
        donut.setTotalColor(requireContext(), R.color.primary_light);
        donut.setTotal(deviceTotalGb);
        donut.setProgress1(deviceTotalGb - deviceFreeGb); // Total size taken on the device
        donut.setProgress2(hentoidUsageGb); // Size taken by Hentoid

        ((TextView) requireViewById(rootView, R.id.memory_total)).setText(getResources().getString(R.string.memory_total, deviceTotalGb));
        ((TextView) requireViewById(rootView, R.id.memory_free)).setText(getResources().getString(R.string.memory_free, deviceFreeGb));
        ((TextView) requireViewById(rootView, R.id.memory_hentoid)).setText(getResources().getString(R.string.memory_hentoid, hentoidUsageGb));

        table = requireViewById(rootView, R.id.memory_details_table);
        addRow(table, "Source", "Books", "Size");

        // Sort sources by largest size
        List<Map.Entry<Site, ImmutablePair<Integer, Long>>> sitesBySize = Stream.of(memUsage).sortBy(entry -> -entry.getValue().right).toList();
        for (Map.Entry<Site, ImmutablePair<Integer, Long>> entry : sitesBySize) {
            addRow(table, entry.getKey().getDescription(), entry.getValue().left + "", String.format(Locale.US, "%.1f MB", entry.getValue().right / (1024.0 * 1024)));
        }

        // Make details fold/unfold
        foldUnfoldArrow = requireViewById(rootView, R.id.memory_details_icon);
        requireViewById(rootView, R.id.memory_details).setOnClickListener(v -> onDetailsClick());
    }

    private void onDetailsClick() {
        if (View.VISIBLE == table.getVisibility()) {
            foldUnfoldArrow.setImageResource(R.drawable.ic_drop_down);
            table.setVisibility(View.GONE);
        } else {
            foldUnfoldArrow.setImageResource(R.drawable.ic_drop_up);
            table.setVisibility(View.VISIBLE);
        }
    }

    private void addRow(@NonNull TableLayout table, @NonNull String... values) {
        TableRow.LayoutParams tableParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        TableRow tableRow = new TableRow(requireContext());
        tableRow.setLayoutParams(tableParams);

        int column = 1;
        for (String value : values) {
            TextView textView = new TextView(requireContext());
            textView.setLayoutParams(new TableRow.LayoutParams(column++));
            textView.setText(value);
            textView.setPadding(_4dp, _4dp, _4dp, _4dp);
            tableRow.addView(textView);
        }

        table.addView(tableRow);
    }
}
