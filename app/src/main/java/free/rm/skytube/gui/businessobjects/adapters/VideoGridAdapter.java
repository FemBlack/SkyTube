/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.gui.businessobjects.adapters;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.ads.AdChoicesView;
import com.facebook.ads.AdIconView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdsManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.businessobjects.YouTube.GetYouTubeVideos;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeVideosTask;
import free.rm.skytube.gui.businessobjects.MainActivityListener;

/**
 * An adapter that will display videos in a {@link android.widget.GridView}.
 */
public class VideoGridAdapter extends RecyclerViewAdapterEx<YouTubeVideo, GridViewHolder> {

	/** Class used to get YouTube videos from the web. */
	private GetYouTubeVideos	getYouTubeVideos;
	/** Set to true to display channel information (e.g. channel name) and allows user to open and
	 *  browse the channel;  false to hide such information. */
	private boolean				showChannelInfo = true;
	/** Current video category */
	private VideoCategory		currentVideoCategory = null;

	// This allows the grid items to pass messages back to MainActivity
	protected MainActivityListener listener;

	/** If this is set, new videos being displayed will be saved to the database, if subscribed.
	 *  RM:  This is only set and used by ChannelBrowserFragment */
	private YouTubeChannel			youTubeChannel;

	/** Holds a progress bar */
	private SwipeRefreshLayout      swipeRefreshLayout = null;

	private GridViewHolder activeGridViewHolder;

	private static final String TAG = VideoGridAdapter.class.getSimpleName();

	private List<NativeAd> mAdItems;
	private NativeAdsManager mNativeAdsManager;
	private Context context;

	private static final int AD_DISPLAY_FREQUENCY = 5;
	private static final int POST_TYPE = 0;
	private static final int AD_TYPE = 1;


	/**
	 * @see #VideoGridAdapter(Context, boolean)
	 */
	public VideoGridAdapter(Context context) {
		this(context, true);
	}

	public VideoGridAdapter(Context context, NativeAdsManager
			nativeAdsManager) {
		this(context, true);
		mNativeAdsManager = nativeAdsManager;
		mAdItems = new ArrayList<>();
		this.context = context;
	}

	public void setListener(MainActivityListener listener) {
		this.listener = listener;
	}

	/**
	 * Constructor.
	 *
	 * @param context			Context.
	 * @param showChannelInfo	True to display channel information (e.g. channel name) and allows
	 *                          user to open and browse the channel; false to hide such information.
	 */
	public VideoGridAdapter(Context context, boolean showChannelInfo) {
		super(context);
		this.getYouTubeVideos = null;
		this.showChannelInfo = showChannelInfo;
	}


	/**
	 * Set the video category.  Upon set, the adapter will download the videos of the specified
	 * category asynchronously.
	 *
	 * @see #setVideoCategory(VideoCategory, String)
	 */
	public void setVideoCategory(VideoCategory videoCategory) {
		setVideoCategory(videoCategory, null);
	}


	/**
	 * Set the video category.  Upon set, the adapter will download the videos of the specified
	 * category asynchronously.
	 *
	 * @param videoCategory	The video category you want to change to.
	 * @param searchQuery	The search query.  Should only be set if videoCategory is equal to
	 *                      SEARCH_QUERY.
	 */
	public void setVideoCategory(VideoCategory videoCategory, String searchQuery) {
		// do not change the video category if its the same!
		if (videoCategory == currentVideoCategory)
			return;

		try {
			Log.i(TAG, videoCategory.toString());


			// create a new instance of GetYouTubeVideos
			this.getYouTubeVideos = videoCategory.createGetYouTubeVideos();
			this.getYouTubeVideos.init();

			// set the query
			if (searchQuery != null) {
				getYouTubeVideos.setQuery(searchQuery);
			}

			// set current video category
			this.currentVideoCategory = videoCategory;

			// get the videos from the web asynchronously
			new GetYouTubeVideosTask(getYouTubeVideos, this, swipeRefreshLayout, true).executeInParallel();
		} catch (IOException e) {
			Log.e(TAG, "Could not init " + videoCategory, e);
			Toast.makeText(getContext(),
					String.format(getContext().getString(R.string.could_not_get_videos), videoCategory.toString()),
					Toast.LENGTH_LONG).show();
			this.currentVideoCategory = null;
		}
	}



	@Override
	public GridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


		if (viewType == AD_TYPE) {
			View inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout
					.native_ad_layout_facebook, parent, false);
			return new AdHolder(inflatedView);
		} else {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_cell, parent, false);
			final GridViewHolder gridViewHolder = new GridViewHolder(v, listener, showChannelInfo);
			gridViewHolder.setGridViewHolderListener(new GridViewHolder.GridViewHolderListener() {
				@Override
				public void onClick() {
					activeGridViewHolder = gridViewHolder;
				}
			});
			return gridViewHolder;
		}
	}
    @Override
    public int getItemViewType(int position) {
		/*if ((position % AD_DISPLAY_FREQUENCY) == 0 && position!= 0) {
			return  AD_TYPE;
		}
        return POST_TYPE;*/
		return position % AD_DISPLAY_FREQUENCY == 0 ? AD_TYPE : POST_TYPE;
    }


	public void refreshActiveGridViewHolder() {
		if(activeGridViewHolder != null)
			activeGridViewHolder.updateViewsData(getContext());
	}

	/**
	 * Refresh the video grid, by running the task to get the videos again.
	 */
	public void refresh() {
		refresh(false);
	}


	/**
	 * Refresh the video grid, by running the task to get the videos again.
	 *
	 * @param clearVideosList If set to true, it will clear out any previously loaded videos (found
	 *                        in this adapter).
	 */
	public void refresh(boolean clearVideosList) {
		if (getYouTubeVideos != null) {
			if (clearVideosList) {
				getYouTubeVideos.reset();
			}

			new GetYouTubeVideosTask(getYouTubeVideos, this, swipeRefreshLayout, clearVideosList).executeInParallel();
		}
	}

    @Override
    public int getItemCount() {
        return  getList().size() + mAdItems.size();
    }

	@Override
	public void onBindViewHolder(GridViewHolder viewHolder, int position) {
        if (viewHolder.getItemViewType() == AD_TYPE) {
            NativeAd ad;

            if (mAdItems.size() > position / AD_DISPLAY_FREQUENCY) {
                ad = mAdItems.get(position / AD_DISPLAY_FREQUENCY);
            } else {
                ad = mNativeAdsManager.nextNativeAd();
                mAdItems.add(ad);
            }

            AdHolder adHolder = (AdHolder) viewHolder;
            adHolder.adChoicesContainer.removeAllViews();

            if (ad != null) {

                adHolder.tvAdTitle.setText(ad.getAdvertiserName());
                adHolder.tvAdBody.setText(ad.getAdBodyText());
                adHolder.tvAdSocialContext.setText(ad.getAdSocialContext());
                adHolder.tvAdSponsoredLabel.setText(ad.getSponsoredTranslation());
                adHolder.btnAdCallToAction.setText(ad.getAdCallToAction());
                adHolder.btnAdCallToAction.setVisibility(
                        ad.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);
                AdChoicesView adChoicesView = new AdChoicesView(context,
                        ad, true);
                adHolder.adChoicesContainer.addView(adChoicesView, 0);

                List<View> clickableViews = new ArrayList<>();
                clickableViews.add(adHolder.ivAdIcon);
                clickableViews.add(adHolder.mvAdMedia);
                clickableViews.add(adHolder.btnAdCallToAction);
                ad.registerViewForInteraction(
                        adHolder.itemView,
                        adHolder.mvAdMedia,
                        adHolder.ivAdIcon,
                        clickableViews);
            }
        } else {

            if (viewHolder != null) {
                //Calculate where the next postItem index is by subtracting ads we've shown.
				int index = position - (position / AD_DISPLAY_FREQUENCY) - 1;
                viewHolder.updateInfo(get(index), getContext(), listener);
            }

            // if it reached the bottom of the list, then try to get the next page of videos
            if (position >= getItemCount() - 1) {
                Log.w(TAG, "BOTTOM REACHED!!!");
                if(getYouTubeVideos != null)
                    new GetYouTubeVideosTask(getYouTubeVideos, this, swipeRefreshLayout, false).executeInParallel();
            }
        }



	}


	public void setSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
		this.swipeRefreshLayout = swipeRefreshLayout;
	}

	public void setYouTubeChannel(YouTubeChannel youTubeChannel) {
		this.youTubeChannel = youTubeChannel;
	}

	public YouTubeChannel getYouTubeChannel() {
		return youTubeChannel;
	}


    private static class AdHolder extends GridViewHolder {
		MediaView mvAdMedia;
		AdIconView ivAdIcon;
		TextView tvAdTitle;
		TextView tvAdBody;
		TextView tvAdSocialContext;
		TextView tvAdSponsoredLabel;
		Button btnAdCallToAction;
		LinearLayout adChoicesContainer;

		AdHolder(View view) {
			super(view);

			mvAdMedia = (MediaView) view.findViewById(R.id.native_ad_media);
			tvAdTitle = (TextView) view.findViewById(R.id.native_ad_title);
			tvAdBody = (TextView) view.findViewById(R.id.native_ad_body);
			tvAdSocialContext = (TextView) view.findViewById(R.id.native_ad_social_context);
			tvAdSponsoredLabel = (TextView) view.findViewById(R.id.native_ad_sponsored_label);
			btnAdCallToAction = (Button) view.findViewById(R.id.native_ad_call_to_action);
			ivAdIcon = (AdIconView) view.findViewById(R.id.native_ad_icon);
			adChoicesContainer = (LinearLayout) view.findViewById(R.id.ad_choices_container);

		}
	}

}
