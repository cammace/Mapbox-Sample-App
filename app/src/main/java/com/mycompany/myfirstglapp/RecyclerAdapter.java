package com.mycompany.myfirstglapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    // This adapter is for the search autocomplete

    private List<String> dataSource;
    private Context context;

    public RecyclerAdapter(Context context, List<String> dataSource){
        this.dataSource = dataSource;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.autocomplete_list_single_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String item = dataSource.get(position);
        holder.titleTextView.setText(item);
    }

    @Override
    public int getItemCount() {
        return (null != dataSource ? dataSource.size() : 0);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        public TextView titleTextView;
        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = (TextView)itemView.findViewById(android.R.id.text1);
        }
    }
}// End RecyclerAdapter