package free.rm.skytube.gui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YoutubeDownloader;

public class ShareBottomDialog extends BottomBaseDialog<ShareBottomDialog> {
    @BindView(R.id.ll_wechat_friend_circle)
    LinearLayout mLlWechatFriendCircle;
    @BindView(R.id.ll_wechat_friend) LinearLayout mLlWechatFriend;
    @BindView(R.id.ll_qq) LinearLayout mLlQq;
    @BindView(R.id.ll_sms) LinearLayout mLlSms;

    public ShareBottomDialog(Context context, View animateView) {
        super(context, animateView);
    }

    public ShareBottomDialog(Context context) {
        super(context);
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
                YoutubeDownloader.shareVideoWhatsApp();
                dismiss();
            }
        });
        mLlWechatFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YoutubeDownloader.shareVideoFacebook();
                dismiss();
            }
        });
        mLlQq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YoutubeDownloader.shareVideoInstagram();
                dismiss();
            }
        });
        mLlSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //T.showShort(mContext, "短信");
                dismiss();
            }
        });
    }
}