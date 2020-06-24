package hnw.mmk.mlw;

import android.app.*;
import android.os.*;
import java.net.*;
import android.content.*;
import android.net.*;
import android.widget.*;
import java.io.*;
import android.text.*;
import org.json.*;
import android.content.pm.*;
import android.util.*;
import java.lang.reflect.*;
import android.provider.*;
import android.database.*;
import static android.content.Context.DOWNLOAD_SERVICE;

public class AppUpdater
{
	private static String versionName = null;
	private static String downloadLink = "";
    private static int versionCode=0;
	private static DownloadManager mDownloadManager;
    private static long mDownloadedFileID;
	private static String Blogid;
	private static String Postid;
	private static Activity Context;
	public static void check(Activity context,String blogid,String postid){
		check();
		Blogid=blogid;
		Postid=postid;
		Context=context;
    }
    private static void check()
	{
        new AsyncTask<Void,Void,String>(){

            @Override
            protected String doInBackground(Void... p1)
			{
				HttpURLConnection httpConn=null;
				URL myurl=null;
				String result="";
                try
				{
					String url="https://www.blogger.com/feeds/" +Blogid+ "/posts/default/" + Postid+"?alt=json";
					myurl = new URL(url);
					httpConn = (HttpURLConnection)myurl.openConnection();
					httpConn.setUseCaches(false);
					httpConn.setRequestMethod("GET");
					InputStream is = httpConn.getInputStream();
					BufferedReader rd = new BufferedReader(new InputStreamReader(is));
					String line;
					StringBuffer response = new StringBuffer(); 
					while ((line = rd.readLine()) != null)
					{
						response.append(line);
						response.append('\n');
					}
					rd.close();
					result = response.toString().trim();
					result=Html.fromHtml(result).toString();
					try{
						JSONObject jo=new JSONObject(result);
						jo = jo.getJSONObject("entry");
						result=jo.getJSONObject("content").getString("$t");
					}catch(Exception e){
						e.printStackTrace();
						return null;
					}
					return result;
				}
				catch (Exception e)
				{
					e.printStackTrace();
					return null;
				}
				finally
				{
					if (httpConn != null)
					{
						httpConn.disconnect(); 
					}
				}
            }

            @Override
            protected void onPostExecute(String response)
			{
                super.onPostExecute(response);

                if (response != null)
				{
                    PackageManager manager = Context.getPackageManager();
					PackageInfo info;
					int currentVersion = 0;
					try
					{
						info = manager.getPackageInfo(Context.getPackageName(), 0);
						currentVersion = info.versionCode;
						JSONObject jo=new JSONObject(response);
						versionCode = jo.getInt("versionCode");
						versionName = jo.getString("versionName");
						downloadLink = "https://drive.google.com/uc?export=download&id="+jo.getString("link");
						if(versionCode>currentVersion){
							updateDialog(Context);
						}
					}

					catch (Exception e)
					{
						e.printStackTrace();
					}
                }
				else
				{
					Log.e("appupdate","sever error");

				}
            }
        }.execute();
    }
	private static void updateDialog(final Activity context){
		mDownloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder.setTitle("App Update!");
		builder.setCancelable(false);
		builder.setMessage("New update available. (" + versionName + ")");
		final AlertDialog ad=builder.create();
		ad.setButton(AlertDialog.BUTTON_POSITIVE,"update",
			new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface p1, int p2)
				{
					dlFile(downloadLink, "ML_Wallpaper" + versionName + ".apk");
				}
			});
		ad.show();

	}
	private static void dlFile(String url, String fileName){
		// this.url=url;
		// this.fileName=fileName;
        try {
			String mBaseFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+"/";
			if (!new File(mBaseFolderPath).exists()) {
				new File(mBaseFolderPath).mkdir();
			}
			File myFile = new File(mBaseFolderPath + fileName);
			if (!myFile.exists()) {
				String mFilePath = "file://" + mBaseFolderPath + fileName;
				Uri downloadUri = Uri.parse(url);
				DownloadManager.Request mRequest = new DownloadManager.Request(downloadUri);
				mRequest.setDestinationUri(Uri.parse(mFilePath));
				mRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
				mDownloadedFileID = mDownloadManager.enqueue(mRequest);
				IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
				Context.registerReceiver(downloadReceiver, filter);
				Toast.makeText(Context, "Starting Download : " + fileName, Toast.LENGTH_SHORT).show();
			} else {
				openFile(myFile.toString());
			}
		} catch (Exception e) {
			Context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		}
	}
	private static BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //check if the broadcast message is for our enqueued download
            final Uri uri = mDownloadManager.getUriForDownloadedFile(mDownloadedFileID);
            final String apk = getRealPathFromURI(uri);
            Toast.makeText(context, "Downloaded : "+new File(apk).getName(), Toast.LENGTH_SHORT).show();
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
				.setTitle("Download Completed!")
				.setCancelable(false)
				.setMessage("Please install this latest apk! ")
				.setPositiveButton("Install Now", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						openFile(apk);
					}
				});
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };

    private static void openFile(String apk){
        if(Build.VERSION.SDK_INT>=24){ try{ Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure"); m.invoke(null); }catch(Exception e){ e.printStackTrace(); } }
        Intent intent2 = new Intent(Intent.ACTION_VIEW);
        intent2.setDataAndType(Uri.parse("file://"+apk), "application/vnd.android.package-archive");
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Toast.makeText(Context, "Please install!!!", Toast.LENGTH_SHORT).show();
        Context.startActivity(intent2);
    }
	private static String getRealPathFromURI (Uri contentUri) {
        String path = null;
        String[] proj = { MediaStore.MediaColumns.DATA };
        Cursor cursor = Context.getContentResolver().query(contentUri, proj, null, null, null);
        assert cursor != null;
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            path = cursor.getString(column_index);
        }
        cursor.close();
        return path;
    }
}


