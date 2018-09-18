package free.rm.skytube.gui.dialog;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;

import static free.rm.skytube.app.SkyTubeApp.getStr;

public class VibeShareBottomDialog extends BottomBaseDialog<VibeShareBottomDialog> {
    @BindView(R.id.ll_wechat_friend_circle)
    LinearLayout mLlWechatFriendCircle;
    @BindView(R.id.ll_wechat_friend) LinearLayout mLlWechatFriend;
    @BindView(R.id.ll_qq) LinearLayout mLlQq;
    @BindView(R.id.ll_sms) LinearLayout mLlSms;

    private Context mContext;
    public VibeShareBottomDialog(Context context, View animateView) {
        super(context, animateView);
    }

    public VibeShareBottomDialog(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public View onCreateView() {
        View inflate = View.inflate(mContext, R.layout.dialog_share, null);
        ButterKnife.bind(this, inflate);

        return inflate;
    }

    @Override
    public void setUiBeforShow() {

        mLlWechatFriendCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareVideoWhatsApp();
                dismiss();
            }
        });
        mLlWechatFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareVideoFacebook();
                dismiss();
            }
        });
        mLlQq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareVideoInstagram();
                dismiss();
            }
        });
        mLlSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidShare();
                dismiss();
            }
        });
    }


    private void shareVideoWhatsApp() {

        if(!appInstalledOrNot("com.whatsapp")){
            Toast.makeText(mContext,
                    R.string.whatsapp_install,
                    Toast.LENGTH_LONG).show();
        }else{
            Intent videoshare = new Intent(Intent.ACTION_SEND);
            videoshare.setType("text/plain");
            videoshare.putExtra(Intent.EXTRA_TEXT, getStr(R.string.share_msg));
            videoshare.setPackage("com.whatsapp");
            videoshare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(videoshare);
        }
    }

    private void shareVideoFacebook() {

        if(!appInstalledOrNot("com.facebook.katana")){
            Toast.makeText(getContext(),
                    R.string.fb_install,
                    Toast.LENGTH_LONG).show();
        }else{

            Intent videoshare = new Intent(Intent.ACTION_SEND);
            videoshare.setType("text/plain");
            videoshare.putExtra(Intent.EXTRA_TEXT, getStr(R.string.share_msg));
            videoshare.setPackage("com.facebook.katana");
            videoshare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(videoshare);
        }
    }

    private void shareVideoInstagram() {

        if(!appInstalledOrNot("com.instagram.android")){
            Toast.makeText(getContext(),
                    R.string.instagram_install,
                    Toast.LENGTH_LONG).show();
        }else{
            Intent videoshare = new Intent(Intent.ACTION_SEND);
            videoshare.setType("text/plain");
            videoshare.putExtra(Intent.EXTRA_TEXT, getStr(R.string.share_msg));
            videoshare.setPackage("com.instagram.android");
            videoshare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(videoshare);
        }
    }

    private void androidShare() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, mContext.getString(R.string.share_msg));
        mContext.startActivity(Intent.createChooser(share, "Share Vibe to your loved ones"));
    }

    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = getContext().getPackageManager();
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
}