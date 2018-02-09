package com.example.administrator.phonecall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by Administrator on 2018/1/16 0016.
 */

public class TextList extends Activity {

    private static final int FINISH_TEXT = 0;
    public static final String FILE_SELECT = "file_select";
    public static final int INTENT_MAIN = 1;
    private List<TextBean> list = new ArrayList<>();
    private File file;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case FINISH_TEXT:
                    if (list != null && list.size()>0){
                        showList();
                    }else{
                        showText();
                    }
                    break;
            }
        }
    };

    private ListView lv_text;
    private TextView tv_tag;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_list);
        sdPermission(this);
        String sd_path = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"phoneCall";
        file = new File(sd_path);
        if (!file.exists()){
            file.mkdirs();
        }
        new Thread(){
            @Override
            public void run() {
                super.run();
                traverseFolder(file);
                handler.sendEmptyMessage(FINISH_TEXT);
            }
        }.start();
        initView();

    }

    private void initView() {
        lv_text = findViewById(R.id.lv_text);
        tv_tag = findViewById(R.id.tv_tag);
    }

    private void showText() {
        tv_tag.setText("对不起，没有找到txt文件");
    }

    private void showList() {
        tv_tag.setVisibility(View.GONE);
        lv_text.setVisibility(View.VISIBLE);
        lv_text.setAdapter(new MyAdapter());
        lv_text.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = getIntent();
                intent.putExtra(FILE_SELECT,list.get(i));
                setResult(INTENT_MAIN,intent);
                finish();
            }
        });
    }

    public void traverseFolder(File file) {
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files.length == 0) {
                return;
            } else {
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                        traverseFolder(file2);
                    } else {
                        if (file2.getName().endsWith(".txt")) {//格式为txt文件
                            //获取并计算文件大小
                            long size = file2.length();
                            String t_size = "";
                            if (size <= 1024) {
                                t_size = size + "B";
                            } else if (size > 1024 && size <= 1024 * 1024) {
                                size /= 1024;
                                t_size = size + "KB";
                            } else {
                                size = size / (1024 * 1024);
                                t_size = size + "MB";
                            }
                            TextBean bean = new TextBean(t_size, file2.getName(), file2.getAbsolutePath());
                            list.add(bean);
                        }
                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
        }
    }

    class MyAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if (view == null){
                holder = new ViewHolder();
                view = LayoutInflater.from(TextList.this).inflate(R.layout.item_lv_text, null);
                holder.tv_name = view.findViewById(R.id.tv_file_name);
                holder.tv_path = view.findViewById(R.id.tv_file_path);
                holder.tv_size = view.findViewById(R.id.tv_file_size);
                view.setTag(holder);
            }else{
                holder = (ViewHolder) view.getTag();
            }
            holder.tv_name.setText(list.get(i).getFile_name()+"");
            holder.tv_path.setText(list.get(i).getFile_txt_path()+"");
            holder.tv_size.setText(list.get(i).getFile_size()+"");
            return view;
        }
    }

    class ViewHolder{
        TextView tv_name;
        TextView tv_path;
        TextView tv_size;
    }
    Object invokeVolumeList = null;
    Method getVolumeList= null;
    StorageManager mStorageManager;

    private void sdPermission(Context context) {
        mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        try {
            getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            try {
                final Class<?>storageValumeClazz = Class.forName("android.os.storage.StorageVolume");
                final Method getPath= storageValumeClazz.getMethod("getPath");
                Method isRemovable = storageValumeClazz.getMethod("isRemovable");
                Method mGetState = null;
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    try {
                        mGetState = storageValumeClazz.getMethod("getState");
                    } catch(NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    invokeVolumeList= getVolumeList.invoke(mStorageManager);
                }catch (Exception e) {
                }
                final int length = Array.getLength(invokeVolumeList);
                for(int i = 0; i<length ;i++) {
                    final Object storageValume= Array.get(invokeVolumeList, i);//
                    final String path =(String) getPath.invoke(storageValume);
                    final boolean removable =(Boolean) isRemovable.invoke(storageValume);
                    String state = null;
                    if (mGetState !=null) {
                        state = (String) mGetState.invoke(storageValume);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG,"couldn't talkto MountService", e);
            }
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
