package com.enigma.parapo.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.enigma.parapo.R;
import com.enigma.parapo.models.IndividualLocation;

import java.util.List;

/**
 * RecyclerView adapter to display a list of location cards on top of the map
 */
public class LocationRecyclerViewAdapter extends
        RecyclerView.Adapter<LocationRecyclerViewAdapter.ViewHolder> {

    private List<IndividualLocation> listOfLocations;
    private Context context;
    private int selectedTheme;
    private static ClickListener clickListener;
    private Drawable emojiForCircle = null;
    private Drawable backgroundCircle = null;
    private int upperCardSectionColor = 0;

    private int locationNameColor = 0;
    private int locationAddressColor = 0;
    private int locationFareColor = 0;
    private int locationFareHeaderColor = 0;
    private int locationDropOffColor = 0;
    private int locationDropOffHeaderColor = 0;
    private int locationDistanceNumColor = 0;
    private int milesAbbreviationColor = 0;

    public LocationRecyclerViewAdapter(List<IndividualLocation> styles,
                                       Context context, ClickListener cardClickListener, int selectedTheme) {
        this.context = context;
        this.listOfLocations = styles;
        this.selectedTheme = selectedTheme;
        this.clickListener = cardClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int singleRvCardToUse = R.layout.single_location_map_view_rv_card;
        View itemView = LayoutInflater.from(parent.getContext()).inflate(singleRvCardToUse, parent, false);
        return new ViewHolder(itemView);
    }

    public interface ClickListener {
        void onItemClick(int position);
    }

    @Override
    public int getItemCount() {
        return listOfLocations.size();
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {

        IndividualLocation locationCard = listOfLocations.get(position);

        card.nameTextView.setText(locationCard.getName());
        card.addressTextView.setText(locationCard.getAddress());
        card.fareTextView.setText(locationCard.getFare());
        card.dropOffTextView.setText(locationCard.getDropOff());
        card.distanceNumberTextView.setText(locationCard.getDistance());

        if (selectedTheme == R.style.AppTheme_Default) {
            emojiForCircle = ResourcesCompat.getDrawable(context.getResources(), R.drawable.default_theme_icon, null);
            backgroundCircle = ResourcesCompat.getDrawable(context.getResources(), R.drawable.white_circle, null);
            setColors(R.color.colorPrimaryDark_default, R.color.black, R.color.black, R.color.black,
                    R.color.black, R.color.black,
                    R.color.black, R.color.black, R.color.black);
            setAlphas(card, .37f, .37f, 100f, .37f,
                    100f,
                    .37f);
        }

        card.emojiImageView.setImageDrawable(emojiForCircle);
        card.constraintUpperColorSection.setBackgroundColor(upperCardSectionColor);
        card.backgroundCircleImageView.setImageDrawable(backgroundCircle);
        card.nameTextView.setTextColor(locationNameColor);
        card.fareTextView.setTextColor(locationFareColor);
        card.dropOffTextView.setTextColor(locationDropOffColor);
        card.dropOffHeaderTextView.setTextColor(locationDropOffHeaderColor);
        card.distanceNumberTextView.setTextColor(locationDistanceNumColor);
        card.milesAbbreviationTextView.setTextColor(milesAbbreviationColor);
        card.addressTextView.setTextColor(locationAddressColor);
        card.fareHeaderTextView.setTextColor(locationFareHeaderColor);
    }

    private void setColors(int colorForUpperCard, int colorForName, int colorForAddress,
                           int colorForDropOff, int colorForDropOffHeader, int colorForFare,
                           int colorForFareHeader, int colorForDistanceNum, int colorForMilesAbbreviation) {
        upperCardSectionColor = ResourcesCompat.getColor(context.getResources(), colorForUpperCard, null);
        locationNameColor = ResourcesCompat.getColor(context.getResources(), colorForName, null);
        locationAddressColor = ResourcesCompat.getColor(context.getResources(), colorForAddress, null);
        locationDropOffColor = ResourcesCompat.getColor(context.getResources(), colorForDropOff, null);
        locationDropOffHeaderColor = ResourcesCompat.getColor(context.getResources(), colorForDropOffHeader, null);
        locationFareColor = ResourcesCompat.getColor(context.getResources(), colorForFare, null);
        locationFareHeaderColor = ResourcesCompat.getColor(context.getResources(), colorForFareHeader, null);
        locationDistanceNumColor = ResourcesCompat.getColor(context.getResources(), colorForDistanceNum, null);
        milesAbbreviationColor = ResourcesCompat.getColor(context.getResources(), colorForMilesAbbreviation, null);
    }

    private void setAlphas(ViewHolder card, float addressAlpha, float dropOffHeaderAlpha, float dropOffNumAlpha,
                           float fareHeaderAlpha, float fareAlpha, float milesAbbreviationAlpha) {
        card.addressTextView.setAlpha(addressAlpha);
        card.dropOffHeaderTextView.setAlpha(dropOffHeaderAlpha);
        card.dropOffTextView.setAlpha(dropOffNumAlpha);
        card.fareHeaderTextView.setAlpha(fareHeaderAlpha);
        card.fareTextView.setAlpha(fareAlpha);
        card.milesAbbreviationTextView.setAlpha(milesAbbreviationAlpha);
    }


    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView nameTextView;
        TextView addressTextView;
        TextView fareTextView;
        TextView dropOffTextView;
        TextView distanceNumberTextView;
        TextView dropOffHeaderTextView;
        TextView milesAbbreviationTextView;
        TextView fareHeaderTextView;
        ConstraintLayout constraintUpperColorSection;
        CardView cardView;
        ImageView backgroundCircleImageView;
        ImageView emojiImageView;

        ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.location_name_tv);
            addressTextView = itemView.findViewById(R.id.location_description_tv);
            fareTextView = itemView.findViewById(R.id.location_fare_tv);
            fareHeaderTextView = itemView.findViewById(R.id.fare_header_tv);
            dropOffTextView = itemView.findViewById(R.id.location_drop_off_tv);
            backgroundCircleImageView = itemView.findViewById(R.id.background_circle);
            emojiImageView = itemView.findViewById(R.id.emoji);
            constraintUpperColorSection = itemView.findViewById(R.id.constraint_upper_color);
            distanceNumberTextView = itemView.findViewById(R.id.distance_num_tv);
            dropOffHeaderTextView = itemView.findViewById(R.id.drop_off_header_tv);
            milesAbbreviationTextView = itemView.findViewById(R.id.miles_mi_tv);
            cardView = itemView.findViewById(R.id.map_view_location_card);
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onItemClick(getLayoutPosition());
                }
            });
        }

        @Override
        public void onClick(View view) {
        }
    }
}
