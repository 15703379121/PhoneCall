package com.example.administrator.phonecall;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int SPLIT_FILE = 0;
    private static final int SPLIT_FAIL = 1;
    private static final String FILE_TAG = "file_tag";
    private static final String FILE_NAME = "file_name";
    private static final String FILE_PATH = "file_path";
    private static final int PHONE_CALL = 2;
    private static final int CALL_FINISH = 3;
    private static final int INTENT_PHONE = 4;
    private static final int CALLING_PHONE = 5;
    private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 6;
    private TextView tv_path;
    private TextBean file_select;
    private List<PhoneBean> phoneList;
    private int tag;
    private Vibrator mVibrator;
    private MyAdapter mAdapter;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SPLIT_FILE:
                    //显示电话列表
                    phoneListShow();
                    break;
                case CALLING_PHONE:
                    //播打电话
                    phoneCall();
                    break;
                case SPLIT_FAIL:
                    Toast.makeText(MainActivity.this, "文件内容格式不正确，请重新选择文件", Toast.LENGTH_LONG).show();
                    break;
                case CALL_FINISH:
                    Toast.makeText(MainActivity.this, "打CALL完成", Toast.LENGTH_SHORT).show();
                    bt_submit.setText("打CALL结束");
                    called = false;
                    calling = false;
                    file_txt_path = "";
                    break;
                case INTENT_PHONE:
                    lv_phone.smoothScrollToPosition(tag);
                    String number = phoneList.get(tag).getPhone();
                    phoneList.remove(tag);
                    phoneList.add(tag,new PhoneBean(number,true));
                    mAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    private String file_name;
    private String file_txt_path;
    private String number;
    private SharedPreferences sp;
    // 电话管理器
    private TelephonyManager tm;
    // 监听器对象
    private MyListener listener;
    private ListView lv_phone;
    private Button bt_submit;
    private boolean called = false;
    private boolean calling = false;
    private int n;
    private ExecutorService singleThreadExecutor;
    private EditText et_time;
    private Button bt_stop;

    private void startCall() {
        try{
            // 检查是否获得了权限（Android6.0运行时权限）
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
                // 没有获得授权，申请授权
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.CALL_PHONE)) {
                    // 返回值：
//                          如果app之前请求过该权限,被用户拒绝, 这个方法就会返回true.
//                          如果用户之前拒绝权限的时候勾选了对话框中”Don’t ask again”的选项,那么这个方法会返回false.
//                          如果设备策略禁止应用拥有这条权限, 这个方法也返回false.
                    // 弹窗需要解释为何需要该权限，再次请求授权
                    Toast.makeText(MainActivity.this, "请授权！", Toast.LENGTH_LONG).show();

                    // 帮跳转到该应用的设置界面，让用户手动授权
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }else{
                    // 不需要解释为何需要该权限，直接请求授权
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CALL_PHONE},
                            MY_PERMISSIONS_REQUEST_CALL_PHONE);
                }
            }else {
                CallPhone();
            }

        }catch (Exception e){
            handler.sendEmptyMessage(SPLIT_FAIL);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = this.getSharedPreferences("phoneCallCache",MODE_PRIVATE);
        singleThreadExecutor = Executors.newSingleThreadExecutor();
        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mVibrator=(Vibrator)getApplication().getSystemService(Service.VIBRATOR_SERVICE);
        sdPermission(this);
        listener = new MyListener();
        tag = sp.getInt(FILE_TAG, -1);
        file_name = sp.getString(FILE_NAME, "");
        file_txt_path = sp.getString(FILE_PATH,"");
        initView();
    }

    private void initView() {
        tv_path = (TextView) findViewById(R.id.tv_path);
        lv_phone = findViewById(R.id.lv_phone);
        et_time = findViewById(R.id.et_time);
        findViewById(R.id.tv_output).setOnClickListener(this);
        findViewById(R.id.bt_path).setOnClickListener(this);
//        bt_stop = findViewById(R.id.bt_stop);
//        bt_stop.setOnClickListener(this);
        bt_submit = findViewById(R.id.bt_submit);
        bt_submit.setOnClickListener(this);
        if (!TextUtils.isEmpty(file_name)) {
            tv_path.setText(file_name);
        }

        new Thread() {
            @Override
            public void run() {
                super.run();
                splitFile();
                handler.sendEmptyMessage(SPLIT_FILE);
            }
        }.start();
    }
    // 处理权限申请的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_CALL_PHONE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 授权成功，继续打电话
                    CallPhone();
                } else {
                    // 授权失败！
                    Toast.makeText(this, "授权失败！", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }

    }

    private void CallPhone() {
        // 已经获得授权，可以打电话
        number = phoneList.get(tag).getPhone();
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivityForResult(intent,PHONE_CALL);
        handler.sendEmptyMessage(INTENT_PHONE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_path:
                calling = false;
                tm.listen(listener,PhoneStateListener.LISTEN_NONE);
                bt_submit.setText("拨打");
                Intent intent = new Intent(this, TextList.class);
                startActivityForResult(intent,0);
                break;
            case R.id.bt_submit:
                if (TextUtils.isEmpty(file_txt_path)){
                    Toast.makeText(this, "请选择文件", Toast.LENGTH_SHORT).show();
                    return;
                }
              /*  if (stop){
                    Toast.makeText(this, "已停止，请重新选择电话列表", Toast.LENGTH_SHORT).show();
                    return;
                }*/
                calling = !calling;
                if (calling){
                    called = false;
                    bt_submit.setText("暂停");
                    handler.sendEmptyMessage(CALLING_PHONE);
                }else{
                    tm.listen(listener, PhoneStateListener.LISTEN_NONE);
                    bt_submit.setText("拨打");
                    called = false;
                }
                break;
            case R.id.tv_output:
                outputFile();
                break;
         /*   case R.id.bt_stop:
                if (!stop) {
                    calling = false;
                    tm.listen(listener, PhoneStateListener.LISTEN_NONE);
                    stop = true;
                    bt_submit.setText("拨打");
                    outputFile();
                    Toast.makeText(this, "已停止拨打并导出数据", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show();
                }
                break;*/
        }
    }
//    private boolean stop = false;
    private void phoneCall() {
        if (phoneList != null && phoneList.size() > 0 ) {
            //开始打电话
            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
        } else {
            Toast.makeText(MainActivity.this, "文件内容格式不正确，请重新选择文件", Toast.LENGTH_LONG).show();
        }
    }

    private void phoneListShow() {
        if (phoneList != null && phoneList.size() > 0) {
            //显示列表
            mAdapter = new MyAdapter();
            lv_phone.setAdapter(mAdapter);
            if (tag > -1){
                lv_phone.smoothScrollToPosition(tag);
            }
        } else {
            Toast.makeText(MainActivity.this, "文件内容格式不正确，请重新选择文件", Toast.LENGTH_LONG).show();
        }
    }

    String output_path = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"phone_call/output";
    private void outputFile() {
        if (file_name == null || phoneList == null || phoneList.size() == 0 ) {
            Toast.makeText(this, "请选择文件", Toast.LENGTH_SHORT).show();
        }else{
            Calendar now = Calendar.getInstance();
            String fileName = now.get(Calendar.YEAR) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.DAY_OF_MONTH) + "log.csv";
//            String fileName = file_name + System.currentTimeMillis()+".csv";
//            String fileName = "log.csv";
            for (PhoneBean bean : phoneList) {
                if(bean.isState()){
                    OutputUtil.writeTxtToFile(bean.getPhone()+",已拨出", output_path, fileName);
                }else{
                    OutputUtil.writeTxtToFile(bean.getPhone()+",未拨出", output_path, fileName);
                }
            }
            Toast.makeText(this, "导出文件成功", Toast.LENGTH_SHORT).show();
        }
    }

    private void splitFile() {
        try {
            InputStream instream = new FileInputStream(file_txt_path);
            if (instream != null) {
                InputStreamReader inputreader =
                        new InputStreamReader(instream, "utf-8");
                BufferedReader buffreader =
                        new BufferedReader(inputreader);
                String line="";
                //分行读取
                phoneList = new ArrayList<>();
                try{
                    while (( line = buffreader.readLine()) != null) {
                        if (phoneList.size() > tag) {
                            phoneList.add(new PhoneBean(line, false));
                        }else{
                            phoneList.add(new PhoneBean(line,true));
                        }
                    }
                }catch (Exception e){
                    handler.sendEmptyMessage(SPLIT_FAIL);
                }
                instream.close();
            }
        }
        catch (java.io.FileNotFoundException e) {
            Log.d("TestFile", "The File doesn't not exist.");
        }
        catch (IOException e)  {
            Log.d("TestFile", e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && resultCode == TextList.INTENT_MAIN){
            file_select = (TextBean) data.getSerializableExtra(TextList.FILE_SELECT);
            file_name = file_select.getFile_name();
            file_txt_path = file_select.getFile_txt_path();
            tag = -1;
            sp.edit().putString(FILE_NAME,file_name).commit();
            sp.edit().putString(FILE_PATH,file_txt_path).commit();
            sp.edit().putInt(FILE_TAG,-1).commit();
            tv_path.setText(file_name);
            bt_submit.setText("拨打");
//            stop = false;
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    splitFile();
                    handler.sendEmptyMessage(SPLIT_FILE);
                }
            }.start();
        }
        if (requestCode == PHONE_CALL){
        }
    }

    private class MyListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            try {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE://空闲状态。
                        singleThreadExecutor.execute(new Runnable() {
                            public void run() {
                                if (calling) {
                                    if (called) {
                                        try {
                                            String timeStr = et_time.getText().toString();
                                            int time = 0;
                                            if (!TextUtils.isEmpty(timeStr)) {
                                                time = Integer.parseInt(timeStr);
                                            }else{
                                                time = 3;
                                            }
                                            Thread.sleep(time*1000);
                                            if (calling) {
                                                tag++;
                                                if (tag < phoneList.size()) {
                                                    startCall();
                                                } else {
                                                    handler.sendEmptyMessage(CALL_FINISH);
                                                }
                                            }
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }else{
                                        if (calling) {
                                            tag++;
                                            if (tag < phoneList.size()) {
                                                called = true;
                                                startCall();
                                            } else {
                                                handler.sendEmptyMessage(CALL_FINISH);
                                            }
                                        }
                                    }
                                }
                            }
                        });
                        break;
                    case TelephonyManager.CALL_STATE_RINGING://零响状态。

                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK://通话状态
                        mVibrator.vibrate(new long[]{100,100,100,1000},-1);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sp.edit().putInt(FILE_TAG,tag).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sp.edit().putInt(FILE_TAG,tag).commit();
        if (tm != null && listener != null){
            tm.listen(listener,PhoneStateListener.LISTEN_NONE);
        }
    }

    class MyAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return phoneList.size();
        }

        @Override
        public PhoneBean getItem(int i) {
            return phoneList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if (view == null){
                holder = new ViewHolder();
                view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_phone,null);
                holder.tv_phone= view.findViewById(R.id.tv_phone);
                holder.tv_state= view.findViewById(R.id.tv_state);
                view.setTag(holder);
            }else{
                holder = (ViewHolder) view.getTag();
            }
            holder.tv_phone.setText(phoneList.get(i).getPhone());
            if (phoneList.get(i).isState()) {
                holder.tv_state.setText("已拨打");
            }else{
                holder.tv_state.setText("未拨打");
            }
            return view;
        }
    }

    class ViewHolder{
        TextView tv_phone;
        TextView tv_state;
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
