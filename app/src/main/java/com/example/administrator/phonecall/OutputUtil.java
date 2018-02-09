package com.example.administrator.phonecall;

import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Created by Administrator on 2018/1/17 0017.
 */

public class OutputUtil {
    // 将字符串写入到文本文件中
    public static void writeTxtToFile(String strcontent, String filePath, String fileName) {
        //生成文件夹之后，再生成文件，不然会出错
        makeFilePath(filePath, fileName);

        String strFilePath = filePath+fileName;
        // 每次写入时，都换行写
        String strContent = strcontent + "\r\n";
        try {
            File file = new File(strFilePath);
            RandomAccessFile raf;
            if (!file.exists()) {
                Log.d("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
                Log.e("radish", "writeTxtToFile: 创建文件" );
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(file.length());
                byte [] bs = { (byte)0xEF, (byte)0xBB, (byte)0xBF};   //new added
                raf.write(bs);
                raf.write(("电话,状态" + "\r\n").getBytes());
            }else{
                 raf = new RandomAccessFile(file, "rwd");
            }
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }

    // 生成文件
    public static File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
                Log.e("radish", "writeTxtToFile: 创建文件" );
                RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                raf.seek(file.length());
                byte [] bs = { (byte)0xEF, (byte)0xBB, (byte)0xBF};   //new added
                raf.write(bs);
                raf.write(("电话,状态" + "\r\n").getBytes());
                raf.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    // 生成文件夹
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            Log.i("error:", e+"");
        }
    }
}
