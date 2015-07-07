package com.cattsoft.phone.quality.ui.fragments.menu;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.cattsoft.phone.quality.R;
import com.cattsoft.phone.quality.ui.fragments.RoboLazyFragment;
import com.cattsoft.phone.quality.utils.PrefUtils;
import com.cattsoft.phone.quality.utils.SpeedServer;
import com.cattsoft.phone.quality.utils.SpeedServerMatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import roboguice.inject.InjectView;

import java.util.List;
import java.util.Map;

/**
 * Created by yushiwei on 15-5-11.
 */
public class SpeedServerChoiceFragment extends RoboLazyFragment {

    protected SpeedServerChoiceFragment.CallBack callBack;
    @InjectView(R.id.auto_get)
    private Button autoget;
    @InjectView(R.id.speed_server_expandable_list_view)
    private ExpandableListView listView;

    private SpeedServerMatch speedServerMatch = null;
    /**
     * 自动匹配逻辑
     */
    private Handler autoMatchHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            speedServerMatch = null;
            Log.d(".....","-----3");
            autoget.setSelected(true);
            if(msg.what != 0){
                autoget.setText("自动匹配测速服务器失败");
                Toast.makeText(getActivity(), "自动匹配测速服务器失败", Toast.LENGTH_SHORT).show();}
            else{
                autoget.setText("自动获取");
               SpeedServer speedServer = (SpeedServer) msg.obj;
                Log.d("SpeedServerChoiceFragment","----"+speedServer.getServer()+":"+speedServer.getPort());
                callBack.onChildClick(speedServer.getName(),speedServer.getServer()+":"+speedServer.getPort());
            }
            return false;
        }
    });


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(".....","-----2");
        return inflater.inflate(R.layout.layout_speed_server_choice, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(".....","-----1");
        autoget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != speedServerMatch)
                    return;
                speedServerMatch = new SpeedServerMatch(getActivity(), autoMatchHandler);
                speedServerMatch.execute();
            }
        });

        listView.setAdapter(new Adapter(view.getContext()));
        listView.setOnChildClickListener(new ClickChild(this));
        listView.setOnGroupExpandListener(new GroupExpand(listView));
        listView.requestFocus();
    }

    @Override
    public void onResume() {
        Log.d(".....","-----4");
        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            callBack = (SpeedServerChoiceFragment.CallBack) activity;
        } catch (ClassCastException e) {
            Log.w(this.getTag(), activity.toString() + " must implement OnHeadlineSelectedListener", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public interface CallBack {
        public void onChildClick(String showText, String ip);
    }

}

class GroupExpand implements ExpandableListView.OnGroupExpandListener {

    private ExpandableListView listView;

    public GroupExpand(ExpandableListView listView) {
        this.listView = listView;
    }

    @Override
    public void onGroupExpand(int groupPosition) {
        int count = listView.getExpandableListAdapter().getGroupCount();
        for (int i = 0; i < count; i++) {
            if (i != groupPosition && listView.isGroupExpanded(i)) {
                listView.collapseGroup(i);
            }
        }
    }
}

class ClickChild implements ExpandableListView.OnChildClickListener {

    private SpeedServerChoiceFragment fragment;

    public ClickChild(SpeedServerChoiceFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        TextView tv = (TextView) v.findViewById(R.id.speed_choice_child_checked_text_view);
        Toast.makeText(parent.getContext(), tv.getText(), Toast.LENGTH_SHORT).show();
        String ip = String.valueOf(parent.getExpandableListAdapter().getChild(groupPosition, childPosition));
        Log.d("SpeedServerChoiceFragment","---"+ip);
        fragment.callBack.onChildClick(tv.getText().toString(), ip);
        return true;
    }
}
class Data {
    public final String name;

    public final String ip;

    public Data(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    public static Data valueOf(String name, String ip) {
        return new Data(name, ip);
    }
 }
class Adapter extends BaseExpandableListAdapter {

    private LinearLayout groupLayout;

    private LinearLayout childrenLayout;

    private CheckedTextView groupText;

    private CheckedTextView childText;

    private LayoutInflater inflater;

    private Map<String, List<Data>> listData;

    private String[] serverConfig;

    private String[] groupNames;

    public Adapter(Context context) {
        this.serverConfig = context.getResources().getStringArray(R.array.bandwidth_server);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        initListData();
    }

    private void initListData() {
        listData = Maps.newLinkedHashMap();
        for (int i = 0; i < serverConfig.length; i++) {
            String serverInfo = serverConfig[i];
            String[] info = serverInfo.split("-");
            if (info.length != 3) {
                throw new IllegalArgumentException("服务器配置有错误!" + serverInfo);
            }
            if (!listData.containsKey(info[0])) {
                listData.put(info[0], Lists.<Data>newLinkedList());
            }
            listData.get(info[0]).add(Data.valueOf(info[1], info[2]));
        }

        groupNames = listData.keySet().toArray(new String[0]);
    }

    private String getGroupName(int index) {
        return groupNames[index];
    }

    @Override
    public int getGroupCount() {
        return groupNames.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return listData.get(getGroupName(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groupNames[groupPosition];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return listData.get(getGroupName(groupPosition)).get(childPosition).ip;
    }

    public String getChildName(int groupPosition, int childPosition) {
        return listData.get(getGroupName(groupPosition)).get(childPosition).name;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.layout_expandable_list_group, null);
            Log.i("", convertView.getWindowVisibility() + "");
            Log.i("", convertView.getSystemUiVisibility() + "");
        }
        TextView textView = (TextView) convertView.findViewById(R.id.speed_choice_group_checked_text_view);
        textView.setText(String.valueOf(getGroup(groupPosition)));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.layout_expandable_list_child, null);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.speed_choice_child_checked_text_view);
        textView.setText(String.valueOf(getChildName(groupPosition, childPosition)));
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}