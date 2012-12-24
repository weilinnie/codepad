package info.winiex.codepad.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class WebChrome2 extends WebViewClient {
	private ProgressDialog mProgressDialog;
	private Activity context;
	public WebChrome2(Activity context){
		this.context = context;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {

		if (url.startsWith("mailto:")) {
			url = url.replaceFirst("mailto:", "");
			url = url.trim();
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("plain/text").putExtra(Intent.EXTRA_EMAIL,
					new String[] { url });
			context.startActivity(i);
			return true;
		}
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		context.startActivity(intent);

		return true;
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		if (mProgressDialog != null)
			mProgressDialog.dismiss();
		mProgressDialog = null;

		super.onPageFinished(view, url);
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		mProgressDialog = ProgressDialog.show(view.getContext(),
				"Please wait...", "Opening File...", true);
		super.onPageStarted(view, url, favicon);
	}
}