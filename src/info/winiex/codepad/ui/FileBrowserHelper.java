package info.winiex.codepad.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import info.winiex.codepad.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FileBrowserHelper {

	public enum DISPLAYMODE {
		ABSOLUTE, RELATIVE;
	}

	private final String[] fileTypes = { ".java", ".cpp", ".cc", ".c", ".html",
			".htm", ".xhtml", ".js", ".mxml", ".pl", ".py", ".rb", ".xml",
			".css", ".el", ".lisp", ".scm", ".lua", ".ml", ".vb", ".bas",
			".sql" };

	public final DISPLAYMODE displayMode = DISPLAYMODE.RELATIVE;

	public List<String> directoryEntries = new ArrayList<String>();
	private Activity context;
	public ListView listView;
	public File currentDirectory;
	public File selectedFile;

	public FileBrowserHelper(Activity context, ListView listView) {
		this(context, listView, "/sdcard/");
	}

	public FileBrowserHelper(Activity context, ListView listView, String path) {
		this.context = context;
		this.listView = listView;
		currentDirectory = new File(path);
		browseTo(currentDirectory);
	}

	public void browseToRoot() {
		browseTo(new File("/sdcard/"));
	}

	public void upOneLevel() {
		if (this.currentDirectory.getParent() != null)
			this.browseTo(this.currentDirectory.getParentFile());
	}

	public void refreshListView(File dir) {
		if (dir.isDirectory()) {
			browseTo(dir);
		}
	}

	public void browseTo(final File aDirectory) {
		if (aDirectory.isDirectory()) {
			this.currentDirectory = aDirectory;
			fill(aDirectory.listFiles());
		} else {

			if (!this.isFileSupported(aDirectory.toString())) {
				AlertDialog.Builder alert = new AlertDialog.Builder(context);

				alert.setTitle("File Not Supported")
						.setMessage(
								"You know i am a \"CodePad\", so i can't open this type of file.Sorry, but please go on.")
						.setNeutralButton("OK", null).create().show();
				return;
			}

			if (context instanceof FileBrowser) {
				Intent resultIntent = new Intent(
						android.content.Intent.ACTION_VIEW, Uri.parse("file://"
								+ aDirectory.getAbsolutePath()));

				context.setResult(Activity.RESULT_OK, resultIntent);
				context.finish();
			} else if (context instanceof HTMLViewerPlusPlus) {
				((HTMLViewerPlusPlus) context).mSlidingDrawer.animateClose();
				((HTMLViewerPlusPlus) context).loadFile(
						Uri.parse("file://" + aDirectory.getAbsolutePath()),
						"text/html");

			}
		}
	}

	private void fill(File[] files) {
		this.directoryEntries.clear();

		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {

			e1.printStackTrace();
		}

		if (this.currentDirectory.getParent() != null) {
			this.directoryEntries.add("..");
		}
		if (files != null) {
			switch (this.displayMode) {
			case ABSOLUTE:
				for (File file : files) {
					this.directoryEntries.add(file.getPath());
				}
				break;
			case RELATIVE:
				int currentPathStringLenght = this.currentDirectory
						.getAbsolutePath().length();
				for (File file : files) {
					this.directoryEntries.add(file.getAbsolutePath().substring(
							currentPathStringLenght));
				}
				break;
			}
		}

		ArrayAdapter<String> directoryList = new ArrayAdapter<String>(context,
				R.layout.file_row, this.directoryEntries);

		listView.setAdapter(directoryList);
	}

	private boolean isFileSupported(String file) {
		boolean result = false;

		int length = this.fileTypes.length;

		for (int i = 0; i < length; ++i) {
			if (file.endsWith(fileTypes[i])) {
				result = true;
			}
		}

		return result;
	}

}
