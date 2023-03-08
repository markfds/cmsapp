package com.example.hp.myapplication.Model;

public class Food {
    private String Name, Image,Description,Price;

    public Food(){

    }

    public Food(String name, String image, String description, String price, String menuId){
        Name = name;
        Image = image;
        Description = description;
        Price = price;


    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getImage() {
        return Image;
    }

    public void setImage(String image) {
        Image = image;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getPrice() {
        return Price;
    }

    public void setPrice(String price) {
        Price = price;
    }


}
