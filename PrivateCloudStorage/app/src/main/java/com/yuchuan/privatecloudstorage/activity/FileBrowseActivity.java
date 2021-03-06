package com.yuchuan.privatecloudstorage.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.widget.PullRefreshLayout;
import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.yuchuan.privatecloudstorage.R;
import com.yuchuan.privatecloudstorage.config.Config;
import com.yuchuan.privatecloudstorage.json.FileData;
import com.yuchuan.privatecloudstorage.net.GetServerFileList;
import com.yuchuan.privatecloudstorage.net.Mkdir;
import com.yuchuan.privatecloudstorage.net.Rename;
import com.yuchuan.privatecloudstorage.net.RmFile;
import com.yuchuan.privatecloudstorage.util.IntentClassify;
import com.yuchuan.privatecloudstorage.util.Util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by haroldmiao on 2015/2/25.
 */
public class FileBrowseActivity extends Activity {
    private ListView fileList;
    private BootstrapButton mkdir;
    private BootstrapButton upload;
    private TextView tvRootPathInfo;
    private TextView yunpan;
    private TextView source;
    private TextView me;
    private Button back;
    //private TextView root;
    private String rootPath;
    private int selectPos;
    private BootstrapButton transfer;

    private GlobalSettings settings = GlobalSettings.getInstance();


    private List<FileData> fileDatas = new ArrayList<FileData>();
    private HttpHandler<File> cacheFilehandler;
    private BroadcastReceiver mCacheFileReceiver;
    private boolean mCacheFileReceiverRegistered;

    PullRefreshLayout xiahua;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("FileBrowseActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_browse);
        initView();
        initEvent();
        mCacheFileReceiver = new CacheFileReceiver();

        Intent intent = getIntent();
        rootPath = intent.getStringExtra("PATH");
        //root.setText(rootPath);
        String[] paths = rootPath.split("/");
        String setInfo = paths[paths.length - 1].replace("/", "");
        if (setInfo.length() > 20) {
            setInfo = setInfo.substring(0, 19) + "....";
        }
        tvRootPathInfo.setText(setInfo);
        getServerFileList(rootPath);
    }


    @Override
    public void onResume() {
        registerCancleCacheFileReceiver();
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterCancleCacheFileReceiver();
        super.onPause();
    }

    private void registerCancleCacheFileReceiver() {
        unregisterCancleCacheFileReceiver();
        IntentFilter intentToReceiveFilter = new IntentFilter();
        intentToReceiveFilter
                .addAction("CancleCacheFile");
        LocalBroadcastManager.getInstance(FileBrowseActivity.this).registerReceiver(
                mCacheFileReceiver, intentToReceiveFilter);
        mCacheFileReceiverRegistered = true;
    }

    private void unregisterCancleCacheFileReceiver() {
        if (mCacheFileReceiverRegistered) {
            LocalBroadcastManager.getInstance(FileBrowseActivity.this).unregisterReceiver(
                    mCacheFileReceiver);
            mCacheFileReceiverRegistered = false;
        }
    }

    private void initView() {
        //root = (TextView) findViewById(R.id.tv_root_path);
        yunpan = (TextView) findViewById(R.id.tv_cloud);
        source = (TextView) findViewById(R.id.tv_source);
        me = (TextView) findViewById(R.id.tv_me);
        fileList = (ListView) findViewById(R.id.lv_file_list);
        mkdir = (BootstrapButton) findViewById(R.id.btn_mkdir);
        upload = (BootstrapButton) findViewById(R.id.btn_upload);
        transfer = (BootstrapButton) findViewById(R.id.btn_transfer);
        back = (Button) findViewById(R.id.btn_back);
        tvRootPathInfo = (TextView) findViewById(R.id.tv_root_path_info);

        xiahua = (PullRefreshLayout) findViewById(R.id.swipeRefreshLayout);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                // 添加操作

                final HashMap<String, Object> f = (HashMap<String, Object>) fileList.getAdapter().getItem(selectPos);

                new AlertDialog.Builder(FileBrowseActivity.this)
                        .setTitle("删除")
                        .setMessage("你确定要删除吗")
                        .setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        try {
                                            new RmFile("/" + URLEncoder.encode(f.get("FilePath").toString(), "UTF-8"), "kkk",
                                                    new RmFile.SuccessCallback() {
                                                @Override
                                                public void onSuccess(String result) {
                                                    Log.i("FileBrowseActivity", "onSuccess");
                                                    getServerFileList(rootPath);
                                                }
                                            }, new RmFile.FailCallback() {
                                                @Override
                                                public void onFail() {
                                                    Toast.makeText(FileBrowseActivity.this,
                                                            "删除 ： " + f.get("FilePath").toString() + " 失败",
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();


//                try {
//                    new RmFile("/" + URLEncoder.encode(f.get("FilePath").toString(), "UTF-8"), "kkk", new RmFile.SuccessCallback() {
//                        @Override
//                        public void onSuccess(String result) {
//                            Log.i("FileBrowseActivity", "onSuccess");
//
//                            //getServerFileList(rootPath);
//                            try {
//                                getServerFileList(URLEncoder.encode(rootPath, "UTF-8"));
//                            } catch (UnsupportedEncodingException e) {
//                                e.printStackTrace();
//                            }
//                            Toast.makeText(FileBrowseActivity.this,
//                                    "删除 ： " + f.get("FilePath").toString() + " 成功",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    }, new RmFile.FailCallback() {
//
//                        @Override
//                        public void onFail() {
//                            Log.i("FileBrowseActivity", "onFail");
//                            Toast.makeText(FileBrowseActivity.this,
//                                    "删除 ： " + f.get("FilePath").toString() + " 失败",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }

                break;

            case 1:
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.comm_rename_dialog,
                        (ViewGroup) findViewById(R.id.comm_rename_dialog));
                final BootstrapEditText et_rename = (BootstrapEditText)layout.findViewById(R.id.et_rename_name);
                final HashMap<String, Object> f2 = (HashMap<String, Object>) fileList.getAdapter().getItem(selectPos);
                String oldName = f2.get("FileName").toString();
                et_rename.setText(oldName);
                new AlertDialog.Builder(FileBrowseActivity.this).setTitle("重命名").setView(layout)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String newName = et_rename.getText().toString();
                                try {
                                    new Rename("/" + URLEncoder.encode(f2.get("FilePath").toString(), "UTF-8"),
                                            URLEncoder.encode(rootPath + "/" + newName, "UTF-8"), "kkk", new Rename.SuccessCallback() {
                                        @Override
                                        public void onSuccess(String result) {
                                            Log.i("MainActivity", "onSuccess");

                                            try {
                                                getServerFileList(URLEncoder.encode(rootPath, "UTF-8"));
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }, new Rename.FailCallback() {

                                        @Override
                                        public void onFail() {
                                            Toast.makeText(FileBrowseActivity.this,
                                                "重命名 ： " + f2.get("FilePath").toString() + " 失败",
                                                Toast.LENGTH_LONG).show();
                                        }
                                    });
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }

                            }
                        }).
                        setNegativeButton("取消", null)
                        .show();
//                RenameDialog dialog = new RenameDialog(FileBrowseActivity.this,
//                        new RenameDialog.LeaveMyDialogListener() {
//                            @Override
//                            public void onClick(View view, String newName) {
//                                HashMap<String, Object> f2 = (HashMap<String, Object>) fileList.getAdapter().getItem(selectPos);
//                                try {
//                                    new Rename("/" + URLEncoder.encode(f2.get("FilePath").toString(), "UTF-8"),
//                                            URLEncoder.encode(rootPath + "/" + newName, "UTF-8"), "kkk", new Rename.SuccessCallback() {
//                                        @Override
//                                        public void onSuccess(String result) {
//                                            Log.i("MainActivity", "onSuccess");
//
//                                            getServerFileList(rootPath);
//                                        }
//                                    }, new Rename.FailCallback() {
//
//                                        @Override
//                                        public void onFail() {
//                                            Log.i("MainActivity", "onFail");
//                                        }
//                                    });
//                                } catch (UnsupportedEncodingException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        });
//                dialog.setCancelable(false);
//                dialog.show();

                break;
            case 2:
                HashMap<String, Object> f3 = (HashMap<String, Object>) fileList.getAdapter().getItem(selectPos);
                String downUrl = Config.PLAY_URL + f3.get("FilePath").toString();

                if (settings.downFlag.containsKey(downUrl) == false) {
                    settings.downFlag.put(downUrl, true);
                    downloadFile(downUrl, Config.STORE_BASE_PATH + f3.get("FilePath").toString());
                } else {
                    Toast.makeText(FileBrowseActivity.this,
                            "已经在下载中。。。",
                            Toast.LENGTH_SHORT).show();
                }


                Toast.makeText(FileBrowseActivity.this,
                        "正在为您加密传输",
                        Toast.LENGTH_SHORT).show();

                break;

            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    private synchronized void publishProgress(LocalBroadcastManager mBroadcastManager,
                                              String fileName, int progresses, String speeds, boolean status) {
        Intent i = new Intent();
        i.setAction("DownloadingProgress");
        i.putExtra("fileName", fileName);
        i.putExtra("progress", progresses);
        i.putExtra("speeds", speeds);
        i.putExtra("status", status);
        mBroadcastManager.sendBroadcast(i);
    }

    private synchronized void publishProgressUpload(LocalBroadcastManager mBroadcastManager,
                                                    String fileName, int progresses, String speeds, boolean status) {
        Intent i = new Intent();
        i.setAction("UploadingProgress");
        i.putExtra("fileName", fileName);
        i.putExtra("progress", progresses);
        i.putExtra("speeds", speeds);
        i.putExtra("status", status);
        mBroadcastManager.sendBroadcast(i);
    }

    private void downloadFile(final String url, String storePath) {
        HttpUtils http = new HttpUtils();
        final LocalBroadcastManager mBroadcastManager = LocalBroadcastManager.getInstance(this);
        HttpHandler handler = http.download(url,
                storePath,
                false, // 如果目标文件存在，接着未完成的部分继续下载。服务器不支持RANGE时将从新下载。
                true, // 如果从请求返回信息中获取到文件名，下载完成后自动重命名。
                new RequestCallBack<File>() {

                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onLoading(long total, long current, boolean isUploading) {
                        Log.i("FileBrowseActivity", current + "/" + total);
                        publishProgress(mBroadcastManager, url, (int)((current*100/total)), current + "/" + total, true);
                        Log.i("FileBrowseActivity", String.valueOf((current*100/total)));
                    }

                    @Override
                    public void onSuccess(ResponseInfo<File> responseInfo) {
                        Toast.makeText(FileBrowseActivity.this, "下载文件: " + "成功", Toast.LENGTH_LONG).show();
                        settings.downFlag.remove(url);

                    }


                    @Override
                    public void onFailure(HttpException error, String msg) {
                        Toast.makeText(FileBrowseActivity.this, "下载文件: " + "失败", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadFile(String url, final String path) {
        Log.i("FileBrowseActivity", "uploadFile");
        RequestParams params = new RequestParams();
        //params.addHeader("name", "value");
        params.addQueryStringParameter("action", "upload");
        final String names[] = path.split("/");
        params.addQueryStringParameter("path", rootPath + "/" + names[names.length -1]);
        params.addBodyParameter("file", new File(path));
        final LocalBroadcastManager mBroadcastManagerUpload = LocalBroadcastManager.getInstance(this);
        HttpUtils http = new HttpUtils();
        http.send(HttpRequest.HttpMethod.POST,
                url,
                params,
                new RequestCallBack<String>() {

                    @Override
                    public void onStart() {
                        Log.i("FileBrowseActivity", "onStart");
                    }

                    @Override
                    public void onLoading(long total, long current, boolean isUploading) {
                        if (isUploading) {
                            //testTextView.setText("upload: " + current + "/" + total);
                            publishProgressUpload(mBroadcastManagerUpload, path, (int)((current*100/total)), current + "/" + total, true);
                        } else {
                            //testTextView.setText("reply: " + current + "/" + total);
                        }
                    }

                    @Override
                    public void onSuccess(ResponseInfo<String> responseInfo) {
                        //testTextView.setText("reply: " + responseInfo.result);
                        Log.i("FileBrowseActivity", "onSuccess");
                        Toast.makeText(FileBrowseActivity.this, "上传文件: " +
                                "[" + names[names.length -1] + "]成功", Toast.LENGTH_LONG).show();
                        try {
                            getServerFileList(URLEncoder.encode(rootPath, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(HttpException error, String msg) {
                        //testTextView.setText(error.getExceptionCode() + ":" + msg);
                        Log.i("FileBrowseActivity", "onFailure");
                        Toast.makeText(FileBrowseActivity.this, "上传文件: " +
                                "[" + names[names.length -1] + "]失败, 请检查你的网络连接情况!", Toast.LENGTH_LONG).show();
                    }
                });
    }


    public class CacheFileReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    "CancleCacheFile")) {
                cacheFilehandler.cancel();
            }
        }
    }

    private void cacheFile(final String url, final String storePath, final String fileType) {
        HttpUtils http = new HttpUtils();
        final LocalBroadcastManager mCacheFileFinish = LocalBroadcastManager.getInstance(this);
        final LocalBroadcastManager mCacheFileProgress = LocalBroadcastManager.getInstance(this);

        RequestCallBack<File> cacheFile = new RequestCallBack<File>() {
            @Override
            public void onStart() {

            }

            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                Intent i = new Intent();
                i.setAction("CacheFileProgress");
                i.putExtra("Progress", (int) ((current * 100 / total)));
                mCacheFileProgress.sendBroadcast(i);
            }

            @Override
            public void onSuccess(ResponseInfo<File> fileResponseInfo) {
                Toast.makeText(FileBrowseActivity.this, "打开文件: " + "成功", Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                switch (fileType) {
                    case "pdf":
                        intent = IntentClassify.getPdfFileIntent(storePath);
                        break;
                    case "txt":
                        intent = IntentClassify.getTextFileIntent(storePath, false);
                        break;
                    case "pic":
                        intent = IntentClassify.getImageFileIntent(storePath);
                        break;
                }
                startActivity(intent);

                Intent i = new Intent();

                i.setAction("CacheFileFinish");
                mCacheFileFinish.sendBroadcast(i);
            }

            @Override
            public void onFailure(HttpException e, String s) {
                Toast.makeText(FileBrowseActivity.this, "打开文件: " + "失败", Toast.LENGTH_LONG).show();
            }
        };
        cacheFilehandler = http.download(url, storePath, false, true, cacheFile);

//        final HttpHandler handler = http.download(url,
//                storePath,
//                false, // 如果目标文件存在，接着未完成的部分继续下载。服务器不支持RANGE时将从新下载。
//                true, // 如果从请求返回信息中获取到文件名，下载完成后自动重命名。
//                new RequestCallBack<File>() {
//
//                    @Override
//                    public void onStart() {
//                        settings.preTime = System.nanoTime();
//                    }
//
//                    @Override
//                    public void onLoading(long total, long current, boolean isUploading) {
//
//                    }
//
//                    @Override
//                    public void onSuccess(ResponseInfo<File> responseInfo) {
//                        Toast.makeText(MainActivity.this, "打开文件: " + "成功", Toast.LENGTH_LONG).show();
//                        Intent intent = IntentClassify.getPdfFileIntent(storePath);
//                        startActivity(intent);
//                    }
//
//
//                    @Override
//                    public void onFailure(HttpException error, String msg) {
//                        Toast.makeText(MainActivity.this, "打开文件: " + "失败", Toast.LENGTH_LONG).show();
//                    }
//                });
    }


    private void openFile(String path, final int position, final String fileType) {
        final OpenFileDialog dialog = new OpenFileDialog(FileBrowseActivity.this,
                new OpenFileDialog.LeaveMyDialogListener() {
                    @Override
                    public void onClick() {
                        Log.i("MainActivity", "onClick");
                        HashMap<String, Object> f = (HashMap<String, Object>) fileList.getAdapter().getItem(position);
                        String downUrl = Config.PLAY_URL + f.get("FilePath").toString();
                        Log.i("MainActivity", downUrl);
                        cacheFile(downUrl, Config.STORE_BASE_PATH + f.get("FilePath").toString(), fileType);
                    }
                });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void initEvent() {
        xiahua.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    getServerFileList(URLEncoder.encode(rootPath, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //Toast.makeText(MainActivity.this, "你点击了第" + (position + 1) + "项", Toast.LENGTH_SHORT).show();
                FileData f = fileDatas.get(position);
                String path = null;
                String path_encoder = null;
                path = f.getPath();

                try {
                    path_encoder = URLEncoder.encode(f.getPath(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String type = f.getType();
                String name = f.getName();
                if (type.equals("dir")) {
                    Intent intent;
                    intent = new Intent(FileBrowseActivity.this, FileBrowseActivity.class);
                    intent.putExtra("PATH", path);
                    startActivity(intent);
                } else {
                    String postfix = Util.getFilePostfix(name);
                    if (postfix.equalsIgnoreCase("mp4")) {
                        Intent intent = IntentClassify.getVideoFileIntent(path_encoder);
                        startActivity(intent);
                    }
                    if (postfix.equalsIgnoreCase("pdf")) {
//                        Intent intent = IntentClassify.getPdfFileIntent(path_encoder);
//                        startActivity(intent);
                        openFile(path, position, "pdf");
                    }
                    if (postfix.equalsIgnoreCase("png") || postfix.equalsIgnoreCase("jpg")) {
//                        Intent intent = IntentClassify.getImageFileIntent(path_encoder);
//                        startActivity(intent);
                        openFile(path, position, "pic");
                    }
                    if (postfix.equalsIgnoreCase("txt")) {
//                        Intent intent = IntentClassify.getTextFileIntent(path_encoder, true);
//                        startActivity(intent);
                        openFile(path, position, "txt");
                    }
                }
            }
        });

        fileList.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenu.ContextMenuInfo menuInfo) {
//                menu.add(0, 0, 0, "删除");
//                menu.add(0, 1, 0, "重命名");
//                menu.add(0, 2, 0, "下载");
                HashMap<String, Object> f = (HashMap<String, Object>) fileList.getAdapter().getItem(selectPos);
                String type = (String) f.get("FileType");
                if (type.equals("dir")) {
                    menu.add(0, 0, 0, "删除");
                    menu.add(0, 1, 0, "重命名");
                } else {
                    menu.add(0, 0, 0, "删除");
                    menu.add(0, 1, 0, "重命名");
                    menu.add(0, 2, 0, "下载");
                }
            }
        });

        fileList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectPos = position;

                return false;
            }
        });

        mkdir.setOnClickListener(new View.OnClickListener() {
            String tmp;

            @Override
            public void onClick(View v) {
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.comm_mkdir_dialog,
                        (ViewGroup) findViewById(R.id.comm_mkdir_dialog));
                final BootstrapEditText et_mkdir = (BootstrapEditText)layout.findViewById(R.id.et_dir_name);
                new AlertDialog.Builder(FileBrowseActivity.this).setTitle("新建文件夹").setView(layout)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String name = et_mkdir.getText().toString();
                                try {
                                    new Mkdir(URLEncoder.encode(rootPath + "/" + name, "UTF-8"), "kkk", new Mkdir.SuccessCallback() {

                                        @Override
                                        public void onSuccess(String result) {
                                            Log.i("MainActivity", "onSuccess");

                                            //Toast.makeText(MainActivity.this, "创建文件夹: " + tmp + ", 请回退刷新查看", Toast.LENGTH_LONG).show();
                                            try {
                                                getServerFileList(URLEncoder.encode(rootPath, "UTF-8"));
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }

                                        }
                                    }, new Mkdir.FailCallback() {

                                        @Override
                                        public void onFail() {
                                            Log.i("MainActivity", "onFail");
                                        }
                                    });
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }

                            }
                        }).
                        setNegativeButton("取消", null)
                        .show();
//                MkdirDialog dialog = new MkdirDialog(FileBrowseActivity.this,
//                        new MkdirDialog.LeaveMyDialogListener() {
//                            @Override
//                            public void onClick(View view, String dirD) {
//                                try {
//                                    tmp = URLEncoder.encode(dirD, "UTF-8");
//                                } catch (UnsupportedEncodingException e) {
//                                    e.printStackTrace();
//                                }
//                                try {
//                                    new Mkdir(URLEncoder.encode(rootPath + "/" + tmp, "UTF-8"), "kkk", new Mkdir.SuccessCallback() {
//
//                                        @Override
//                                        public void onSuccess(String result) {
//                                            Log.i("FileBrowseActivity", "onSuccess");
//                                            //Toast.makeText(FileBrowseActivity.this, "创建文件夹: " + tmp + ", 请回退刷新查看", Toast.LENGTH_LONG).show();
//                                            try {
//                                                getServerFileList(URLEncoder.encode(rootPath, "UTF-8"));
//                                            } catch (UnsupportedEncodingException e) {
//                                                e.printStackTrace();
//                                            }
//                                        }
//                                    }, new Mkdir.FailCallback() {
//
//                                        @Override
//                                        public void onFail() {
//                                            Log.i("FileBrowseActivity", "onFail");
//                                        }
//                                    });
//                                } catch (UnsupportedEncodingException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        });
//                dialog.setCancelable(false);
//                dialog.show();
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileDialog dialog = new FileDialog.Builder(FileBrowseActivity.this)
                        .setFileMode(FileDialog.FILE_MODE_OPEN_MULTI)
                        .setCancelable(true).setCanceledOnTouchOutside(false)
                        .setTitle("选择要上传的文件")
                        .setFileSelectListener(new FileDialog.FileDialogListener() {
                            @Override
                            public void onFileSelected(ArrayList<File> files) {
                                if (files.size() > 0) {
                                    //Log.i("FileBrowseActivity", files.get(0).toString());
                                    uploadFile(Config.URL, files.get(0).toString());
                                }
                            }

                            @Override
                            public void onFileCanceled() {
                                //ToastUtil.showToast(getActivity(), "Copy Cancelled!");
                            }
                        }).create(FileBrowseActivity.this);
                dialog.show();
            }
        });


        back.setOnClickListener(new View.OnClickListener() {
            String tmp;

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        transfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, DownloadListActivity.class);
//                startActivity(intent);

                Intent intent = new Intent(FileBrowseActivity.this, TransferActivity.class);
                startActivity(intent);
            }
        });


        yunpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        source.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FileBrowseActivity.this, MeActivity.class);
                yunpan.setTextColor(Color.parseColor("#008000"));
                source.setTextColor(Color.parseColor("#000000"));
                startActivity(intent);
            }
        });

    }

    public class FileItemComparator implements Comparator<FileData> {
        @Override
        public int compare(FileData lhs, FileData rhs) {
            if (! lhs.getType().equals(rhs.getType())) {
                // 如果一个是文件，一个是文件夹，优先按照类型排序
                if (lhs.getType().equals("dir")) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                // 如果同是文件夹或者文件，则按名称排序
                return lhs.getName().toLowerCase()
                        .compareTo(rhs.getName().toLowerCase());
            }
        }
    }

    public void sortList(List<FileData> list) {
        FileItemComparator comparator = new FileItemComparator();
        Collections.sort(list, comparator);
    }

    private void getServerFileList(String path) {
        try {
            new GetServerFileList(URLEncoder.encode(path, "UTF-8"), "kkk", new GetServerFileList.SuccessCallback() {
                @Override
                public void onSuccess(String result) {
                    Log.i("MainActivity", "onSuccess");
                    if (result.equals("{}")) {

                    } else {
                        Gson gson = new Gson();
                        fileDatas = gson.fromJson(result, new TypeToken<List<FileData>>() {
                        }.getType());
                        sortList(fileDatas);
                        MyAdapter mAdapter = new MyAdapter(FileBrowseActivity.this);
                        fileList.setAdapter(mAdapter);
                        //mAdapter.notifyDataSetChanged();
                    }

                    xiahua.setRefreshing(false);
                }
            }, new GetServerFileList.FailCallback() {

                @Override
                public void onFail() {
                    Log.i("MainActivity", "onFail");
                    Toast.makeText(FileBrowseActivity.this,"连接服务器失败，请检查你的网络", Toast.LENGTH_LONG).show();
                    xiahua.setRefreshing(false);
                }


            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }


    private ArrayList<HashMap<String, Object>> getDate(){
        ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();

        for(int i = 0; i < fileDatas.size() ; i++)
        {
            FileData f = fileDatas.get(i);
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("FileName", f.getName());
            map.put("FileSize", f.getSize());
            map.put("FilePath", f.getPath());
            map.put("FileType", f.getType());
            map.put("ModifyData", f.getModify());
            listItem.add(map);
        }
        return listItem;
    }


    public  class MyAdapter extends BaseAdapter {

        private LayoutInflater mInflater;//得到一个LayoutInfalter对象用来导入布局

        /*构造函数*/
        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return getDate().size();//返回数组的长度
        }

        @Override
        public Object getItem(int position) {
            return getDate().get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
        /*书中详细解释该方法*/
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            //观察convertView随ListView滚动情况
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.activity_main_list_item, null);
                holder = new ViewHolder();
                /*得到各个控件的对象*/
                holder.img = (ImageView) convertView.findViewById(R.id.iv_img);
                holder.fileName = (TextView) convertView.findViewById(R.id.tv_file_name);
                holder.fileSize = (TextView) convertView.findViewById(R.id.tv_file_size);
                convertView.setTag(holder);//绑定ViewHolder对象
            }
            else {
                holder = (ViewHolder)convertView.getTag();//取出ViewHolder对象
            }
            /*设置TextView显示的内容，即我们存放在动态数组中的数据*/
            holder.fileName.setText(getDate().get(position).get("FileName").toString());

//            long tmpSize = (long) getDate().get(position).get("FileSize");
//
//            if (tmpSize <= 1024) {
//                holder.fileSize.setText(tmpSize + "B");
//            } else if (tmpSize > 1024 && tmpSize <= 1024*1024) {
//                holder.fileSize.setText(tmpSize/1024 + "KB");
//            } else if (tmpSize > 1024*1024 && tmpSize <= 1024*1024*1024) {
//                holder.fileSize.setText(tmpSize/(1024 *1024) + "MB");
//            } else if (tmpSize > 1024*1024*1024 && tmpSize <= 1024*1024*1024*1024) {
//                holder.fileSize.setText(tmpSize/(1024 *1024*1024) + "GB");
//            } else {
//                holder.fileSize.setText(tmpSize + "B");
//            }
            String type = getDate().get(position).get("FileType").toString();

            if (type.equals("dir")) {
                holder.fileSize.setText(
                        Util.timeStamp2Date(getDate().get(position).get("ModifyData").toString(), "yyyy-MM-dd HH:mm:ss"));

            } else {

                long tmpSize = (long) getDate().get(position).get("FileSize");
                String tmpSizeStr = null;
                double calcSize;
                DecimalFormat df = new DecimalFormat("#.00");

                if (tmpSize <= 1024) {
                    tmpSizeStr = tmpSize + "B";
                } else if (tmpSize > 1024 && tmpSize <= 1024*1024) {
                    calcSize = tmpSize/1024.00;
                    tmpSizeStr = df.format(calcSize) + "KB";
                } else if (tmpSize > 1024*1024 && tmpSize <= 1024*1024*1024) {
                    calcSize = tmpSize/(1024.00 *1024.00);
                    tmpSizeStr = df.format(calcSize) + "MB";
                } else if (tmpSize > 1024*1024*1024 && tmpSize <= 1024*1024*1024*1024) {
                    calcSize = tmpSize/(1024.00 *1024.00*1024.00);
                    tmpSizeStr = df.format(calcSize) + "GB";
                } else {
                    holder.fileSize.setText(tmpSize + "B");
                }

                holder.fileSize.setText(tmpSizeStr + "  " +
                                Util.timeStamp2Date(getDate().get(position).get("ModifyData").toString(), "yyyy-MM-dd HH:mm:ss")
                );
            }


            //holder.fileSize.setText(getDate().get(position).get("FileSize").toString());

            if (type.equals("dir")) {
                if (holder.img != null) {
                    holder.fileSize.setText(Util.timeStamp2Date(getDate().get(position).get("ModifyData").toString(), "yyyy-MM-dd HH:mm:ss"));
                    holder.img.setImageResource(R.drawable.folder);
                }
            } else {
                String postfix = Util.getFilePostfix(getDate().get(position).get("FileName").toString());
                if (postfix.equalsIgnoreCase("mp4") || postfix.equalsIgnoreCase("avi") || postfix.equalsIgnoreCase("ts")
                        || postfix.equalsIgnoreCase("mpg") || postfix.equalsIgnoreCase("mpeg") || postfix.equalsIgnoreCase("rmvb")) {
                    holder.img.setImageResource(R.drawable.video);
                } else if (postfix.equalsIgnoreCase("pdf")) {
                    holder.img.setImageResource(R.drawable.pdf);
                } else if (postfix.equalsIgnoreCase("txt")) {
                    holder.img.setImageResource(R.drawable.txt);
                } else if (postfix.equalsIgnoreCase("zip") || postfix.equalsIgnoreCase("gz")) {
                    holder.img.setImageResource(R.drawable.yasuo);
                }  else if (postfix.equalsIgnoreCase("png") || postfix.equalsIgnoreCase("jpg")) {
                    holder.img.setImageResource(R.drawable.picture);
                }
                    else {
                    holder.img.setImageResource(R.drawable.file);
                }
            }

            return convertView;
        }

    }
    /*存放控件*/
    static class ViewHolder{
        public ImageView img;
        public TextView fileName;
        public TextView fileSize;
    }


}
