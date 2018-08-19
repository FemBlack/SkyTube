package free.rm.skytube.businessobjects;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.DownloadedVideosDb;
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.FFtask;
import timber.log.Timber;

import static free.rm.skytube.app.SkyTubeApp.getContext;
import static free.rm.skytube.app.SkyTubeApp.getStr;
import static free.rm.skytube.businessobjects.db.DownloadedVideosDb.AUDIO;
import static free.rm.skytube.businessobjects.db.DownloadedVideosDb.UNDERSCORE;
import static free.rm.skytube.businessobjects.db.DownloadedVideosDb.VIDEO;

public class YoutubeDownloader implements MoPubView.BannerAdListener {

    private YouTubeVideo            youTubeVideo = null;

    private String  dirType = null;
    /** The title that will be displayed by the Android's download manager. */
    private String  title = null;
    /** The description that will be displayed by the Android's download manager. */
    private String  description = null;
    private String  outputFileName = null;
    private String  outputFileExtension = null;
    private YtFile youtubeFile = null;
    public static final String YOUTUBE_URL = "https://www.youtube.com/watch?v=";
    private Context context = null;
    private Handler handler = new Handler();

    public YoutubeDownloader(YouTubeVideo youTubeVideo,Context mContext) {
        this.youTubeVideo = youTubeVideo;
        this.context = mContext;
    }

    public void setVariables() {
        dirType = Environment.DIRECTORY_MOVIES;
        title = youTubeVideo.getTitle();
        description = getStr(R.string.video) + " ― " + youTubeVideo.getChannelName();
        outputFileName = youTubeVideo.getId();
        outputFileExtension = "mp4";
    }

    public void getYoutubeDownloadVideoList(String youtubeLink,final boolean isVideo) {
        new YouTubeExtractor(context) {

            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                Map<String,YtFile> map = new LinkedHashMap<>();
                List<YtFile> list = new ArrayList<>();
                if (ytFiles == null) {
                    // Something went wrong we got no urls. Always check this.
                    //finish();
                    return;
                }
                // Iterate over itags
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    // youtubeFile represents one file with its url and meta data
                    YtFile ytFile = ytFiles.get(itag);

                    // Just add videos in a decent format => height -1 = audio
                    if (ytFile.getFormat().getHeight() == -1 && !isVideo) {
                        String str = (ytFile.getFormat().getHeight() == -1) ? "Audio " +
                                ytFile.getFormat().getAudioBitrate() + " kbit/s" :
                                ytFile.getFormat().getHeight() + "p";
                        list.add(ytFile);
                    }
                    if (!ytFile.getFormat().isDashContainer() && ytFile.getFormat().getHeight() >= 360 && isVideo) {
                        String str = (ytFile.getFormat().getHeight() == -1) ? "Audio " +
                                ytFile.getFormat().getAudioBitrate() + " kbit/s" :
                                ytFile.getFormat().getHeight() + "p";
                        map.put(str,ytFile);
                    }
                }
                if (isVideo) {
                    showListDialog(map);
                } else {
                    if (list.size() >= 1) {
                        youtubeFile = list.get(0);
                        new DownloadFile().execute(youtubeFile);
                    } else {
                        Toast.makeText(getContext(),
                                R.string.error_download_audio,
                                Toast.LENGTH_LONG).show();
                    }
                }

            }
        }.extract(youtubeLink, false, false);
    }

    private void showListDialog(final Map<String,YtFile> map) {
        new MaterialDialog.Builder(context)
                .title(R.string.download_video)
                .items(map.keySet())
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        /**
                         * If you use alwaysCallSingleChoiceCallback(), which is discussed below,
                         * returning false here won't allow the newly selected radio button to actually be selected.
                         **/
                        youtubeFile = map.get(dialog.getItems().get(which));
                        new DownloadFile().execute(youtubeFile);

                        return true;
                    }
                })
                .positiveText(R.string.ok).choiceWidgetColor(ColorStateList.valueOf(context.getResources().getColor(R.color.dialog_title)))
                .show();
    }

    // DownloadFile AsyncTask
    private class DownloadFile extends AsyncTask<YtFile, Integer, String> {

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
            md = new MaterialDialog.Builder(context)
                    .title( (youtubeFile.getFormat().getHeight() == -1)  ? R.string.download_audio  :   R.string.download_video)
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
            mMoPubView.setAdUnitId(getStr(R.string.mopub_medium_ad_unit_id));
            mMoPubView.loadAd();
            md.show();
        }

        @Override
        protected String doInBackground(YtFile... ytFiles) {
            try {

                // if the external storage is not available then halt the download operation
                if (!isExternalStorageAvailable()) {
                    onExternalStorageNotAvailable();
                    return "";
                }

                Uri remoteFileUri = Uri.parse(youtubeFile.getUrl());
                String  downloadFileName = getCompleteFileName(outputFileName, remoteFileUri);

                // if there's already a local file for this video for some reason, then do not redownload the
                // file and halt
                file = new File(Environment.getExternalStoragePublicDirectory(dirType), downloadFileName);
				/*if (file.exists()) {
					onFileDownloadCompleted(true, file);
					return "";
				}*/

                URL url = new URL(youtubeFile.getUrl());
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
            textView.setTextColor(context.getResources().getColor(R.color.skytube_theme_colour));

        }

        @Override
        protected void onPostExecute(String file_url) {
            //md.dismiss();

            if (youtubeFile.getFormat().getHeight() == -1) {
                textView.setText(R.string.process_Audio_Image);
                downloadThumbnailImage(file);
            }else {
                onFileDownloadCompleted(true, file);
            }
        }
    }

    private String getThumbnailUrl() {
        return youTubeVideo.getThumbnailMaxResUrl() != null  ?  youTubeVideo.getThumbnailMaxResUrl()  :  youTubeVideo.getThumbnailUrl();
    }


    /**
     * Downloads a video thumbnail.
     */
    private class ThumbnailDownloader extends FileDownloader implements Serializable {

        private String  audioPath = null;

        public ThumbnailDownloader(String localAudioPath) {
            super();
            audioPath = localAudioPath;
        }

        @Override
        public void onFileDownloadStarted() {
        }

        @Override
        public void onFileDownloadCompleted(boolean success, Uri localFileUri) {
            /*Toast.makeText(getContext(),
                    success  ?  R.string.thumbnail_downloaded  :  R.string.thumbnail_download_error,
                    Toast.LENGTH_LONG)
                    .show();*/
            if (success) {
                checkFFMEG(localFileUri.toString(),audioPath,localFileUri.toString());
            }
        }

        @Override
        public void onExternalStorageNotAvailable() {
            Toast.makeText(getContext(),
                    R.string.external_storage_not_available,
                    Toast.LENGTH_LONG).show();
        }

    }

    private void downloadThumbnailImage(File localAudioPath) {
        // download the thumbnail
        new ThumbnailDownloader(localAudioPath.toURI().toString())
                .setRemoteFileUrl(getThumbnailUrl())
                .setDirType(Environment.DIRECTORY_PICTURES)
                .setTitle(youTubeVideo.getTitle())
                .setDescription((R.string.thumbnail) + " ― " + youTubeVideo.getChannelName())
                .setOutputFileName(youTubeVideo.getId())
                .setAllowedOverRoaming(true).download();
        ;
    }


    private void checkFFMEG(String selectedPathImage, String selectedPathAudio, String output) {
        if (FFmpeg.getInstance(getContext()).isSupported()) {
            // ffmpeg is supported
            //versionFFmpeg();
            processAudioAndImage(selectedPathImage,selectedPathAudio,output);
        } else {
            // ffmpeg is not supported
            Toast.makeText(getContext(),"ffmpeg not supported!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void versionFFmpeg() {
        FFmpeg.getInstance(getContext()).execute(new String[]{"-version"}, new ExecuteBinaryResponseHandler() {
            @Override
            public void onSuccess(String message) {
                Timber.d(message);
            }

            @Override
            public void onProgress(String message) {
                Timber.d(message);
            }
        });

    }

    private void processAudioAndImage(final String selectedPathImage, final String selectedPathAudio, final String output) {
        File mf = Environment.getExternalStorageDirectory();

        String livestream = mf.getAbsoluteFile()+"/smile.jpg";

        String folderpth = mf.getAbsoluteFile()+"/test.MP3";
        //final String output1 = new File(Environment.getExternalStorageDirectory(), "video.mp4").getAbsolutePath();
        final String  downloadFileName = getCompleteFileName( );
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), downloadFileName);
        final String output1 = file.getAbsolutePath();
        //String cmd="ffmpeg -i "+ livestream +" -i "+ folderpth +" -acodec copy "+ output;
        String[] command = {"-i", selectedPathImage, "-i",selectedPathAudio,"-acodec","copy",output1};

        final FFtask task = FFmpeg.getInstance(getContext()).execute(command, new ExecuteBinaryResponseHandler() {
            @Override
            public void onStart() {
                Timber.d( "on start");
            }

            @Override
            public void onFinish() {
                Timber.d("on finish");
				/*handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						Timber.d("RESTART RENDERING");
						processAudioAndImage(selectedPathImage,selectedPathAudio,downloadFileName);
					}
				}, 5000);*/
            }

            @Override
            public void onSuccess(String message) {
                Timber.d(message);
            }

            @Override
            public void onProgress(String message) {
                Timber.d(message);
            }

            @Override
            public void onFailure(String message) {
                Timber.d(message);
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Timber.d("STOPPING THE RENDERING!");
                task.sendQuitSignal();
            }
        }, 8000);

        if (task.isProcessCompleted()) {
            Timber.d("Process Completed");
            onFileDownloadCompleted(true, file);
        }
    }


    private String getCompleteFileName(String outputFileName, Uri remoteFileUri) {
        String fileExt = (outputFileExtension != null)  ?  outputFileExtension  :   MimeTypeMap.getFileExtensionFromUrl(remoteFileUri.toString());
        String mime = (youtubeFile.getFormat().getHeight() == -1)  ? AUDIO  :   VIDEO;
        return outputFileName +UNDERSCORE+mime+ "." + fileExt;
    }

    private String getCompleteFileName() {
        String fileExt = "mp4";
        return youTubeVideo.getId() +"IMAGE_AUDIO"+ "." + fileExt;
    }

    /*public void onFileDownloadStarted() {
        Toast.makeText(getContext(),
                String.format(getContext().getString(R.string.starting_video_download), youTubeVideo.getTitle()),
                Toast.LENGTH_LONG).show();
    }*/

    public void onFileDownloadCompleted(boolean success, File localFile) {
        if (success) {
            success = DownloadedVideosDb.getVideoDownloadsDb().add(youTubeVideo, localFile.toURI().toString(), youtubeFile);
        }

	/*	Toast.makeText(getContext(),
				String.format(getContext().getString(success ? R.string.video_downloaded : R.string.video_download_stream_error), youTubeVideo.getTitle()),
				Toast.LENGTH_LONG).show();*/

        shareVideoWhatsApp(localFile);
    }

    public void onExternalStorageNotAvailable() {
        Toast.makeText(getContext(),
                R.string.external_storage_not_available,
                Toast.LENGTH_LONG).show();
    }

    public void shareVideoWhatsApp(File file) {

        if(!appInstalledOrNot("com.whatsapp")){
            Toast.makeText(getContext(),
                    R.string.whatsapp_install,
                    Toast.LENGTH_LONG).show();
        }else{
            Uri uri = (android.os.Build.VERSION.SDK_INT >= 24)
                    ? FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file)  // we now need to call FileProvider.getUriForFile() due to security changes in Android 7.0+
                    : Uri.fromFile(file);

            Intent videoshare = new Intent(Intent.ACTION_SEND);
            videoshare.setType("*/*");
            videoshare.putExtra(Intent.EXTRA_TEXT, getStr(R.string.share_msg));
            videoshare.setPackage("com.whatsapp");
            videoshare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            videoshare.putExtra(Intent.EXTRA_STREAM,uri);

            context.startActivity(videoshare);
        }
    }

    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = context.getPackageManager();
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

    /**
     * Checks if the external storage is available for read and write.
     *
     * @return True if the external storage is available.
     */
    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private int getWidth() {
        return (int) context.getResources().getDimension(R.dimen.mrect_width);
    }

    private int getHeight() {
        return (int) context.getResources().getDimension(R.dimen.mrect_height);
    }

    public String getOutputFileExtension() {
        return outputFileExtension;
    }

    public void setOutputFileExtension(String outputFileExtension) {
        this.outputFileExtension = outputFileExtension;
    }

    // BannerAdListener
    @Override
    public void onBannerLoaded(MoPubView banner) {
        Toast.makeText(getContext(),
               "loaded",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
        final String errorMessage = (errorCode != null) ? errorCode.toString() : "";
        Toast.makeText(getContext(),
                " failed to load: " + errorMessage,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBannerClicked(MoPubView banner) {
        Toast.makeText(getContext(),
                "clicked",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBannerExpanded(MoPubView banner) {
        Toast.makeText(getContext(),
                "expanded",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBannerCollapsed(MoPubView banner) {
        Toast.makeText(getContext(),
                "collapsed",
                Toast.LENGTH_LONG).show();
    }
}
