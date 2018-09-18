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

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.util.List;

import free.rm.skytube.R;
import free.rm.skytube.gui.businessobjects.preferences.ActionBarPreferenceActivity;
import free.rm.skytube.gui.fragments.preferences.AboutPreferenceFragment;
import free.rm.skytube.gui.fragments.preferences.BackupPreferenceFragment;
import free.rm.skytube.gui.fragments.preferences.OthersPreferenceFragment;
import free.rm.skytube.gui.fragments.preferences.PrivacyPreferenceFragment;
import free.rm.skytube.gui.fragments.preferences.VideoBlockerPreferenceFragment;
import free.rm.skytube.gui.fragments.preferences.VideoPlayerPreferenceFragment;

/**
 * The preferences activity allows the user to change the settings of this app.
 */
public class PreferencesActivity extends ActionBarPreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView v = getListView();
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);

		if (!AppLaunchActivity.isPurchased) {
		// Create a banner ad
		AdView mAdView = new AdView(this);
		mAdView.setAdSize(AdSize.SMART_BANNER);
		mAdView.setAdUnitId(getString(R.string.banner_ad_unit_id_2));

		// Create an ad request.
		AdRequest.Builder adRequestBuilder = new AdRequest.Builder();

		// Add the AdView to the view hierarchy.
		layout.addView(mAdView);

		// Start loading the ad.
		mAdView.loadAd(adRequestBuilder.build());
		v.addFooterView(layout);
		}
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return (fragmentName.equals(VideoPlayerPreferenceFragment.class.getName())
			|| fragmentName.equals(VideoBlockerPreferenceFragment.class.getName())
			|| fragmentName.equals(BackupPreferenceFragment.class.getName())
			|| fragmentName.equals(OthersPreferenceFragment.class.getName())
			|| fragmentName.equals(AboutPreferenceFragment.class.getName())
			|| fragmentName.equals(PrivacyPreferenceFragment.class.getName()));
	}

	/*@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Toast.makeText(getContext(),
						R.string.whatsapp_install,
						Toast.LENGTH_LONG).show();
				// Old versions of Android did not handle when the user presses the back (<-) action
				// bar's button.  Hence why it is explicitly being handled...
				onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}*/


	/*@Override
	public void onBackPressed() {
		Toast.makeText(getContext(),
				R.string.error_download_audio,
				Toast.LENGTH_LONG).show();
		super.onBackPressed();
		//Execute your code here
		//finish();

	}*/

}
