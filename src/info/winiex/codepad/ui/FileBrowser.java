package info.winiex.codepad.ui;

import java.io.File;
import java.io.FileInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

/***
 * @author John Lombard This class belongs to Jonh Lombard from anddev.org, its
 *         a very basic class but its a good starting point for a filebrowser i
 *         made a few changes to make it compatible to 1.6, all the credit
 *         belong to him.
 */
public class FileBrowser extends ListActivity {

	private FileBrowserHelper helper;
	private final int directoryMenu = -1;
	private final int fileMenu = -2;

	boolean setProjectHome;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setTitle("CodePad - FileBrowser");
		helper = new FileBrowserHelper(this, this.getListView());

		this.getListView().setOnCreateContextMenuListener(
				new ListView.OnCreateContextMenuListener() {

					@Override
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {

						AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;

						TextView item = (TextView) adapterMenuInfo.targetView;

						helper.selectedFile = new File(helper.currentDirectory
								.toString() + item.getText());

						if (helper.selectedFile.isDirectory()) {
							menu.add(directoryMenu, Menu.FIRST, 0,
									"Set As Project Home");
							menu.add(directoryMenu, Menu.FIRST + 1, 0,
									"Go Into");
							menu.add(directoryMenu, Menu.FIRST + 2, 0,
									"View Properties");
						} else if (helper.selectedFile.isFile()) {
							menu.add(fileMenu, Menu.FIRST, 0, "View Code");
							menu.add(fileMenu, Menu.FIRST + 1, 0,
									"View Properties");
						}

					}
				});

		this.getListView().setBackgroundColor(0xffffffff);
		this.getListView().setCacheColorHint(0x00000000);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (item.getGroupId() == this.directoryMenu) {

			switch (item.getItemId()) {
			case Menu.FIRST:
				SharedPreferences pref = getSharedPreferences(
						"com.william.codepad.settings", MODE_PRIVATE);
				pref.edit()
						.putString("projectHome",
								helper.selectedFile.toString()).commit();
				break;
			case Menu.FIRST + 1:
				helper.browseTo(helper.selectedFile);
				break;
			case Menu.FIRST + 2:

				String fileType = helper.selectedFile.isDirectory() ? "Directory"
						: "File";
				int fileNum = 0;

				fileNum = helper.selectedFile.listFiles().length;

				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle("File Properties")
						.setMessage(
								"File Type : " + fileType + "\n"
										+ "Number Of Files : " + fileNum)
						.setNeutralButton("Ok", null).create().show();
				break;
			}

		} else if (item.getGroupId() == this.fileMenu) {

			switch (item.getItemId()) {
			case Menu.FIRST:
				helper.browseTo(helper.selectedFile);
				break;
			case Menu.FIRST + 1:
				String fileType = "File";
				int fileSize = 0;

				try {
					fileSize = new FileInputStream(helper.selectedFile)
							.available();
				} catch (Exception e) {
					e.printStackTrace();
				}

				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle("File Properties")
						.setMessage(
								"File Type : " + fileType + "\n"
										+ "File Size : " + fileSize + "bytes")
						.setNeutralButton("Ok", null).create().show();
				break;
			}

		}

		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		int selectionRowID = (int) l.getItemIdAtPosition(position);
		String selectedFileString = helper.directoryEntries.get(selectionRowID);

		if (selectedFileString.equals("..")) {
			helper.upOneLevel();
		} else {
			File clickedFile = null;
			switch (helper.displayMode) {
			case RELATIVE:
				clickedFile = new File(
						helper.currentDirectory.getAbsolutePath()
								+ helper.directoryEntries.get(selectionRowID));
				break;
			case ABSOLUTE:
				clickedFile = new File(
						helper.directoryEntries.get(selectionRowID));
				break;
			}
			if (clickedFile != null)
				helper.browseTo(clickedFile);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (helper.currentDirectory.getParent() == null) {

				this.finish();
				return false;
			}
			helper.upOneLevel();
			return true;
		}

		return true;
	}

}