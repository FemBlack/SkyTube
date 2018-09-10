/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.gui.activities.MainActivity;
import free.rm.skytube.gui.activities.YouTubePlayerActivity;
import free.rm.skytube.gui.fragments.YouTubePlayerFragment;

/**
 * Launches YouTube player.
 */
public class YouTubePlayer {

	private static String NO_OF_AD_LAUNCH = "AD_LAUNCH_COUNT";
	private static int adLaunchCount = 0;
	private static InterstitialAd mInterstitialAd;

	/**
	 * Launches the custom-made YouTube player so that the user can view the selected video.
	 *
	 * @param youTubeVideo Video to be viewed.
	 */
	public static void launch(YouTubeVideo youTubeVideo, Context context) {
		Intent i = new Intent(context, YouTubePlayerActivity.class);
		i.putExtra(YouTubePlayerFragment.YOUTUBE_VIDEO_OBJ, youTubeVideo);
		context.startActivity(i);
		/*if (getLaunchCount() >= 3) {
			loadInterstitialAd(context);

		} else {
			adLaunchCount++;
			saveLaunchCountToPreference();
		}*/
		if (!MainActivity.isPurchased) {
			loadInterstitialAd(context);
		}
	}


	/**
	 * Launches the custom-made YouTube player so that the user can view the selected video.
	 *
	 * @param videoUrl URL of the video to be watched.
	 */
	public static void launch(String videoUrl, Context context) {
		Intent i = new Intent(context, YouTubePlayerActivity.class);
		i.setAction(Intent.ACTION_VIEW);
		i.setData(Uri.parse(videoUrl));
		context.startActivity(i);
	}

	/*private static void saveLaunchCountToPreference() {
		SharedPreferences.Editor editor = SkyTubeApp.getPreferenceManager().edit();
		editor.putInt(NO_OF_AD_LAUNCH, adLaunchCount);
		editor.apply();
	}

	private static int getLaunchCount() {
		return SkyTubeApp.getPreferenceManager().getInt(NO_OF_AD_LAUNCH,0);
	}*/
	private static void loadInterstitialAd(Context context) {
		mInterstitialAd = new InterstitialAd(context);
		mInterstitialAd.setAdUnitId(context.getString(R.string.interstitial_ad_unit_id));
		mInterstitialAd.loadAd(new AdRequest.Builder().build());
		mInterstitialAd.setAdListener(new AdListener() {
			public void onAdLoaded() {

			}

			public void onAdClosed() {

			}

			public void onAdClicked() {
			}

			public void onAdFailedToLoad(int var1) {
			}

		});
	}

	public static void displayAd() {
		if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
			mInterstitialAd.show();
			/*adLaunchCount = 0;
			saveLaunchCountToPreference();*/
		}
	}

}
