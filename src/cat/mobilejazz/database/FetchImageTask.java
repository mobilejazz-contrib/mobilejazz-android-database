package cat.mobilejazz.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import cat.mobilejazz.utilities.debug.Debug;
import cat.mobilejazz.utilities.io.IOUtils;

public class FetchImageTask<V> extends AsyncTask<Void, Void, Bitmap> {

	public static interface ImageSetter<V> {

		public void onSetImage(V view, Bitmap bitmap);

	}

	public static class ImageViewSetter implements ImageSetter<ImageView> {

		@Override
		public void onSetImage(ImageView view, Bitmap bitmap) {
			view.setImageBitmap(bitmap);
		}

	}

	public static class TextViewSetter implements ImageSetter<TextView> {

		@Override
		public void onSetImage(TextView view, Bitmap bitmap) {
			view.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(view.getResources(), bitmap), null, null,
					null);
		}

	}

	public static class ImageOptions {

		public int maxWidth;
		public int sourceDensity;

		public ImageOptions() {
			maxWidth = 0;
			sourceDensity = DisplayMetrics.DENSITY_DEFAULT;
		}

	}

	private static TrustManager sTrustAll = new X509TrustManager() {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	};

	private String mImageUrl;
	private String mDefaultUrl;
	private int mDefaultDrawable;
	private V mView;
	private Context mContext;
	private ImageSetter<V> mImageSetter;

	private ImageOptions mOptions;

	public FetchImageTask(Context context, String imageUrl, String defaultUrl, V view, ImageSetter<V> imageSetter) {
		super();
		mContext = context;
		mImageUrl = imageUrl;
		mDefaultUrl = defaultUrl;
		mView = view;
		mImageSetter = imageSetter;
	}

	public FetchImageTask(Context context, String imageUrl, int defaultDrawable, V view, ImageSetter<V> imageSetter) {
		this(context, imageUrl, defaultDrawable, view, imageSetter, new ImageOptions());
	}

	public FetchImageTask(Context context, String imageUrl, int defaultDrawable, V view, ImageSetter<V> imageSetter,
			ImageOptions options) {
		super();
		mContext = context;
		mImageUrl = imageUrl;
		mDefaultDrawable = defaultDrawable;
		mView = view;
		mImageSetter = imageSetter;
		mOptions = options;
	}

	private File getCacheFile() {
		return new File(mContext.getCacheDir(), mImageUrl);
	}

	private Options getBitmapOptions() {
		Options options = new Options();
		options.inDensity = mOptions.sourceDensity;
		options.inTargetDensity = mContext.getResources().getDisplayMetrics().densityDpi;
		return options;
	}

	private Bitmap decodeFile(File cacheFile) throws IOException {
		InputStream in = new FileInputStream(cacheFile);

		if (mOptions.maxWidth > 0) {
			Options options = getBitmapOptions();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(in, null, options);
			in.close();
			in = null;
			// save width and height
			int inWidth = options.outWidth;

			options = getBitmapOptions();
			options.inSampleSize = (int) Math.ceil(inWidth / mOptions.maxWidth);
			return BitmapFactory.decodeStream(new FileInputStream(cacheFile), null, options);
		} else {
			return BitmapFactory.decodeStream(in, null, getBitmapOptions());
		}

	}

	@Override
	protected void onPreExecute() {
		File cacheFile = getCacheFile();
		if (cacheFile.exists()) {
			try {
				onPostExecute(decodeFile(cacheFile));
				cancel(true);
			} catch (IOException e) {
				// should not happen (see if)
				Debug.logException(e);
			}
		}
	}

	@Override
	protected Bitmap doInBackground(Void... params) {
		File cacheFile = getCacheFile();
		OutputStream cache = null;
		try {
			URL imageUrl = new URL(mImageUrl);
			HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
			//connection.setUseCaches(true);
			//connection.setInstanceFollowRedirects(true);

			if (connection instanceof HttpsURLConnection) {
				SSLContext sslCtx = SSLContext.getInstance("TLS");
				sslCtx.init(null, new TrustManager[] { sTrustAll }, null);

				((HttpsURLConnection) connection).setSSLSocketFactory(sslCtx.getSocketFactory());
			}

			if (!cacheFile.getParentFile().exists()) {
				cacheFile.getParentFile().mkdirs();
			}
			cache = new FileOutputStream(cacheFile);
			IOUtils.copy(connection.getInputStream(), cache);

			return decodeFile(cacheFile);
		} catch (MalformedURLException e) {
			Debug.error("Could not retrieve image from %s", mImageUrl);
			Debug.logException(e);
		} catch (IOException e) {
			Debug.error("Could not retrieve image from %s", mImageUrl);
			Debug.logException(e);
		} catch (KeyManagementException e) {
			Debug.error("Could not retrieve image from %s", mImageUrl);
			Debug.logException(e);
		} catch (NoSuchAlgorithmException e) {
			Debug.error("Could not retrieve image from %s", mImageUrl);
			Debug.logException(e);
		}
		if (cacheFile.exists()) {
			cacheFile.delete();
		}
		return null;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		if (result == null) {
			if (mDefaultUrl != null && !mImageUrl.equals(mDefaultUrl)) {
				// default url:
				fetchImage(mContext, mDefaultUrl, mDefaultUrl, mView, mImageSetter);
			} else if (mDefaultDrawable != 0) {
				// default res id:
				mImageSetter.onSetImage(mView, BitmapFactory.decodeResource(mContext.getResources(), mDefaultDrawable));
			}
		} else {
			mImageSetter.onSetImage(mView, result);
		}
	}

	public static <V> void fetchImage(Context context, String imageUrl, String defaultUrl, V view,
			ImageSetter<V> imageSetter) {
		new FetchImageTask<V>(context, imageUrl, defaultUrl, view, imageSetter).execute();
	}

	public static <V> void fetchImage(Context context, String imageUrl, int defaultDrawable, V view,
			ImageSetter<V> imageSetter) {
		new FetchImageTask<V>(context, imageUrl, defaultDrawable, view, imageSetter).execute();
	}

	public static <V> void fetchImage(Context context, String imageUrl, int defaultDrawable, V view,
			ImageSetter<V> imageSetter, ImageOptions options) {
		new FetchImageTask<V>(context, imageUrl, defaultDrawable, view, imageSetter, options).execute();
	}

	public static <V extends View> void fetchImage(String imageUrl, String defaultUrl, V view,
			ImageSetter<V> imageSetter) {
		new FetchImageTask<V>(view.getContext(), imageUrl, defaultUrl, view, imageSetter).execute();
	}

	public static <V extends View> void fetchImage(String imageUrl, int defaultDrawable, V view,
			ImageSetter<V> imageSetter) {
		new FetchImageTask<V>(view.getContext(), imageUrl, defaultDrawable, view, imageSetter).execute();
	}

	public static void fetchImage(String imageUrl, String defaultUrl, ImageView view) {
		new FetchImageTask<ImageView>(view.getContext(), imageUrl, defaultUrl, view, new ImageViewSetter()).execute();
	}

	public static void fetchImage(String imageUrl, int defaultDrawable, ImageView view) {
		new FetchImageTask<ImageView>(view.getContext(), imageUrl, defaultDrawable, view, new ImageViewSetter())
				.execute();
	}

	public static void fetchImage(String imageUrl, String defaultUrl, TextView view) {
		new FetchImageTask<TextView>(view.getContext(), imageUrl, defaultUrl, view, new TextViewSetter()).execute();
	}

	public static void fetchImage(String imageUrl, int defaultDrawable, TextView view) {
		new FetchImageTask<TextView>(view.getContext(), imageUrl, defaultDrawable, view, new TextViewSetter())
				.execute();
	}
}
