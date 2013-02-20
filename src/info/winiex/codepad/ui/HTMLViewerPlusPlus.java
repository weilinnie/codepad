package info.winiex.codepad.ui;

import info.winiex.codepad.R;
import info.winiex.codepad.handler.CDocumentHandler;
import info.winiex.codepad.handler.CppDocumentHandler;
import info.winiex.codepad.handler.CssDocumentHandler;
import info.winiex.codepad.handler.DocumentHandler;
import info.winiex.codepad.handler.HtmlDocumentHandler;
import info.winiex.codepad.handler.JavaDocumentHandler;
import info.winiex.codepad.handler.JavascriptDocumentHandler;
import info.winiex.codepad.handler.LispDocumentHandler;
import info.winiex.codepad.handler.LuaDocumentHandler;
import info.winiex.codepad.handler.MlDocumentHandler;
import info.winiex.codepad.handler.MxmlDocumentHandler;
import info.winiex.codepad.handler.PerlDocumentHandler;
import info.winiex.codepad.handler.PythonDocumentHandler;
import info.winiex.codepad.handler.RubyDocumentHandler;
import info.winiex.codepad.handler.SqlDocumentHandler;
import info.winiex.codepad.handler.TextDocumentHandler;
import info.winiex.codepad.handler.VbDocumentHandler;
import info.winiex.codepad.handler.XmlDocumentHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

public class HTMLViewerPlusPlus extends Activity {

	/**
	 * The WebView that is placed in this Activity
	 */
	private WebView mWebView;

	private FileBrowserHelper helper;

	SlidingDrawer mSlidingDrawer;
	private TextView drawerHandle;
	private ListView drawerContent;
	private String projectHome;
	private SharedPreferences settings;
	private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener;

	private String currentFile;

	/**
	 * As the file content is loaded completely into RAM first, set a limitation
	 * on the file size so we don't use too much RAM. If someone wants to load
	 * content that is larger than this, then a content provider should be used.
	 */
	static final int MAXFILESIZE = 16172;
	static final String LOGTAG = "HTMLViewerPlusPlus";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CookieSyncManager.createInstance(this);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);

		settings = getSharedPreferences("com.william.codepad.settings",
				MODE_PRIVATE);
		settings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				if ("projectHome".equals(key)) {
					String projectHome = sharedPreferences.getString(key,
							"/sdcard/");
					helper.refreshListView(new File(projectHome));
				}
			}

		});

		projectHome = settings.getString("projectHome", "/sdcard/");
		String lastOpen = settings.getString("lastOpen", null);

		mSlidingDrawer = (SlidingDrawer) findViewById(R.id.drawer);
		drawerHandle = (TextView) findViewById(R.id.handle);
		drawerContent = (ListView) findViewById(R.id.content);
		mWebView = (WebView) findViewById(R.id.webView);

		mSlidingDrawer
				.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {

					@Override
					public void onDrawerClosed() {
						HTMLViewerPlusPlus.this.setTitle("CodePad");
						drawerHandle.setText("");
						drawerHandle.setBackgroundColor(0x00ffffff);
						mWebView.setClickable(true);
					}
				});

		mSlidingDrawer
				.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {

					@Override
					public void onDrawerOpened() {
						HTMLViewerPlusPlus.this
								.setTitle("CodePad - Project Home");
						drawerHandle.setText("Project Home");
						drawerHandle.setBackgroundColor(0xccffffff);
						mWebView.setClickable(false);
					}
				});

		drawerContent
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						int selectionRowID = (int) parent
								.getItemIdAtPosition(position);
						String selectedFileString = helper.directoryEntries
								.get(selectionRowID);
						if (selectedFileString.equals("..")) {
							helper.upOneLevel();
						} else {
							File clickedFile = null;
							switch (helper.displayMode) {
							case RELATIVE:
								clickedFile = new File(helper.currentDirectory
										.getAbsolutePath()
										+ helper.directoryEntries
												.get(selectionRowID));
								break;
							case ABSOLUTE:
								clickedFile = new File(helper.directoryEntries
										.get(selectionRowID));
								break;
							}
							if (clickedFile != null) {
								helper.browseTo(clickedFile);
							}
						}
					}

				});

		helper = new FileBrowserHelper(this, drawerContent, projectHome);

		mWebView.setWebViewClient(new WebChrome2(this));

		WebSettings s = mWebView.getSettings();
		s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
		s.setUseWideViewPort(false);
		s.setAllowFileAccess(true);
		s.setBuiltInZoomControls(true);
		s.setLightTouchEnabled(true);
		s.setLoadsImagesAutomatically(true);
		s.setPluginsEnabled(false);
		s.setSupportZoom(true);
		s.setDefaultZoom(ZoomDensity.CLOSE);
		s.setSupportMultipleWindows(true);
		s.setJavaScriptEnabled(true);

		if (savedInstanceState != null) {
			mWebView.restoreState(savedInstanceState);
		} else {
			Intent intent = getIntent();
			if (intent.getData() != null) {
				Uri uri = intent.getData();
				if ("file".equals(uri.getScheme())) { // are we opening a file,
					loadFile(uri, intent.getType());
				} else {
					mWebView.loadUrl(intent.getData().toString());
				}
			} else {
				// Home Screen, Simple explanation
				if (lastOpen != null) {
					loadFile(Uri.parse(lastOpen), "text/plain");
				} else {
					mWebView.loadUrl("file:///android_asset/home.html");
				}
				setTitle("CodePad - Home");
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		mWebView.saveState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	/**
	 * Modify the menus according to the searching mode and matches
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.open_menu:
			openFileIntent();
			break;
		case R.id.search_menu:
			showSearchDialog();
			break;
		case R.id.select_menu:
			selectAndCopyText();
			break;
		case R.id.home_menu:
			loadHomeScreen();
			break;
		case R.id.help_menu:
			loadHelpScreen();
			break;
		case R.id.about_menu:
			loadAboutScreen();
			break;
		case R.id.quit_menu:
			quitApplication();
			break;
		}
		return false;

	}

	/**
	 * Added to avoid refreshing the page on orientation change saw it on
	 * stackoverflow, dont remember wich article
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * Gets the result from the file picker activity thats the only intent im
	 * actually calling (and expecting results from) right now
	 */
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (resultCode == RESULT_OK) {

			Uri uri = intent.getData();
			if (uri != null) {
				String path = uri.toString();
				if (path.toLowerCase().startsWith("file://")) {
					path = (new File(URI.create(path))).getAbsolutePath();
					loadFile(Uri.parse(path), "text/html");
				}
			}
		}
	}

	/**
	 * Get a Document Handler Depending on the filename extension
	 * 
	 * @param filename
	 *            The filename to retrieve the handler from
	 * @return The new document handler
	 */
	public DocumentHandler getHandlerByExtension(String filename) {
		DocumentHandler handler = null;

		if (filename.endsWith(".java"))
			handler = new JavaDocumentHandler();
		if (filename.endsWith(".cpp") || filename.endsWith(".cc"))
			handler = new CppDocumentHandler();
		if (filename.endsWith(".c"))
			handler = new CDocumentHandler();
		if (filename.endsWith(".html") || filename.endsWith(".htm")
				|| filename.endsWith(".xhtml"))
			handler = new HtmlDocumentHandler();
		if (filename.endsWith(".js"))
			handler = new JavascriptDocumentHandler();
		if (filename.endsWith(".mxml"))
			handler = new MxmlDocumentHandler();
		if (filename.endsWith(".pl"))
			handler = new PerlDocumentHandler();
		if (filename.endsWith(".py"))
			handler = new PythonDocumentHandler();
		if (filename.endsWith(".rb"))
			handler = new RubyDocumentHandler();
		if (filename.endsWith(".xml"))
			handler = new XmlDocumentHandler();
		if (filename.endsWith(".css"))
			handler = new CssDocumentHandler();
		if (filename.endsWith(".el") || filename.endsWith(".lisp")
				|| filename.endsWith(".scm"))
			handler = new LispDocumentHandler();
		if (filename.endsWith(".lua"))
			handler = new LuaDocumentHandler();
		if (filename.endsWith(".ml"))
			handler = new MlDocumentHandler();
		if (filename.endsWith(".vb") || filename.endsWith(".bas"))
			handler = new VbDocumentHandler();
		if (filename.endsWith(".sql"))
			handler = new SqlDocumentHandler();

		if (handler == null)
			handler = new TextDocumentHandler();
		Log.v(LOGTAG, " Handler: " + filename);
		return handler;
	}

	/**
	 * Call the intent to open files
	 */
	public void openFileIntent() {
		Intent fileIntent = new Intent(HTMLViewerPlusPlus.this,
				FileBrowser.class);
		startActivityForResult(fileIntent, 10);

		/*
		 * Next Version Feature, support more explorer intents besides the built
		 * in one Intent intent = new Intent(); Uri startDir =
		 * Uri.fromFile(Environment.getExternalStorageDirectory());
		 * 
		 * if (isIntentAvailable(this,
		 * "vnd.android.cursor.dir/lysesoft.andexplorer.file")) { //AndExplorer
		 * intent.setAction(Intent.ACTION_PICK); intent.setDataAndType(startDir,
		 * "vnd.android.cursor.dir/lysesoft.andexplorer.file");
		 * intent.putExtra("explorer_title", "Select a file");
		 * intent.putExtra("browser_title_background_color", "440000AA");
		 * intent.putExtra("browser_title_foreground_color", "FFFFFFFF");
		 * intent.putExtra("browser_list_background_color", "00000066");
		 * intent.putExtra("browser_list_fontscale", "120%");
		 * intent.putExtra("browser_list_layout", "2");
		 * startActivityForResult(intent, PICK_REQUEST_CODE); } else if
		 * (isIntentAvailable(this,"org.openintents.action.PICK_FILE")) {
		 * //OIFileManager intent.setType("org.openintents.action.PICK_FILE");
		 * intent.setData(startDir); startActivityForResult(intent,
		 * PICK_REQUEST_CODE); } else { Toast.makeText(getApplicationContext(),
		 * "No File Manager Detected", 2000).show(); }
		 */

	}

	/***
	 * Closes the application
	 */
	public void quitApplication() {
		settings.edit().putString("lastOpen", this.currentFile).commit();
		finish();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	/**
	 * Loads the home screen
	 */
	public void loadHomeScreen() {
		mWebView.getSettings().setUseWideViewPort(false);
		mWebView.loadUrl("file:///android_asset/home.html");
		this.currentFile = "file:///android_asset/home.html";
		this.setTitle("CodePad - Home");
	}

	/**
	 * Loads the help screen
	 */
	public void loadHelpScreen() {
		mWebView.getSettings().setUseWideViewPort(false);
		mWebView.loadUrl("file:///android_asset/help.html");
		this.currentFile = "file:///android_asset/help.html";
		this.setTitle("CodePad - Help");
	}

	public void loadAboutScreen() {
		mWebView.getSettings().setUseWideViewPort(false);
		mWebView.loadUrl("file:///android_asset/about.html");
		this.currentFile = "file:///android_asset/about.html";
		this.setTitle("CodePad - About");
	}

	/**
	 * Load the HTML file into the webview by converting it to a data: URL. If
	 * there were any relative URLs, then they will fail as the webview does not
	 * allow access to the file:/// scheme for accessing the local file system,
	 * 
	 * Note: Before actually loading the info in webview, i add the prettify
	 * libraries to do the syntax highlight also i organize the data where it
	 * has to be. works fine now but it needs some work
	 * 
	 * @param uri
	 *            file URI pointing to the content to be loaded
	 * @param mimeType
	 *            mimetype provided
	 */
	void loadFile(Uri uri, String mimeType) {
		this.mWebView.freeMemory();

		if (uri.toString().equals(this.currentFile)) {
			return;
		}
		this.currentFile = uri.toString();

		String path = uri.getPath();

		DocumentHandler handler = getHandlerByExtension(path);

		File f = new File(path);
		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new InputStreamReader(
					new FileInputStream(f)), 100);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!f.exists()) {
			Log.e(LOGTAG, "File doesnt exists: " + path);
			return;
		}

		if (handler == null) {
			Log.e(LOGTAG, "Filetype not supported");
			Toast.makeText(HTMLViewerPlusPlus.this, "Filetype not supported",
					2000);
			return;
		}

		StringBuilder contentString = new StringBuilder();

		setTitle("CodePad - " + uri.getLastPathSegment());
		contentString.append("<html><head><title>" + uri.getLastPathSegment()
				+ "</title>");
		contentString
				.append("<link href='file:///android_asset/prettify.css' rel='stylesheet' type='text/css'/> ");
		contentString
				.append("<script src='file:///android_asset/prettify.js' type='text/javascript'></script> ");
		contentString.append(handler.getFileScriptFiles());
		contentString
				.append("</head><body onload='prettyPrint()'><code class='"
						+ handler.getFilePrettifyClass() + "'>");
		contentString.append(1 + "   ");

		StringBuilder codeString = new StringBuilder();
		int i = 2;
		while (true) {
			int temp = 0;
			try {
				temp = bf.read();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (temp == -1) {
				break;
			}
			if (temp == '\n') {
				codeString.append((char) temp);
				codeString.append(i + "   ", 0, 4);
				i++;
				try {
					temp = bf.read();
				} catch (Exception e) {
					e.printStackTrace();
					
				}
			}
			codeString.append((char) temp);
		}

		contentString.append(handler.getFileFormattedString(codeString
				.toString()));
		contentString
				.append("</code><br /><br /><br /><br /><br /></body></html> ");
		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.loadDataWithBaseURL("file:///android_asset/",
				contentString.toString(), handler.getFileMimeType(), "", "");

		Log.v(LOGTAG, "File Loaded: " + path);
	}

	/**
	 * Select Text in the webview and automatically sends the selected text to
	 * the clipboard
	 */
	public void selectAndCopyText() {
		try {

			setTitle("CodePad - Select");
			KeyEvent shiftPressEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
					KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
			shiftPressEvent.dispatch(mWebView);

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Clear all the matches in the search
	 */
	public void clearSearch() {
		mWebView.clearMatches();
	}

	/**
	 * Find Next Match in Search
	 */
	public void nextSearch() {
		mWebView.findNext(true);
	}

	/**
	 * Search inside the webview
	 */
	public void showSearchDialog() {

		final RelativeLayout window = (RelativeLayout) findViewById(R.id.window);

		window.removeView(this.mSlidingDrawer);

		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.search, null);

		final EditText text = (EditText) view.findViewById(R.id.search_text);

		Button search;
		Button next;
		Button exit;
		search = (Button) view.findViewById(R.id.search);
		next = (Button) view.findViewById(R.id.search_next);
		exit = (Button) view.findViewById(R.id.exit_search);

		search.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					Method m = WebView.class.getMethod("setFindIsUp",
							Boolean.TYPE);
					m.invoke(mWebView, true);
				} catch (Throwable ignored) {
				}
				String textStr = text.getText().toString();
				HTMLViewerPlusPlus.this.mWebView.findAll(textStr);
			}
		});

		next.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				HTMLViewerPlusPlus.this.nextSearch();

			}
		});

		exit.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					Method m = WebView.class.getMethod("setFindIsUp",
							Boolean.TYPE);
					m.invoke(mWebView, false);
				} catch (Throwable ignored) {
				}
				window.removeView(view);
				window.addView(HTMLViewerPlusPlus.this.mSlidingDrawer);
				HTMLViewerPlusPlus.this.setTitle("CodePad");
			}
		});

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.FILL_PARENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		window.addView(view, params);

		setTitle("CodePad - Search");

		/*
		 * AlertDialog.Builder alert = new AlertDialog.Builder(this);
		 * 
		 * alert.setTitle("Find Text"); alert.setMessage("Enter text to find:");
		 * 
		 * // Set an EditText view to get user input final EditText inputText =
		 * new EditText(this); alert.setView(inputText);
		 * 
		 * alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		 * public void onClick(DialogInterface dialog, int whichButton) { String
		 * value = inputText.getText().toString(); mWebView.findAll(value); }
		 * });
		 * 
		 * alert.setNegativeButton("Cancel", new
		 * DialogInterface.OnClickListener() { public void
		 * onClick(DialogInterface dialog, int whichButton) {
		 * 
		 * } });
		 * 
		 * alert.show();
		 */
	}

	@Override
	protected void onResume() {
		super.onResume();
		CookieSyncManager.getInstance().startSync();
	}

	@Override
	protected void onStop() {
		super.onStop();

		CookieSyncManager.getInstance().stopSync();
		mWebView.stopLoading();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mWebView.destroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {

			if (this.mSlidingDrawer.isOpened()) {
				mSlidingDrawer.animateClose();
				return false;
			}

			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Exit")
					.setMessage("Quit Android CodePad?")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									HTMLViewerPlusPlus.this.quitApplication();

								}
							}).setNegativeButton("No", null).create().show();

		}
		return false;
	}
}
