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

package free.rm.skytube.gui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.AsyncTaskParallel;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.GetVideosDetailsByIDs;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannelInterface;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetVideoDescriptionTask;
import free.rm.skytube.businessobjects.YouTube.Tasks.GetYouTubeChannelInfoTask;
import free.rm.skytube.businessobjects.YouTube.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.YoutubeDownloader;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.db.Tasks.CheckIfUserSubbedToChannelTask;
import free.rm.skytube.businessobjects.db.Tasks.IsVideoBookmarkedTask;
import free.rm.skytube.businessobjects.interfaces.GetDesiredStreamListener;
import free.rm.skytube.businessobjects.interfaces.YouTubePlayerFragmentInterface;
import free.rm.skytube.gui.activities.MainActivity;
import free.rm.skytube.gui.activities.ThumbnailViewerActivity;
import free.rm.skytube.gui.businessobjects.PlayerViewGestureDetector;
import free.rm.skytube.gui.businessobjects.SkyTubeMaterialDialog;
import free.rm.skytube.gui.businessobjects.SubscribeButton;
import free.rm.skytube.gui.businessobjects.adapters.CommentsAdapter;
import free.rm.skytube.gui.businessobjects.fragments.ImmersiveModeFragment;
import hollowsoft.slidingdrawer.OnDrawerOpenListener;
import hollowsoft.slidingdrawer.SlidingDrawer;

/**
 * A fragment that holds a standalone YouTube player (version 2).
 */
public class YouTubePlayerV2Fragment extends ImmersiveModeFragment implements YouTubePlayerFragmentInterface {

	private YouTubeVideo		youTubeVideo = null;
	private YouTubeChannel      youTubeChannel = null;

	private PlayerView          playerView;
	private SimpleExoPlayer     player;
	private long				playerInitialPosition = 0;

	private Menu                menu = null;

	private TextView			videoDescTitleTextView = null;
	private ImageView			videoDescChannelThumbnailImageView = null;
	private TextView			videoDescChannelTextView = null;
	private SubscribeButton     videoDescSubscribeButton = null;
	private TextView			videoDescViewsTextView = null;
	private ProgressBar         videoDescLikesBar = null;
	private TextView			videoDescLikesTextView = null;
	private TextView			videoDescDislikesTextView = null;
	private View                videoDescRatingsDisabledTextView = null;
	private TextView			videoDescPublishDateTextView = null;
	private TextView			videoDescriptionTextView = null;
	private View				loadingVideoView = null;
	private SlidingDrawer       videoDescriptionDrawer = null;
	private SlidingDrawer		commentsDrawer = null;
	private View				commentsProgressBar = null,
								noVideoCommentsView = null;
	private CommentsAdapter     commentsAdapter = null;
	private ExpandableListView  commentsExpandableListView = null;
	//private AdView mAdView;

	public static final String YOUTUBE_VIDEO_OBJ = "YouTubePlayerFragment.yt_video_obj";
	public static final String YOUTUBE_URL = "https://www.youtube.com/watch?v=";


	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		hideNavigationBar();

		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_youtube_player_v2, container, false);

		// prevent the device from sleeping while playing
		if (getActivity() != null  &&  (getActivity().getWindow()) != null) {
			getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		// indicate that this fragment has an action bar menu
		setHasOptionsMenu(true);

//		final View decorView = getActivity().getWindow().getDecorView();
//		decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
//			@Override
//			public void onSystemUiVisibilityChange(int visibility) {
//				hideNavigationBar();
//			}
//		});

		///if (savedInstanceState != null)
		///	videoCurrentPosition = savedInstanceState.getInt(VIDEO_CURRENT_POSITION, 0);

		if (youTubeVideo == null) {
			// initialise the views
			initViews(view);

			// get which video we need to play...
			Bundle bundle = getActivity().getIntent().getExtras();
			if (bundle != null  &&  bundle.getSerializable(YOUTUBE_VIDEO_OBJ) != null) {
				// ... either the video details are passed through the previous activity
				youTubeVideo = (YouTubeVideo) bundle.getSerializable(YOUTUBE_VIDEO_OBJ);
				setUpHUDAndPlayVideo();

				getVideoInfoTasks();
			} else {
				// ... or the video URL is passed to SkyTube via another Android app
				new GetVideoDetailsTask().executeInParallel();
			}

		}

		return view;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		//renewAd();
	}

	/*private void renewAd() {
		// Remove the ad keeping the attributes
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mAdView.getLayoutParams();
		*//*final boolean orientation = getContext().getResources().getBoolean(R.bool.is_landscape);
		if (orientation) {
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
		} else {
			lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
		}
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);*//*

		RelativeLayout parentLayout = (RelativeLayout) mAdView.getParent();
		parentLayout.removeView(mAdView);

// Re-initialise the ad
		mAdView.destroy();
		mAdView = new AdView(getContext());
		mAdView.setAdSize(com.google.android.gms.ads.AdSize.SMART_BANNER);
		mAdView.setAdUnitId(getContext().getString(R.string.banner_ad_unit_id));
		mAdView.setId(R.id.adView);
		mAdView.setLayoutParams(lp);
		parentLayout.addView(mAdView);

// Re-fetch add and check successful load
		*//*AdRequest adRequest = new AdRequest.Builder()
				.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
				.addTestDevice(parent.getString(R.string.test_device_id))
				.build();*//*

		AdRequest.Builder adRequest = new AdRequest.Builder();
		//adRequest.addTestDevice("C284D5A398D80F7CE733BAAC7372C233");
		mAdView.loadAd(adRequest.build());
	}*/


	/**
	 * Initialise the views.
	 *
	 * @param view Fragment view.
	 */
	private void initViews(View view) {
		// setup the toolbar / actionbar
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		// setup the player
		final PlayerViewGestureHandler playerViewGestureHandler;
		playerView = view.findViewById(R.id.player_view);
		playerViewGestureHandler = new PlayerViewGestureHandler();
		playerViewGestureHandler.initView(view);
		playerView.setOnTouchListener(playerViewGestureHandler);
		playerView.requestFocus();

		DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

		TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
		DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

		player = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector);
		player.setPlayWhenReady(true);
		playerView.setPlayer(player);

		loadingVideoView = view.findViewById(R.id.loadingVideoView);

		videoDescriptionDrawer = view.findViewById(R.id.des_drawer);
		videoDescTitleTextView = view.findViewById(R.id.video_desc_title);
		videoDescChannelThumbnailImageView = view.findViewById(R.id.video_desc_channel_thumbnail_image_view);
		videoDescChannelThumbnailImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (youTubeChannel != null) {
					Intent i = new Intent(getActivity(), MainActivity.class);
					i.setAction(MainActivity.ACTION_VIEW_CHANNEL);
					i.putExtra(ChannelBrowserFragment.CHANNEL_OBJ, youTubeChannel);
					startActivity(i);
				}
			}
		});
		videoDescChannelTextView = view.findViewById(R.id.video_desc_channel);
		videoDescViewsTextView = view.findViewById(R.id.video_desc_views);
		videoDescLikesTextView = view.findViewById(R.id.video_desc_likes);
		videoDescDislikesTextView = view.findViewById(R.id.video_desc_dislikes);
		videoDescRatingsDisabledTextView = view.findViewById(R.id.video_desc_ratings_disabled);
		videoDescPublishDateTextView = view.findViewById(R.id.video_desc_publish_date);
		videoDescriptionTextView = view.findViewById(R.id.video_desc_description);
		videoDescLikesBar = view.findViewById(R.id.video_desc_likes_bar);
		videoDescSubscribeButton = view.findViewById(R.id.video_desc_subscribe_button);

		commentsExpandableListView = view.findViewById(R.id.commentsExpandableListView);
		commentsProgressBar = view.findViewById(R.id.comments_progress_bar);
		noVideoCommentsView = view.findViewById(R.id.no_video_comments_text_view);
		commentsDrawer = view.findViewById(R.id.comments_drawer);
		commentsDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {
			@Override
			public void onDrawerOpened() {
				if (commentsAdapter == null) {
					commentsAdapter = new CommentsAdapter(getActivity(), youTubeVideo.getId(), commentsExpandableListView, commentsProgressBar, noVideoCommentsView);
				}
			}
		});
		/*mAdView = view.findViewById(R.id.adView);
		AdRequest.Builder adRequest = new AdRequest.Builder();
		//adRequest.addTestDevice("C284D5A398D80F7CE733BAAC7372C233");
		mAdView.loadAd(adRequest.build());*/
	}


	/**
	 * Will setup the HUD's details according to the contents of {@link #youTubeVideo}.  Then it
	 * will try to load and play the video.
	 */
	private void setUpHUDAndPlayVideo() {
		getSupportActionBar().setTitle(youTubeVideo.getTitle());
		videoDescTitleTextView.setText(youTubeVideo.getTitle());
		videoDescChannelTextView.setText(youTubeVideo.getChannelName());
		videoDescViewsTextView.setText(youTubeVideo.getViewsCount());
		videoDescPublishDateTextView.setText(youTubeVideo.getPublishDatePretty());

		if (youTubeVideo.isThumbsUpPercentageSet()) {
			videoDescLikesTextView.setText(youTubeVideo.getLikeCount());
			videoDescDislikesTextView.setText(youTubeVideo.getDislikeCount());
			videoDescLikesBar.setProgress(youTubeVideo.getThumbsUpPercentage());
		} else {
			videoDescLikesTextView.setVisibility(View.GONE);
			videoDescDislikesTextView.setVisibility(View.GONE);
			videoDescLikesBar.setVisibility(View.GONE);
			videoDescRatingsDisabledTextView.setVisibility(View.VISIBLE);
		}


		// ask the user if he wants to resume playing this video (if he has played it in the past...)
		if(!SkyTubeApp.getPreferenceManager().getBoolean(getString(R.string.pref_key_disable_playback_status), false) && PlaybackStatusDb.getVideoDownloadsDb().getVideoWatchedStatus(youTubeVideo).position > 0) {
			new SkyTubeMaterialDialog(getContext())
					.content(R.string.should_resume)
					.positiveText(R.string.resume)
					.onPositive(new MaterialDialog.SingleButtonCallback() {
						@Override
						public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
							playerInitialPosition = PlaybackStatusDb.getVideoDownloadsDb().getVideoWatchedStatus(youTubeVideo).position;
							loadVideo();
						}
					})
					.negativeText(R.string.no)
					.onNegative(new MaterialDialog.SingleButtonCallback() {
						@Override
						public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
							loadVideo();
						}
					})
					.show();
		} else {
			loadVideo();
		}
	}


	/**
	 * Loads the video specified in {@link #youTubeVideo}.
	 */
	private void loadVideo() {
		// if the video is NOT live
		if (!youTubeVideo.isLiveStream()) {
			loadingVideoView.setVisibility(View.VISIBLE);
			if(youTubeVideo.isDownloaded()) {
				Uri uri = youTubeVideo.getFileUri();
				File file = new File(uri.getPath());
				// If the file for this video has gone missing, remove it from the Database and then play remotely.
				if(!file.exists()) {
					DownloadedVideosDb.getVideoDownloadsDb().remove(youTubeVideo);
					Toast.makeText(getContext(),
									getContext().getString(R.string.playing_video_file_missing),
									Toast.LENGTH_LONG).show();
					loadVideo();
				} else {
					loadingVideoView.setVisibility(View.GONE);

					Logger.i(YouTubePlayerV2Fragment.this, ">> PLAYING LOCALLY: %s", youTubeVideo);
					playVideo(uri);
				}
			} else {
				youTubeVideo.getDesiredStream(new GetDesiredStreamListener() {
					@Override
					public void onGetDesiredStream(StreamMetaData desiredStream) {
						// hide the loading video view (progress bar)
						loadingVideoView.setVisibility(View.GONE);

						// Play the video.  Check if this fragment is visible before playing the
						// video.  It might not be visible if the user clicked on the back button
						// before the video streams are retrieved (such action would cause the app
						// to crash if not catered for...).
						if (isVisible()) {
							Logger.i(YouTubePlayerV2Fragment.this, ">> PLAYING: %s", desiredStream.getUri());
							playVideo(desiredStream.getUri());
						}
					}

					@Override
					public void onGetDesiredStreamError(String errorMessage) {
						if (errorMessage != null && getContext() != null) {
							new AlertDialog.Builder(getContext())
											.setMessage(errorMessage)
											.setTitle(R.string.error_video_play)
											.setCancelable(false)
											.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialog, int which) {
													getActivity().finish();
												}
											})
											.show();
						}
					}
				});
			}
		} else {
			// video is live:  ask the user if he wants to play the video using an other app
			new AlertDialog.Builder(getContext())
					.setMessage(R.string.warning_live_video)
					.setTitle(R.string.error_video_play)
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							closeActivity();
						}
					})
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							playVideoExternally();
							closeActivity();
						}
					})
					.show();
		}
	}


	/**
	 * Play video.
	 *
	 * @param videoUri  The Uri of the video that is going to be played.
	 */
	private void playVideo(Uri videoUri) {
		// Check if this fragment is visible before playing the video.  It might not be visible if
		// the user clicked on the back button (and hence cancelling this operation)...
			DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(getContext(), "ST. Agent", new DefaultBandwidthMeter());
			ExtractorMediaSource.Factory extMediaSourceFactory = new ExtractorMediaSource.Factory(dataSourceFactory);
			ExtractorMediaSource mediaSource = extMediaSourceFactory.createMediaSource(videoUri);
			player.prepare(mediaSource);
			if (playerInitialPosition > 0)
				player.seekTo(playerInitialPosition);
	}


	/**
	 * Play the video using an external app
	 */
	private void playVideoExternally() {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(youTubeVideo.getVideoUrl()));
		startActivity(browserIntent);
	}


	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		// Hide the download video option if mobile downloads are not allowed and the device is connected through mobile, and the video isn't already downloaded
		boolean allowDownloadsOnMobile = SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_allow_mobile_downloads), true);
		if((SkyTubeApp.isConnectedToWiFi() || (SkyTubeApp.isConnectedToMobile() && allowDownloadsOnMobile))) {
			menu.findItem(R.id.download_video).setVisible(true);
		} else {
			menu.findItem(R.id.download_video).setVisible(false);
		}
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_youtube_player, menu);

		this.menu = menu;

		// Will now check if the video is bookmarked or not (and then update the menu accordingly).
		//
		// youTubeVideo might be null if we have only passed the video URL to this fragment (i.e.
		// the app is still trying to construct youTubeVideo in the background).
		if (youTubeVideo != null)
			new IsVideoBookmarkedTask(youTubeVideo, menu).executeInParallel();
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_reload_video:
				loadVideo();
				return true;

			case R.id.menu_open_video_with:
				playVideoExternally();
				player.stop();
				return true;

			case R.id.share:
				youTubeVideo.shareVideo(getContext());
				return true;

			case R.id.copyurl:
				youTubeVideo.copyUrl(getContext());
				return true;

			/*case R.id.bookmark_video:
				youTubeVideo.bookmarkVideo(getContext(), menu);
				return true;

			case R.id.unbookmark_video:
				youTubeVideo.unbookmarkVideo(getContext(), menu);
				return true;*/

			case R.id.view_thumbnail:
				Intent i = new Intent(getActivity(), ThumbnailViewerActivity.class);
				i.putExtra(ThumbnailViewerActivity.YOUTUBE_VIDEO, youTubeVideo);
				startActivity(i);
				return true;

			case R.id.download_video:
				if (youTubeVideo != null) {
					if (youTubeVideo.isDownloaded(true)) {
						new YoutubeDownloader(youTubeVideo,getActivity());
						YoutubeDownloader.videoFile = new File(youTubeVideo.getFileUri(true).getPath());
						YoutubeDownloader.shareVideo();
					} else {
						YoutubeDownloader youtubeDownloader = new YoutubeDownloader(youTubeVideo,getActivity());
						youtubeDownloader.setVariables();
						youtubeDownloader.getYoutubeDownloadVideoList(YOUTUBE_URL + youTubeVideo.getId(),true);
					}
				}

				return true;
			case R.id.download_audio:
				if (youTubeVideo != null) {
					if (youTubeVideo.isDownloaded(false)) {
						new YoutubeDownloader(youTubeVideo,getActivity());
						YoutubeDownloader.videoFile = new File(youTubeVideo.getFileUri(false).getPath());
						YoutubeDownloader.shareVideo();
					} else {
						YoutubeDownloader youtubeDownloader = new YoutubeDownloader(youTubeVideo,getActivity());
						youtubeDownloader.setVariables();
						youtubeDownloader.setOutputFileExtension("mp3");
						youtubeDownloader.getYoutubeDownloadVideoList(YOUTUBE_URL + youTubeVideo.getId(),false);
					}
				}

				return true;

			/*case R.id.block_channel:
				youTubeChannel.blockChannel();*/

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/**
	 * Called when the options menu is closed.
	 *
	 * <p>The Navigation Bar is displayed when the Option Menu is visible.  Hence the objective of
	 * this method is to hide the Navigation Bar once the Options Menu is hidden.</p>
	 */
	public void onMenuClosed() {
		hideNavigationBar();
	}

	/*private void showListDialog(final Map<String,YtFile> map) {
		new MaterialDialog.Builder(getActivity())
				.title(R.string.download_video)
				.items(map.keySet())
				.itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
					@Override
					public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
						*//**
						 * If you use alwaysCallSingleChoiceCallback(), which is discussed below,
						 * returning false here won't allow the newly selected radio button to actually be selected.
						 **//*
						new DownloadFile().execute(map.get(dialog.getItems().get(which)).getUrl(),"");
						return true;
					}
				})
				.positiveText(R.string.ok).choiceWidgetColor(ColorStateList.valueOf(getActivity().getResources().getColor(R.color.dialog_title)))
				.show();
	}
	private void getYoutubeDownloadVideoList(String youtubeLink) {
		new YouTubeExtractor(getActivity()) {

			@Override
			public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
				Map<String,YtFile> map = new LinkedHashMap<>();
				if (ytFiles == null) {
					// Something went wrong we got no urls. Always check this.
					//finish();
					return;
				}
				// Iterate over itags
				for (int i = 0, itag; i < ytFiles.size(); i++) {
					itag = ytFiles.keyAt(i);
					// ytFile represents one file with its url and meta data
					YtFile ytFile = ytFiles.get(itag);

					// Just add videos in a decent format => height -1 = audio
					if (ytFile.getFormat().getHeight() == -1) {
						String str = (ytFile.getFormat().getHeight() == -1) ? "Audio " +
								ytFile.getFormat().getAudioBitrate() + " kbit/s" :
								ytFile.getFormat().getHeight() + "p";
						map.put(str,ytFile);
					}
					if (!ytFile.getFormat().isDashContainer() && ytFile.getFormat().getHeight() >= 360) {
						String str = (ytFile.getFormat().getHeight() == -1) ? "Audio " +
								ytFile.getFormat().getAudioBitrate() + " kbit/s" :
								ytFile.getFormat().getHeight() + "p";
						map.put(str,ytFile);
					}
				}
				showListDialog(map);
			}
		}.extract(youtubeLink, false, false);
	}*/

	// DownloadFile AsyncTask
/*	private class DownloadFile extends AsyncTask<String, Integer, String> {

		MaterialDialog md;
		private MoPubView mMoPubView;
		ProgressBar progressBar;
		private int progressStatus = 0;
		private TextView textView;
		private File file;
		boolean mkdirs;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			md = new MaterialDialog.Builder(getActivity())
					.title(R.string.download_video)
					.customView(R.layout.mrect_ad, true)
					.build();
			mMoPubView = (MoPubView) md.findViewById(R.id.banner_mopubview);
			progressBar = (ProgressBar) md.findViewById(R.id.progressBar);
			progressBar.setMax(100);
			textView = (TextView) md.findViewById(R.id.textView);
			progressStatus += 1;

			RelativeLayout.LayoutParams layoutParams =
					(RelativeLayout.LayoutParams) mMoPubView.getLayoutParams();
			layoutParams.width = getWidth();
			layoutParams.height = getHeight();
			mMoPubView.setLayoutParams(layoutParams);
			mMoPubView.setAdUnitId(getStr(R.string.mopub_native_ad_unit_id));
			mMoPubView.loadAd();
			md.show();
		}

		@Override
		protected String doInBackground(String... Url) {
			try {

				// if the external storage is not available then halt the download operation
				if (!isExternalStorageAvailable()) {
					onExternalStorageNotAvailable();
					return "";
				}


				Uri     remoteFileUri = Uri.parse(Url[0]);
				String  downloadFileName = getCompleteFileName(outputFileName, remoteFileUri);

				// if there's already a local file for this video for some reason, then do not redownload the
				// file and halt
				file = new File(Environment.getExternalStoragePublicDirectory(dirType), downloadFileName);
				*//*if (file.exists()) {
					onFileDownloadCompleted(true, file);
					return "";
				}*//*

				URL url = new URL(Url[0]);
				URLConnection connection = url.openConnection();
				connection.connect();

				// Detect the file lenghth
				int fileLength = connection.getContentLength();


				// Download the file
				InputStream input = new BufferedInputStream(url.openStream());
				String filepath = Environment.getExternalStoragePublicDirectory(dirType).getPath();

				File myDir = new File(filepath);
				if (!myDir.exists()) {
					mkdirs = myDir.mkdirs();
				}


				File f = new File(filepath, downloadFileName);

				// Save the downloaded file
				OutputStream output = new FileOutputStream(f);

				byte data[] = new byte[1024];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					total += count;
					// Publish the progress
					publishProgress((int) (total * 100 / fileLength));
					output.write(data, 0, count);
				}

				// Close connection
				output.flush();
				output.close();
				input.close();
			} catch (final Exception e) {
				// Error Log
				Log.e("Error", e.getMessage());
				e.printStackTrace();
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getContext(),mkdirs + ">>>>>>>>>>>>>"+ e.getMessage(),
								Toast.LENGTH_LONG).show();
					}
				});
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);

			progressBar.setProgress(progress[0]);
			textView.setText(progress[0]+"/"+progressBar.getMax());
			if(isAdded()) {
				textView.setTextColor(getResources().getColor(R.color.skytube_theme_colour));
			}
		}

		@Override
		protected void onPostExecute(String file_url) {
			//md.dismiss();
			onFileDownloadCompleted(true, file);
		}
	}

	public int getWidth() {
		return (int) getResources().getDimension(R.dimen.mrect_width);
	}

	public int getHeight() {
		return (int) getResources().getDimension(R.dimen.mrect_height);
	}

	private void setVariables(YouTubeVideo youTubeVideo) {
		dirType = Environment.DIRECTORY_MOVIES;
		title = youTubeVideo.getTitle();
		description = getStr(R.string.video) + " ― " + youTubeVideo.getChannelName();
		outputFileName = youTubeVideo.getId();
		outputFileExtension = "mp4";
		this.youTubeVideo = youTubeVideo;
	}

	private String getCompleteFileName(String outputFileName, Uri remoteFileUri) {
		String fileExt = (outputFileExtension != null)  ?  outputFileExtension  :   MimeTypeMap.getFileExtensionFromUrl(remoteFileUri.toString());
		return outputFileName + "." + fileExt;
	}

	public void onFileDownloadStarted() {
		Toast.makeText(getContext(),
				String.format(getContext().getString(R.string.starting_video_download), youTubeVideo.getTitle()),
				Toast.LENGTH_LONG).show();
	}

	public void onFileDownloadCompleted(boolean success, File localFile) {
		if (success) {
			success = DownloadedVideosDb.getVideoDownloadsDb().add(youTubeVideo, localFile.toURI().toString());
		}

		*//*Toast.makeText(getContext(),
				String.format(getContext().getString(success ? R.string.video_downloaded : R.string.video_download_stream_error), youTubeVideo.getTitle()),
				Toast.LENGTH_LONG).show();*//*

		shareVideoWhatsApp(localFile);
	}

	public void onExternalStorageNotAvailable() {
		Toast.makeText(getContext(),
				R.string.external_storage_not_available,
				Toast.LENGTH_LONG).show();
	}*/




	/*private boolean appInstalledOrNot(String uri) {
		PackageManager pm = getActivity().getPackageManager();
		boolean app_installed;
		try {
			pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
			app_installed = true;
		}
		catch (PackageManager.NameNotFoundException e) {
			app_installed = false;
		}
		return app_installed;
	}

	*//**
	 * Checks if the external storage is available for read and write.
	 *
	 * @return True if the external storage is available.
	 *//*
	private boolean isExternalStorageAvailable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}*/

	/**
	 * Will asynchronously retrieve additional video information such as channel avatar ...etc
	 */
	private void getVideoInfoTasks() {
		// get Channel info (e.g. avatar...etc) task
		new GetYouTubeChannelInfoTask(getContext(), new YouTubeChannelInterface() {
			@Override
			public void onGetYouTubeChannel(YouTubeChannel youTubeChannel) {
				YouTubePlayerV2Fragment.this.youTubeChannel = youTubeChannel;

				videoDescSubscribeButton.setChannel(YouTubePlayerV2Fragment.this.youTubeChannel);
				if (youTubeChannel != null) {
					if(getActivity() != null)
						Glide.with(getActivity())
								.load(youTubeChannel.getThumbnailNormalUrl())
								.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
								.into(videoDescChannelThumbnailImageView);
				}
			}
		}).executeInParallel(youTubeVideo.getChannelId());

		// get the video description
		new GetVideoDescriptionTask(youTubeVideo, new GetVideoDescriptionTask.GetVideoDescriptionTaskListener() {
			@Override
			public void onFinished(String description) {
				videoDescriptionTextView.setText(description);
			}
		}).executeInParallel();

		// check if the user has subscribed to a channel... if he has, then change the state of
		// the subscribe button
		new CheckIfUserSubbedToChannelTask(videoDescSubscribeButton, youTubeVideo.getChannelId()).execute();
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		// stop the player from playing (when this fragment is going to be destroyed) and clean up
		player.stop();
		player = null;
		playerView.setPlayer(null);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * This task will, from the given video URL, get the details of the video (e.g. video name,
	 * likes ...etc).
	 */
	private class GetVideoDetailsTask extends AsyncTaskParallel<Void, Void, YouTubeVideo> {

		private String videoUrl = null;


		@Override
		protected void onPreExecute() {
			String url = getUrlFromIntent(getActivity().getIntent());

			try {
				// YouTube sends subscriptions updates email in which its videos' URL are encoded...
				// Hence we need to decode them first...
				videoUrl = URLDecoder.decode(url, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				Logger.e(this, "UnsupportedEncodingException on " + videoUrl + " encoding = UTF-8", e);
				videoUrl = url;
			}
		}


		/**
		 * The video URL is passed to SkyTube via another Android app (i.e. via an intent).
		 *
		 * @return The URL of the YouTube video the user wants to play.
		 */
		private String getUrlFromIntent(final Intent intent) {
			String url = null;

			if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
				url = intent.getData().toString();
			}

			return url;
		}


		/**
		 * Returns an instance of {@link YouTubeVideo} from the given {@link #videoUrl}.
		 *
		 * @return {@link YouTubeVideo}; null if an error has occurred.
		 */
		@Override
		protected YouTubeVideo doInBackground(Void... params) {
			String videoId = YouTubeVideo.getYouTubeIdFromUrl(videoUrl);
			YouTubeVideo youTubeVideo = null;

			if (videoId != null) {
				try {
					GetVideosDetailsByIDs getVideo = new GetVideosDetailsByIDs();
					getVideo.init(videoId);
					List<YouTubeVideo> youTubeVideos = getVideo.getNextVideos();

					if (youTubeVideos.size() > 0)
						youTubeVideo = youTubeVideos.get(0);
				} catch (IOException ex) {
					Logger.e(this, "Unable to get video details, where id=" + videoId, ex);
				}
			}

			return youTubeVideo;
		}


		@Override
		protected void onPostExecute(YouTubeVideo youTubeVideo) {
			if (youTubeVideo == null) {
				// invalid URL error (i.e. we are unable to decode the URL)
				String err = String.format(getString(R.string.error_invalid_url), videoUrl);
				Toast.makeText(getActivity(), err, Toast.LENGTH_LONG).show();

				// log error
				Logger.e(this, err);

				// close the video player activity
				closeActivity();
			} else {
				YouTubePlayerV2Fragment.this.youTubeVideo = youTubeVideo;

				// setup the HUD and play the video
				setUpHUDAndPlayVideo();

				getVideoInfoTasks();

				// will now check if the video is bookmarked or not (and then update the menu
				// accordingly)
				new IsVideoBookmarkedTask(youTubeVideo, menu).executeInParallel();
			}
		}
	}



	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * This will handle any gesture swipe event performed by the user on the player view.
	 */
	private class PlayerViewGestureHandler extends PlayerViewGestureDetector {

		private ImageView           indicatorImageView = null;
		private TextView            indicatorTextView = null;
		private RelativeLayout      indicatorView = null;

		private boolean             isControllerVisible = true;
		private VideoBrightness     videoBrightness;
		private float               startVolumePercent = -1.0f;
		private long                startVideoTime = -1;

		private static final int    MAX_VIDEO_STEP_TIME = 60 * 1000;


		PlayerViewGestureHandler() {
			super(getContext());

			videoBrightness = new VideoBrightness(getActivity());
			playerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
				@Override
				public void onVisibilityChange(int visibility) {
					isControllerVisible = (visibility == View.VISIBLE);
				}
			});
		}


		void initView(View view) {
			indicatorView = view.findViewById(R.id.indicatorView);
			indicatorImageView = view.findViewById(R.id.indicatorImageView);
			indicatorTextView = view.findViewById(R.id.indicatorTextView);
		}


		@Override
		public void onCommentsGesture() {
			commentsDrawer.animateOpen();
		}


		@Override
		public void onVideoDescriptionGesture() {
			videoDescriptionDrawer.animateOpen();
		}


		@Override
		public void onDoubleTap() {
			// if the user is playing a video...
			if (player.getPlayWhenReady()) {
				// pause video
				player.setPlayWhenReady(false);
				player.getPlaybackState();
			} else {
				// play video
				player.setPlayWhenReady(true);
				player.getPlaybackState();
			}

			playerView.hideController();
		}


		@Override
		public boolean onSingleTap() {
			return showOrHideHud();
		}


		/**
		 * Hide or display the HUD depending if the HUD is currently visible or not.
		 */
		private boolean showOrHideHud() {
			if (commentsDrawer.isOpened()) {
				commentsDrawer.animateClose();
				return !isControllerVisible;
			}

			if (videoDescriptionDrawer.isOpened()) {
				videoDescriptionDrawer.animateClose();
				return !isControllerVisible;
			}

			if (isControllerVisible) {
				playerView.hideController();
				hideNavigationBar();
			} else {
				playerView.showController();
			}

			return false;
		}


		@Override
		public void onGestureDone() {
			videoBrightness.onGestureDone();
			startVolumePercent = -1.0f;
			startVideoTime = -1;
			hideIndicator();
		}


		@Override
		public void adjustBrightness(double adjustPercent) {
			// adjust the video's brightness
			videoBrightness.setVideoBrightness(adjustPercent, getActivity());

			// set indicator
			indicatorImageView.setImageResource(R.drawable.ic_brightness);
			indicatorTextView.setText(videoBrightness.getBrightnessString());

			// Show indicator. It will be hidden once onGestureDone will be called
			showIndicator();
		}


		@Override
		public void adjustVolumeLevel(double adjustPercent) {
			// We are setting volume percent to a value that should be from -1.0 to 1.0. We need to limit it here for these values first
			if (adjustPercent < -1.0f) {
				adjustPercent = -1.0f;
			} else if (adjustPercent > 1.0f) {
				adjustPercent = 1.0f;
			}

			AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
			final int STREAM = AudioManager.STREAM_MUSIC;

			// Max volume will return INDEX of volume not the percent. For example, on my device it is 15
			int maxVolume = audioManager.getStreamMaxVolume(STREAM);
			if (maxVolume == 0) return;

			if (startVolumePercent < 0) {
				// We are getting actual volume index (NOT volume but index). It will be >= 0.
				int curVolume = audioManager.getStreamVolume(STREAM);
				// And counting percents of maximum volume we have now
				startVolumePercent = curVolume * 1.0f / maxVolume;
			}
			// Should be >= 0 and <= 1
			double targetPercent = startVolumePercent + adjustPercent;
			if (targetPercent > 1.0f) {
				targetPercent = 1.0f;
			} else if (targetPercent < 0) {
				targetPercent = 0;
			}

			// Calculating index. Test values are 15 * 0.12 = 1 ( because it's int)
			int index = (int) (maxVolume * targetPercent);
			if (index > maxVolume) {
				index = maxVolume;
			} else if (index < 0) {
				index = 0;
			}
			audioManager.setStreamVolume(STREAM, index, 0);

			indicatorImageView.setImageResource(R.drawable.ic_volume);
			indicatorTextView.setText(index * 100 / maxVolume + "%");

			// Show indicator. It will be hidden once onGestureDone will be called
			showIndicator();
		}

		@Override
		public void adjustVideoPosition(double adjustPercent, boolean forwardDirection) {
			long totalTime = player.getDuration();

			if (adjustPercent < -1.0f) {
				adjustPercent = -1.0f;
			} else if (adjustPercent > 1.0f) {
				adjustPercent = 1.0f;
			}

			if (startVideoTime < 0) {
				startVideoTime = player.getCurrentPosition();
			}
			// adjustPercent: value from -1 to 1.
			double positiveAdjustPercent = Math.max(adjustPercent, -adjustPercent);
			// End of line makes seek speed not linear
			long targetTime = startVideoTime + (long) (MAX_VIDEO_STEP_TIME * adjustPercent * (positiveAdjustPercent / 0.1));
			if (targetTime > totalTime) {
				targetTime = totalTime;
			}
			if (targetTime < 0) {
				targetTime = 0;
			}

			String targetTimeString = formatDuration(targetTime / 1000);

			if (forwardDirection) {
				indicatorImageView.setImageResource(R.drawable.ic_forward);
				indicatorTextView.setText(targetTimeString);
			} else {
				indicatorImageView.setImageResource(R.drawable.ic_rewind);
				indicatorTextView.setText(targetTimeString);
			}

			showIndicator();

			player.seekTo(targetTime);
		}


		@Override
		public Rect getPlayerViewRect() {
			return new Rect(playerView.getLeft(), playerView.getTop(), playerView.getRight(), playerView.getBottom());
		}


		private void showIndicator() {
			indicatorView.setVisibility(View.VISIBLE);
		}


		private void hideIndicator() {
			indicatorView.setVisibility(View.GONE);
		}


		/**
		 * Returns a (localized) string for the given duration (in seconds).
		 *
		 * @param duration
		 * @return  a (localized) string for the given duration (in seconds).
		 */
		private String formatDuration(long duration) {
			long    h = duration / 3600;
			long    m = (duration - h * 3600) / 60;
			long    s = duration - (h * 3600 + m * 60);
			String  durationValue;

			if (h == 0) {
				durationValue = String.format(Locale.getDefault(),"%1$02d:%2$02d", m, s);
			} else {
				durationValue = String.format(Locale.getDefault(),"%1$d:%2$02d:%3$02d", h, m, s);
			}

			return durationValue;
		}

	}

	@Override
	public void videoPlaybackStopped() {
		player.stop();
		if(!SkyTubeApp.getPreferenceManager().getBoolean(getString(R.string.pref_key_disable_playback_status), false)) {
			PlaybackStatusDb.getVideoDownloadsDb().setVideoPosition(youTubeVideo, player.getCurrentPosition());
		}
	}



	////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Adjust video's brightness.  Once the brightness is adjust, it is saved in the preferences to
	 * be used when a new video is played.
	 */
	private static class VideoBrightness {

		/** Current video brightness. */
		private float   brightness;
		/** Initial video brightness. */
		private float   initialBrightness;
		private static final String SAVE_BRIGHTNESS_FLAG = "VideoBrightness.SAVE_BRIGHTNESS_FLAG";

		/**
		 * Constructor:  load the previously saved video brightness from the preference and set it.
		 *
		 * @param activity  Activity.
		 */
		public VideoBrightness(final Activity activity) {
			loadBrightnessFromPreference();
			initialBrightness = brightness;

			setVideoBrightness(brightness, activity);
		}


		/**
		 * Set the video brightness.  Once the video brightness is updated, save it in the preference.
		 *
		 * @param adjustPercent Percentage.
		 * @param activity      Activity.
		 */
		public void setVideoBrightness(double adjustPercent, final Activity activity) {
			// We are setting brightness percent to a value that should be from -1.0 to 1.0. We need to limit it here for these values first
			if (adjustPercent < -1.0f) {
				adjustPercent = -1.0f;
			} else if (adjustPercent > 1.0f) {
				adjustPercent = 1.0f;
			}

			// set the brightness instance variable
			setBrightness(initialBrightness + (float) adjustPercent);
			// adjust the video brightness as per this.brightness
			adjustVideoBrightness(activity);
			// save brightness to the preference
			saveBrightnessToPreference();
		}


		/**
		 * Adjust the video brightness.
		 *
		 * @param activity  Current activity.
		 */
		private void adjustVideoBrightness(final Activity activity) {
			WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
			lp.screenBrightness = brightness;
			activity.getWindow().setAttributes(lp);
		}


		/**
		 * Saves {@link #brightness} to preference.
		 */
		private void saveBrightnessToPreference() {
			SharedPreferences.Editor editor = SkyTubeApp.getPreferenceManager().edit();
			editor.putFloat(SAVE_BRIGHTNESS_FLAG, brightness);
			editor.apply();
		}


		/**
		 * Loads the brightness from preference and set the {@link #brightness} instance variable.
		 */
		private void loadBrightnessFromPreference() {
			final float brightnessPref = SkyTubeApp.getPreferenceManager().getFloat(SAVE_BRIGHTNESS_FLAG, 1);
			setBrightness(brightnessPref);
		}


		/**
		 * Set the {@link #brightness} instance variable.
		 *
		 * @param brightness    Brightness (from 0.0 to 1.0).
		 */
		private void setBrightness(float brightness) {
			if (brightness < 0) {
				brightness = 0;
			} else if (brightness > 1) {
				brightness = 1;
			}

			this.brightness = brightness;
		}


		/**
		 * @return Brightness as string:  e.g. "21%"
		 */
		public String getBrightnessString() {
			return ((int) (brightness * 100)) + "%";
		}


		/**
		 * To be called once the swipe gesture is done/completed.
		 */
		public void onGestureDone() {
			initialBrightness = brightness;
		}

	}

	@Override
	public void onPause() {
		player.setPlayWhenReady(false);
		player.getPlaybackState();
		super.onPause();
	}

	/*@Override
	public void onResume() {
		super.onResume();
		mAdView.resume();
	}*/
}
