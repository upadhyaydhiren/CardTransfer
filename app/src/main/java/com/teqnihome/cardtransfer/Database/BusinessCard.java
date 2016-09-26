package com.teqnihome.cardtransfer.Database;

/**
 * This is class for save receive card information into database
 * Created by dhiren
 * @author dhiren
 * @see DataBaseHelper
 */
public class BusinessCard {
    int id;
    String createdDate;
    String name;
    String email;
    String phone;
    String picture;
  public   BusinessCard(){

    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}
