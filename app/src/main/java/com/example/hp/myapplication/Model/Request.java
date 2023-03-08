package com.example.hp.myapplication.Model;

import java.util.List;

public class Request {
    private String email;
    private String total;
    private List<Order> foods;
    //list

    public Request(String name, String s, String toString, List<Order> cart){

    }

    public Request(String email, String total, List<Order> foods) {
        this.email = email;
        this.total = total;
        this.foods = foods;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public List<Order> getFoods() {
        return foods;
    }

    public void setFoods(List<Order> foods) {
        this.foods = foods;
    }
}
