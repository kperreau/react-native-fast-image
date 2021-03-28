package com.dylanvann.fastimage;

import android.app.Activity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.views.imagehelper.ImageSource;

import java.io.File;

class FastImageViewModule extends ReactContextBaseJavaModule {

    private static final String REACT_CLASS = "FastImageView";
    private static ReactApplicationContext mContext = null;

    FastImageViewModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactMethod
    public void preload(final ReadableArray sources) {
        final Activity activity = getCurrentActivity();
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < sources.size(); i++) {
                    final ReadableMap source = sources.getMap(i);
                    final FastImageSource imageSource = FastImageViewConverter.getImageSource(activity, source);

                    Glide
                            .with(activity.getApplicationContext())
                            // This will make this work for remote and local images. e.g.
                            //    - file:///
                            //    - content://
                            //    - res:/
                            //    - android.resource://
                            //    - data:image/png;base64
                            .load(
                                    imageSource.isBase64Resource() ? imageSource.getSource() :
                                    imageSource.isResource() ? imageSource.getUri() : imageSource.getGlideUrl()
                            )
                            .apply(FastImageViewConverter.getOptions(activity, imageSource, source))
                            .preload();
                }
            }
        });
    }

    public double getDirSize(File dir) {
        if (dir == null) {
            return 0;
        }
        if (!dir.isDirectory()) {
            return 0;
        }
        double dirSize = 0;
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                dirSize += file.length();
            } else if (file.isDirectory()) {
                dirSize += file.length();
                dirSize += getDirSize(file); // 递归调用继续统计
            }
        }
        return dirSize;
    }

    @ReactMethod
    public void getCacheSize (Promise promise) {
        try {
            File cacheDir = Glide.getPhotoCacheDir(mContext);
            File parentFile = cacheDir.getParentFile();
            double size = getDirSize(parentFile);
            promise.resolve(size);
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void clearCache (final Promise promise) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Glide.get(mContext).clearDiskCache();
                    promise.resolve(true);
                }
            }).start();

        } catch (Exception e) {
            promise.reject(e);
        }
    }
}
