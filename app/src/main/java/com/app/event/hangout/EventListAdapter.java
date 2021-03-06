package com.app.event.hangout;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.google.android.gms.ads.formats.NativeContentAdView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "EventListAdapter";

    private List<Event> eventList;

    private DatabaseReference databaseReference;

    private Context context;

    //  TYPE_ITEM and TYPE_ADS are identification of item type
    //  TYPE_ITEM = event
    //  TYPE_ADS = ads
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_ADS = 1;

    private AdLoader.Builder builder;
    private LayoutInflater inflater;


    // Keep position of the ads in the list
    private Map<Integer, Object> map = new HashMap<>();

    // Keep the real position of the event in the original event list
    private Map<Integer, Integer> originalPositions = new HashMap<>();

    private static final String ADMOB_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";
    private static final String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713";


    /**
     * Constructor for EventListAdapter
     * @param events events that are showing on screen
     */
    public EventListAdapter(List<Event> events, Context context) {
        this.context = context;
        eventList = events;
        databaseReference = FirebaseDatabase.getInstance().getReference();

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // The idea is to create a new EventList that holds both ads and original events, if
        // corresponding position is ads, add to the map and put empty event in corresponding
        // location, for example, if we have 4 events passed in, we want to do create following list
        // and export ads location
        //  <List Position> :0              1           2              3             4         5
        //                   Event1       Ads1       Events2         Events3        Ads2   Event4

        eventList = new ArrayList<Event>();
        int count = 0;
        for (int i = 0; i < events.size(); i++) {
            if (i % 2 == 1) {
                map.put(i + count, new Object());
                count++;
                eventList.add(new Event());
            }
            eventList.add(events.get(i));
            originalPositions.put(eventList.size() - 1, i);
        }
    }

    /**
     * Use ViewHolder to hold view widget, view holder is required to be used in recycler view
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView location;
        public TextView description;
        public TextView time;
        public ImageView imgview;
        public View layout;

        public ImageView img_view_good;
        public ImageView img_view_comment;

        public TextView good_number;
        public TextView comment_number;

        public ViewHolder(View v) {
            super(v);
            layout = v;
            title = (TextView) v.findViewById(R.id.event_item_title);
            location = (TextView) v.findViewById(R.id.event_item_location);
            description = (TextView) v.findViewById(R.id.event_item_description);
            time = (TextView) v.findViewById(R.id.event_item_time);
            imgview = (ImageView) v.findViewById(R.id.event_item_img);
            img_view_good = (ImageView) v.findViewById(R.id.event_good_img);
            img_view_comment = (ImageView) v.findViewById(R.id.event_comment_img);
            good_number = (TextView) v.findViewById(R.id.event_good_number);
            comment_number = (TextView) v.findViewById(R.id.event_comment_number);
        }
    }

    /**
     * View Holder Class for advertisement
     */
    public class ViewHolderAds extends RecyclerView.ViewHolder {
        public FrameLayout frameLayout;
        ViewHolderAds(View v) {
            super(v);
            frameLayout = (FrameLayout)v;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return map.containsKey(position) ? TYPE_ADS : TYPE_ITEM;
    }

    /**
     * OnBindViewHolder will render created view holder on screen
     * @param holder View Holder created for each position
     * @param position position needs to show
     */

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        Integer originalPosition = originalPositions.get(position);
        originalPosition = originalPosition == null ? position : originalPosition;
        switch (holder.getItemViewType()) {
            // According to different view type, show corresponding view
            case TYPE_ITEM:
                ViewHolder viewHolderItem = (ViewHolder) holder;
                viewHolderItem.layout.setBackgroundColor(
                        ContextCompat.getColor(context,
                                Utils.colorSet[originalPosition % Utils.colorSet.length])
//                        ContextCompat.getColor(context, R.color.Aqua)
                );
                configureItemView(viewHolderItem, position);
                break;
            case TYPE_ADS:
                ViewHolderAds viewHolderAds = (ViewHolderAds) holder;
                refreshAd(viewHolderAds.frameLayout);
                break;
        }
    }

    /**
     * refresh ads, there are several steps falling through
     * First, load advertisement from remote
     * Second, add content to ads view
     * @param frameLayout
     */
    private void refreshAd(final FrameLayout frameLayout) {
        AdLoader.Builder builder = new AdLoader.Builder(context, ADMOB_AD_UNIT_ID);
        builder.forContentAd(new NativeContentAd.OnContentAdLoadedListener() {
            @Override
            public void onContentAdLoaded(NativeContentAd ad) {
                NativeContentAdView adView = (NativeContentAdView) inflater
                        .inflate(R.layout.ads_contain, null);
                populateContentAdView(ad, adView);
                frameLayout.removeAllViews();
                frameLayout.addView(adView);
            }
        });

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }


    private void populateContentAdView(NativeContentAd nativeContentAd,
                                       NativeContentAdView adView) {
        adView.setHeadlineView(adView.findViewById(R.id.ads_headline));
        adView.setImageView(adView.findViewById(R.id.ads_image));
        adView.setBodyView(adView.findViewById(R.id.ads_body));
        adView.setAdvertiserView(adView.findViewById(R.id.ads_advertiser));

        // Some assets are guaranteed to be in every NativeContentAd.
        ((TextView) adView.getHeadlineView()).setText(nativeContentAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(nativeContentAd.getBody());
        ((TextView) adView.getAdvertiserView()).setText(nativeContentAd.getAdvertiser());

        List<NativeAd.Image> images = nativeContentAd.getImages();

        if (images.size() > 0) {
            ((ImageView) adView.getImageView()).setImageDrawable(images.get(0).getDrawable());
        }

        // Assign native ad object to the native view.
        adView.setNativeAd(nativeContentAd);
    }


    /**
     * By calling this method, each ViewHolder will be initiated and passed to OnBindViewHolder
     * for rendering
     * @param parent
     * @param viewType
     * @return
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v;
        switch (viewType) {
            case TYPE_ITEM:
                v = inflater.inflate(R.layout.event_list_item, parent, false);
                viewHolder = new ViewHolder(v);
                break;
            case TYPE_ADS:
                v = inflater.inflate(R.layout.ads_container_layout,
                        parent, false);
                viewHolder = new ViewHolderAds(v);
                break;
        }
        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Show Event
     * @param holder event view holder
     * @param position position of the event
     */
    private void configureItemView(final ViewHolder holder, final int position) {
        final Event event = eventList.get(position);
        holder.title.setText(event.getTitle());
        String[] locations = event.getAddress().split(",");
        if (locations.length >= 3) {
            holder.location.setText(locations[1] + "," + locations[2]);
        } else {
            holder.location.setText("unknown area");
        }
        holder.description.setText(event.getDescription());
        holder.time.setText(Utils.timeTransformer(event.getTime()));

        holder.good_number.setText(String.valueOf(event.getLike()));

        holder.comment_number.setText(String.valueOf(event.getCommentNumber()));

        if (event.getImgUri() != null) {
            final String url = event.getImgUri();
            holder.imgview.setVisibility(View.VISIBLE);
            new AsyncTask<Void, Void, Bitmap>(){
                @Override
                protected Bitmap doInBackground(Void... params) {
                    return Utils.getBitmapFromURL(url);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    holder.imgview.setImageBitmap(bitmap);
                }
            }.execute();
        } else {
            holder.imgview.setVisibility(View.GONE);
        }

        // When user likes the event, push like number to firebase database
        holder.img_view_good.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                databaseReference.child("events").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Event recordedEvent = snapshot.getValue(Event.class);
                            if (recordedEvent.getId().equals(event.getId())) {
                                int number = recordedEvent.getLike();
                                holder.good_number.setText(String.valueOf(number + 1));
                                snapshot.getRef().child("like").setValue(number + 1);
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CommentActivity.class);
                String eventId = event.getId();
                intent.putExtra("EventID", eventId);
                context.startActivity(intent);
            }
        });
    }
}
