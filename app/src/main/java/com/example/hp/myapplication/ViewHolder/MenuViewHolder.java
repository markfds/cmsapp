package com.example.hp.myapplication.ViewHolder;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.hp.myapplication.Interface.ItemClickListener;
import com.example.hp.myapplication.R;

public class MenuViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView txtMenuName;
     public ImageView imageView;
     public CardView cv1;
    public CardView cv2;
    public CardView cv3;

     private ItemClickListener itemClickListener;
    public MenuViewHolder(final View itemView) {
        super(itemView);
        cv1=(CardView)itemView.findViewById(R.id.cardv1);
        cv2=(CardView)itemView.findViewById(R.id.cardv2);
        cv3=(CardView)itemView.findViewById(R.id.cardv3);
        cv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                txtMenuName = (TextView)itemView.findViewById(R.id.menu_name);
                imageView = (ImageView)itemView.findViewById(R.id.menu_image);

            }
        });
        cv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtMenuName = (TextView)itemView.findViewById(R.id.menu_name1);
                imageView = (ImageView)itemView.findViewById(R.id.menu_image1);

            }
        });
        cv3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtMenuName = (TextView)itemView.findViewById(R.id.menu_name2);
                imageView = (ImageView)itemView.findViewById(R.id.menu_image2);

            }
        });

        itemView.setOnClickListener(this);
    }

    public void setItemClickListener(ItemClickListener itemClickListener)
    {
        this.itemClickListener = itemClickListener;
    }

    @Override
    public void onClick(View v) {
        itemClickListener.onClick(v,getAdapterPosition(),false);
    }
}
