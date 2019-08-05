package com.mbientlab.metawear.tutorial.exergame;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
    private String[][] data = new String[5][3];
    private static final String TAG = "serious";
    private static List<String> repValues = new ArrayList<>();
    private ArrayList<String> mImageNames = new ArrayList<>();
    private ArrayList<String> mImages = new ArrayList<>();
    static ArrayList<String> reps = new ArrayList<>();
    private Context mContext;
    //private int[] reps = new int[9];
    //private int[] sets = new int[9];
    //private int[] weights = new int[9];

    public RecyclerViewAdapter(ArrayList<String> mImageNames, ArrayList<String> mImages, Context mContext ) {
        this.mImageNames = mImageNames;
        this.mImages = mImages;
        this.mContext = mContext;
        //this.reps = reps;
        //this.sets = sets;
        //this.weights = weights;
        //int[] reps, int[] sets, int[] weights
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(com.mbientlab.metawear.tutorial.exergame.R.layout.layout_workout, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.i(TAG, "onBindViewHolder: called.");
        holder.rep.setText(data[position][0]);
        holder.set.setText(data[position][1]);
        holder.weight.setText(data[position][2]);
        Glide.with(mContext)
                .asBitmap()
                .load(mImages.get(position))
                .into(holder.activtyImage);

        holder.activityName.setText(mImageNames.get(position));

        holder.activityLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: clicked on: " + mImageNames.get(position) + ", " + data[position][0] + ", " + data[position][1] + ", " + data[position][2]);
                Toast.makeText(mContext, mImageNames.get(position), Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mImageNames.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public EditText rep, set, weight;
        ConstraintLayout activityLayout;
        public Button activityButton;
        CircleImageView activtyImage;
        TextView activityName;
        FloatingActionButton fab;

        public ViewHolder(View itemView) {
            super(itemView);
            activtyImage = itemView.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.circle_image);
            activityName = itemView.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.activityName);
            activityLayout = itemView.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.activity_layout);
            rep = itemView.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.repText);
            set = itemView.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.setsText);
            weight = itemView.findViewById(com.mbientlab.metawear.tutorial.exergame.R.id.weightText);

            rep.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    //data[getAdapterPosition()][0] = "0";
                    //data[getAdapterPosition()][1] = "0";
                    //data[getAdapterPosition()][2] = "0";
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s) {
                    data[getAdapterPosition()][0] = s.toString();
                    reps.add(s.toString());
                    Log.i(TAG, "Reps: " + getAdapterPosition() + "0" + s.toString());
                }
            });

            set.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    //data[getAdapterPosition()][0] = "0";
                    //data[getAdapterPosition()][1] = "0";
                    //data[getAdapterPosition()][2] = "0";
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s) {
                    data[getAdapterPosition()][1] = s.toString();
                    Log.i(TAG, "Sets: " + getAdapterPosition() + "1" + s.toString());
                }
            });

            weight.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    //data[getAdapterPosition()][0] = "0";
                    //data[getAdapterPosition()][1] = "0";
                    //data[getAdapterPosition()][2] = "0";
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s) {
                    data[getAdapterPosition()][2] = s.toString();
                    Log.i(TAG, "Weight: " +  getAdapterPosition() + "2" + s.toString());
                }
            });
            if(fab != null){
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                });
            } else
                Log.i(TAG, "fab null");


        }
    }

    public ArrayList<String> getmImageNames(){
        return mImageNames;
    };

    public String[][] getData(){
        return data;
    };
}
