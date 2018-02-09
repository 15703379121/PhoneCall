package com.example.administrator.phonecall;

/**
 * Created by Administrator on 2018/1/17 0017.
 */

public class PhoneBean {
    String phone;
    boolean state;

    public PhoneBean(String phone, boolean state) {
        this.phone = phone;
        this.state = state;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }
}
