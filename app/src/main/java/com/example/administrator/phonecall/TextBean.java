package com.example.administrator.phonecall;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/1/16 0016.
 */

public class TextBean implements Serializable {
    private String file_size;
    private String file_name;
    private String file_txt_path;

    public TextBean(){}

    public TextBean(String file_size, String file_name, String file_txt_path) {
        this.file_size = file_size;
        this.file_name = file_name;
        this.file_txt_path = file_txt_path;
    }

    public String getFile_size() {
        return file_size;
    }

    public void setFile_size(String file_size) {
        this.file_size = file_size;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getFile_txt_path() {
        return file_txt_path;
    }

    public void setFile_txt_path(String file_txt_path) {
        this.file_txt_path = file_txt_path;
    }

    @Override
    public String toString() {
        return "TextBean{" +
                "file_size='" + file_size + '\'' +
                ", file_name='" + file_name + '\'' +
                ", file_txt_path='" + file_txt_path + '\'' +
                '}';
    }
}
