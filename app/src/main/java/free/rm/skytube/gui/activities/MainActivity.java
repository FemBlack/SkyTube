/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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

package free.rm.skytube.gui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.ads.Ad;
import com.facebook.ads.AdChoicesView;
import com.facebook.ads.AdError;
import com.facebook.ads.AdIconView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubePlaylist;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;
import free.rm.skytube.gui.businessobjects.updates.UpdatesCheckerTask;
import free.rm.skytube.gui.fragments.ChannelBrowserFragment;
import free.rm.skytube.gui.fragments.MainFragment;
import free.rm.skytube.gui.fragments.PlaylistVideosFragment;
import free.rm.skytube.gui.fragments.SearchVideoGridFragment;
import free.rm.skytube.gui.fragments.VideosGridFragment;
import timber.log.Timber;

import static free.rm.skytube.app.SkyTubeApp.getContext;

/**
 * Main activity (launcher).  This activity holds {@link free.rm.skytube.gui.fragments.VideosGridFragment}.
 */
public class MainActivity extends AppCompatActivity implements MainActivityListener,Serializable,PermissionsActivity.PermissionsTask {

	@BindView(R.id.fragment_container)
	protected transient FrameLayout           fragmentContainer;

	private transient MainFragment            mainFragment;
	private transient SearchVideoGridFragment searchVideoGridFragment;
	private transient ChannelBrowserFragment  channelBrowserFragment;
	/** Fragment that shows Videos from a specific Playlist */
	private transient PlaylistVideosFragment  playlistVideosFragment;
	//private transient VideoBlockerPlugin      videoBlockerPlugin;

	private transient boolean dontAddToBackStack = false;

	/** Set to true of the UpdatesCheckerTask has run; false otherwise. */
	private static boolean updatesCheckerTaskRan = false;

	public static final String ACTION_VIEW_CHANNEL = "MainActivity.ViewChannel";
	public static final String ACTION_VIEW_FEED = "MainActivity.ViewFeed";
	public static final String ACTION_VIEW_PLAYLIST = "MainActivity.ViewPlaylist";
	private static final String MAIN_FRAGMENT   = "MainActivity.MainFragment";
	private static final String SEARCH_FRAGMENT = "MainActivity.SearchFragment";
	private static final String CHANNEL_BROWSER_FRAGMENT = "MainActivity.ChannelBrowserFragment";
	private static final String PLAYLIST_VIDEOS_FRAGMENT = "MainActivity.PlaylistVideosFragment";
	private static final String VIDEO_BLOCKER_PLUGIN = "MainActivity.VideoBlockerPlugin";
    private transient InterstitialAd mInterstitialAd;

	public static final String LOGTAG = VideosGridFragment.class.getName();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.app_name_vibe);
        displayPermissionsActivity();
		// check for updates (one time only)
		if (!updatesCheckerTaskRan) {
			new UpdatesCheckerTask(this, false).executeInParallel();
			updatesCheckerTaskRan = true;
		}

		SkyTubeApp.setFeedUpdateInterval();
		// Delete any missing downloaded videos
		new DownloadedVideosDb.RemoveMissingVideosTask().executeInParallel();

		setContentView(R.layout.activity_fragment_holder);
		ButterKnife.bind(this);

		if(fragmentContainer != null) {
			if(savedInstanceState != null) {
				mainFragment = (MainFragment)getSupportFragmentManager().getFragment(savedInstanceState, MAIN_FRAGMENT);
				searchVideoGridFragment = (SearchVideoGridFragment) getSupportFragmentManager().getFragment(savedInstanceState, SEARCH_FRAGMENT);
				channelBrowserFragment = (ChannelBrowserFragment) getSupportFragmentManager().getFragment(savedInstanceState, CHANNEL_BROWSER_FRAGMENT);
				playlistVideosFragment = (PlaylistVideosFragment) getSupportFragmentManager().getFragment(savedInstanceState, PLAYLIST_VIDEOS_FRAGMENT);
			}

			// If this Activity was called to view a particular channel, display that channel via ChannelBrowserFragment, instead of MainFragment
			String action = getIntent().getAction();
			if(ACTION_VIEW_CHANNEL.equals(action)) {
				dontAddToBackStack = true;
				YouTubeChannel channel = (YouTubeChannel) getIntent().getSerializableExtra(ChannelBrowserFragment.CHANNEL_OBJ);
				onChannelClick(channel);
			} else if(ACTION_VIEW_PLAYLIST.equals(action)) {
				dontAddToBackStack = true;
				YouTubePlaylist playlist = (YouTubePlaylist)getIntent().getSerializableExtra(PlaylistVideosFragment.PLAYLIST_OBJ);
				onPlaylistClick(playlist);
			} else {
				if(mainFragment == null) {
					mainFragment = new MainFragment();
					// If we're coming here via a click on the Notification that new videos for subscribed channels have been found, make sure to
					// select the Feed tab.
					if(action != null && action.equals(ACTION_VIEW_FEED)) {
						Bundle args = new Bundle();
						args.putBoolean(MainFragment.SHOULD_SELECTED_FEED_TAB, true);
						mainFragment.setArguments(args);
					}
					getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mainFragment).commit();
				}
			}
		}

		/*if (savedInstanceState != null) {
			// restore the video blocker plugin
			this.videoBlockerPlugin = (VideoBlockerPlugin) savedInstanceState.getSerializable(VIDEO_BLOCKER_PLUGIN);
			this.videoBlockerPlugin.setActivity(this);
		} else {
			this.videoBlockerPlugin = new VideoBlockerPlugin(this);
		}*/

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
		mInterstitialAd.setAdListener(new AdListener() {
			public void onAdLoaded() {
				// Call displayInterstitial() function
				//displayInterstitial();
			}

			public void onAdClosed() {
				// Request a new ad if one isn't already loaded, hide the button, and kick off the timer.
				if (!mInterstitialAd.isLoading() && !mInterstitialAd.isLoaded()) {
					AdRequest adRequest = new AdRequest.Builder().build();
					mInterstitialAd.loadAd(adRequest);
				}

			}

			public void onAdClicked() {
			}

			public void onAdFailedToLoad(int var1) {
				//AppUtil.startApp(service, appInfo);
			}

		});

		if (!isInternetIsConnected(this)) {
			new MaterialDialog.Builder(this)
					.content(R.string.no_internet)
					.positiveText(R.string.ok)
					.show();
			return;
		}
	}

    public void displayPermissionsActivity() {
        Intent i = new Intent(getContext(), PermissionsActivity.class);
        i.putExtra(PermissionsActivity.PERMISSIONS_TASK_OBJ, this);
        startActivity(i);
    }

	@Override
	public void onExternalStoragePermissionsGranted() {

	}

	public static boolean isInternetIsConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork != null) { // connected to the internet
			if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
				// connected to wifi
				return true;

			} else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
				// connected to the mobile provider's data plan
				return true;
			}
		} else {
			// not connected to the internet
			return false;
		}
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if(mainFragment != null)
			getSupportFragmentManager().putFragment(outState, MAIN_FRAGMENT, mainFragment);
		if(searchVideoGridFragment != null && searchVideoGridFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, SEARCH_FRAGMENT, searchVideoGridFragment);
		if(channelBrowserFragment != null && channelBrowserFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, CHANNEL_BROWSER_FRAGMENT, channelBrowserFragment);
		if(playlistVideosFragment != null && playlistVideosFragment.isVisible())
			getSupportFragmentManager().putFragment(outState, PLAYLIST_VIDEOS_FRAGMENT, playlistVideosFragment);

		// save the video blocker plugin
		//outState.putSerializable(VIDEO_BLOCKER_PLUGIN, videoBlockerPlugin);
	}


	@Override
	protected void onResume() {
		super.onResume();

		// Activity may be destroyed when the devices is rotated, so we need to make sure that the
		// channel play list is holding a reference to the activity being currently in use...
		if (channelBrowserFragment != null)
			channelBrowserFragment.getChannelPlaylistsFragment().setMainActivityListener(this);
	}


	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.main_activity_menu, menu);

		// setup the video blocker notification icon which will be displayed in the tool bar
		//videoBlockerPlugin.setupIconForToolBar(menu);

		// setup the SearchView (actionbar)
		final MenuItem searchItem = menu.findItem(R.id.menu_search);
		final SearchView searchView = (SearchView) searchItem.getActionView();

		searchView.setQueryHint(getString(R.string.search_videos));

		// set the query hints to be equal to the previously searched text
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(final String newText) {
				// if the user does not want to have the search string saved, then skip the below...
				/*if (SkyTubeApp.getPreferenceManager().getBoolean(getString(R.string.pref_key_disable_search_history), false)
						||  newText == null  ||  newText.length() <= 1) {
					return false;
				}

				SearchHistoryCursorAdapter searchHistoryCursorAdapter = (SearchHistoryCursorAdapter) searchView.getSuggestionsAdapter();
				Cursor cursor = SearchHistoryDb.getSearchHistoryDb().getSearchCursor(newText);

				// if the adapter has not been created, then create it
				if (searchHistoryCursorAdapter == null) {
					searchHistoryCursorAdapter = new SearchHistoryCursorAdapter(getBaseContext(),
							R.layout.search_hint,
							cursor,
							new String[]{SearchHistoryTable.COL_SEARCH_TEXT},
							new int[]{android.R.id.text1},
							0);
					searchHistoryCursorAdapter.setSearchHistoryClickListener(new SearchHistoryClickListener() {
						@Override
						public void onClick(String query) {
							displaySearchResults(query);
						}
					});
					searchView.setSuggestionsAdapter(searchHistoryCursorAdapter);
				} else {
					// else just change the cursor...
					searchHistoryCursorAdapter.changeCursor(cursor);
				}*/

				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				// hide the keyboard
				searchView.clearFocus();

				/*if(!SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_disable_search_history), false)) {
					// Save this search string into the Search History Database (for Suggestions)
					SearchHistoryDb.getSearchHistoryDb().insertSearchText(query);
				}*/

				displaySearchResults(query);

				return true;
			}
		});

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			/*case R.id.menu_blocker:
				videoBlockerPlugin.onMenuBlockerIconClicked();
				return true;*/
			case R.id.menu_preferences:
                if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Timber.d("The interstitial wasn't loaded yet.");
                }
				Intent i = new Intent(this, PreferencesActivity.class);
				startActivity(i);
				return true;
			case R.id.menu_enter_video_url:
				displayEnterVideoUrlDialog();
				return true;
			case android.R.id.home:
				if(mainFragment == null || !mainFragment.isVisible()) {
					onBackPressed();
					return true;
				}
		}

		return super.onOptionsItemSelected(item);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}


	/**
	 * Display the Enter Video URL dialog.
	 */
	private void displayEnterVideoUrlDialog() {
		final AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
			.setView(R.layout.dialog_enter_video_url)
			.setTitle(R.string.enter_video_url)
			.setPositiveButton(R.string.play, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// get the inputted URL string
					final String videoUrl = ((EditText)((AlertDialog) dialog).findViewById(R.id.dialog_url_edittext)).getText().toString();

					// play the video
					YouTubePlayer.launch(videoUrl, MainActivity.this);
				}
			})
			.setNegativeButton(R.string.cancel, null)
			.show();

		// paste whatever there is in the clipboard (hopefully it is a video url)
		((EditText) alertDialog.findViewById(R.id.dialog_url_edittext)).setText(getClipboardItem());

		// clear URL edittext button
		alertDialog.findViewById(R.id.dialog_url_clear_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((EditText) alertDialog.findViewById(R.id.dialog_url_edittext)).setText("");
			}
		});
	}


	/**
	 * Return the last item stored in the clipboard.
	 *
	 * @return	{@link String}
	 */
	private String getClipboardItem() {
		String              clipboardText    = "";
		ClipboardManager    clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

		// if the clipboard contain data ...
		if (clipboardManager != null  &&  clipboardManager.hasPrimaryClip()) {
			ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);

			// gets the clipboard as text.
			clipboardText = item.getText().toString();
		}

		return clipboardText;
	}


	@Override
	public void onBackPressed() {
		if (mainFragment != null  &&  mainFragment.isVisible()) {
			// If the Subscriptions Drawer is open, close it instead of minimizing the app.
			if(mainFragment.isDrawerOpen()) {
				mainFragment.closeDrawer();
			} else {
				showExitDialog();
			}
		} else {
			super.onBackPressed();
		}
	}

	private MaterialDialog md;
	private TextView textView;
	private ProgressBar progressBar;

	private void showExitDialog() {
		md = new MaterialDialog.Builder(this)
				.title(  R.string.exit_msg )
				.customView(R.layout.mrect_ad_facebook, true)
				.positiveText(R.string.ok)
				.negativeText(R.string.cancel)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(MaterialDialog dialog, DialogAction which) {
						// On Android, when the user presses back button, the Activity is destroyed and will be
						// recreated when the user relaunches the app.
						// We do not want that behaviour, instead then the back button is pressed, the app will
						// be **minimized**.
						Intent startMain = new Intent(Intent.ACTION_MAIN);
						startMain.addCategory(Intent.CATEGORY_HOME);
						startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(startMain);
					}
				})
				.onNegative(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(MaterialDialog dialog, DialogAction which) {
						md.dismiss();
					}
				})
				.build();
		textView = (TextView) md.findViewById(R.id.textView);
		progressBar = (ProgressBar) md.findViewById(R.id.progressBar);
		progressBar.setVisibility(View.GONE);
		//textView.setText(R.string.exit_msg);
		loadNativeAd(md);
		md.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				if (nativeAd != null) {
					nativeAd.unregisterView();
					nativeAd.destroy();
				}
			}
		});
		md.setCancelable(false);
		md.show();
	}

	NativeAd nativeAd;
	private LinearLayout adView;
	/* private final int MAX_NUMBER_OF_RETRIES = 3;
     private int retryCount = 0;*/
	private void loadNativeAd(final MaterialDialog md) {
		// Instantiate a NativeAd object.
		// NOTE: the placement ID will eventually identify this as your App, you can ignore it for
		// now, while you are testing and replace it later when you have signed up.
		// While you are using this temporary code you will only get test ads and if you release
		// your code like this to the Google Play your users will not receive ads (you will get a no fill error).
		nativeAd = new NativeAd(this, "2363436417216774_2365566657003750");
		nativeAd.setAdListener(new NativeAdListener() {
			@Override
			public void onMediaDownloaded(Ad ad) {

			}

			@Override
			public void onError(Ad ad, AdError adError) {
                /*if(retryCount < MAX_NUMBER_OF_RETRIES) {
                    retryCount += 1;
                    nativeAd.loadAd();
                }*/
			}

			@Override
			public void onAdLoaded(Ad ad) {
				// Race condition, load() called again before last ad was displayed
				if (nativeAd == null || nativeAd != ad) {
					return;
				}

				nativeAd.unregisterView();


				// Inflate Native Ad into Container
				inflateAd(nativeAd,md);
			}

			@Override
			public void onAdClicked(Ad ad) {

			}

			@Override
			public void onLoggingImpression(Ad ad) {

			}
		});
		// Request an ad
		nativeAd.loadAd();
	}

	private void inflateAd(NativeAd nativeAd, MaterialDialog md) {

		nativeAd.unregisterView();

		// Add the Ad view into the ad container.
		LinearLayout nativeAdContainer = (LinearLayout) md.findViewById(R.id.native_ad_container);
		LayoutInflater inflater = LayoutInflater.from(this);
		// Inflate the Ad view.  The layout referenced should be the one you created in the last step.
		adView = (LinearLayout) inflater.inflate(R.layout.native_ad_layout_facebook, nativeAdContainer, false);
		nativeAdContainer.addView(adView);

		// Add the AdChoices icon
		LinearLayout adChoicesContainer = (LinearLayout) md.findViewById(R.id.ad_choices_container);
		AdChoicesView adChoicesView = new AdChoicesView(this, nativeAd, true);
		adChoicesContainer.addView(adChoicesView, 0);

		// Create native UI using the ad metadata.
		AdIconView nativeAdIcon = adView.findViewById(R.id.native_ad_icon);
		TextView nativeAdTitle = adView.findViewById(R.id.native_ad_title);
		MediaView nativeAdMedia = adView.findViewById(R.id.native_ad_media);
		TextView nativeAdSocialContext = adView.findViewById(R.id.native_ad_social_context);
		TextView nativeAdBody = adView.findViewById(R.id.native_ad_body);
		TextView sponsoredLabel = adView.findViewById(R.id.native_ad_sponsored_label);
		Button nativeAdCallToAction = adView.findViewById(R.id.native_ad_call_to_action);

		// Set the Text.
		nativeAdTitle.setText(nativeAd.getAdvertiserName());
		nativeAdBody.setText(nativeAd.getAdBodyText());
		nativeAdSocialContext.setText(nativeAd.getAdSocialContext());
		nativeAdCallToAction.setVisibility(nativeAd.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);
		nativeAdCallToAction.setText(nativeAd.getAdCallToAction());
		sponsoredLabel.setText(nativeAd.getSponsoredTranslation());

		// Create a list of clickable views
		List<View> clickableViews = new ArrayList<>();
		clickableViews.add(nativeAdTitle);
		clickableViews.add(nativeAdMedia);
		clickableViews.add(nativeAdCallToAction);


		// Register the Title and CTA button to listen for clicks.
		nativeAd.registerViewForInteraction(
				adView,
				nativeAdMedia,
				nativeAdIcon,
				clickableViews);
	}


	private void switchToFragment(Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		transaction.replace(R.id.fragment_container, fragment);
		if(!dontAddToBackStack)
			transaction.addToBackStack(null);
		else
			dontAddToBackStack = false;
		transaction.commit();
	}


	@Override
	public void onChannelClick(YouTubeChannel channel) {
		Bundle args = new Bundle();
		args.putSerializable(ChannelBrowserFragment.CHANNEL_OBJ, channel);
		switchToChannelBrowserFragment(args);
	}


	@Override
	public void onChannelClick(String channelId) {
		Bundle args = new Bundle();
		args.putString(ChannelBrowserFragment.CHANNEL_ID, channelId);
		switchToChannelBrowserFragment(args);
	}


	private void switchToChannelBrowserFragment(Bundle args) {
		channelBrowserFragment = new ChannelBrowserFragment();
		channelBrowserFragment.getChannelPlaylistsFragment().setMainActivityListener(this);
		channelBrowserFragment.setArguments(args);
		switchToFragment(channelBrowserFragment);
	}


	@Override
	public void onPlaylistClick(YouTubePlaylist playlist) {
		playlistVideosFragment = new PlaylistVideosFragment();
		Bundle args = new Bundle();
		args.putSerializable(PlaylistVideosFragment.PLAYLIST_OBJ, playlist);
		playlistVideosFragment.setArguments(args);
		switchToFragment(playlistVideosFragment);
	}


	/**
	 * Switch to the Search Video Grid Fragment with the selected query to search for videos.
	 * @param query
	 */
	private void displaySearchResults(String query) {
		// open SearchVideoGridFragment and display the results
		searchVideoGridFragment = new SearchVideoGridFragment();
		Bundle bundle = new Bundle();
		bundle.putString(SearchVideoGridFragment.QUERY, query);
		searchVideoGridFragment.setArguments(bundle);
		switchToFragment(searchVideoGridFragment);
	}



	//////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * A module/"plugin"/icon that displays the total number of blocked videos.
	 */
	/*private static class VideoBlockerPlugin implements VideoBlocker.VideoBlockerListener,
			BlockedVideosDialog.BlockedVideosDialogListener,
			Serializable {

		private ArrayList<VideoBlocker.BlockedVideo> blockedVideos = new ArrayList<>();
		private transient AppCompatActivity activity = null;


		VideoBlockerPlugin(AppCompatActivity activity) {
			// notify this class whenever a video is blocked...
			VideoBlocker.setVideoBlockerListener(this);
			this.activity = activity;
		}


		public void setActivity(AppCompatActivity activity) {
			this.activity = activity;
		}


		@Override
		public void onVideoBlocked(VideoBlocker.BlockedVideo blockedVideo) {
			blockedVideos.add(blockedVideo);
			activity.invalidateOptionsMenu();
		}


		*//**
		 * Setup the video blocker notification icon which will be displayed in the tool bar.
 		 *//*
		void setupIconForToolBar(final Menu menu) {
			if (getTotalBlockedVideos() > 0) {
				// display a red bubble containing the number of blocked videos
				ActionItemBadge.update(activity,
						menu.findItem(R.id.menu_blocker),
						ContextCompat.getDrawable(activity, R.drawable.ic_video_blocker),
						ActionItemBadge.BadgeStyles.RED,
						getTotalBlockedVideos());
			} else {
				// Else, set the bubble to transparent.  This is required so that when the user
				// clicks on the icon, the app will be able to detect such click and displays the
				// BlockedVideosDialog (otherwise, the ActionItemBadge would just ignore such clicks.
				ActionItemBadge.update(activity,
						menu.findItem(R.id.menu_blocker),
						ContextCompat.getDrawable(activity, R.drawable.ic_video_blocker),
						new BadgeStyle(BadgeStyle.Style.DEFAULT, com.mikepenz.actionitembadge.library.R.layout.menu_action_item_badge, Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE),
						"");
			}
		}


		void onMenuBlockerIconClicked() {
			new BlockedVideosDialog(activity, this, blockedVideos).show();
		}


		@Override
		public void onClearBlockedVideos() {
			blockedVideos.clear();
			activity.invalidateOptionsMenu();
		}


		*//**
		 * @return Total number of blocked videos.
		 *//*
		private int getTotalBlockedVideos() {
			return blockedVideos.size();
		}

	}
*/



}
