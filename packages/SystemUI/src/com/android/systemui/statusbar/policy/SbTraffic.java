package com.android.systemui.statusbar.policy;

import java.text.DecimalFormat;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.AttributeSet;
import java.util.ArrayList;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.R;

public class SbTraffic extends LinearLayout {
	private static final String TAG = "StatusBar.SbTraffic";
	private Context mContext;
	private TrafficStats mTrafficStats;
	private boolean mAttached;
	private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
	
	private ArrayList<TrafficStateChangeCallback> mChangeCallbacks =
		new ArrayList<TrafficStateChangeCallback>();
	public interface TrafficStateChangeCallback {
		public void OnTrafficChanged(int speed);
	}
	private ImageView mTrafficIcon;
	private ViewGroup mTrafficGroup;
	private int mLevel = -1;
	boolean showTraffic;
	Handler mTrafficHandler;
	public  static float speed = 0;
	float totalRxBytes;
	
	public SbTraffic(Context context, AttributeSet attrs) {
		super (context,attrs);
		mContext = context;
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (!mAttached) {
			mAttached = true;
			init();
			mTrafficGroup = (ViewGroup)findViewById(R.id.traffic_combo);
			mTrafficIcon = (ImageView)findViewById(R.id.traffic);
			addIconView(mTrafficIcon);
		
			SettingsObserver settingsObserver = new SettingsObserver(new Handler ());
			settingsObserver.observe();
		}	
		updateSettings();
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mAttached) {
		    getContext().unregisterReceiver(mTrafficBroadcastReceiver);	
		    mAttached=false;
		}
	}
	
	private void init() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mContext.registerReceiver(mTrafficBroadcastReceiver, filter);
	}
	
	public void addIconView(ImageView v) {
		mIconViews.add(v);
	}
	
	public void addStateChangedCallback(TrafficStateChangeCallback cb) {
		mChangeCallbacks.add(cb);
	}
	
   	private BroadcastReceiver mTrafficBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			}
		}
	};
	
	private void setTrafficIcon(int level) {
		mLevel = level;
		ContentResolver cr = mContext.getContentResolver();
		int icon = R.drawable.stat_sys_traffic;
		ImageView v = mIconViews.get(0);
		v.setImageResource(icon);
		v.setImageLevel(level);
	}
	
	class SettingsObserver extends ContentObserver {
		SettingsObserver(Handler handler) {
			super(handler);
		}
		
		void observe() {
			ContentResolver resolver = mContext.getContentResolver();
					resolver.registerContentObserver(Settings.System
					.getUriFor(Settings.System.STATUS_BAR_TRAFFIC),
					false,this);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			updateSettings();
		}
	}
	
	private void updateSettings() {
		ContentResolver cr = mContext.getContentResolver();
		showTraffic = (Settings.System.getInt(cr, Settings.System.STATUS_BAR_TRAFFIC,1) == 1);
		if (showTraffic && getConnectAvailable()) {
			if (mAttached) {
				updateTraffic();
			}
			mTrafficIcon.setVisibility(View.VISIBLE);
			setVisibility(View.VISIBLE);
		} else {
			setVisibility(View.GONE);
		}
	}
		
	public void updateTraffic() {
		mTrafficHandler = new Handler () {
			@Override
			public void handleMessage(Message msg) {
			 	speed = (mTrafficStats.getTotalRxBytes()
			 		-totalRxBytes)/1024/3;
			 	totalRxBytes = mTrafficStats.getTotalRxBytes();
			 	final int level = (int)speed;
				if (speed > 100) speed=100;
			 	setTrafficIcon(level);
			 	Log.v("SbTraffic","Setting icon level to"+String.valueOf(level));
			 	update();
			 	super.handleMessage(msg);
			}
		};
	totalRxBytes=mTrafficStats.getTotalRxBytes();
	mTrafficHandler.sendEmptyMessage(0);
	}

        private boolean getConnectAvailable() {
                try {
                        ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                        if (connectivityManager.getActiveNetworkInfo().isConnected())
                                return true;
                        else
                                return false;
                } catch (Exception ex) {
                }
                return false;
        }


	public void update() {
		mTrafficHandler.removeCallbacks(mRunnable);
		mTrafficHandler.postDelayed(mRunnable, 3000);
	}	
	Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			mTrafficHandler.sendEmptyMessage(0);
		}
	};

}
