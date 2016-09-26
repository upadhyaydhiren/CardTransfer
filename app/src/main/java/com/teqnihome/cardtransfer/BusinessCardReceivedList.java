package com.teqnihome.cardtransfer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.teqnihome.cardtransfer.Database.BusinessCard;
import com.teqnihome.cardtransfer.Database.DataBaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * This is Business Card receive list activity that extends AppCompatActivity
 * Created by dhiren
 * @author dhiren
 * @see AppCompatActivity
 * @see com.teqnihome.cardtransfer.BusinessCardReceivedList.BusinessCardAdapter
 * @see BusinessCard
 */
public class BusinessCardReceivedList extends AppCompatActivity {
    List<BusinessCard> businessCardList;
    DataBaseHelper db;
    RecyclerView recycleListView;
    LinearLayoutManager layoutManager = null;
    BusinessCardAdapter businessCardAdapter;
    static LinearLayout linearLayout;
    static View showView;
    static ImageView imageCard;
    static EditText nameValue;
    static EditText emailValue;
    static EditText phoneValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.business_card_received);
        businessCardList = new ArrayList<>();
        db = new DataBaseHelper(this);
        businessCardList = db.getAllBusinessCard();
        db.closeDB();
        linearLayout = (LinearLayout) findViewById(R.id.viewCard);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        recycleListView = (RecyclerView) findViewById(R.id.list_business_received);
        recycleListView.setHasFixedSize(true);
        showView = LayoutInflater.from(this).inflate(R.layout.businesscardlayout, null);
        linearLayout.addView(showView);
        imageCard = (ImageView) showView.findViewById(R.id.card_avatar);
        nameValue = (EditText) showView.findViewById(R.id.card_name);
        emailValue = (EditText) showView.findViewById(R.id.card_email);
        phoneValue = (EditText) showView.findViewById(R.id.card_phone);

        ImageView imageCardEdit = (ImageView) showView.findViewById(R.id.card_avatar_edit_overlay);
        imageCardEdit.setVisibility(View.GONE);

        nameValue.setEnabled(false);
        emailValue.setEnabled(false);
        phoneValue.setEnabled(false);

        nameValue.setTextColor(Color.BLACK);
        emailValue.setTextColor(Color.BLACK);
        phoneValue.setTextColor(Color.BLACK);


        RelativeLayout layout = (RelativeLayout) showView.findViewById(R.id.card_layout_title);
        layout.setVisibility(View.GONE);

        if (businessCardList.size() == 0) {
            TextView textView = (TextView) findViewById(R.id.list_received_user);
            textView.setText("No Cards Received");
        }

        setRecycleView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, com.teqnihome.cardtransfer.BusinessCard.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    public void setRecycleView() {
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        businessCardAdapter = new BusinessCardAdapter(this, businessCardList, layoutManager);
        recycleListView.setLayoutManager(layoutManager);
        //recycleListView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recycleListView.setAdapter(businessCardAdapter);

    }


    static class BusinessCardAdapter extends RecyclerView.Adapter<BusinessCardAdapter.BusinessCardViewHolder> implements View.OnClickListener {
        private static final String TAG = "BusinessCardAdapter";
        private Context mContext;
        LinearLayoutManager layoutManager;
        List<BusinessCard> businessCards;

        public BusinessCardAdapter(Context c, List<BusinessCard> businessCardList, LinearLayoutManager layoutManager) {
            mContext = c;
            this.layoutManager = layoutManager;
            businessCards = businessCardList;

        }

        @Override
        public void onClick(View v) {
            if (linearLayout.getVisibility() == View.VISIBLE) {
                linearLayout.setVisibility(View.GONE);
            } else {

                for (BusinessCard card : businessCards) {

                    Log.d(TAG, "onClick:  card  id  " + card.getId() + "   view id  " + v.getTag());
                    Log.d(TAG, "onClick: " + linearLayout.getVisibility() + "   " + View.VISIBLE);

                    if (card.getId() == (int) v.getTag() && linearLayout.getVisibility() != View.VISIBLE) {
                        String name = card.getName();
                        String email = card.getEmail();
                        String phone = card.getPhone();
                        String picture = card.getPicture();

                        imageCard.setImageURI(Uri.parse(picture));
                        nameValue.setText(name);
                        emailValue.setText(email);
                        phoneValue.setText(phone);
                        linearLayout.setVisibility(View.VISIBLE);

                        break;


                    }
                }
            }


        }

        @Override
        public BusinessCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder: in create view Holder");
            View v = LayoutInflater.from(mContext).inflate(R.layout.business_layout_single_card, parent, false);
            BusinessCardViewHolder holder = new BusinessCardViewHolder(v, layoutManager, viewType);
            return holder;
        }

        @Override
        public void onBindViewHolder(final BusinessCardViewHolder holder, int position) {

            BusinessCard businessCard = businessCards.get(position);

            holder.itemView.setTag(businessCard.getId());
            holder.imageView.setImageURI(Uri.parse(businessCard.getPicture()));
            holder.name.setText(businessCard.getName());
            holder.itemView.setOnClickListener(this);
            holder.imageView.setBackground(mContext.getResources().getDrawable(R.drawable.avatar_placeholder));
            holder.imageView.setBackgroundResource(R.drawable.avatar_placeholder);


        }

        @Override
        public int getItemCount() {
            return businessCards.size();
        }

        public class BusinessCardViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView name;
            LinearLayoutManager linearLayoutManager;

            public BusinessCardViewHolder(View w, LinearLayoutManager layoutManager, int type) {
                super(w);
                if (type == 0) {
                    imageView = (ImageView) w.findViewById(R.id.card_avatar_received);
                    name = (TextView) w.findViewById(R.id.card_name_received);
                    linearLayoutManager = layoutManager;
                }
            }

        }


    }
}
