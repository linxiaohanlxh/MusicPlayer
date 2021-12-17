package com.example.musicplayer;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    public static final int DOWNLOAD_SUCCESS = 0;  //下载成功
    public static final int DOWNLOAD_FAILED = 1;  //下载失败
    public static final int DOWNLOAD_PAUSED = 2;  //暂停下载
    public static final int DOWNLOAD_CANCELED = 3;  //取消下载

    private long lastProgress; //记录最新下载进度

    private boolean isCanceled = false;
    private boolean isPaused = false;

    private DownloadListener listener;

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... strings) {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try {
            long downloadedLength = 0;  //记录文件已下载进度大小
            String downloadUrl = strings[0]; //下载路径
            //通过下载路径得到下载的文件名
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/'));
            //获取系统目录
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            //如果文件存在本地
            if (file.exists()) {
                downloadedLength = file.length();
            }
            long needDownloadLength = getContentLength(downloadUrl); //得到需要下载的文件大小
            if (needDownloadLength == 0) {
                //需要下载的进度大小为0，说明没找到要下载的文件，返回下载失败
                return DOWNLOAD_FAILED;
            } else if (needDownloadLength == downloadedLength) {
                //如果已下载进度等于需要下载进度，说明下载完成了，返回下载成功
                return DOWNLOAD_SUCCESS;
            }
            //需要下载进度既不为0，也不等于已下载进度，说明要从网络来下载
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder() // 断点下载，指定从哪个字节开始下载
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-").url(downloadUrl).build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();
                //不存在自动创建名为file的文件
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadedLength); // 跳过已下载的字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(b)) != -1) {
                    //检测是否按下取消
                    if (isCanceled) {
                        return DOWNLOAD_CANCELED;
                    } else if (isPaused) {
                        //检测是否按下暂停
                        return DOWNLOAD_PAUSED;
                    } else {
                        total += len;
                        savedFile.write(b,0, len);
                        // 计算已下载的百分比
                        int progress = (int) ((total + downloadedLength) * 100 / needDownloadLength);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return DOWNLOAD_SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (savedFile != null) {
                    savedFile.close();
                }
                //点击了取消，删除文件
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return DOWNLOAD_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress) {
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case DOWNLOAD_SUCCESS:
                listener.onSuccess();
                break;
            case DOWNLOAD_FAILED:
                listener.onFailed();
                break;
            case DOWNLOAD_PAUSED:
                listener.onPaused();
                break;
            case DOWNLOAD_CANCELED:
                listener.onCanceled();
                break;
            default:
                break;
        }
    }

    public void pauseDownload() {
        isPaused = true;
    }

    public void cancelDownload() {
        isCanceled = true;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}

